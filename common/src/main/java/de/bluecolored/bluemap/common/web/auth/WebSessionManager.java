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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Creates, resolves and persists web sessions.
 * <p>The raw session token is only ever put into the client cookie - on disk (and in memory) only
 * its sha-256 hash is kept, so a leaked {@code sessions.json} cannot be used to log in.</p>
 */
public class WebSessionManager {

    public static final String COOKIE_NAME = "bluemap_auth";

    private static final long DAY_MILLIS = 24 * 60 * 60 * 1000L;
    private static final long SAVE_INTERVAL_MILLIS = 60 * 60 * 1000L;
    private static final int FORMAT_VERSION = 1;
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final Path file;
    private final long sessionMillis;
    private final long rememberMillis;
    private final long idleTimeoutMillis;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Map<String, WebSession> sessions = new ConcurrentHashMap<>();
    private final AtomicLong lastSave = new AtomicLong(0);

    public WebSessionManager(Path file, int sessionDays, int rememberDays, int idleTimeoutMinutes) {
        this.file = file;
        this.sessionMillis = Math.max(1, sessionDays) * DAY_MILLIS;
        this.rememberMillis = Math.max(1, rememberDays) * DAY_MILLIS;
        this.idleTimeoutMillis = idleTimeoutMinutes > 0 ? idleTimeoutMinutes * 60_000L : 0;
    }

    public Path getFile() {
        return file;
    }

    public long getLifetimeMillis(boolean remember) {
        return remember ? rememberMillis : sessionMillis;
    }

    public long getIdleTimeoutMillis() {
        return idleTimeoutMillis;
    }

    // ---------------------------------------------------------------- lifecycle

    /**
     * Creates a new session for an account and returns the raw token to put into the cookie.
     */
    public String createSession(String accountName, boolean remember) throws IOException {
        long now = System.currentTimeMillis();
        String token = generateToken();

        WebSession session = new WebSession(accountName, remember, now, now + getLifetimeMillis(remember), idleTimeoutMillis);
        sessions.put(hashToken(token), session);

        // a login always gets persisted immediately
        save(now, true);
        return token;
    }

    /**
     * Resolves a raw cookie token to its session, sliding the expiry forward.
     *
     * @return the session, or {@code null} if the token is unknown or expired
     */
    public @Nullable WebSession resolve(@Nullable String rawToken) {
        if (rawToken == null || rawToken.isEmpty()) return null;

        String key = hashToken(rawToken);
        WebSession session = sessions.get(key);
        if (session == null) return null;

        long now = System.currentTimeMillis();
        if (session.isExpired(now)) {
            sessions.remove(key);
            return null;
        }

        session.setExpiresAt(now + getLifetimeMillis(session.isRemember()));
        try {
            save(now, false);
        } catch (IOException ignored) {
            // sliding the expiry is best-effort, a failed save only means the old expiry is used after a restart
        }

        return session;
    }

    /**
     * Convenience for callers that only have the raw {@code Cookie} header value.
     */
    public @Nullable WebSession resolveCookieHeader(@Nullable String cookieHeader) {
        return resolve(parseCookie(cookieHeader, COOKIE_NAME));
    }

    public void destroy(@Nullable String rawToken) throws IOException {        if (rawToken == null || rawToken.isEmpty()) return;
        if (sessions.remove(hashToken(rawToken)) != null) save(System.currentTimeMillis(), true);
    }

    /**
     * Removes every session of an account. Used when the account is deleted.
     */
    public void destroyAll(String accountName) throws IOException {
        boolean changed = false;
        Iterator<Map.Entry<String, WebSession>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, WebSession> entry = iterator.next();
            if (entry.getValue().getAccountName().equalsIgnoreCase(accountName)) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) save(System.currentTimeMillis(), true);
    }

    // ---------------------------------------------------------------- persistence

    public void load() throws IOException {
        sessions.clear();
        if (!Files.exists(file)) return;

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Data data = gson.fromJson(reader, Data.class);
            if (data == null || data.sessions == null) return;

            long now = System.currentTimeMillis();
            for (Map.Entry<String, SessionData> entry : data.sessions.entrySet()) {
                SessionData stored = entry.getValue();
                if (stored == null || stored.account == null) continue;
                if (stored.expiresAt <= now) continue; // expired while the server was down
                WebSession session = new WebSession(
                        stored.account,
                        stored.remember,
                        stored.createdAt,
                        stored.expiresAt,
                        idleTimeoutMillis
                );
                if (stored.lastActivityAt > 0) session.touch();
                sessions.put(entry.getKey(), session);
            }
        } catch (JsonParseException ex) {
            throw new IOException("Failed to parse " + file.getFileName(), ex);
        }
    }

    private void save(long now, boolean force) throws IOException {
        if (!force && now - lastSave.get() < SAVE_INTERVAL_MILLIS) return;
        lastSave.set(now);

        // drop expired sessions while we are writing anyway
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(now));

        Data data = new Data();
        data.version = FORMAT_VERSION;
        data.sessions = new LinkedHashMap<>();
        for (Map.Entry<String, WebSession> entry : sessions.entrySet()) {
            WebSession session = entry.getValue();
            SessionData stored = new SessionData();
            stored.account = session.getAccountName();
            stored.remember = session.isRemember();
            stored.createdAt = session.getCreatedAt();
            stored.expiresAt = session.getExpiresAt();
            stored.lastActivityAt = session.getLastActivityAt();
            data.sessions.put(entry.getKey(), stored);
        }

        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);

        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        }

        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Reads a single cookie value from a {@code Cookie} header value.
     */
    public static @Nullable String parseCookie(@Nullable String cookieHeader, String name) {
        if (cookieHeader == null || cookieHeader.isEmpty()) return null;

        for (String part : cookieHeader.split(";")) {
            int separator = part.indexOf('=');
            if (separator < 0) continue;
            String key = part.substring(0, separator).trim();
            if (!key.equals(name)) continue;
            String value = part.substring(separator + 1).trim();
            if (value.isEmpty()) return null;
            // strip optional surrounding quotes
            if (value.length() > 1 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
                value = value.substring(1, value.length() - 1);
            return value.isEmpty() ? null : value;
        }

        return null;
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return TOKEN_ENCODER.encodeToString(bytes);
    }

    private static String hashToken(String token) {
        return PasswordHash.sha256Hex(token);
    }

    @SuppressWarnings("FieldMayBeFinal")
    private static class Data {
        private int version = FORMAT_VERSION;
        private Map<String, SessionData> sessions;
    }

    @SuppressWarnings("FieldMayBeFinal")
    private static class SessionData {
        private String account;
        private boolean remember;
        private long createdAt;
        private long expiresAt;
        private long lastActivityAt;
    }

}
