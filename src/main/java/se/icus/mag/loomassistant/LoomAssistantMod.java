/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant;

import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;
import se.icus.mag.loomassistant.config.LoomAssistantConfig;
import se.icus.mag.loomassistant.gui.panel.LoomRecipePanel;
import se.icus.mag.loomassistant.gui.extensions.LoomScreenExtension;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;

public class LoomAssistantMod implements ModInitializer {
    public static final String MOD_ID = "loom-assistant";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final WeakHashMap<Object, LoomScreenExtension> SCREEN_EXTENSIONS = new WeakHashMap<>();

    public static void registerExtension(Object screen, LoomScreenExtension extension) {
        SCREEN_EXTENSIONS.put(screen, extension);
    }

    public static LoomRecipePanel getPanel(Object screen) {
        LoomScreenExtension ext = SCREEN_EXTENSIONS.get(screen);
        return ext != null ? ext.getPanel() : null;
    }

    public static LoomScreenExtension getExtension(Object screen) {
        return SCREEN_EXTENSIONS.get(screen);
    }

    public static LoomAssistantConfig getConfig() {
        return AutoConfig.getConfigHolder(LoomAssistantConfig.class).getConfig();
    }

    public static Registry<BannerPattern> getBannerPatternRegistry(Minecraft client) {
        if (client.level != null) {
            try {
                return client.level
                        .registryAccess()
                        .lookup(Registries.BANNER_PATTERN)
                        .orElse(null);
            } catch (RuntimeException e) {
                LOGGER.debug("Failed to get registry from world", e);
            }
        }
        return null;
    }

    public static ItemStack createBannerStack(
            Item baseBannerItem, Registry<BannerPattern> registry, List<BannerRecipeLayer> layers) {
        ItemStack stack = new ItemStack(baseBannerItem);

        if (registry == null || layers.isEmpty()) {
            return stack;
        }

        try {
            BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
            for (BannerRecipeLayer layer : layers) {
                try {
                    Identifier patternId = Identifier.tryParse(layer.patternId());
                    if (patternId == null) {
                        continue;
                    }

                    Optional<Holder.Reference<BannerPattern>> entry = registry.get(patternId);
                    if (entry.isEmpty()) {
                        LOGGER.debug("Pattern not found in registry: {}", patternId);
                        continue;
                    }

                    builder.add(entry.get(), layer.getDyeColorEnum());
                } catch (RuntimeException e) {
                    LOGGER.debug("Error processing banner pattern: {}", layer.patternId(), e);
                }
            }
            stack.set(DataComponents.BANNER_PATTERNS, builder.build());
        } catch (RuntimeException e) {
            LOGGER.debug("Error creating banner patterns component", e);
        }

        return stack;
    }

    @Override
    public void onInitialize() {
        LoomAssistantMod.LOGGER.info("Initializing LoomAssistantMod");
        AutoConfig.register(LoomAssistantConfig.class, GsonConfigSerializer::new);
        BannerStorage.getInstance().load();
    }
}
