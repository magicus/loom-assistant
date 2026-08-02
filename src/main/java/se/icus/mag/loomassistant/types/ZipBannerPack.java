/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types;

import java.io.IOException;
import java.nio.file.Path;

public class ZipBannerPack extends BannerPack {
    public ZipBannerPack(BannerPackMetadata metadata, Path path, Path bannersPath) throws IOException {
        super(metadata, path);
        loadRecipesFromPath(bannersPath);
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
    public BannerRecipe updateBannerRecipe(BannerRecipe recipe) {
        throw new IllegalStateException(
                "Cannot update recipe in read-only pack " + getMetadata().id());
    }

    @Override
    public void removeBannerRecipe(String recipeId) {
        throw new IllegalStateException(
                "Cannot remove recipe from read-only pack " + getMetadata().id());
    }
}
