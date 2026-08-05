/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui.screens;

import net.minecraft.world.item.DyeColor;
import se.icus.mag.loomassistant.config.LoomAssistantConfig;

public final class DyeColorSorting {
    // Spectrum order: warm reds → orange/yellow → greens → cool blues → purples/pinks → neutrals
    private static final DyeColor[] RAINBOW_ORDER = {
        DyeColor.RED,
        DyeColor.ORANGE,
        DyeColor.YELLOW,
        DyeColor.LIME,
        DyeColor.GREEN,
        DyeColor.CYAN,
        DyeColor.LIGHT_BLUE,
        DyeColor.BLUE,
        DyeColor.PURPLE,
        DyeColor.MAGENTA,
        DyeColor.PINK,
        DyeColor.BROWN,
        DyeColor.WHITE,
        DyeColor.LIGHT_GRAY,
        DyeColor.GRAY,
        DyeColor.BLACK,
    };

    private DyeColorSorting() {}

    public static DyeColor[] sorted(LoomAssistantConfig.ColorSortOrder order) {
        if (order == LoomAssistantConfig.ColorSortOrder.RAINBOW) {
            return RAINBOW_ORDER.clone();
        }
        return DyeColor.values().clone();
    }
}
