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
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.web.auth.WebAuthService;
import de.bluecolored.bluemap.core.logger.Logger;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.text;

/**
 * Starts the web-registration process by issuing a one-time passcode that is then entered on the website.
 */
@RequiredArgsConstructor
public class WebRegisterCommand {

    private final Plugin plugin;

    @Command("webregister")
    @Permission("bluemap.webregister")
    public Component registerSelf(CommandSource source) {
        String playerName = source.getPlayerName().orElse(null);
        UUID playerUuid = source.getPlayerUuid().orElse(null);

        if (playerName == null || playerUuid == null) {
            return lines(
                    text("❌ This command needs to be executed by a player.").color(NEGATIVE_COLOR),
                    format("Use % to issue a passcode for an online player.",
                            command("/bluemap webregister <player>")).color(BASE_COLOR)
            );
        }

        return issue(source, playerName, playerUuid).message();
    }

    @Command("webregister <player>")
    @Permission("bluemap.webregister.other")
    public Component registerOther(CommandSource source, @Argument("player") @Parser("online-player") String playerName) {
        Player target = findOnlinePlayer(playerName);
        if (target == null) {
            return format("❌ There is no online player named %.", playerName).color(NEGATIVE_COLOR);
        }

        IssuedPasscode issued = issue(source, target.getName().toPlainString(), target.getUuid());
        if (issued.passcode() == null) return issued.message();

        // let the player know as well, so they do not have to receive the code from the admin
        boolean notified = plugin.getServerInterface().sendMessageToPlayer(
                target.getUuid(),
                lines(
                        format("✔ A web-account registration was started for you.").color(POSITIVE_COLOR),
                        format("Passcode: %", text(issued.passcode()).color(HIGHLIGHT_COLOR)).color(BASE_COLOR),
                        format("Open % and enter it there.",
                                text(publicUrl()).color(HIGHLIGHT_COLOR)).color(BASE_COLOR),
                        text("Do not reuse your Minecraft password for the website.").color(WARNING_COLOR)
                )
        );

        if (notified) return issued.message();

        return lines(
                issued.message(),
                text("ℹ The player could not be notified in-game, please pass the code on privately.")
                        .color(WARNING_COLOR)
        );
    }

    private IssuedPasscode issue(CommandSource source, String accountName, UUID playerUuid) {
        WebAuthService webAuth = plugin.getWebAuth();
        if (webAuth == null) {
            return new IssuedPasscode(null, lines(
                    text("❌ The web-login system is disabled.").color(NEGATIVE_COLOR),
                    format("Enable % in % and reload BlueMap.",
                            text("auth.enabled").color(HIGHLIGHT_COLOR),
                            text("webserver.conf").color(HIGHLIGHT_COLOR)).color(BASE_COLOR)
            ));
        }

        if (!WebAuthService.isValidName(accountName)) {
            return new IssuedPasscode(null, format("❌ % is not a valid account name (1-16 of a-z, 0-9, _).", accountName)
                    .color(NEGATIVE_COLOR));
        }

        String issuedBy = source.getPlayerName().orElse("console");

        String passcode;
        try {
            passcode = webAuth.issuePasscode(accountName, playerUuid, issuedBy);
        } catch (IOException ex) {
            Logger.global.logError("Failed to save the web-account file!", ex);
            return new IssuedPasscode(null, text("❌ Failed to save the web-account file! See the server log for details.")
                    .color(NEGATIVE_COLOR));
        }

        if (passcode == null) {
            return new IssuedPasscode(null, lines(
                    format("❌ % already has a web-account.", accountName).color(NEGATIVE_COLOR),
                    format("An administrator can delete it with % and start over.",
                            command("/bluemap webunregister " + accountName)).color(BASE_COLOR)
            ));
        }

        return new IssuedPasscode(passcode, lines(
                format("✔ Web-registration started for %.", accountName).color(POSITIVE_COLOR),
                format("Passcode: %", text(passcode).color(HIGHLIGHT_COLOR)).color(BASE_COLOR),
                format("Valid for % minutes, and only once.",
                        text(String.valueOf(passcodeExpiryMinutes())).color(HIGHLIGHT_COLOR)).color(BASE_COLOR),
                format("Open % and enter the passcode there.",
                        text(publicUrl()).color(HIGHLIGHT_COLOR)).color(BASE_COLOR),
                text("Do not reuse your Minecraft password for the website.").color(WARNING_COLOR)
        ));
    }

    /**
     * @param passcode the passcode, or {@code null} if none was issued
     * @param message the message to show to whoever executed the command
     */
    private record IssuedPasscode(@Nullable String passcode, Component message) {
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

}
