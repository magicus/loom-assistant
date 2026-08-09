/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import se.icus.mag.loomassistant.LoomAssistantMod;

/**
 * Tracks banner packs that were downloaded from the online repository.
 * Stored in config/loom-assistant/downloaded-bannerpacks.json.
 */
public class InstalledPackRegistry {
    private static final String REGISTRY_FILE = "downloaded-bannerpacks.json";
    private static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path registryFile;
    private final Map<String, InstalledPackState> entries = new LinkedHashMap<>();

    public InstalledPackRegistry() {
        this.registryFile = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(LoomAssistantMod.MOD_ID)
                .resolve(REGISTRY_FILE);
        load();
    }

    private void load() {
        entries.clear();
        if (!Files.exists(registryFile)) return;
        try {
            String content = Files.readString(registryFile, StandardCharsets.UTF_8);
            RegistryData data = GSON.fromJson(content, RegistryData.class);
            if (data != null && data.installedPacks != null) {
                for (InstalledPackState state : data.installedPacks) {
                    if (state != null && state.packId() != null) {
                        entries.put(state.packId(), state);
                    }
                }
            }
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.error("Failed to load installed pack registry from {}", registryFile, e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(registryFile.getParent());
            RegistryData data = new RegistryData();
            data.installedPacks = new ArrayList<>(entries.values());
            String json = GSON.toJson(data);
            Path tmp = registryFile.getParent().resolve(REGISTRY_FILE + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, registryFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, registryFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.error("Failed to save installed pack registry to {}", registryFile, e);
        }
    }

    public boolean isManaged(String packId) {
        return entries.containsKey(packId);
    }

    public InstalledPackState getState(String packId) {
        return entries.get(packId);
    }

    public void put(InstalledPackState state) {
        entries.put(state.packId(), state);
        save();
    }

    public void remove(String packId) {
        if (entries.remove(packId) != null) {
            save();
        }
    }

    private static class RegistryData {
        List<InstalledPackState> installedPacks;
    }
}
