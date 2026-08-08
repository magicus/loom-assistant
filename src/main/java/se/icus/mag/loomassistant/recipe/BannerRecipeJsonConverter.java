/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

public class BannerRecipeJsonConverter extends BannerRecipeConverter<String> {
    @Override
    public String fromRecipe(BannerRecipe recipe) {
        JsonElement element = BannerRecipe.CODEC
                .encodeStart(JsonOps.INSTANCE, recipe)
                .getOrThrow(msg -> new IllegalStateException("Failed to encode BannerRecipe: " + msg));
        return BannerRecipe.GSON.toJson(element);
    }

    @Override
    public BannerRecipe toRecipe(String source) {
        JsonElement element = JsonParser.parseString(source);
        return BannerRecipe.CODEC
                .parse(JsonOps.INSTANCE, element)
                .getOrThrow(msg -> new IllegalStateException("Failed to parse BannerRecipe: " + msg));
    }
}
