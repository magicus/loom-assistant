/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.extensions;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;

/**
 * Renders banner preview thumbnails in the side panel.
 */
public final class PreviewExtension {
    private PreviewExtension() {}

    public static void render(
            GuiGraphicsExtractor context, BannerRecipe banner, LoomMenu handler, int x, int y, int size) {
        // Create a banner item stack with patterns applied
        ItemStack bannerStack = createBannerWithPatterns(banner);

        // Render the item using DrawContext
        context.item(bannerStack, x, y);
    }

    /**
     * Creates a banner ItemStack with all patterns from the BannerRecipe applied.
     */
    public static ItemStack createBannerWithPatterns(BannerRecipe banner) {
        ItemStack stack = new ItemStack(banner.getBaseBannerItem());

        List<BannerRecipeLayer> layers = banner.getLayers();
        if (!layers.isEmpty()) {
            try {
                BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();

                Registry<BannerPattern> registry = getBannerPatternRegistry();

                if (registry != null) {
                    for (BannerRecipeLayer layer : layers) {
                        try {
                            String patternIdStr = layer.patternId();
                            Identifier patternId = Identifier.tryParse(patternIdStr);

                            if (patternId != null) {
                                Optional<Holder.Reference<BannerPattern>> entry = registry.get(patternId);

                                if (entry.isPresent()) {
                                    builder.add(entry.get(), layer.getDyeColorEnum());
                                } else {
                                    LoomAssistantMod.LOGGER.debug("Pattern not found in registry: {}", patternId);
                                }
                            }
                        } catch (RuntimeException e) {
                            LoomAssistantMod.LOGGER.debug("Error processing banner pattern: {}", layer.patternId(), e);
                        }
                    }
                }

                stack.set(DataComponents.BANNER_PATTERNS, builder.build());
            } catch (RuntimeException e) {
                LoomAssistantMod.LOGGER.debug("Error creating banner patterns component", e);
            }
        }

        return stack;
    }

    private static Registry<BannerPattern> getBannerPatternRegistry() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            try {
                // Try getOptional first
                return client.level
                        .registryAccess()
                        .lookup(Registries.BANNER_PATTERN)
                        .orElse(null);
            } catch (RuntimeException e) {
                LoomAssistantMod.LOGGER.debug("Failed to get registry from world", e);
            }
        }
        return null;
    }
}
