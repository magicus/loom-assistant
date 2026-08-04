/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class DirectoryBannerPack extends BannerPack {
    public DirectoryBannerPack(BannerPackMetadata metadata, Path path) {
        super(metadata, path);
    }

    public DirectoryBannerPack(BannerPackMetadata metadata, Path path, Path bannersPath, Path categoriesPath)
            throws IOException {
        this(metadata, path);
        loadRecipesFromPath(bannersPath);
        loadCategoriesFromPath(categoriesPath);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public BannerRecipe addBannerRecipe(BannerRecipe recipe) throws IOException {
        BannerRecipe normalized = normalizeBannerRecipeForPack(recipe);
        writeRecipe(normalized);
        return normalized;
    }

    @Override
    public BannerRecipe updateBannerRecipe(BannerRecipe recipe) throws IOException {
        if (recipe.id() == null || recipe.id().isBlank()) {
            throw new IllegalArgumentException("recipe id is required for update");
        }
        return addBannerRecipe(recipe);
    }

    @Override
    public void removeBannerRecipe(String recipeId) throws IOException {
        deleteRecipeFile(recipeId);
    }

    public void addBannerCategory(String namespace, BannerRecipeCategory category) throws IOException {
        writeCategoryFile(getPath().resolve(CATEGORIES_DIR), namespace, category);
    }

    public BannerRecipe moveRecipeTo(BannerPack target, String recipeId) throws IOException {
        if (target.isReadOnly()) {
            throw new IllegalArgumentException("cannot move to read-only pack");
        }
        BannerRecipe recipe = getDesign(recipeId);
        if (recipe == null) {
            throw new IllegalArgumentException("recipe not found: " + recipeId);
        }
        BannerRecipe moved = target.addBannerRecipe(recipe.withId(null));
        removeBannerRecipe(recipeId);
        return moved;
    }

    private BannerRecipe normalizeBannerRecipeForPack(BannerRecipe recipe) {
        String id = recipe.id();
        if (id == null || id.isBlank()) {
            return recipe.withId(
                    getMetadata().id() + ":" + UUID.randomUUID().toString().replace("-", ""));
        } else if (!id.contains(":")) {
            return recipe.withId(getMetadata().id() + ":" + id);
        }
        return recipe;
    }

    private void writeRecipe(BannerRecipe recipe) throws IOException {
        Path designFile = getRecipeFile(recipe.id());
        Files.createDirectories(designFile.getParent());
        try (Writer writer = Files.newBufferedWriter(designFile)) {
            writer.write(recipe.toJson());
        }
        includeRecipe(recipe);
    }

    private void deleteRecipeFile(String recipeId) throws IOException {
        excludeRecipe(recipeId);
        Files.deleteIfExists(getRecipeFile(recipeId));
    }

    private Path getRecipeFile(String recipeId) {
        int colon = recipeId.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("recipe id must be in namespace:name format: " + recipeId);
        }
        String namespace = recipeId.substring(0, colon);
        String name = recipeId.substring(colon + 1);
        return getPath().resolve(BANNERS_DIR).resolve(namespace).resolve(name + ".json");
    }
}
