/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.data;

import net.minecraft.world.item.DyeColor;
import se.icus.mag.loomassistant.types.BannerDesignLayer;

/**
 * Represents a single pattern layer on a banner.
 * Stores the pattern identifier and dye color used.
 */
public record BannerPatternLayer(String patternId, String dyeColor) {
    public DyeColor getDyeColorEnum() {
        return DyeColor.byName(dyeColor, DyeColor.WHITE);
    }

    public static BannerPatternLayer of(String patternId, DyeColor color) {
        return new BannerPatternLayer(patternId, color.getName());
    }

    public BannerDesignLayer toType() {
        return BannerDesignLayer.of(patternId, dyeColor);
    }

    public static BannerPatternLayer fromType(BannerDesignLayer layer) {
        return new BannerPatternLayer(layer.pattern().toString(), layer.color().getName());
    }
}
