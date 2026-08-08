/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.extensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.ItemStack;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.recipe.BannerRecipe;

/**
 * Renders banner preview thumbnails in the side panel.
 */
public final class PreviewExtension {
    private PreviewExtension() {}

    public static void render(
            GuiGraphicsExtractor context, BannerRecipe banner, LoomMenu handler, int x, int y, int size) {
        // Create a banner item stack with patterns applied
        ItemStack bannerStack = LoomAssistantMod.createBannerStack(
                banner.getBaseBannerItem(), LoomAssistantMod.getBannerPatternRegistry(Minecraft.getInstance()), banner.getLayers());

        // Render the item using DrawContext
        context.item(bannerStack, x, y);
    }

}
