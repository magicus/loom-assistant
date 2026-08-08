/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe.converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;

public class BannerRecipeItemConverter extends BannerRecipeConverter<ItemStack> {
    @Override
    public ItemStack fromRecipe(BannerRecipe recipe) {
        Minecraft client = Minecraft.getInstance();
        return toItem(
                LoomAssistantMod.getBannerPatternRegistry(client), recipe.getBaseBannerItem(), recipe.getLayers());
    }

    @Override
    public BannerRecipe toRecipe(ItemStack source) {
        return fromItem(source);
    }

    public static BannerRecipe fromItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (!(stack.getItem() instanceof BannerItem bannerItem)) return null;

        Component customName = stack.getCustomName();
        String description = customName != null ? customName.getString() : BannerRecipe.DEFAULT_DESCRIPTION;
        return fromBannerPatterns(description, bannerItem.getColor(), stack.get(DataComponents.BANNER_PATTERNS));
    }

    public static ItemStack toItem(Minecraft client, BannerRecipe recipe) {
        return toItem(
                LoomAssistantMod.getBannerPatternRegistry(client), recipe.getBaseBannerItem(), recipe.getLayers());
    }

    public static ItemStack toItem(
            Registry<BannerPattern> registry, Item baseBannerItem, List<BannerRecipeLayer> layers) {
        ItemStack stack = new ItemStack(baseBannerItem);

        if (registry == null || layers.isEmpty()) return stack;

        try {
            BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
            for (BannerRecipeLayer layer : layers) {
                try {
                    Identifier patternId = Identifier.tryParse(layer.patternId());
                    if (patternId == null) continue;

                    Optional<Holder.Reference<BannerPattern>> entry = registry.get(patternId);
                    if (entry.isEmpty()) {
                        LoomAssistantMod.LOGGER.debug("Pattern not found in registry: {}", patternId);
                        continue;
                    }

                    builder.add(entry.get(), layer.getDyeColorEnum());
                } catch (RuntimeException e) {
                    LoomAssistantMod.LOGGER.debug("Error processing banner pattern: {}", layer.patternId(), e);
                }
            }
            stack.set(DataComponents.BANNER_PATTERNS, builder.build());
        } catch (RuntimeException e) {
            LoomAssistantMod.LOGGER.debug("Error creating banner patterns component", e);
        }

        return stack;
    }

    private static BannerRecipe fromBannerPatterns(
            String description, DyeColor baseColor, BannerPatternLayers patterns) {
        List<BannerRecipeLayer> parsedLayers = new ArrayList<>();
        if (patterns != null) {
            for (BannerPatternLayers.Layer layer : patterns.layers()) {
                parsedLayers.add(BannerRecipeLayer.of(
                        layer.pattern().getRegisteredName(), layer.color().getName()));
            }
        }
        return new BannerRecipe(
                null, description, null, null, BannerRecipe.DEFAULT_CATEGORY, baseColor.getName(), parsedLayers);
    }
}
