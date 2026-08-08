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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;
import se.icus.mag.loomassistant.config.LoomAssistantConfig;
import se.icus.mag.loomassistant.gui.panel.LoomRecipePanel;
import se.icus.mag.loomassistant.gui.extensions.LoomScreenExtension;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
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

    /**
     * Creates a banner ItemStack with all patterns from the BannerRecipe applied.
     */
    public static ItemStack createBannerWithPatterns(BannerRecipe banner) {
        ItemStack stack = new ItemStack(banner.getBaseBannerItem());

        List<BannerRecipeLayer> layers = banner.getLayers();
        if (!layers.isEmpty()) {
            try {
                BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();

                Registry<BannerPattern> registry = getBannerPatternRegistry(Minecraft.getInstance());

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
                                    LOGGER.debug("Pattern not found in registry: {}", patternId);
                                }
                            }
                        } catch (RuntimeException e) {
                            LOGGER.debug("Error processing banner pattern: {}", layer.patternId(), e);
                        }
                    }
                }

                stack.set(DataComponents.BANNER_PATTERNS, builder.build());
            } catch (RuntimeException e) {
                LOGGER.debug("Error creating banner patterns component", e);
            }
        }

        return stack;
    }

    public static ItemStack createLayerPreviewStack(BannerRecipeLayer layer) {
        ItemStack stack = new ItemStack(Items.BANNER.white());
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return stack;
        }

        try {
            var patternRegistry = mc.level.registryAccess().lookup(Registries.BANNER_PATTERN);
            if (patternRegistry.isEmpty()) {
                return stack;
            }
            Identifier id = Identifier.tryParse(layer.patternId());
            if (id == null) {
                return stack;
            }

            var entry = patternRegistry.get().get(id);
            if (entry.isEmpty()) {
                return stack;
            }

            BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
            builder.add(entry.get(), layer.getDyeColorEnum());
            stack.set(DataComponents.BANNER_PATTERNS, builder.build());
        } catch (RuntimeException ignored) {
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
