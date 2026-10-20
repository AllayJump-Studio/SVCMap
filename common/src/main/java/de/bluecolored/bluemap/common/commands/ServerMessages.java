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
package de.bluecolored.bluemap.common.commands;

import de.bluecolored.bluemap.core.logger.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Localized texts for the in-game commands.
 * <p>Texts are picked by the <b>player's own client language</b>, not by the server language.</p>
 * <p>The bundled languages can be overridden - and new ones added - in {@code <configFolder>/lang/<tag>.conf}.
 * Only the keys that are present in such a file are overridden, everything else falls back to the
 * bundled language and finally to english.</p>
 */
public class ServerMessages {

    private static final String RESOURCE_PATH = "/de/bluecolored/bluemap/lang/";
    private static final String[] BUNDLED_LANGUAGES = {"en", "zh-CN", "zh-TW", "zh-HK", "lzh"};
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String FOLDER_NAME = "lang";

    public static final Component ICON_POSITIVE = Component.text("✔ ").color(TextFormat.POSITIVE_COLOR);
    public static final Component ICON_NEGATIVE = Component.text("❌ ").color(TextFormat.NEGATIVE_COLOR);
    public static final Component ICON_WARNING = Component.text("⚠ ").color(TextFormat.WARNING_COLOR);

    private final Map<String, Map<String, String>> languages;

    private ServerMessages(Map<String, Map<String, String>> languages) {
        this.languages = languages;
    }

    /**
     * Only the texts that are bundled with the jar. Never throws.
     */
    public static ServerMessages bundledOnly() {
        return load(null);
    }

    /**
     * Loads the bundled texts and applies the overrides from {@code <configFolder>/lang/}.
     *
     * @param configFolder the plugin config folder, or {@code null} to skip the overrides
     */
    public static ServerMessages load(@Nullable Path configFolder) {
        Map<String, Map<String, String>> languages = new LinkedHashMap<>();

        for (String tag : BUNDLED_LANGUAGES) {
            try {
                Map<String, String> messages = loadResource(tag);
                // copy into a mutable map, the overrides below are merged into the same map
                if (!messages.isEmpty()) languages.put(tag, new LinkedHashMap<>(messages));
            } catch (IOException | RuntimeException ex) {
                Logger.global.logError("Failed to load the bundled command-texts for '" + tag + "'!", ex);
            }
        }

        if (configFolder != null) {
            Path folder = configFolder.resolve(FOLDER_NAME);
            if (Files.isDirectory(folder)) {
                try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.conf")) {
                    for (Path file : files) {
                        String tag = file.getFileName().toString();
                        tag = tag.substring(0, tag.length() - ".conf".length());
                        try {
                            Map<String, String> overrides = loadFile(file);
                            languages.computeIfAbsent(tag, key -> new LinkedHashMap<>()).putAll(overrides);
                        } catch (IOException | RuntimeException ex) {
                            Logger.global.logError("Failed to load the command-texts from " + file + "!", ex);
                        }
                    }
                } catch (IOException ex) {
                    Logger.global.logError("Failed to read the command-text folder " + folder + "!", ex);
                }
            }
        }

        return new ServerMessages(languages);
    }

    // ---------------------------------------------------------------- access

    /**
     * The raw text for a key, without any formatting.
     */
    public String get(Locale locale, String key) {
        String value = find(locale, key);
        return value != null ? value : key;
    }

    /**
     * Builds a component for a message, applying one color to the text and highlighting the
     * placeholders. {@code %s} in the text is replaced by the arguments, in order.
     * <p>Arguments that already are a {@link Component} are inserted as they are.</p>
     */
    public Component line(Locale locale, TextColor color, String key, Object... args) {
        String template = get(locale, key);
        String[] parts = template.split("%s", -1);

        Component result = Component.text(parts[0], color);
        for (int i = 0; i < args.length && i + 1 < parts.length; i++) {
            Object arg = args[i];
            if (arg instanceof Component component) {
                result = result.append(component);
            } else {
                result = result.append(Component.text(String.valueOf(arg), TextFormat.HIGHLIGHT_COLOR));
            }
            result = result.append(Component.text(parts[i + 1], color));
        }

        return result;
    }

    public Component positive(Locale locale, String key, Object... args) {
        return ICON_POSITIVE.append(line(locale, TextFormat.POSITIVE_COLOR, key, args));
    }

    public Component negative(Locale locale, String key, Object... args) {
        return ICON_NEGATIVE.append(line(locale, TextFormat.NEGATIVE_COLOR, key, args));
    }

    public Component info(Locale locale, String key, Object... args) {
        return line(locale, TextFormat.BASE_COLOR, key, args);
    }

    public Component warning(Locale locale, String key, Object... args) {
        return ICON_WARNING.append(line(locale, TextFormat.WARNING_COLOR, key, args));
    }

    private @Nullable String find(Locale locale, String key) {
        String tag = locale.toLanguageTag();
        String value = findByTag(tag, key);
        if (value != null) return value;

        value = findByTag(locale.getLanguage(), key);
        if (value != null) return value;

        return findByTag(DEFAULT_LANGUAGE, key);
    }

    private @Nullable String findByTag(String tag, String key) {
        Map<String, String> messages = languages.get(tag);
        if (messages == null) return null;
        return messages.get(key);
    }

    // ---------------------------------------------------------------- loading

    private static Map<String, String> loadResource(String tag) throws IOException {
        URL url = ServerMessages.class.getResource(RESOURCE_PATH + tag + ".conf");
        if (url == null) return Map.of();

        ConfigurationNode root = HoconConfigurationLoader.builder()
                .url(url)
                .build()
                .load();

        return flatten(root);
    }

    private static Map<String, String> loadFile(Path file) throws IOException {
        ConfigurationNode root = HoconConfigurationLoader.builder()
                .path(file)
                .build()
                .load();

        return flatten(root);
    }

    private static Map<String, String> flatten(ConfigurationNode node) {
        Map<String, String> messages = new LinkedHashMap<>();
        flatten(node, "", messages);
        return Collections.unmodifiableMap(messages);
    }

    private static void flatten(ConfigurationNode node, String prefix, Map<String, String> target) {
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            ConfigurationNode child = entry.getValue();

            if (child.isMap()) {
                flatten(child, key, target);
            } else {
                String value = child.getString();
                if (value != null) target.put(key, value);
            }
        }
    }

}
