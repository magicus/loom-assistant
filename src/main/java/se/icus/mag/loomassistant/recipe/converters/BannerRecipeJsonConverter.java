/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe.converters;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;

public class BannerRecipeJsonConverter extends BannerRecipeConverter<String> {
    public static final Codec<BannerRecipe> CODEC;

    static {
        CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("description").forGetter(BannerRecipe::description),
                        Codec.STRING.optionalFieldOf("author").forGetter(d -> Optional.ofNullable(d.author())),
                        Codec.STRING.optionalFieldOf("url").forGetter(d -> Optional.ofNullable(d.url())),
                        Codec.STRING
                                .optionalFieldOf("category", BannerRecipe.DEFAULT_CATEGORY)
                                .forGetter(BannerRecipe::category),
                        Codec.STRING.fieldOf("banner_color").forGetter(BannerRecipe::bannerColor),
                        BannerRecipeLayer.CODEC.listOf().fieldOf("layers").forGetter(BannerRecipe::layers))
                .apply(
                        instance,
                        (description, author, url, category, bannerColor, layers) -> new BannerRecipe(
                                null,
                                description,
                                author.orElse(null),
                                url.orElse(null),
                                category,
                                bannerColor,
                                layers)));
    }

    @Override
    public String fromRecipe(BannerRecipe recipe) {
        JsonElement element = CODEC.encodeStart(JsonOps.INSTANCE, recipe)
                .getOrThrow(msg -> new IllegalStateException("Failed to encode BannerRecipe: " + msg));
        return BannerRecipe.GSON.toJson(element);
    }

    @Override
    public BannerRecipe toRecipe(String source) {
        JsonElement element = JsonParser.parseString(source);
        return CODEC.parse(JsonOps.INSTANCE, element)
                .getOrThrow(msg -> new IllegalStateException("Failed to parse BannerRecipe: " + msg));
    }
}
