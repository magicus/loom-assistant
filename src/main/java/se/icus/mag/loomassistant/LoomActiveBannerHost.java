/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant;

import java.util.Map;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public interface LoomActiveBannerHost {
    void loomassistant$setPendingActiveBannerStack(ItemStack stack);

    void loomassistant$setPersistentDyeSwitchState(boolean enabled, Map<DyeColor, DyeColor> replacements);
}
