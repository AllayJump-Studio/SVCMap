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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@ConfigSerializable
@Getter
public class PluginConfig {

    private boolean livePlayerMarkers = true;

    private List<String> hiddenGameModes = new ArrayList<>();
    private boolean hideVanished = true;
    private boolean hideInvisible = true;
    private boolean hideSneaking = false;
    private boolean hideDifferentWorld = false;
    private int hideBelowSkyLight = 0;
    private int hideBelowBlockLight = 0;

    private int writeMarkersInterval = 0;
    private int writePlayersInterval = 0;

    private boolean skinDownload = true;

    private int playerRenderLimit = -1;

    private ChatConfig chat = new ChatConfig();

    @ConfigSerializable
    @Getter
    public static class ChatConfig {

        private boolean enabled = true;

        /**
         * Messages longer than this are truncated before they are sent to the webapp.
         */
        private int messageLengthLimit = 256;

        /**
         * How many messages are kept in memory, so a (re)connecting webapp can load the recent chat.
         */
        private int historyLength = 100;

        /**
         * Whether a message from the webapp is sent into the game <i>as the player</i>, so it goes
         * through the normal chat event and is subject to chat filters, mutes and other plugins.
         * <p>If the player is offline this is not possible, and the message is broadcast directly
         * (prefixed with the account name) instead.</p>
         */
        private boolean useVanillaChatEvent = true;

        /**
         * The prefix that is put in front of a webapp message when it is sent as the player, so it
         * stays distinguishable from messages typed in-game. Empty disables the prefix.
         */
        private String ingamePrefix = "[WEB] ";

    }

}
