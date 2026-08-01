/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.icus.mag.loomassistant.data.BannerStorage;

public class LoomAssistantMod implements ModInitializer {
    public static final String MOD_ID = "loom-assistant";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LoomAssistantMod.LOGGER.info("Initializing LoomAssistantMod");
        BannerStorage.getInstance().load();
    }
}
