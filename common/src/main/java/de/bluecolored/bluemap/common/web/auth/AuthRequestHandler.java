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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.bluecolored.bluemap.common.web.http.HttpRequest;
import de.bluecolored.bluemap.common.web.http.HttpRequestHandler;
import de.bluecolored.bluemap.common.web.http.HttpResponse;
import de.bluecolored.bluemap.common.web.http.HttpStatusCode;
import de.bluecolored.bluemap.core.logger.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The login/registration endpoints of the webapp.
 * <p>Mounted at {@code api/auth/(.*)}, so the request path inside this handler is the endpoint name.</p>
 */
public class AuthRequestHandler implements HttpRequestHandler {

    private static final int MAX_BODY_BYTES = 4096;

    private static final long FAILURE_WINDOW_MILLIS = 5 * 60 * 1000L;
    private static final int MAX_FAILURES = 5;

    private static final Gson GSON = new Gson();

    private final WebAuthService auth;
    private final WebSessionManager sessions;
    private final boolean chatAvailable;
    private final Map<String, FailureWindow> failures = new ConcurrentHashMap<>();

    public AuthRequestHandler(WebAuthService auth, WebSessionManager sessions, boolean chatAvailable) {
        this.auth = auth;
        this.sessions = sessions;
        this.chatAvailable = chatAvailable;
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        String path = request.getPath() == null ? "" : request.getPath();
        String method = request.getMethod() == null ? "" : request.getMethod().toUpperCase(Locale.ROOT);

        try {
            switch (path) {
                case "session":
                    return session(request);
                case "login":
                    return isPost(method) ? login(request) : methodNotAllowed();
                case "register":
                    return isPost(method) ? register(request) : methodNotAllowed();
                case "logout":
                    return isPost(method) ? logout(request) : methodNotAllowed();
                case "touch":
                    return isPost(method) ? touch(request) : methodNotAllowed();
                default:
                    return error(HttpStatusCode.NOT_FOUND, "not-found");
            }
        } catch (IOException ex) {
            Logger.global.logError("Failed to access the web-account storage!", ex);
            return error(HttpStatusCode.INTERNAL_SERVER_ERROR, "storage-error");
        } catch (RuntimeException ex) {
            Logger.global.logError("Failed to handle a web-auth request!", ex);
            return error(HttpStatusCode.INTERNAL_SERVER_ERROR, "internal-error");
        }
    }

    // ---------------------------------------------------------------- endpoints

    private HttpResponse session(HttpRequest request) {
        WebSession session = currentSession(request);

        JsonObject body = new JsonObject();
        body.addProperty("authenticated", session != null);
        body.addProperty("hasAccounts", auth.hasAccounts());
        body.addProperty("minPasswordLength", auth.getMinPasswordLength());
        body.addProperty("chatEnabled", chatAvailable);

        if (session != null) {
            body.addProperty("player", session.getAccountName());
            body.addProperty("idle", session.isIdle());
            body.addProperty("expiresAt", session.getExpiresAt());
        }

        return json(HttpStatusCode.OK, body, null);
    }

    private HttpResponse login(HttpRequest request) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) return error(HttpStatusCode.BAD_REQUEST, "bad-format");

        String username = optString(body, "username");
        String password = optString(body, "password");
        if (username == null || password == null) return error(HttpStatusCode.BAD_REQUEST, "bad-format");
        boolean remember = optBoolean(body, "rememberMe");

        String failureKey = clientAddress(request) + "|" + WebAuthService.normalizeName(username);
        long now = System.currentTimeMillis();
        if (isLocked(failureKey, now)) return error(HttpStatusCode.TOO_MANY_REQUESTS, "too-many-attempts");

        if (!auth.verifyPassword(username, password)) {
            registerFailure(failureKey, now);
            // deliberately does not distinguish between "unknown account" and "wrong password"
            return error(HttpStatusCode.UNAUTHORIZED, "bad-credentials");
        }

        failures.remove(failureKey);
        auth.recordLogin(username);

        String accountName = WebAuthService.normalizeName(username);
        String token = sessions.createSession(accountName, remember);

        JsonObject result = new JsonObject();
        result.addProperty("authenticated", true);
        result.addProperty("player", accountName);
        return json(HttpStatusCode.OK, result, sessionCookie(token, sessions.getLifetimeMillis(remember)));
    }

    private HttpResponse register(HttpRequest request) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) return error(HttpStatusCode.BAD_REQUEST, "bad-format");

        String username = optString(body, "username");
        String passcode = optString(body, "passcode");
        String password = optString(body, "password");
        if (username == null || passcode == null || password == null) {
            return error(HttpStatusCode.BAD_REQUEST, "bad-format");
        }

        String failureKey = clientAddress(request) + "|register|" + WebAuthService.normalizeName(username);
        long now = System.currentTimeMillis();
        if (isLocked(failureKey, now)) return error(HttpStatusCode.TOO_MANY_REQUESTS, "too-many-attempts");

        WebAuthService.RegistrationResult result = auth.completeRegistration(username, passcode, password);
        if (result != WebAuthService.RegistrationResult.OK) {
            if (result == WebAuthService.RegistrationResult.INVALID_PASSCODE) registerFailure(failureKey, now);

            HttpStatusCode status = switch (result) {
                case ACCOUNT_EXISTS -> HttpStatusCode.CONFLICT;
                case PASSCODE_EXPIRED, PASSWORD_TOO_SHORT -> HttpStatusCode.BAD_REQUEST;
                default -> HttpStatusCode.UNAUTHORIZED;
            };

            return error(status, switch (result) {
                case ACCOUNT_EXISTS -> "account-exists";
                case NO_PENDING_REGISTRATION -> "no-pending-registration";
                case PASSCODE_EXPIRED -> "passcode-expired";
                case INVALID_PASSCODE -> "invalid-passcode";
                case PASSWORD_TOO_SHORT -> "password-too-short";
                default -> "register-failed";
            });
        }

        failures.remove(failureKey);

        // registration logs the user in right away
        String accountName = WebAuthService.normalizeName(username);
        String token = sessions.createSession(accountName, false);

        JsonObject response = new JsonObject();
        response.addProperty("authenticated", true);
        response.addProperty("player", accountName);
        return json(HttpStatusCode.OK, response, sessionCookie(token, sessions.getLifetimeMillis(false)));
    }

    private HttpResponse logout(HttpRequest request) throws IOException {
        sessions.destroy(currentToken(request));

        JsonObject body = new JsonObject();
        body.addProperty("authenticated", false);
        return json(HttpStatusCode.OK, body, clearSessionCookie());
    }

    private HttpResponse touch(HttpRequest request) {
        WebSession session = currentSession(request);
        if (session == null) return error(HttpStatusCode.UNAUTHORIZED, "login-required");

        // user activity: this is what resumes the map updates after an idle period
        session.touch();

        JsonObject body = new JsonObject();
        body.addProperty("idle", false);
        return json(HttpStatusCode.OK, body, null);
    }

    // ---------------------------------------------------------------- helpers

    private @Nullable WebSession currentSession(HttpRequest request) {
        return sessions.resolve(currentToken(request));
    }

    private @Nullable String currentToken(HttpRequest request) {
        return WebSessionManager.parseCookie(cookieHeader(request), WebSessionManager.COOKIE_NAME);
    }

    private static @Nullable String cookieHeader(HttpRequest request) {
        var header = request.getHeader("cookie");
        if (header == null) return null;
        List<String> values = header.getValues();
        if (values.isEmpty()) return null;
        return String.join("; ", values);
    }

    private static String clientAddress(HttpRequest request) {
        try {
            return request.getSource().getHostAddress();
        } catch (RuntimeException ex) {
            return "unknown";
        }
    }

    private @Nullable JsonObject readJson(HttpRequest request) {
        byte[] body = request.getBody();
        if (body == null || body.length == 0 || body.length > MAX_BODY_BYTES) return null;
        try {
            JsonElement element = JsonParser.parseString(new String(body, StandardCharsets.UTF_8));
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static @Nullable String optString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return null;
        String value = element.getAsString();
        return value.isEmpty() ? null : value;
    }

    private static boolean optBoolean(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return false;
        try {
            return element.getAsBoolean();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean isPost(String method) {
        return "POST".equals(method);
    }

    private static HttpResponse methodNotAllowed() {
        return error(HttpStatusCode.METHOD_NOT_ALLOWED, "method-not-allowed");
    }

    private static HttpResponse error(HttpStatusCode status, String code) {
        JsonObject body = new JsonObject();
        body.addProperty("error", code);
        return json(status, body, null);
    }

    private static HttpResponse json(HttpStatusCode status, JsonObject body, @Nullable String setCookie) {
        HttpResponse response = new HttpResponse(status);
        response.setHeader("Content-Type", "application/json; charset=utf-8");
        response.setHeader("Cache-Control", "no-store");
        if (setCookie != null) response.setHeader("Set-Cookie", setCookie);
        response.setBody(GSON.toJson(body));
        return response;
    }

    private static String sessionCookie(String token, long lifetimeMillis) {
        long maxAgeSeconds = Math.max(1, lifetimeMillis / 1000);
        return WebSessionManager.COOKIE_NAME + "=" + token +
                "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + maxAgeSeconds;
    }

    private static String clearSessionCookie() {
        return WebSessionManager.COOKIE_NAME + "=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0";
    }

    // ---------------------------------------------------------------- brute-force protection

    private boolean isLocked(String key, long now) {
        FailureWindow window = failures.get(key);
        if (window == null) return false;
        if (now - window.start > FAILURE_WINDOW_MILLIS) {
            failures.remove(key);
            return false;
        }
        return window.count >= MAX_FAILURES;
    }

    private void registerFailure(String key, long now) {
        failures.compute(key, (k, window) -> {
            if (window == null || now - window.start > FAILURE_WINDOW_MILLIS) {
                FailureWindow fresh = new FailureWindow();
                fresh.start = now;
                fresh.count = 1;
                return fresh;
            }
            window.count++;
            return window;
        });
    }

    private static class FailureWindow {
        private long start;
        private int count;
    }

}
