/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant;

import java.util.WeakHashMap;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;
import se.icus.mag.loomassistant.config.LoomAssistantConfig;
import se.icus.mag.loomassistant.gui.extensions.LoomScreenExtension;
import se.icus.mag.loomassistant.gui.panel.LoomRecipePanel;

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

    @Override
    public void onInitialize() {
        LoomAssistantMod.LOGGER.info("Initializing LoomAssistantMod");
        AutoConfig.register(LoomAssistantConfig.class, GsonConfigSerializer::new);
        BannerStorage.getInstance().load();
    }
}
