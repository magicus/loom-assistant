/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe;

public class BannerRecipeJsonConverter extends BannerRecipeConverter<String> {
    @Override
    public String fromRecipe(BannerRecipe recipe) {
        return recipe.toJson();
    }

    @Override
    public BannerRecipe toRecipe(String source) {
        return BannerRecipe.fromJson(source);
    }
}
