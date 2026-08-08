/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant;

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

public class LoomAssistantMod implements ModInitializer {
    public static final String MOD_ID = "loom-assistant";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final LoomScreenState LOOM_STATE = new LoomScreenState();
    private static final LoomScreenStateManager LOOM_MANAGER = new LoomScreenStateManager(LOOM_STATE);

    public static LoomScreenStateManager getLoomManager() {
        return LOOM_MANAGER;
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
