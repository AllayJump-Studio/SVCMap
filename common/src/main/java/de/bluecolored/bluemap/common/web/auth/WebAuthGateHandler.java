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

import de.bluecolored.bluemap.common.web.http.HttpRequest;
import de.bluecolored.bluemap.common.web.http.HttpRequestHandler;
import de.bluecolored.bluemap.common.web.http.HttpResponse;
import de.bluecolored.bluemap.common.web.http.HttpStatusCode;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A single gate in front of everything the webserver serves.
 * <p>Denying access here (instead of in every handler) means a newly added endpoint can not
 * accidentally leak map data before login.</p>
 * <p>Only the webapp shell (needed to render the login screen) and the auth endpoints stay public.</p>
 */
public class WebAuthGateHandler implements HttpRequestHandler {

    private static final String[] PUBLIC_PREFIXES = {
            "assets/",
            "lang/",
            "api/auth/"
    };

    private static final String[] PUBLIC_FILES = {
            "",
            "index.html",
            "favicon.ico"
    };

    private final HttpRequestHandler delegate;
    private final WebSessionManager sessions;
    private final boolean requireLogin;

    public WebAuthGateHandler(HttpRequestHandler delegate, WebSessionManager sessions, boolean requireLogin) {
        this.delegate = delegate;
        this.sessions = sessions;
        this.requireLogin = requireLogin;
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        String path = normalize(request.getPath());

        if (!requireLogin || isPublic(path)) return delegate.handle(request);

        WebSession session = sessions.resolve(WebSessionManager.parseCookie(cookieHeader(request), WebSessionManager.COOKIE_NAME));
        if (session == null) return deny("login-required");

        // idle: stop serving map data until the user moves the view again (POST api/auth/touch)
        if (session.isIdle()) return deny("idle");

        return delegate.handle(request);
    }

    private static String normalize(@Nullable String path) {
        if (path == null) return "";
        if (path.startsWith("/")) path = path.substring(1);
        return path;
    }

    private static boolean isPublic(String path) {
        for (String file : PUBLIC_FILES) {
            if (path.equals(file)) return true;
        }
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    private static @Nullable String cookieHeader(HttpRequest request) {
        var header = request.getHeader("cookie");
        if (header == null) return null;
        List<String> values = header.getValues();
        if (values.isEmpty()) return null;
        return String.join("; ", values);
    }

    private static HttpResponse deny(String code) {
        HttpResponse response = new HttpResponse(HttpStatusCode.UNAUTHORIZED);
        response.setHeader("Content-Type", "application/json; charset=utf-8");
        response.setHeader("Cache-Control", "no-store");
        response.setBody("{\"error\":\"" + code + "\"}");
        return response;
    }

}
