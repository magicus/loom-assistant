/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

public record BannerRecipeLayer(Identifier pattern, DyeColor color) {
    public static final Codec<BannerRecipeLayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Identifier.CODEC.fieldOf("pattern").forGetter(BannerRecipeLayer::pattern),
                    DyeColor.CODEC.fieldOf("color").forGetter(BannerRecipeLayer::color))
            .apply(instance, BannerRecipeLayer::new));

    public BannerRecipeLayer {
        if (pattern == null) {
            throw new IllegalArgumentException("pattern identifier cannot be null");
        }
        if (color == null) {
            throw new IllegalArgumentException("color cannot be null");
        }
    }

    public static BannerRecipeLayer of(String pattern, String color) {
        Identifier parsed = Identifier.tryParse(pattern);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid pattern identifier: " + pattern);
        }
        DyeColor parsedColor = DyeColor.byName(color, null);
        if (parsedColor == null) {
            throw new IllegalArgumentException("Invalid color: " + color);
        }
        return new BannerRecipeLayer(parsed, parsedColor);
    }

    // Bridge methods matching the old BannerRecipeLayer API
    public String patternId() {
        return pattern.toString();
    }

    public DyeColor getDyeColorEnum() {
        return color;
    }
}
