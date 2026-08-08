/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.bannerpack.BannerPack;
import se.icus.mag.loomassistant.bannerpack.BannerPackMetadata;
import se.icus.mag.loomassistant.bannerpack.DirectoryBannerPack;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeCategory;

public class BannerPackRepository {
    public static final String LOCAL_PACK_ID = "local";

    private final Path packsRoot;
    private final Map<String, BannerPack> packs = new LinkedHashMap<>();

    public BannerPackRepository(Path packsRoot) {
        this.packsRoot = packsRoot;
    }

    public void load() {
        packs.clear();

        try {
            Files.createDirectories(packsRoot);
            ensureRootPackExists();

            try (Stream<Path> stream = Files.list(packsRoot)) {
                stream.sorted().forEach(entry -> {
                    try {
                        if (Files.isDirectory(entry)) {
                            loadDirectoryPack(entry);
                        } else if (entry.getFileName().toString().endsWith(".zip")) {
                            loadZipPack(entry);
                        }
                    } catch (IOException e) {
                        LoomAssistantMod.LOGGER.error("Failed to load banner pack from {}", entry, e);
                    }
                });
            }

            // Load JAR-bundled packs
            try {
                JarBannerPackLoader jarLoader = new JarBannerPackLoader();
                for (BannerPack pack : jarLoader.loadBundledPacks()) {
                    String packId = pack.getMetadata().id();
                    if (packs.containsKey(packId)) {
                        LoomAssistantMod.LOGGER.warn(
                                "Bundled banner pack '{}' conflicts with a user pack, skipping", packId);
                    } else {
                        packs.put(packId, pack);
                        LoomAssistantMod.LOGGER.info("Loaded bundled banner pack: {}", packId);
                    }
                }
            } catch (Exception e) {
                LoomAssistantMod.LOGGER.debug("No bundled banner packs found or error loading them", e);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize banner pack repository", e);
        }
    }

    public Path getPacksRoot() {
        return packsRoot;
    }

    public Map<String, BannerPack> getPacks() {
        return packs;
    }

    public BannerPack getPack(String packId) {
        return packs.get(packId);
    }

    public List<BannerRecipe> getAllBannerRecipes() {
        List<BannerRecipe> result = new ArrayList<>();
        for (BannerPack pack : packs.values()) {
            result.addAll(pack.getDesigns());
        }
        return result;
    }

    public BannerRecipe getBannerRecipeById(String recipeId) {
        for (BannerPack pack : packs.values()) {
            BannerRecipe recipe = pack.getDesign(recipeId);
            if (recipe != null) {
                return recipe;
            }
        }
        return null;
    }

    public String getBannerRecipePackId(String recipeId) {
        for (BannerPack pack : packs.values()) {
            if (pack.getDesign(recipeId) != null) {
                return pack.getMetadata().id();
            }
        }
        return null;
    }

    public BannerPack createPack(String packId, String name) {
        String normalizedId = normalizeId(packId);
        if (LOCAL_PACK_ID.equals(normalizedId)) {
            throw new IllegalArgumentException("local pack already exists and cannot be created manually");
        }
        if (packs.containsKey(normalizedId)) {
            throw new IllegalArgumentException("pack already exists: " + normalizedId);
        }

        Path packDir = packsRoot.resolve(normalizedId);
        try {
            BannerPackMetadata metadata = new BannerPackMetadata(normalizedId, name);
            DirectoryBannerPack pack = BannerPack.createDirectoryPack(packDir, metadata);
            packs.put(normalizedId, pack);
            return pack;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create pack " + normalizedId, e);
        }
    }

    public void deletePack(String packId) {
        String normalizedId = normalizeId(packId);
        if (LOCAL_PACK_ID.equals(normalizedId)) {
            throw new IllegalArgumentException("local pack cannot be deleted");
        }

        BannerPack pack = requirePack(normalizedId);
        if (pack.isReadOnly()) {
            throw new IllegalArgumentException("zip packs are read-only and cannot be deleted through repository");
        }

        try {
            deleteRecursively(pack.getPath());
            packs.remove(normalizedId);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete pack " + normalizedId, e);
        }
    }

    public BannerRecipe moveBannerRecipe(String sourcePackId, String targetPackId, String recipeId) {
        String sourceId = normalizeId(sourcePackId);
        String targetId = normalizeId(targetPackId);
        BannerPack sourcePack = requirePack(sourceId);
        BannerPack targetPack = requirePack(targetId);
        if (!(sourcePack instanceof DirectoryBannerPack dirSource)) {
            throw new IllegalArgumentException("cannot move from read-only pack; copy instead");
        }
        try {
            return dirSource.moveRecipeTo(targetPack, recipeId);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to move recipe " + recipeId, e);
        }
    }

    public BannerRecipe copyBannerRecipe(String sourcePackId, String targetPackId, String recipeId) {
        String sourceId = normalizeId(sourcePackId);
        String targetId = normalizeId(targetPackId);
        BannerPack sourcePack = requirePack(sourceId);
        BannerPack targetPack = requirePack(targetId);
        try {
            return sourcePack.copyRecipeTo(targetPack, recipeId);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to copy recipe " + recipeId, e);
        }
    }

    private void ensureRootPackExists() {
        Path localPackDir = packsRoot.resolve(LOCAL_PACK_ID);
        Path localBannersDir = localPackDir.resolve(BannerPack.BANNERS_DIR);
        Path localCategoriesDir = localPackDir.resolve(BannerPack.CATEGORIES_DIR);

        try {
            Files.createDirectories(localBannersDir);
            Path mcmeta = localPackDir.resolve(BannerPack.MCMETA_FILE);
            if (!Files.exists(mcmeta)) {
                BannerPackMetadata metadata = new BannerPackMetadata(LOCAL_PACK_ID, "Local");
                BannerPack.writeMcmeta(localPackDir, metadata);
                writeDefaultCategories(localCategoriesDir);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create local pack", e);
        }
    }

    private static void writeDefaultCategories(Path categoriesDir) throws IOException {
        String ns = "loom-assistant";
        for (BannerRecipeCategory cat : new BannerRecipeCategory[] {
            new BannerRecipeCategory("flags", "Flags", "minecraft:map"),
            new BannerRecipeCategory("letters", "Letters", "minecraft:book"),
            new BannerRecipeCategory("logos", "Logos", "minecraft:blaze_powder"),
            new BannerRecipeCategory("misc", "Misc", "minecraft:lava_bucket"),
            new BannerRecipeCategory("nature", "Nature", "minecraft:poppy"),
        }) {
            BannerPack.writeCategoryFile(categoriesDir, ns, cat);
        }
        writeCategoryLangFile(categoriesDir, "sv_se", new String[][] {
            {"flags", "Flaggor"},
            {"letters", "Bokst\u00e4ver"},
            {"logos", "Logotyper"},
            {"misc", "\u00d6vrigt"},
            {"nature", "Natur"},
        });
    }

    private static void writeCategoryLangFile(Path categoriesDir, String locale, String[][] entries)
            throws IOException {
        Path langDir = categoriesDir.resolve("lang");
        Files.createDirectories(langDir);
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        for (String[] entry : entries) {
            obj.addProperty(entry[0], entry[1]);
        }
        try (java.io.Writer w = java.nio.file.Files.newBufferedWriter(langDir.resolve(locale + ".json"))) {
            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(obj, w);
        }
    }

    private BannerPack requirePack(String packId) {
        BannerPack pack = packs.get(packId);
        if (pack == null) {
            throw new IllegalArgumentException("pack not found: " + packId);
        }
        return pack;
    }

    private void loadDirectoryPack(Path packDir) throws IOException {
        String packId = normalizeId(packDir.getFileName().toString());
        BannerPack pack = BannerPack.loadDirectoryPack(packDir, packId);
        if (pack != null) {
            packs.put(packId, pack);
        }
    }

    private void loadZipPack(Path zipPath) throws IOException {
        String zipName = zipPath.getFileName().toString();
        String packId = normalizeId(zipName.substring(0, zipName.length() - ".zip".length()));
        BannerPack pack = BannerPack.loadZipPack(zipPath, packId);
        if (pack != null) {
            packs.put(packId, pack);
        }
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("pack id cannot be empty");
        }
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }

    private static void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(directory)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed deleting " + path, e);
                }
            });
        }
    }
}
