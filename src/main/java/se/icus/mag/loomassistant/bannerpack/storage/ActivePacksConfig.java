/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import se.icus.mag.loomassistant.LoomAssistantMod;

/**
 * Manages which banner packs are currently active/enabled.
 *
 * <p>Configuration is stored as JSON in the config directory as
 * "bannerpacks.json". The "local" pack is included in the configuration on
 * first startup.
 */
public class ActivePacksConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configFile;
    private List<String> activePacks;

    public ActivePacksConfig(Path configFile) {
        this.configFile = configFile;
        this.activePacks = new ArrayList<>();
        load();
    }

    public void reloadFromDisk() {
        load();
    }

    private void load() {
        activePacks.clear();

        if (Files.exists(configFile)) {
            try {
                String content = Files.readString(configFile, StandardCharsets.UTF_8);
                ConfigData data = GSON.fromJson(content, ConfigData.class);
                if (data != null && data.activePacks != null) {
                    activePacks = new ArrayList<>(data.activePacks);
                }
            } catch (IOException e) {
                LoomAssistantMod.LOGGER.error("Failed to load active packs config from {}", configFile, e);
            }
        } else {
            // Initialize with local pack on first startup
            activePacks.add(BannerPackRepository.LOCAL_PACK_ID);
            save();
        }
    }

    private void save() {
        try {
            Files.createDirectories(configFile.getParent());
            ConfigData data = new ConfigData();
            data.activePacks = new ArrayList<>(activePacks);
            String json = GSON.toJson(data);
            Files.writeString(configFile, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.error("Failed to save active packs config to {}", configFile, e);
        }
    }

    /**
     * Gets the list of active pack IDs.
     */
    public List<String> getActivePacks() {
        return new ArrayList<>(activePacks);
    }

    /**
     * Sets which packs are active.
     */
    public void setActivePacks(List<String> packIds) {
        activePacks = new ArrayList<>(packIds);
        save();
    }

    /**
     * Adds a pack to the active list if not already present.
     */
    public void enablePack(String packId) {
        if (!activePacks.contains(packId)) {
            activePacks.add(packId);
            save();
        }
    }

    /**
     * Removes a pack from the active list (unless it's the local pack).
     */
    public void disablePack(String packId) {
        if (!BannerPackRepository.LOCAL_PACK_ID.equals(packId)) {
            if (activePacks.remove(packId)) {
                save();
            }
        }
    }

    /**
     * Checks if a pack is currently active.
     */
    public boolean isPackActive(String packId) {
        return activePacks.contains(packId);
    }

    /**
     * Moves a pack to a new position in the active list.
     */
    public void movePackToPosition(String packId, int newIndex) {
        if (activePacks.remove(packId)) {
            if (newIndex >= activePacks.size()) {
                activePacks.add(packId);
            } else {
                activePacks.add(newIndex, packId);
            }
            save();
        }
    }

    private static class ConfigData {
        protected List<String> activePacks;
    }
}
