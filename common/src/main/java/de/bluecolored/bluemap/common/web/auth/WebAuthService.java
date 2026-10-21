/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.common.web.auth;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Account logic for the webapp: issuing registration passcodes, completing registrations,
 * verifying passwords and removing accounts.
 * <p>HTTP and session handling live in a separate class, this one has no web dependency.</p>
 */
public class WebAuthService {

    /**
     * Allowed account names. Deliberately the minecraft-name rule: 1-16 of [A-Za-z0-9_].
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{1,16}$");

    /**
     * Passcode alphabet without easily confused characters (no 0/1/l/o/i).
     */
    private static final char[] PASSCODE_CHARS = "23456789abcdefghjkmnpqrstuvwxyz".toCharArray();
    private static final int PASSCODE_LENGTH = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final WebAuthStore store;
    private final int passcodeExpiryMinutes;
    private final int minPasswordLength;

    public WebAuthService(Path file, int passcodeExpiryMinutes, int minPasswordLength) {
        this.store = new WebAuthStore(file);
        this.passcodeExpiryMinutes = Math.max(1, passcodeExpiryMinutes);
        this.minPasswordLength = Math.max(1, minPasswordLength);
    }

    public void load() throws IOException {
        store.load();
    }

    public Path getFile() {
        return store.getFile();
    }

    public int getMinPasswordLength() {
        return minPasswordLength;
    }

    // ---------------------------------------------------------------- accounts

    public boolean isRegistered(String playerName) {
        return store.getAccount(normalizeName(playerName)) != null;
    }

    public boolean hasAccounts() {
        return store.hasAccounts();
    }

    public Set<String> getAccountNames() {
        return store.getAccountNames();
    }

    public @Nullable WebAccount getAccount(String playerName) {
        return store.getAccount(normalizeName(playerName));
    }

    /**
     * The name to show for an account: the original spelling if it is known, otherwise the
     * normalized account name.
     */
    public String getDisplayName(String playerName) {
        WebAccount account = store.getAccount(normalizeName(playerName));
        if (account != null) {
            String displayName = account.getPlayerName();
            if (displayName != null && !displayName.isBlank()) return displayName;
        }
        return normalizeName(playerName);
    }

    /**
     * Verifies a player-name/password pair. Uses a constant-time hash comparison and never
     * tells the caller *why* it failed.
     */
    public boolean verifyPassword(String playerName, @Nullable String password) {
        WebAccount account = store.getAccount(normalizeName(playerName));
        if (account == null) return false;
        return PasswordHash.verify(password, account.getPasswordHash());
    }

    /**
     * Records a successful login.
     */
    public void recordLogin(String playerName) throws IOException {
        WebAccount account = store.getAccount(normalizeName(playerName));
        if (account == null) return;
        account.setLastLogin(System.currentTimeMillis());
        store.save();
    }

    /**
     * Removes an account.
     *
     * @return {@code true} if an account was removed
     */
    public boolean unregister(String playerName) throws IOException {
        String name = normalizeName(playerName);
        if (store.removeAccount(name) == null) return false;
        store.removePendingRegistration(name);
        store.save();
        return true;
    }

    // ---------------------------------------------------------------- registration

    /**
     * Issues a one-time registration passcode for a player.
     *
     * @param playerName the minecraft player name (also the future account name)
     * @param playerUuid the uuid of that player, stored on the account
     * @param issuedBy who issued the passcode (player name or "console"), for auditing
     * @return the plain-text passcode, or {@code null} if the player already has an account
     */
    public @Nullable String issuePasscode(String playerName, UUID playerUuid, @Nullable String issuedBy) throws IOException {
        String name = normalizeName(playerName);
        if (store.getAccount(name) != null) return null;

        String passcode = generatePasscode();

        PendingRegistration registration = new PendingRegistration();
        registration.setPlayerUuid(playerUuid);
        // remember the original spelling for display, the account itself stays lower-cased
        registration.setPlayerName(playerName.trim());
        registration.setCodeHash(PasswordHash.sha256Hex(normalizePasscode(passcode)));
        registration.setExpiresAt(System.currentTimeMillis() + passcodeExpiryMinutes * 60_000L);
        registration.setIssuedBy(issuedBy);

        // replacing an existing entry is intentional: only one passcode can be valid at a time
        store.putPendingRegistration(name, registration);
        store.save();

        return passcode;
    }

    /**
     * Completes a registration with the passcode that was issued in-game.
     * <p>The pending registration is consumed on success and removed when it has expired.</p>
     */
    public synchronized RegistrationResult completeRegistration(String playerName, @Nullable String passcode, @Nullable String password) throws IOException {
        String name = normalizeName(playerName);

        if (store.getAccount(name) != null) return RegistrationResult.ACCOUNT_EXISTS;

        PendingRegistration registration = store.getPendingRegistration(name);
        if (registration == null) return RegistrationResult.NO_PENDING_REGISTRATION;

        if (registration.isExpired(System.currentTimeMillis())) {
            store.removePendingRegistration(name);
            store.save();
            return RegistrationResult.PASSCODE_EXPIRED;
        }

        if (passcode == null || !PasswordHash.equalsConstantTime(
                PasswordHash.sha256Hex(normalizePasscode(passcode)),
                registration.getCodeHash())) {
            return RegistrationResult.INVALID_PASSCODE;
        }

        if (password == null || password.length() < minPasswordLength) {
            return RegistrationResult.PASSWORD_TOO_SHORT;
        }

        WebAccount account = new WebAccount(
                registration.getPlayerUuid(),
                PasswordHash.hash(password),
                System.currentTimeMillis()
        );
        account.setPlayerName(registration.getPlayerName());

        store.putAccount(name, account);
        store.removePendingRegistration(name); // one-time passcode
        store.save();

        return RegistrationResult.OK;
    }

    public @Nullable PendingRegistration getPendingRegistration(String playerName) {
        return store.getPendingRegistration(normalizeName(playerName));
    }

    // ---------------------------------------------------------------- helpers

    public static String normalizeName(@Nullable String playerName) {
        return playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizePasscode(@Nullable String passcode) {
        return passcode == null ? "" : passcode.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValidName(@Nullable String playerName) {
        return playerName != null && NAME_PATTERN.matcher(playerName).matches();
    }

    private static String generatePasscode() {
        StringBuilder sb = new StringBuilder(PASSCODE_LENGTH + 1);
        for (int i = 0; i < PASSCODE_LENGTH; i++) {
            if (i == PASSCODE_LENGTH / 2) sb.append('-');
            sb.append(PASSCODE_CHARS[RANDOM.nextInt(PASSCODE_CHARS.length)]);
        }
        return sb.toString();
    }

    public enum RegistrationResult {
        OK,
        ACCOUNT_EXISTS,
        NO_PENDING_REGISTRATION,
        PASSCODE_EXPIRED,
        INVALID_PASSCODE,
        PASSWORD_TOO_SHORT
    }

}
