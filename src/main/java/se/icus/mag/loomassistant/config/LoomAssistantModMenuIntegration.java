/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfigClient;
import se.icus.mag.loomassistant.config.clothconfig.ConfigButtonsHooks;

public class LoomAssistantModMenuIntegration implements ModMenuApi {
    private static boolean hooksInstalled;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ensureProviderRegistered();
            return AutoConfigClient.getConfigScreen(LoomAssistantConfig.class, parent)
                    .get();
        };
    }

    private static void ensureProviderRegistered() {
        if (hooksInstalled) return;
        ConfigButtonsHooks.installFor(LoomAssistantConfig.class);
        hooksInstalled = true;
    }
}
