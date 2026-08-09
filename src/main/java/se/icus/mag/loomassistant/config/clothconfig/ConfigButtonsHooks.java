/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.config.clothconfig;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.shedaniel.autoconfig.AutoConfigClient;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.icus.mag.loomassistant.LoomAssistantMod;

/**
 * Hooks into Cloth Config to add support for action buttons in the config UI.
 */
public final class ConfigButtonsHooks {
    private static final Set<Class<? extends ConfigData>> INSTALLED = new HashSet<>();

    private ConfigButtonsHooks() {}

    public static synchronized void installFor(Class<? extends ConfigData> configClass) {
        if (!INSTALLED.add(configClass)) return;

        AutoConfigClient.getGuiRegistry(configClass)
                .registerAnnotationTransformer(ConfigButtonsHooks::appendButtonsBelow, ConfigButtons.class);
    }

    private static List<AbstractConfigListEntry> appendButtonsBelow(
            List<AbstractConfigListEntry> original,
            String i18n,
            Field field,
            Object config,
            Object defaults,
            me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess registry) {
        ConfigButtons definition = field.getAnnotation(ConfigButtons.class);
        if (definition == null || definition.value().length == 0) {
            return original;
        }

        List<ActionButtonListEntry.ActionButtonSpec> actions = Arrays.stream(definition.value())
                .map(action -> new ActionButtonListEntry.ActionButtonSpec(
                        Component.translatable(action.buttonLabelKey()),
                        screen -> openConfiguredScreen(action.screenClass(), screen)))
                .toList();

        ActionButtonListEntry buttonsEntry = new ActionButtonListEntry(Component.empty(), actions);

        List<AbstractConfigListEntry> combined = new java.util.ArrayList<>(original.size() + 1);
        combined.addAll(original);
        combined.add(buttonsEntry);
        return combined;
    }

    private static void openConfiguredScreen(Class<? extends Screen> screenClass, Screen configScreen) {
        LoomAssistantMod.LOGGER.info("[Config] Action button clicked for {}", screenClass.getName());
        try {
            Screen targetScreen = screenClass.getConstructor(Screen.class).newInstance(configScreen);
            Minecraft.getInstance().gui.setScreen(targetScreen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LoomAssistantMod.LOGGER.error("[Config] Failed to open configured screen {}", screenClass.getName(), e);
        }
    }
}
