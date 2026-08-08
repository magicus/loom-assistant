/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import se.icus.mag.loomassistant.LoomAssistantMod;

public final class LoomStatePersistence {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("loom-assistant").resolve("loom-state.json");

    public LoomPersistedState load() {
        if (!Files.exists(FILE_PATH)) {
            return new LoomPersistedState();
        }

        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            LoomPersistedState state = GSON.fromJson(reader, LoomPersistedState.class);
            return state != null ? state : new LoomPersistedState();
        } catch (JsonSyntaxException e) {
            LoomAssistantMod.LOGGER.warn("Invalid loom state file: {}", FILE_PATH, e);
            return new LoomPersistedState();
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.warn("Failed to read loom state file: {}", FILE_PATH, e);
            return new LoomPersistedState();
        }
    }

    public void save(LoomPersistedState state) {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
                GSON.toJson(state, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save loom state file: " + FILE_PATH, e);
        }
    }
}
