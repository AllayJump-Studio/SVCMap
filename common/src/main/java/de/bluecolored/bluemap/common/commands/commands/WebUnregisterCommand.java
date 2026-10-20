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
package de.bluecolored.bluemap.common.commands.commands;

import de.bluecolored.bluecommands.annotations.Argument;
import de.bluecolored.bluecommands.annotations.Command;
import de.bluecolored.bluecommands.annotations.Parser;
import de.bluecolored.bluemap.common.commands.Permission;
import de.bluecolored.bluemap.common.commands.ServerMessages;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.web.auth.WebAuthService;
import de.bluecolored.bluemap.core.logger.Logger;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.Locale;

import static de.bluecolored.bluemap.common.commands.TextFormat.lines;

/**
 * Deletes a web-account. This is also the "password reset" path: delete the account and let the
 * player register again with {@code /bluemap webregister}.
 * <p>The answer is localized to the client language of the executing player.</p>
 */
@RequiredArgsConstructor
public class WebUnregisterCommand {

    private final Plugin plugin;

    @Command("webunregister <player>")
    @Permission("bluemap.webunregister")
    public Component unregister(CommandSource source, @Argument("player") @Parser("web-account") String playerName) {
        Locale locale = locale(source);
        ServerMessages messages = plugin.getServerMessages();

        WebAuthService webAuth = plugin.getWebAuth();
        if (webAuth == null) {
            return lines(
                    messages.negative(locale, "common.disabled"),
                    messages.info(locale, "common.disabled-hint", "auth.enabled", "webserver.conf")
            );
        }

        try {
            if (!webAuth.unregister(playerName)) {
                return messages.negative(locale, "unregister.missing", playerName);
            }
        } catch (IOException ex) {
            Logger.global.logError("Failed to save the web-account file!", ex);
            return messages.negative(locale, "common.save-failed");
        }

        return lines(
                messages.positive(locale, "unregister.done", playerName),
                messages.info(locale, "unregister.done-hint", "/bluemap webregister")
        );
    }

    private static Locale locale(CommandSource source) {
        return source.getLocale().orElse(Locale.ENGLISH);
    }

}
