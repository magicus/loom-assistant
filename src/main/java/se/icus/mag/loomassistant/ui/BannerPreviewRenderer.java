/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import java.util.ArrayList;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.data.BannerPatternLayer;
import se.icus.mag.loomassistant.data.SavedBanner;

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
    private static ItemStack createBannerWithPatterns(SavedBanner banner) {
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
        if (stack.isEmpty()) return null;

        DyeColor baseColor = getBannerColor(stack);
        if (baseColor == null) return null;

        List<BannerPatternLayer> layers = new ArrayList<>();
        BannerPatternLayers patterns = stack.get(DataComponents.BANNER_PATTERNS);

        if (patterns != null) {
            for (var layer : patterns.layers()) {
                String patternId = layer.pattern().getRegisteredName();
                DyeColor dyeColor = layer.color();
                layers.add(BannerPatternLayer.of(patternId, dyeColor));
            }
        }

        return new SavedBanner(null, baseColor, layers);
    }

    private static DyeColor getBannerColor(ItemStack stack) {
        var item = stack.getItem();
        if (item == Items.BANNER.white()) return DyeColor.WHITE;
        if (item == Items.BANNER.orange()) return DyeColor.ORANGE;
        if (item == Items.BANNER.magenta()) return DyeColor.MAGENTA;
        if (item == Items.BANNER.lightBlue()) return DyeColor.LIGHT_BLUE;
        if (item == Items.BANNER.yellow()) return DyeColor.YELLOW;
        if (item == Items.BANNER.lime()) return DyeColor.LIME;
        if (item == Items.BANNER.pink()) return DyeColor.PINK;
        if (item == Items.BANNER.gray()) return DyeColor.GRAY;
        if (item == Items.BANNER.lightGray()) return DyeColor.LIGHT_GRAY;
        if (item == Items.BANNER.cyan()) return DyeColor.CYAN;
        if (item == Items.BANNER.purple()) return DyeColor.PURPLE;
        if (item == Items.BANNER.blue()) return DyeColor.BLUE;
        if (item == Items.BANNER.brown()) return DyeColor.BROWN;
        if (item == Items.BANNER.green()) return DyeColor.GREEN;
        if (item == Items.BANNER.red()) return DyeColor.RED;
        if (item == Items.BANNER.black()) return DyeColor.BLACK;
        return null;
    }
}
