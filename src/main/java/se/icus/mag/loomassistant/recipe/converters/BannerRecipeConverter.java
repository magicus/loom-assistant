/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe.converters;

import se.icus.mag.loomassistant.recipe.BannerRecipe;

public abstract class BannerRecipeConverter<T> {
    public abstract T fromRecipe(BannerRecipe recipe);

    public abstract BannerRecipe toRecipe(T source);
}
