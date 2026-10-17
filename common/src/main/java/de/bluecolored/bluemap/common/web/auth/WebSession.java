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

import lombok.Getter;
import lombok.Setter;

/**
 * A logged-in web session.
 * <p>Distinguishes two timestamps on purpose:</p>
 * <ul>
 *   <li>{@code expiresAt} - absolute login lifetime, slid forward on every authenticated request</li>
 *   <li>{@code lastActivityAt} - only moved by real user activity (moving the view), which is what
 *       the idle-timeout is based on. Automatic requests (tiles, live-updates) are not activity.</li>
 * </ul>
 */
@Getter
public class WebSession {

    private final String accountName;
    private final boolean remember;
    private final long createdAt;
    private final long idleTimeoutMillis;

    @Setter
    private volatile long expiresAt;

    private volatile long lastActivityAt;

    public WebSession(String accountName, boolean remember, long createdAt, long expiresAt, long idleTimeoutMillis) {
        this.accountName = accountName;
        this.remember = remember;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.lastActivityAt = createdAt;
    }

    /**
     * Marks user activity (moving the view). This is what resumes the map updates after an idle period.
     */
    public void touch() {
        this.lastActivityAt = System.currentTimeMillis();
    }

    public boolean isExpired(long now) {
        return now >= expiresAt;
    }

    /**
     * Whether the server should stop pushing map data to this session because the user did not do
     * anything for the configured amount of time.
     */
    public boolean isIdle() {
        if (idleTimeoutMillis <= 0) return false;
        return System.currentTimeMillis() - lastActivityAt > idleTimeoutMillis;
    }

}
