/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Placeholder screen for banner recipe import/export.
 */
public class BannerRecipeImportExportScreen extends Screen {
    private final Screen previousScreen;

    public BannerRecipeImportExportScreen(Screen previousScreen) {
        super(Component.translatable("loom-assistant.screen.banner_recipe_import_export.title"));
        this.previousScreen = previousScreen;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xAA000000);

        int panelWidth = 270;
        int panelHeight = 90;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF1F1F1F);
        context.outline(panelX, panelY, panelWidth, panelHeight, 0xFF4169E1);

        Component title = this.title;
        int titleX = panelX + (panelWidth - this.font.width(title)) / 2;
        context.text(this.font, title, titleX, panelY + 14, 0xFFFFFFFF, true);

        Component subtitle = Component.translatable("loom-assistant.common.coming_soon");
        int subtitleX = panelX + (panelWidth - this.font.width(subtitle)) / 2;
        context.text(this.font, subtitle, subtitleX, panelY + 44, 0xFFAAAAAA, false);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.gui.setScreen(previousScreen);
            return true;
        }
        return super.keyPressed(event);
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
