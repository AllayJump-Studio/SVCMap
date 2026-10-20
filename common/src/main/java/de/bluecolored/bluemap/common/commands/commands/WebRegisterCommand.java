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
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.web.auth.WebAuthService;
import de.bluecolored.bluemap.core.logger.Logger;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.text;

/**
 * Starts the web-registration process by issuing a one-time passcode that is then entered on the website.
 * <p>The answer is localized to the client language of the executing player.</p>
 */
@RequiredArgsConstructor
public class WebRegisterCommand {

    private final Plugin plugin;

    @Command("webregister")
    @Permission("bluemap.webregister")
    public Component registerSelf(CommandSource source) {
        Locale locale = locale(source);
        String playerName = source.getPlayerName().orElse(null);
        UUID playerUuid = source.getPlayerUuid().orElse(null);

        if (playerName == null || playerUuid == null) {
            return lines(
                    messages().negative(locale, "common.player-only"),
                    messages().info(locale, "common.player-only-hint", command("/bluemap webregister <player>"))
            );
        }

        return issue(locale, playerName, playerUuid).message();
    }

    @Command("webregister <player>")
    @Permission("bluemap.webregister.other")
    public Component registerOther(CommandSource source, @Argument("player") @Parser("online-player") String playerName) {
        Locale locale = locale(source);

        Player target = findOnlinePlayer(playerName);
        if (target == null) {
            return messages().negative(locale, "common.no-online-player", playerName);
        }

        IssuedPasscode issued = issue(locale, target.getName().toPlainString(), target.getUuid());
        if (issued.passcode() == null) return issued.message();

        // let the player know as well, so they do not have to receive the code from the admin
        boolean notified = plugin.getServerInterface().sendMessageToPlayer(
                target.getUuid(),
                lines(
                        messages().positive(locale, "register.target-title"),
                        messages().info(locale, "register.passcode", text(issued.passcode()).color(HIGHLIGHT_COLOR)),
                        messages().info(locale, "register.target-open", text(publicUrl()).color(HIGHLIGHT_COLOR)),
                        messages().warning(locale, "register.warning")
                )
        );

        if (notified) return issued.message();

        return lines(
                issued.message(),
                messages().warning(locale, "register.target-unreachable")
        );
    }

    private IssuedPasscode issue(Locale locale, String accountName, UUID playerUuid) {
        WebAuthService webAuth = plugin.getWebAuth();
        if (webAuth == null) {
            return new IssuedPasscode(null, lines(
                    messages().negative(locale, "common.disabled"),
                    messages().info(locale, "common.disabled-hint",
                            text("auth.enabled").color(HIGHLIGHT_COLOR),
                            text("webserver.conf").color(HIGHLIGHT_COLOR))
            ));
        }

        if (!WebAuthService.isValidName(accountName)) {
            return new IssuedPasscode(null, messages().negative(locale, "common.invalid-name", accountName));
        }

        String passcode;
        try {
            passcode = webAuth.issuePasscode(accountName, playerUuid, "command");
        } catch (IOException ex) {
            Logger.global.logError("Failed to save the web-account file!", ex);
            return new IssuedPasscode(null, messages().negative(locale, "common.save-failed"));
        }

        if (passcode == null) {
            return new IssuedPasscode(null, lines(
                    messages().negative(locale, "register.exists", accountName),
                    messages().info(locale, "register.exists-hint", command("/bluemap webunregister " + accountName))
            ));
        }

        return new IssuedPasscode(passcode, lines(
                messages().positive(locale, "register.issued-title", accountName),
                messages().info(locale, "register.passcode", text(passcode).color(HIGHLIGHT_COLOR)),
                messages().info(locale, "register.expires", String.valueOf(passcodeExpiryMinutes())),
                messages().info(locale, "register.open", text(publicUrl()).color(HIGHLIGHT_COLOR)),
                messages().warning(locale, "register.warning")
        ));
    }

    private ServerMessages messages() {
        return plugin.getServerMessages();
    }

    private static Locale locale(CommandSource source) {
        return source.getLocale().orElse(Locale.ENGLISH);
    }

    private @Nullable Player findOnlinePlayer(String name) {
        for (Player player : plugin.getServerInterface().getOnlinePlayers().values()) {
            if (player.getName().toPlainString().equalsIgnoreCase(name)) return player;
        }
        return null;
    }

    private int passcodeExpiryMinutes() {
        return plugin.getBlueMap().getConfig().getWebserverConfig().getAuth().getPasscodeExpiryMinutes();
    }

    private String publicUrl() {
        String configured = plugin.getBlueMap().getConfig().getWebserverConfig().getAuth().getPublicUrl();
        if (configured != null && !configured.isBlank()) return configured.trim();

        int port = plugin.getBlueMap().getConfig().getWebserverConfig().getPort();
        return "http://<server-address>:" + port + "/";
    }

    /**
     * @param passcode the passcode, or {@code null} if none was issued
     * @param message the message to show to whoever executed the command
     */
    private record IssuedPasscode(@Nullable String passcode, Component message) {
    }

}
