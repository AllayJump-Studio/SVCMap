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
import de.bluecolored.bluemap.core.logger.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * A bounded, thread-safe ring-buffer of the most recent {@link ChatMessage}s.
 * <p>
 * It acts as the {@link Supplier} for the {@code live/chat.json} endpoint, so a client that
 * just (re)connected can load the recent chat history before it starts receiving
 * {@code chat} server-sent events.
 */
public class ChatMessageHistory implements Supplier<String> {

    private final int capacity;
    private final Deque<ChatMessage> messages;

    public ChatMessageHistory(int capacity) {
        this.capacity = capacity;
        this.messages = new ArrayDeque<>(Math.max(1, capacity));
    }

    /**
     * Adds a message to the history, dropping the oldest message if the capacity is reached.
     * <p>Does nothing if the capacity is <code>0</code> or lower.
     */
    public synchronized void add(ChatMessage message) {
        if (capacity <= 0) return;
        while (messages.size() >= capacity) messages.removeFirst();
        messages.addLast(message);
    }

    public synchronized void clear() {
        messages.clear();
    }

    public synchronized int size() {
        return messages.size();
    }

    @Override
    public synchronized String get() {
        try (
                StringWriter jsonString = new StringWriter();
                JsonWriter json = new JsonWriter(jsonString)
        ) {
            json.beginObject();
            json.name("messages").beginArray();
            for (ChatMessage message : messages) message.writeJson(json);
            json.endArray();
            json.endObject();

            json.flush();
            return jsonString.toString();
        } catch (IOException ex) {
            Logger.global.logError("Failed to write live/chat json!", ex);
            return "{\"messages\":[]}";
        }
    }

}
