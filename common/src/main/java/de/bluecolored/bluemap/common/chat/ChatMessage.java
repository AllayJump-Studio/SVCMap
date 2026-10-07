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
package de.bluecolored.bluemap.common.chat;

import com.google.gson.stream.JsonWriter;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

/**
 * A single chat message as it is transferred to the webapp.
 * <p>
 * Instances are immutable and are sent either as part of the {@code live/chat.json} history,
 * or as a single {@code chat} server-sent event.
 * <p>
 * The json representation is:
 * <pre>
 * {
 *   "type": "chat",
 *   "source": "player",
 *   "playerUuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
 *   "playerName": "Notch",
 *   "message": "hello",
 *   "timestamp": 1730000000000
 * }
 * </pre>
 */
@Getter
public class ChatMessage {

    /**
     * The message was sent by a player on the minecraft-server.
     */
    public static final String SOURCE_PLAYER = "player";

    /**
     * The message was sent by a webapp-user.
     */
    public static final String SOURCE_WEB = "web";

    /**
     * The message was created by the server itself (e.g. a broadcast from another plugin).
     */
    public static final String SOURCE_SYSTEM = "system";

    private final String source;
    private final @Nullable UUID playerUuid;
    private final String playerName;
    private final String message;
    private final long timestamp;

    public ChatMessage(String source, @Nullable UUID playerUuid, String playerName, String message, long timestamp) {
        this.source = source;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.message = message;
        this.timestamp = timestamp;
    }

    /**
     * Creates a message that was sent by a player on the minecraft-server.
     */
    public static ChatMessage player(UUID playerUuid, String playerName, String message) {
        return new ChatMessage(SOURCE_PLAYER, playerUuid, playerName, message, System.currentTimeMillis());
    }

    public void writeJson(JsonWriter json) throws IOException {
        json.beginObject();
        json.name("type").value("chat");
        json.name("source").value(source);
        json.name("playerUuid").value(playerUuid == null ? null : playerUuid.toString());
        json.name("playerName").value(playerName);
        json.name("message").value(message);
        json.name("timestamp").value(timestamp);
        json.endObject();
    }

    public String toJson() {
        try (
                StringWriter jsonString = new StringWriter();
                JsonWriter json = new JsonWriter(jsonString)
        ) {
            writeJson(json);
            json.flush();
            return jsonString.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write chat-message json", ex);
        }
    }

}
