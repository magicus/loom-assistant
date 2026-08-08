/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.repo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import se.icus.mag.loomassistant.LoomAssistantMod;

/**
 * Fetches and parses the remote bannerpack repository index.
 */
public class BannerPackRepoClient {
    private static final int TIMEOUT_SECONDS = 15;
    private static final String USER_AGENT = "loom-assistant/" + LoomAssistantMod.MOD_ID;
    private static final Gson GSON = new GsonBuilder().create();

    private final String indexUrl;
    private final HttpClient httpClient;

    public BannerPackRepoClient(String indexUrl) {
        validateUrl(indexUrl);
        this.indexUrl = indexUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Fetches and parses the remote index. Throws IOException on network or parse errors. */
    public RemoteRepoIndex fetchIndex() throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(indexUrl))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Repository returned HTTP " + response.statusCode());
            }
            return parseIndex(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Index fetch interrupted", e);
        }
    }

    /** Downloads a zip to the given path. Caller must verify SHA afterwards. */
    public void downloadZip(String downloadUrl, Path targetPath) throws IOException {
        validateUrl(downloadUrl);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .timeout(Duration.ofSeconds(120))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IOException("Download returned HTTP " + response.statusCode() + " for " + downloadUrl);
            }
            Files.write(targetPath, response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
    }

    private RemoteRepoIndex parseIndex(String json) throws IOException {
        try {
            RemoteRepoIndex index = GSON.fromJson(json, RemoteRepoIndex.class);
            if (index == null) throw new IOException("Empty index response");
            if (index.packs() == null) throw new IOException("Index missing 'packs' field");
            return index;
        } catch (JsonSyntaxException e) {
            throw new IOException("Failed to parse repository index: " + e.getMessage(), e);
        }
    }

    private static void validateUrl(String url) {
        if (url == null || !url.startsWith("https://")) {
            throw new IllegalArgumentException("Repository URL must use HTTPS: " + url);
        }
    }
}
