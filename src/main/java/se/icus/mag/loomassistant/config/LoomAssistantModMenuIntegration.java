/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import java.lang.reflect.Field;
import java.util.List;
import me.shedaniel.autoconfig.AutoConfigClient;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;
import se.icus.mag.loomassistant.gui.screens.packselection.BannerPackSelectionScreen;

public class LoomAssistantModMenuIntegration implements ModMenuApi {
    private static boolean providerRegistered;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ensureProviderRegistered();
            return AutoConfigClient.getConfigScreen(LoomAssistantConfig.class, parent)
                    .get();
        };
    }

    private static void ensureProviderRegistered() {
        if (providerRegistered) return;

        AutoConfigClient.getGuiRegistry(LoomAssistantConfig.class)
                .registerPredicateProvider(
                        LoomAssistantModMenuIntegration::buildManagePacksActionEntry,
                        LoomAssistantModMenuIntegration::isActivateAfterDownloadField);
        providerRegistered = true;
    }

    private static boolean isActivateAfterDownloadField(Field field) {
        return field.getDeclaringClass() == LoomAssistantConfig.BannerPackRepoSettings.class
                && "activateAfterDownload".equals(field.getName());
    }

    private static List<AbstractConfigListEntry> buildManagePacksActionEntry(
            String i18n,
            Field field,
            Object config,
            Object defaults,
            me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess registry) {
        ActionButtonListEntry entry = new ActionButtonListEntry(
                Component.translatable("loom-assistant.config.manage_packs.row"),
                Component.translatable("loom-assistant.config.manage_packs"),
                configScreen -> {
                    LoomAssistantMod.LOGGER.info("[Config] Manage Banner Packs button clicked");
                    try {
                        BannerStorage storage = BannerStorage.getInstance();
                        storage.load();
                        Minecraft.getInstance()
                                .gui
                                .setScreen(new BannerPackSelectionScreen(
                                        storage.getRepository(), storage.getActivePacksConfig(), configScreen));
                    } catch (RuntimeException e) {
                        LoomAssistantMod.LOGGER.error("[Config] Failed to open Banner Pack Selection screen", e);
                    }
                });
        return List.of(entry);
    }
}
