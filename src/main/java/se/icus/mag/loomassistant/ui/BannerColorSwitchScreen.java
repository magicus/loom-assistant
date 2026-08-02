/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import se.icus.mag.loomassistant.LoomActiveBannerHost;
import se.icus.mag.loomassistant.data.SavedBanner;

/**
 * Simple color replacement dialog for active banner dyes.
 */
public class BannerColorSwitchScreen extends Screen {
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 230;
    private static final int ROW_H = 18;

    private final Screen previousScreen;
    private final LoomPanel panel;
    private final List<DyeColor> sourceColors;
    private final EnumMap<DyeColor, DyeColor> targets = new EnumMap<>(DyeColor.class);
    private boolean persistent;
    private Button persistentButton;

    public BannerColorSwitchScreen(Screen previousScreen, LoomPanel panel) {
        super(Component.translatable("loom-assistant.screen.color_switch.title"));
        this.previousScreen = previousScreen;
        this.panel = panel;
        this.sourceColors = panel.getActiveBannerUsedColors();
    }

    @Override
    protected void init() {
        Map<DyeColor, DyeColor> initialTargets = panel.getInitialDyeReplacementTargets(sourceColors);
        for (DyeColor source : sourceColors) {
            targets.put(source, initialTargets.getOrDefault(source, source));
        }

        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - PANEL_H) / 2;

        int rowY = y + 36;
        for (int i = 0; i < sourceColors.size(); i++) {
            DyeColor source = sourceColors.get(i);
            final int index = i;
            int btnY = rowY + index * ROW_H - 1;

            this.addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleTarget(source, -1))
                    .bounds(x + PANEL_W - 58, btnY, 16, 16)
                    .build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleTarget(source, 1))
                    .bounds(x + PANEL_W - 20, btnY, 16, 16)
                    .build());
        }

        this.persistentButton = this.addRenderableWidget(Button.builder(
                        persistentLabel(),
                        button -> {
                            persistent = !persistent;
                            button.setMessage(persistentLabel());
                        })
                .bounds(x + 16, y + PANEL_H - 48, 140, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("loom-assistant.common.ok"), button -> applyAndClose())
                .bounds(x + PANEL_W - 220, y + PANEL_H - 48, 96, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("loom-assistant.common.cancel"), button -> this.onClose())
                .bounds(x + PANEL_W - 110, y + PANEL_H - 48, 96, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xAA000000);

        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - PANEL_H) / 2;

        ctx.fill(x, y, x + PANEL_W, y + PANEL_H, 0xFF242424);
        ctx.outline(x, y, PANEL_W, PANEL_H, 0xFFFFFFFF);

        ctx.text(this.font, this.title, x + 16, y + 12, 0xFFFFFFFF, false);

        int rowY = y + 36;
        int centerX = x + PANEL_W / 2;
        for (int i = 0; i < sourceColors.size(); i++) {
            DyeColor source = sourceColors.get(i);
            DyeColor target = targets.getOrDefault(source, source);
            int lineY = rowY + i * ROW_H;

            ItemStack sourceIcon = new ItemStack(SavedBanner.getDyeItem(source));
            ItemStack targetIcon = new ItemStack(SavedBanner.getDyeItem(target));
            Component sourceName = sourceIcon.getHoverName();
            Component targetName = targetIcon.getHoverName();

            ctx.item(sourceIcon, x + 16, lineY);
            ctx.text(this.font, sourceName, x + 36, lineY + 4, 0xFFDDDDDD, false);

            ctx.text(this.font, Component.literal("->"), centerX - 6, lineY + 4, 0xFFDDDDDD, false);

            ctx.item(targetIcon, centerX + 16, lineY);
            ctx.text(this.font, targetName, centerX + 36, lineY + 4, 0xFFFFFFFF, false);
        }

        super.extractRenderState(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            applyAndClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private void cycleTarget(DyeColor source, int delta) {
        DyeColor current = targets.getOrDefault(source, source);
        DyeColor[] values = DyeColor.values();
        int nextIndex = (current.ordinal() + delta + values.length) % values.length;
        targets.put(source, values[nextIndex]);
    }

    private void applyAndClose() {
        boolean changed = panel.applyDyeSwitch(targets, persistent);
        if (changed && previousScreen instanceof LoomActiveBannerHost host) {
            host.loomassistant$setPendingActiveBannerStack(panel.getActiveBannerStack());
        }
        if (previousScreen instanceof LoomActiveBannerHost host) {
            host.loomassistant$setPersistentDyeSwitchState(
                    panel.isPersistentDyeSwitchEnabled(),
                    panel.getPersistentDyeReplacementMapCopy());
        }
        this.minecraft.gui.setScreen(previousScreen);
    }

    private Component persistentLabel() {
        String prefix = persistent ? "[x] " : "[ ] ";
        return Component.literal(prefix).append(Component.translatable("loom-assistant.screen.color_switch.persistent"));
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(previousScreen);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
