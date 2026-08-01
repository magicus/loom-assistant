/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import se.icus.mag.loomassistant.data.SavedBanner;

/**
 * Renders banner thumbnails using proper 3D item rendering.
 */
public class BannerThumbnailRenderer {
    /**
     * Renders a banner thumbnail at the specified position and size.
     */
    public static void render(GuiGraphicsExtractor context, SavedBanner banner, int x, int y, int size) {
        // Create a simple item stack from the banner's base color
        ItemStack bannerStack = new ItemStack(banner.getBaseBannerItem());

        // Render the item in 3D
        context.item(bannerStack, x, y);
    }
}
