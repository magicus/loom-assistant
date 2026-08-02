/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;
import se.icus.mag.loomassistant.LoomAssistantMod;

public final class LoomUiStateStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("loom-assistant").resolve("ui-state.json");
    private static final String PANEL_OPEN_BY_WORLD_KEY = "panelOpenByWorld";

    private static final Map<String, Boolean> panelOpenByWorld = new HashMap<>();
    private static boolean loaded = false;

    private LoomUiStateStore() {
    }

    public static synchronized boolean isLoomPanelOpen(Minecraft minecraft) {
        loadIfNeeded();
        String worldKey = getWorldKey(minecraft);
        return panelOpenByWorld.getOrDefault(worldKey, false);
    }

    public static synchronized void setLoomPanelOpen(Minecraft minecraft, boolean open) {
        loadIfNeeded();
        String worldKey = getWorldKey(minecraft);
        panelOpenByWorld.put(worldKey, open);
        save();
    }

    private static void loadIfNeeded() {
        if (loaded) {
            return;
        }
        loaded = true;

        if (!Files.exists(FILE_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has(PANEL_OPEN_BY_WORLD_KEY)) {
                return;
            }

            JsonObject perWorld = root.getAsJsonObject(PANEL_OPEN_BY_WORLD_KEY);
            if (perWorld == null) {
                return;
            }

            for (Map.Entry<String, JsonElement> entry : perWorld.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    panelOpenByWorld.put(entry.getKey(), entry.getValue().getAsBoolean());
                }
            }
        } catch (Exception e) {
            LoomAssistantMod.LOGGER.warn("Failed to load loom UI state", e);
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            JsonObject root = new JsonObject();
            JsonObject perWorld = new JsonObject();
            for (Map.Entry<String, Boolean> entry : panelOpenByWorld.entrySet()) {
                perWorld.addProperty(entry.getKey(), entry.getValue());
            }
            root.add(PANEL_OPEN_BY_WORLD_KEY, perWorld);

            try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            LoomAssistantMod.LOGGER.warn("Failed to save loom UI state", e);
        }
    }

    private static String getWorldKey(Minecraft minecraft) {
        if (minecraft == null) {
            return "unknown";
        }

        IntegratedServer singleplayerServer = minecraft.getSingleplayerServer();
        if (singleplayerServer != null) {
            Path worldRoot = singleplayerServer.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            return "sp:" + worldRoot;
        }

        ServerData currentServer = minecraft.getCurrentServer();
        if (currentServer != null && currentServer.ip != null && !currentServer.ip.isBlank()) {
            return "mp:" + currentServer.ip.toLowerCase(Locale.ROOT);
        }

        return "unknown";
    }
}
