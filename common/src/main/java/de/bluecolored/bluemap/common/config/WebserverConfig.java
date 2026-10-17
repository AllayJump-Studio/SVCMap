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
package de.bluecolored.bluemap.common.config;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Map;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@ConfigSerializable
@Getter
public class WebserverConfig {

    private boolean enabled = true;
    private Path webroot = Path.of("bluemap", "web");

    private String ip = "0.0.0.0";
    private int port = 8100;

    private boolean sseEnabled = true;

    private LogConfig log = new LogConfig();

    private Map<String, String> additionalHeaders = Map.of();

    private AuthConfig auth = new AuthConfig();

    public InetAddress resolveIp() throws UnknownHostException {
        if (ip.isEmpty() || ip.equals("0.0.0.0") || ip.equals("::0")) {
            return new InetSocketAddress(0).getAddress();
        } else if (ip.equals("#getLocalHost")) {
            return InetAddress.getLocalHost();
        } else {
            return InetAddress.getByName(ip);
        }
    }

    @ConfigSerializable
    @Getter
    public static class LogConfig {

        private String file = null;
        private boolean append = false;
        private String format = "%1$s \"%3$s %4$s %5$s\" %6$s %7$s";

    }

    /**
     * Web-account / login configuration.
     * <p>Defaults to disabled so that an existing installation does not suddenly lock its map
     * after an update. Register your account in-game first, then enable this.</p>
     */
    @ConfigSerializable
    @Getter
    public static class AuthConfig {

        private boolean enabled = false;

        /**
         * If true, every request that is not part of the login flow is rejected until the user logs in.
         */
        private boolean requireLogin = true;

        private int sessionDays = 7;

        private int rememberDays = 30;

        /**
         * After this many minutes without any activity, the server stops pushing map data to the
         * browser. The client has to show activity (moving the view) to resume.
         * Set to 0 or lower to disable.
         */
        private int idleTimeoutMinutes = 60;

        /**
         * How long a passcode issued by {@code /bluemap webregister} stays valid.
         */
        private int passcodeExpiryMinutes = 10;

        private int minPasswordLength = 8;

        /**
         * Only used to print a usable url in the registration command. Leave empty to fall back to
         * {@code http://<server-ip>:<webserver-port>/}.
         */
        private String publicUrl = "";

    }

}
