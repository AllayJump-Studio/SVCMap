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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Persistence for web-accounts and pending registrations ({@code webauth.json}).
 * <p>All access is synchronized because commands run on the server thread while the web
 * endpoints run on connection threads.</p>
 */
public class WebAuthStore {

    private static final int FORMAT_VERSION = 1;

    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Map<String, WebAccount> accounts = new LinkedHashMap<>();
    private final Map<String, PendingRegistration> pendingRegistrations = new LinkedHashMap<>();

    public WebAuthStore(Path file) {
        this.file = file;
    }

    public Path getFile() {
        return file;
    }

    /**
     * Loads the store from disk. A missing file is not an error (it just means there are no accounts yet),
     * but an unreadable/corrupt file is, so the caller can refuse to continue with an empty account list.
     */
    public synchronized void load() throws IOException {
        accounts.clear();
        pendingRegistrations.clear();

        if (!Files.exists(file)) return;

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Data data = gson.fromJson(reader, Data.class);
            if (data == null) return;
            if (data.accounts != null) accounts.putAll(data.accounts);
            if (data.pendingRegistrations != null) pendingRegistrations.putAll(data.pendingRegistrations);
        } catch (JsonParseException ex) {
            throw new IOException("Failed to parse " + file.getFileName() + " - refusing to start with an empty account list", ex);
        }
    }

    /**
     * Writes the store to disk atomically (temp file + move), so a crash cannot leave a half-written file.
     */
    public synchronized void save() throws IOException {
        Data data = new Data();
        data.version = FORMAT_VERSION;
        data.accounts = new TreeMap<>(accounts);
        data.pendingRegistrations = new TreeMap<>(pendingRegistrations);

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

    public synchronized @Nullable WebAccount getAccount(String name) {
        return accounts.get(name);
    }

    public synchronized void putAccount(String name, WebAccount account) {
        accounts.put(name, account);
    }

    public synchronized @Nullable WebAccount removeAccount(String name) {
        return accounts.remove(name);
    }

    public synchronized @Nullable PendingRegistration getPendingRegistration(String name) {
        return pendingRegistrations.get(name);
    }

    public synchronized void putPendingRegistration(String name, PendingRegistration registration) {
        pendingRegistrations.put(name, registration);
    }

    public synchronized @Nullable PendingRegistration removePendingRegistration(String name) {
        return pendingRegistrations.remove(name);
    }

    public synchronized Set<String> getAccountNames() {
        return Set.copyOf(accounts.keySet());
    }

    public synchronized boolean hasAccounts() {
        return !accounts.isEmpty();
    }

    @SuppressWarnings("FieldMayBeFinal")
    private static class Data {
        private int version = FORMAT_VERSION;
        private Map<String, WebAccount> accounts;
        private Map<String, PendingRegistration> pendingRegistrations;
    }

}
