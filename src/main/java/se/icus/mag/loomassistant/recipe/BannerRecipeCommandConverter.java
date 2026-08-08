/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe;

public class BannerRecipeCommandConverter extends BannerRecipeConverter<String> {
    @Override
    public String fromRecipe(BannerRecipe recipe) {
        return recipe.toCommand();
    }

    @Override
    public BannerRecipe toRecipe(String source) {
        return BannerRecipe.fromCommand(source);
    }
}
