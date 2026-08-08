/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;
import se.icus.mag.loomassistant.gui.screens.packselection.BannerPackSelectionScreen;

public class LoomAssistantModMenuIntegration implements ModMenuApi {
    private static final int MANAGE_PACKS_BUTTON_WIDTH = 200;
    private static final int MANAGE_PACKS_BUTTON_HEIGHT = 20;
    private static final Map<Screen, Boolean> PENDING_MANAGE_PACKS_BUTTON =
            Collections.synchronizedMap(new WeakHashMap<>());

    static {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!PENDING_MANAGE_PACKS_BUTTON.containsKey(screen)) return;
            PENDING_MANAGE_PACKS_BUTTON.remove(screen);

            int x = (scaledWidth - MANAGE_PACKS_BUTTON_WIDTH) / 2;
            int y = Math.max(6, scaledHeight - 54);
            screen.addRenderableWidget(Button.builder(
                            Component.translatable("loom-assistant.config.manage_packs"),
                            button -> {
                                BannerStorage storage = BannerStorage.getInstance();
                                storage.load();
                                client.gui.setScreen(new BannerPackSelectionScreen(
                                        storage.getRepository(), storage.getActivePacksConfig(), screen));
                            })
                    .bounds(x, y, MANAGE_PACKS_BUTTON_WIDTH, MANAGE_PACKS_BUTTON_HEIGHT)
                    .build());
        });
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            Screen configScreen = AutoConfigClient.getConfigScreen(LoomAssistantConfig.class, parent)
                    .get();
            PENDING_MANAGE_PACKS_BUTTON.put(configScreen, Boolean.TRUE);
            return configScreen;
        };
    }
}
