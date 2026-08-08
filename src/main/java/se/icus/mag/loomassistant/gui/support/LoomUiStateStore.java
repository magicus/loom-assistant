/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeJsonConverter;

public final class LoomUiStateStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("loom-assistant").resolve("ui-state.json");
    private static final String PANEL_OPEN_BY_WORLD_KEY = "panelOpenByWorld";
    private static final String ACTIVE_BANNER_BY_WORLD_KEY = "activeBannerByWorld";
    private static final String PERSISTENT_DYE_BY_WORLD_KEY = "persistentDyeByWorld";
    private static final String SELECTED_CATEGORY_BY_WORLD_KEY = "selectedCategoryByWorld";
    private static final String DYE_ENABLED_KEY = "enabled";
    private static final String DYE_REPLACEMENTS_KEY = "replacements";

    private static final Map<String, Boolean> panelOpenByWorld = new HashMap<>();
    private static final Map<String, String> activeBannerRecipeByWorld = new HashMap<>();
    private static final Map<String, PersistentDyeState> persistentDyeByWorld = new HashMap<>();
    private static final Map<String, String> selectedCategoryByWorld = new HashMap<>();
    private static boolean loaded = false;

    public record PersistentDyeState(boolean enabled, Map<DyeColor, DyeColor> replacements) {
        public PersistentDyeState {
            replacements = Map.copyOf(replacements);
        }
    }

    private LoomUiStateStore() {}

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

    public static synchronized ItemStack getPersistedActiveBannerStack(Minecraft minecraft) {
        loadIfNeeded();
        String worldKey = getWorldKey(minecraft);
        String recipeJson = activeBannerRecipeByWorld.get(worldKey);
        if (recipeJson == null || recipeJson.isBlank()) return ItemStack.EMPTY;

        try {
            BannerRecipe banner = BannerRecipe.fromJson(recipeJson);
            return BannerRecipe.toItem(Minecraft.getInstance(), banner);
        } catch (RuntimeException e) {
            LoomAssistantMod.LOGGER.warn("Failed to restore persisted active banner for {}", worldKey, e);
            return ItemStack.EMPTY;
        }
    }

    public static synchronized void setPersistedActiveBannerStack(Minecraft minecraft, ItemStack stack) {
        loadIfNeeded();
        String worldKey = getWorldKey(minecraft);

        if (stack == null || stack.isEmpty()) {
            activeBannerRecipeByWorld.remove(worldKey);
            save();
            return;
        }

        BannerRecipe recipe = BannerRecipe.fromItem(stack);
        if (recipe == null) {
            activeBannerRecipeByWorld.remove(worldKey);
            save();
            return;
        }

        BannerRecipeJsonConverter converter = new BannerRecipeJsonConverter();
        activeBannerRecipeByWorld.put(worldKey, converter.fromRecipe(recipe));
        save();
    }

    public static synchronized PersistentDyeState getPersistentDyeState(Minecraft minecraft) {
        loadIfNeeded();
        String worldKey = getWorldKey(minecraft);
        return persistentDyeByWorld.getOrDefault(worldKey, new PersistentDyeState(false, Map.of()));
    }

    public static synchronized void setPersistentDyeState(
            Minecraft minecraft, boolean enabled, Map<DyeColor, DyeColor> replacements) {
        loadIfNeeded();
        String worldKey = getWorldKey(minecraft);

        EnumMap<DyeColor, DyeColor> normalized = new EnumMap<>(DyeColor.class);
        if (replacements != null) {
            for (Map.Entry<DyeColor, DyeColor> entry : replacements.entrySet()) {
                DyeColor src = entry.getKey();
                DyeColor dst = entry.getValue();
                if (src != null && dst != null && src != dst) {
                    normalized.put(src, dst);
                }
            }
        }

        if (!enabled || normalized.isEmpty()) {
            persistentDyeByWorld.remove(worldKey);
        } else {
            persistentDyeByWorld.put(worldKey, new PersistentDyeState(true, normalized));
        }
        save();
    }

    public static synchronized String getSelectedCategoryId(Minecraft minecraft) {
        loadIfNeeded();
        String worldKey = getWorldKey(minecraft);
        return selectedCategoryByWorld.get(worldKey);
    }

    public static synchronized void setSelectedCategoryId(Minecraft minecraft, String categoryId) {
        loadIfNeeded();
        String worldKey = getWorldKey(minecraft);
        if (categoryId == null || categoryId.isBlank()) {
            selectedCategoryByWorld.remove(worldKey);
        } else {
            selectedCategoryByWorld.put(worldKey, categoryId);
        }
        save();
    }

    private static void loadIfNeeded() {
        if (loaded) return;

        loaded = true;

        if (!Files.exists(FILE_PATH)) return;

        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has(PANEL_OPEN_BY_WORLD_KEY)) return;

            JsonObject perWorld = root.getAsJsonObject(PANEL_OPEN_BY_WORLD_KEY);
            if (perWorld == null) {
                perWorld = new JsonObject();
            }

            for (Map.Entry<String, JsonElement> entry : perWorld.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    panelOpenByWorld.put(entry.getKey(), entry.getValue().getAsBoolean());
                }
            }

            if (root.has(ACTIVE_BANNER_BY_WORLD_KEY)) {
                JsonObject bannersPerWorld = root.getAsJsonObject(ACTIVE_BANNER_BY_WORLD_KEY);
                if (bannersPerWorld != null) {
                    for (Map.Entry<String, JsonElement> entry : bannersPerWorld.entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) {
                            activeBannerRecipeByWorld.put(
                                    entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }
            }

            if (root.has(PERSISTENT_DYE_BY_WORLD_KEY)) {
                JsonObject dyesPerWorld = root.getAsJsonObject(PERSISTENT_DYE_BY_WORLD_KEY);
                if (dyesPerWorld != null) {
                    for (Map.Entry<String, JsonElement> worldEntry : dyesPerWorld.entrySet()) {
                        if (!worldEntry.getValue().isJsonObject()) continue;

                        JsonObject stateObj = worldEntry.getValue().getAsJsonObject();
                        boolean enabled = stateObj.has(DYE_ENABLED_KEY)
                                && stateObj.get(DYE_ENABLED_KEY).getAsBoolean();
                        EnumMap<DyeColor, DyeColor> replacements = new EnumMap<>(DyeColor.class);
                        if (stateObj.has(DYE_REPLACEMENTS_KEY)
                                && stateObj.get(DYE_REPLACEMENTS_KEY).isJsonObject()) {
                            JsonObject replacementsObj = stateObj.getAsJsonObject(DYE_REPLACEMENTS_KEY);
                            for (Map.Entry<String, JsonElement> replacement : replacementsObj.entrySet()) {
                                DyeColor src = DyeColor.byName(replacement.getKey(), null);
                                DyeColor dst = replacement.getValue().isJsonPrimitive()
                                        ? DyeColor.byName(replacement.getValue().getAsString(), null)
                                        : null;
                                if (src != null && dst != null && src != dst) {
                                    replacements.put(src, dst);
                                }
                            }
                        }
                        if (enabled && !replacements.isEmpty()) {
                            persistentDyeByWorld.put(worldEntry.getKey(), new PersistentDyeState(true, replacements));
                        }
                    }
                }
            }

            if (root.has(SELECTED_CATEGORY_BY_WORLD_KEY)) {
                JsonObject categoriesPerWorld = root.getAsJsonObject(SELECTED_CATEGORY_BY_WORLD_KEY);
                if (categoriesPerWorld != null) {
                    for (Map.Entry<String, JsonElement> entry : categoriesPerWorld.entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) {
                            selectedCategoryByWorld.put(
                                    entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }
            }
        } catch (JsonSyntaxException e) {
            throw new RuntimeException(e);
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

            JsonObject activeBannersPerWorld = new JsonObject();
            for (Map.Entry<String, String> entry : activeBannerRecipeByWorld.entrySet()) {
                activeBannersPerWorld.addProperty(entry.getKey(), entry.getValue());
            }
            root.add(ACTIVE_BANNER_BY_WORLD_KEY, activeBannersPerWorld);

            JsonObject persistentDyesPerWorld = new JsonObject();
            for (Map.Entry<String, PersistentDyeState> entry : persistentDyeByWorld.entrySet()) {
                PersistentDyeState state = entry.getValue();
                JsonObject stateObj = new JsonObject();
                stateObj.addProperty(DYE_ENABLED_KEY, state.enabled());

                JsonObject replacementsObj = new JsonObject();
                for (Map.Entry<DyeColor, DyeColor> replacement :
                        state.replacements().entrySet()) {
                    replacementsObj.addProperty(
                            replacement.getKey().getName(),
                            replacement.getValue().getName());
                }
                stateObj.add(DYE_REPLACEMENTS_KEY, replacementsObj);
                persistentDyesPerWorld.add(entry.getKey(), stateObj);
            }
            root.add(PERSISTENT_DYE_BY_WORLD_KEY, persistentDyesPerWorld);

            JsonObject selectedCategoriesPerWorld = new JsonObject();
            for (Map.Entry<String, String> entry : selectedCategoryByWorld.entrySet()) {
                selectedCategoriesPerWorld.addProperty(entry.getKey(), entry.getValue());
            }
            root.add(SELECTED_CATEGORY_BY_WORLD_KEY, selectedCategoriesPerWorld);

            try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            LoomAssistantMod.LOGGER.warn("Failed to save loom UI state", e);
        }
    }

    private static String getWorldKey(Minecraft minecraft) {
        if (minecraft == null) return "unknown";

        IntegratedServer singleplayerServer = minecraft.getSingleplayerServer();
        if (singleplayerServer != null) {
            Path worldRoot = singleplayerServer
                    .getWorldPath(LevelResource.ROOT)
                    .toAbsolutePath()
                    .normalize();
            return "sp:" + worldRoot;
        }

        ServerData currentServer = minecraft.getCurrentServer();
        if (currentServer != null && currentServer.ip != null && !currentServer.ip.isBlank()) {
            return "mp:" + currentServer.ip.toLowerCase(Locale.ROOT);
        }

        return "unknown";
    }
}
