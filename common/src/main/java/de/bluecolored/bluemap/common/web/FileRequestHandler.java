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
package de.bluecolored.bluemap.common.web;

import de.bluecolored.bluemap.common.web.http.*;
import de.bluecolored.bluemap.core.logger.Logger;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

@Getter @Setter
public class FileRequestHandler implements HttpRequestHandler {

    /**
     * Files written by the web-app build carry a content-hash in their name (e.g. {@code index-pw5lMe0d.js}),
     * so their content never changes for a given name.
     */
    private static final Pattern HASHED_FILE_NAME = Pattern.compile("-[A-Za-z0-9_-]{8}\\.[A-Za-z0-9]+$");

    private @NonNull Path webRoot;

    public FileRequestHandler(Path webRoot) {
        this.webRoot = webRoot.normalize();
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        if (!request.getMethod().equalsIgnoreCase("GET"))
            return new HttpResponse(HttpStatusCode.BAD_REQUEST);

        try {
            return generateResponse(request);
        } catch (IOException e) {
            Logger.global.logError("Failed to serve file", e);
            return new HttpResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
    }

    private HttpResponse generateResponse(HttpRequest request) throws IOException {
        String path = request.getPath();
        if (path == null) return new HttpResponse(HttpStatusCode.BAD_REQUEST);

        // normalize path
        if (path.startsWith("/")) path = path.substring(1);
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

        Path filePath;
        try {
            filePath = webRoot.resolve(path);
        } catch (InvalidPathException e){
            return new HttpResponse(HttpStatusCode.NOT_FOUND);
        }

        // check if file is in web-root
        if (!filePath.normalize().startsWith(webRoot)){
            return new HttpResponse(HttpStatusCode.FORBIDDEN);
        }

        // redirect to have correct relative paths
        if (Files.isDirectory(filePath) && !request.getPath().endsWith("/")) {
            HttpResponse response = new HttpResponse(HttpStatusCode.SEE_OTHER);
            response.setHeader("Location", "/" + path + "/" + (request.getRawQueryString().isEmpty() ? "" : "?" + request.getRawQueryString()));
            return response;
        }

        // default to index.html
        if (!Files.exists(filePath) || Files.isDirectory(filePath)){
            filePath = filePath.resolve("index.html");
        }

        if (!Files.exists(filePath) || Files.isDirectory(filePath)){
            return new HttpResponse(HttpStatusCode.NOT_FOUND);
        }

        // don't send php files
        if (filePath.getFileName().toString().endsWith(".php")) {
            return new HttpResponse(HttpStatusCode.FORBIDDEN);
        }

        // check if file is still in web-root and is not a directory
        if (!filePath.normalize().startsWith(webRoot) || Files.isDirectory(filePath)){
            return new HttpResponse(HttpStatusCode.FORBIDDEN);
        }

        // decide how this file may be cached, the web-app files keep their name across updates,
        // so without an explicit header the browser would keep serving stale versions
        String relativePath = webRoot.relativize(filePath.normalize()).toString().replace('\\', '/');
        String cacheControl = toCacheControl(relativePath);

        // check modified
        long lastModified = Files.getLastModifiedTime(filePath).to(TimeUnit.MILLISECONDS);
        HttpHeader modHeader = request.getHeader("If-Modified-Since");
        if (modHeader != null){
            try {
                long since = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(modHeader.getValue())).toEpochMilli();
                if (since + 1000 >= lastModified){
                    return notModified(cacheControl);
                }
            } catch (DateTimeParseException ignored){}
        }

        //check ETag
        String eTag =
                Long.toHexString(Files.size(filePath)) +
                Integer.toHexString(filePath.hashCode()) +
                Long.toHexString(lastModified);
        HttpHeader etagHeader = request.getHeader("If-None-Match");
        if (etagHeader != null){
            if(etagHeader.getValue().equals(eTag)) {
                return notModified(cacheControl);
            }
        }

        //create response
        HttpResponse response = new HttpResponse(HttpStatusCode.OK);
        if (cacheControl != null) response.setHeader("Cache-Control", cacheControl);
        response.setHeader("ETag", eTag);
        if (lastModified > 0) response.setHeader("Last-Modified", DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant
                .ofEpochMilli(lastModified)
                .atOffset(ZoneOffset.UTC)
        ));

        //add content type header
        String filetype = filePath.getFileName().toString();
        int pointIndex = filetype.lastIndexOf('.');
        if (pointIndex >= 0) filetype = filetype.substring(pointIndex + 1);
        String contentType = toContentType(filetype);
        response.setHeader("Content-Type", contentType);

        //send response
        try {
            response.setBody(Files.newInputStream(filePath));
            return response;
        } catch (FileNotFoundException e) {
            return new HttpResponse(HttpStatusCode.NOT_FOUND);
        }
    }

    private static HttpResponse notModified(@Nullable String cacheControl) {
        HttpResponse response = new HttpResponse(HttpStatusCode.NOT_MODIFIED);
        if (cacheControl != null) response.setHeader("Cache-Control", cacheControl);
        return response;
    }

    /**
     * Returns the {@code Cache-Control} value for the given web-root relative path, or {@code null} to keep the
     * default behavior of this handler.
     *
     * <p>The web-app bundle ships a content-hashed file name for everything under {@code assets/}, those can be
     * cached forever. Everything that keeps its name across web-app updates ({@code index.html}, the language files
     * and {@code settings.json}) must be revalidated on every visit, so that browsers pick up a newly deployed
     * web-app (and its translation keys) without the user having to clear the cache. Map- and live-data are left
     * untouched.
     */
    private static @Nullable String toCacheControl(String relativePath) {
        if (relativePath.startsWith("assets/")) {
            String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
            // files copied verbatim from the web-app's public folder keep their plain name, even when that name
            // accidentally looks like a content-hash (e.g. Minecraft-BF3_bo-S.ttf), so they must not be cached forever
            boolean contentHashed = HASHED_FILE_NAME.matcher(fileName).find() && !isCopiedVerbatim(fileName);
            return contentHashed ? "public, max-age=31536000, immutable" : "public, max-age=86400";
        }

        if (relativePath.equals("index.html")
                || relativePath.equals("settings.json")
                || relativePath.startsWith("lang/")) {
            return "no-cache";
        }

        return null;
    }

    /**
     * Whether the given file was copied into the web-app bundle untouched (fonts, sounds), instead of being
     * processed by the bundler which adds the content-hash to the file name.
     */
    private static boolean isCopiedVerbatim(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "ttf", "otf", "woff", "woff2", "mp3", "ogg", "wav", "flac" -> true;
            default -> false;
        };
    }

    private static String toContentType(String fileEnding) {
        return switch (fileEnding) {
            case "json" -> "application/json";
            case "png" -> "image/png";
            case "jpg",
                 "jpeg",
                 "jpe" -> "image/jpeg";
            case "svg" -> "image/svg+xml";
            case "css" -> "text/css";
            case "js" -> "text/javascript";
            case "html",
                 "htm",
                 "shtml" -> "text/html";
            case "xml" -> "text/xml";
            default -> "text/plain";
        };
    }

}
