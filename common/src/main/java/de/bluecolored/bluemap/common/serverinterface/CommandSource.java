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
package de.bluecolored.bluemap.common.serverinterface;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.core.world.World;
import net.kyori.adventure.text.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public interface CommandSource {

    void sendMessage(Component text);

    boolean hasPermission(String permission);

    default Optional<ServerWorld> getWorld() {
        return Optional.empty();
    }

    default Optional<Vector3d> getPosition() {
        return Optional.empty();
    }

    /**
     * The uuid of the player that is executing this command, if the command was executed by a player
     * (and not by the console or a command block).
     */
    default Optional<UUID> getPlayerUuid() {
        return Optional.empty();
    }

    /**
     * The name of the player that is executing this command, if the command was executed by a player.
     */
    default Optional<String> getPlayerName() {
        return Optional.empty();
    }

    /**
     * The client language of the player that is executing this command, if known.
     * <p>Used to answer in the language the player actually reads.</p>
     */
    default Optional<Locale> getLocale() {
        return Optional.empty();
    }

}
