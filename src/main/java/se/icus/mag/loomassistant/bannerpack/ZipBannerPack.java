/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack;

import java.io.IOException;
import java.nio.file.Path;
import se.icus.mag.loomassistant.recipe.BannerRecipe;

public class ZipBannerPack extends BannerPack {
    public ZipBannerPack(BannerPackMetadata metadata, Path path, Path bannersPath, Path categoriesPath, Path langPath)
            throws IOException {
        super(metadata, path);
        loadRecipesFromPath(bannersPath);
        loadCategoriesFromPath(categoriesPath);
        loadCategoryLangFiles(langPath);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public BannerRecipe addBannerRecipe(BannerRecipe recipe) {
        throw new IllegalStateException(
                "Cannot add recipe to read-only pack " + getMetadata().id());
    }

    @Override
    public void updateBannerRecipe(BannerRecipe recipe) {
        throw new IllegalStateException(
                "Cannot update recipe in read-only pack " + getMetadata().id());
    }

    @Override
    public void removeBannerRecipe(String recipeId) {
        throw new IllegalStateException(
                "Cannot remove recipe from read-only pack " + getMetadata().id());
    }
}
