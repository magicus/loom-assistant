/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.extensions;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;

public class WeavingGuide {
    public static boolean resultMatchesExpected(ItemStack activeBannerStack, ItemStack result, int nextLayerIndex) {
        if (!(result.getItem() instanceof BannerItem bannerItem)) return false;
        BannerRecipe recipe = BannerRecipe.fromItem(activeBannerStack);
        if (recipe == null || nextLayerIndex >= recipe.getLayers().size()) return false;
        if (bannerItem.getColor() != recipe.getBannerColorEnum()) return false;

        BannerPatternLayers layers = result.get(DataComponents.BANNER_PATTERNS);
        if (layers == null) return false;
        int expected = nextLayerIndex + 1;
        if (layers.layers().size() != expected) return false;

        for (int i = 0; i < expected; i++) {
            BannerPatternLayers.Layer cur = layers.layers().get(i);
            BannerRecipeLayer exp = recipe.getLayers().get(i);
            if (cur.color() != exp.getDyeColorEnum()) return false;
            String curId = cur.pattern()
                    .unwrapKey()
                    .map(k -> k.identifier().toString())
                    .orElse(null);
            if (curId == null) return false;
            String expId = exp.patternId().contains(":") ? exp.patternId() : "minecraft:" + exp.patternId();
            if (!curId.equals(expId)) return false;
        }
        return true;
    }
}
