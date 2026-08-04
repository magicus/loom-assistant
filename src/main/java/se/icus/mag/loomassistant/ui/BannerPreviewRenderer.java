/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

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
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.data.BannerPatternLayer;
import se.icus.mag.loomassistant.data.SavedBanner;
import se.icus.mag.loomassistant.types.BannerRecipe;

/**
 * Renders banner preview thumbnails in the side panel.
 */
public class BannerPreviewRenderer {
    public static void render(
            GuiGraphicsExtractor context, SavedBanner banner, LoomMenu handler, int x, int y, int size) {
        // Create a banner item stack with patterns applied
        ItemStack bannerStack = createBannerWithPatterns(banner);

        // Render the item using DrawContext
        context.item(bannerStack, x, y);
    }

    /**
     * Creates a banner ItemStack with all patterns from the SavedBanner applied.
     */
    public static ItemStack createBannerWithPatterns(SavedBanner banner) {
        ItemStack stack = new ItemStack(banner.getBaseBannerItem());

        List<BannerPatternLayer> layers = banner.getLayers();
        if (!layers.isEmpty()) {
            try {
                BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();

                Registry<Object> registry = getBannerPatternRegistry();

                if (registry != null) {
                    for (BannerPatternLayer layer : layers) {
                        try {
                            String patternIdStr = layer.patternId();
                            Identifier patternId = Identifier.tryParse(patternIdStr);

                            if (patternId != null) {
                                Optional<Holder.Reference<Object>> entry = registry.get(patternId);

                                if (entry.isPresent()) {
                                    // We need to cast to the specific type expected by the builder
                                    @SuppressWarnings("unchecked")
                                    Holder<Object> castedEntry = entry.get();
                                    builder.add((Holder) castedEntry, layer.getDyeColorEnum());
                                } else {
                                    LoomAssistantMod.LOGGER.debug("Pattern not found in registry: {}", patternId);
                                }
                            }
                        } catch (Exception e) {
                            LoomAssistantMod.LOGGER.debug("Error processing banner pattern: {}", layer.patternId(), e);
                        }
                    }
                }

                stack.set(DataComponents.BANNER_PATTERNS, builder.build());
            } catch (Exception e) {
                LoomAssistantMod.LOGGER.debug("Error creating banner patterns component", e);
            }
        }

        return stack;
    }

    @SuppressWarnings("unchecked")
    private static Registry<Object> getBannerPatternRegistry() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            try {
                // Try getOptional first
                return (Registry<Object>) (Object) client.level
                        .registryAccess()
                        .lookup(Registries.BANNER_PATTERN)
                        .orElse(null);
            } catch (Exception e) {
                LoomAssistantMod.LOGGER.debug("Failed to get registry from world", e);
            }
        }
        return null;
    }

    public static SavedBanner extractBannerData(ItemStack stack) {
        BannerRecipe recipe = BannerRecipe.fromItem(stack);
        return recipe == null ? null : SavedBanner.fromType(recipe);
    }
}
