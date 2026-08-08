/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import se.icus.mag.loomassistant.LoomAssistantMod;

@Config(name = LoomAssistantMod.MOD_ID)
public class LoomAssistantConfig implements ConfigData {
    public enum ColorSortOrder {
        RAINBOW,
        VANILLA
    }

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    ColorSortOrder colorSortOrder = ColorSortOrder.RAINBOW;

    public ColorSortOrder getColorSortOrder() {
        return colorSortOrder;
    }
}
