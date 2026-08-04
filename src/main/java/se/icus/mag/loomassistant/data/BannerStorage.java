/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.types.BannerPack;
import se.icus.mag.loomassistant.types.BannerPackRepository;
import se.icus.mag.loomassistant.types.BannerRecipe;
import se.icus.mag.loomassistant.types.BannerRecipeCategories;
import se.icus.mag.loomassistant.types.BannerRecipeCategory;

/**
 * Compatibility wrapper around the new types-based banner pack backend.
 */
public class BannerStorage {
    private static final Gson GSON = new GsonBuilder().create();

    private static BannerStorage instance;

    private final Path packRootPath;
    // Config dir used for the global categories.json
    private final Path configDir;
    private BannerPackRepository repository;
    private final List<SavedBanner> banners = new ArrayList<>();

    public BannerStorage() {
        this.packRootPath = FabricLoader.getInstance().getGameDir().resolve("bannerpacks");
        this.configDir = FabricLoader.getInstance().getConfigDir().resolve(LoomAssistantMod.MOD_ID);
    }

    public static BannerStorage getInstance() {
        if (instance == null) {
            instance = new BannerStorage();
        }
        return instance;
    }

    public void load() {
        repository = new BannerPackRepository(packRootPath);
        repository.load();
        refreshBannerCache();
        rebuildCategoryRegistry();
        LoomAssistantMod.LOGGER.info(
                "Loaded {} banners from {} packs",
                banners.size(),
                repository.getPacks().size());
    }

    private void rebuildCategoryRegistry() {
        // Priority: global categories.json > pack definitions; fallbacks added for unknown ids.
        Map<String, BannerRecipeCategory> merged = new LinkedHashMap<>();

        // 1. Global categories.json – write default if missing, then read it
        Path globalFile = configDir.resolve("categories.json");
        ensureDefaultCategoriesFile(globalFile);
        if (Files.exists(globalFile)) {
            try (Reader r = Files.newBufferedReader(globalFile)) {
                JsonArray arr = GSON.fromJson(r, JsonArray.class);
                if (arr != null) {
                    for (JsonElement el : arr) {
                        BannerRecipeCategory c = parseCategoryJson(el.getAsJsonObject());
                        if (c != null) merged.put(c.id(), c);
                    }
                }
            } catch (Exception e) {
                LoomAssistantMod.LOGGER.warn("Failed to read global categories.json", e);
            }
        }

        // 2. Pack-level category definitions (may override global)
        for (BannerPack pack : repository.getPacks().values()) {
            for (BannerRecipeCategory c : pack.getCategories()) {
                merged.merge(c.id(), c, BannerRecipeCategory::mergedWith);
            }
        }

        // 3. Implicit fallbacks for every category id mentioned in any recipe
        for (BannerRecipe recipe : repository.getAllBannerRecipes()) {
            String catId = recipe.category();
            if (catId != null && !catId.isBlank() && !merged.containsKey(catId)) {
                merged.put(catId, BannerRecipeCategory.fallback(catId));
            }
        }

        BannerRecipeCategories.setCategories(merged.values());
    }

    private void ensureDefaultCategoriesFile(Path file) {
        if (Files.exists(file)) return;
        JsonArray arr = new JsonArray();
        for (String[] cat : new String[][] {
            {"flags", "Flags", "minecraft:map"},
            {"letters", "Letters", "minecraft:book"},
            {"logos", "Logos", "minecraft:blaze_powder"},
            {"misc", "Misc", "minecraft:lava_bucket"},
            {"nature", "Nature", "minecraft:poppy"},
        }) {
            JsonObject o = new JsonObject();
            o.addProperty("id", cat[0]);
            o.addProperty("description", cat[1]);
            o.addProperty("icon", cat[2]);
            arr.add(o);
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(arr, w);
            }
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.warn("Failed to write default categories.json", e);
        }
    }

    private static BannerRecipeCategory parseCategoryJson(JsonObject obj) {
        if (!obj.has("id")) return null;
        String id = obj.get("id").getAsString();
        if (id.isBlank()) return null;
        String description = obj.has("description") ? obj.get("description").getAsString() : id;
        String icon = obj.has("icon") ? obj.get("icon").getAsString() : "minecraft:lava_bucket";
        return new BannerRecipeCategory(id, description, icon);
    }

    public void save() {
        // Persistence is done on each write operation by BannerPackRepository.
        refreshBannerCache();
    }

    public void addBanner(SavedBanner banner) {
        ensureRepositoryLoaded();
        BannerPack rootPack = requirePack(BannerPackRepository.ROOT_PACK_ID);
        try {
            BannerRecipe created = rootPack.addBannerRecipe(banner.toType());
            LoomAssistantMod.LOGGER.debug("Added banner {} to root", created.id());
            refreshBannerCache();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to add banner to root pack", e);
        }
    }

    public void removeBanner(String id) {
        ensureRepositoryLoaded();
        String packId = repository.getBannerRecipePackId(id);
        if (packId == null) {
            return;
        }

        BannerPack pack = requirePack(packId);
        try {
            pack.removeBannerRecipe(id);
            refreshBannerCache();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to remove banner " + id, e);
        }
    }

    public List<SavedBanner> getBanners() {
        return Collections.unmodifiableList(banners);
    }

    public SavedBanner getBannerById(String id) {
        return banners.stream().filter(b -> b.getId().equals(id)).findFirst().orElse(null);
    }

    public void renameBanner(String id, String newName) {
        ensureRepositoryLoaded();
        String packId = repository.getBannerRecipePackId(id);
        if (packId == null) {
            return;
        }

        BannerRecipe existing = repository.getBannerRecipeById(id);
        if (existing == null) {
            return;
        }

        BannerPack pack = requirePack(packId);
        try {
            pack.updateBannerRecipe(existing.withDescription(newName));
            refreshBannerCache();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to rename banner " + id, e);
        }
    }

    public void updateBannerMetadata(String id, String newName, String newCategory) {
        ensureRepositoryLoaded();
        String packId = repository.getBannerRecipePackId(id);
        if (packId == null) {
            return;
        }

        BannerRecipe existing = repository.getBannerRecipeById(id);
        if (existing == null) {
            return;
        }

        BannerPack pack = requirePack(packId);
        try {
            BannerRecipe updated = existing.withDescription(newName).withCategory(newCategory);
            pack.updateBannerRecipe(updated);
            refreshBannerCache();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to update banner metadata " + id, e);
        }
    }

    public String exportBannerToJson(String bannerId) {
        ensureRepositoryLoaded();
        BannerRecipe recipe = repository.getBannerRecipeById(bannerId);
        if (recipe == null) {
            return null;
        }
        return recipe.toJson();
    }

    public String exportBannerToGiveCommand(String bannerId) {
        ensureRepositoryLoaded();
        BannerRecipe recipe = repository.getBannerRecipeById(bannerId);
        if (recipe == null) {
            return null;
        }
        return recipe.toCommand();
    }

    public SavedBanner importBannerFromJson(String input) {
        ensureRepositoryLoaded();
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmed = input.trim();

        BannerRecipe fromGive = BannerRecipe.fromCommand(trimmed);
        if (fromGive != null) {
            BannerPack rootPack = requirePack(BannerPackRepository.ROOT_PACK_ID);
            try {
                BannerRecipe created = rootPack.addBannerRecipe(fromGive);
                refreshBannerCache();
                return SavedBanner.fromType(created);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to import banner from command", e);
            }
        }

        if (trimmed.startsWith("[")) {
            return importBannerArray(trimmed);
        }

        try {
            BannerRecipe fromTypesJson = BannerRecipe.fromJson(trimmed);
            if (fromTypesJson != null) {
                BannerPack rootPack = requirePack(BannerPackRepository.ROOT_PACK_ID);
                try {
                    BannerRecipe created = rootPack.addBannerRecipe(fromTypesJson);
                    refreshBannerCache();
                    return SavedBanner.fromType(created);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to import banner from JSON", e);
                }
            }
        } catch (JsonSyntaxException ignored) {
            // continue to compatibility import
        }

        return null;
    }

    private SavedBanner importBannerArray(String input) {
        try {
            JsonArray jsonArray = GSON.fromJson(input, JsonArray.class);
            SavedBanner last = null;
            for (JsonElement element : jsonArray) {
                String entry = GSON.toJson(element);
                SavedBanner imported = importBannerFromJson(entry);
                if (imported != null) {
                    last = imported;
                }
            }
            return last;
        } catch (Exception e) {
            LoomAssistantMod.LOGGER.error("Failed to import banner array", e);
            return null;
        }
    }

    private void refreshBannerCache() {
        banners.clear();
        if (repository == null) {
            return;
        }

        for (Map.Entry<String, BannerPack> entry : repository.getPacks().entrySet()) {
            for (BannerRecipe recipe : entry.getValue().getDesigns()) {
                banners.add(SavedBanner.fromType(recipe));
            }
        }
    }

    private void ensureRepositoryLoaded() {
        if (repository == null) {
            load();
        }
    }

    private BannerPack requirePack(String packId) {
        BannerPack pack = repository.getPack(packId);
        if (pack == null) {
            throw new IllegalStateException("Pack not found: " + packId);
        }
        return pack;
    }
}
