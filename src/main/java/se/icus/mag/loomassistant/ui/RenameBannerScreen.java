/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.data.BannerStorage;

/**
 * Screen for renaming a saved banner.
 */
public class RenameBannerScreen extends Screen {
    private static final int EDITOR_WIDTH = 250;
    private static final int EDITOR_HEIGHT = 60;
    private static final int PADDING = 10;

    private final Screen previousScreen;
    private final String bannerId;
    private final String currentName;
    private StringBuilder textBuffer;
    private int cursorPos = 0;

    public RenameBannerScreen(Screen previousScreen, String bannerId, String currentName) {
        super(Component.translatable("loom-assistant.screen.rename_banner.title"));
        this.previousScreen = previousScreen;
        this.bannerId = bannerId;
        this.currentName = currentName;
        this.textBuffer = new StringBuilder(currentName != null ? currentName : "");
        this.cursorPos = this.textBuffer.length();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Draw semi-transparent background
        context.fill(0, 0, this.width, this.height, 0xAA000000);

        // Draw editor background
        int editorX = (this.width - EDITOR_WIDTH) / 2;
        int editorY = (this.height - EDITOR_HEIGHT) / 2;
        context.fill(editorX, editorY, editorX + EDITOR_WIDTH, editorY + EDITOR_HEIGHT, 0xFF1F1F1F);
        context.outline(editorX, editorY, EDITOR_WIDTH, EDITOR_HEIGHT, 0xFFFFFFFF);

        // Draw title
        context.text(
            this.font,
            Component.translatable("loom-assistant.screen.rename_banner.enter_new_name"),
            editorX + PADDING,
            editorY - 20,
            0xFFFFFFFF,
            true);

        // Draw text content
        int textStartX = editorX + PADDING;
        int textStartY = editorY + PADDING;
        String text = textBuffer.toString();
        context.text(this.font, Component.literal(text), textStartX, textStartY, 0xFFFFFFFF, true);

        // Draw cursor
        if (cursorPos >= 0 && cursorPos <= text.length()) {
            String beforeCursor = text.substring(0, cursorPos);
            int cursorX = textStartX + this.font.width(beforeCursor);
            context.fill(cursorX, textStartY, cursorX + 1, textStartY + 10, 0xFFFFFFFF);
        }

        // Draw buttons
        int buttonY = editorY + EDITOR_HEIGHT + PADDING;

        // OK button
        int okButtonX = (this.width / 2) - 60;
        boolean okHovered =
                mouseX >= okButtonX && mouseX < okButtonX + 50 && mouseY >= buttonY && mouseY < buttonY + 20;
        int okColor = okHovered ? 0xFF4CAF50 : 0xFF2E7D32;
        context.fill(okButtonX, buttonY, okButtonX + 50, buttonY + 20, okColor);
        context.text(
            this.font,
            Component.translatable("loom-assistant.common.ok"),
            okButtonX + 15,
            buttonY + 6,
            0xFFFFFFFF,
            true);

        // Cancel button
        int cancelButtonX = (this.width / 2) + 10;
        boolean cancelHovered =
                mouseX >= cancelButtonX && mouseX < cancelButtonX + 50 && mouseY >= buttonY && mouseY < buttonY + 20;
        int cancelColor = cancelHovered ? 0xFFFF5555 : 0xFFCC0000;
        context.fill(cancelButtonX, buttonY, cancelButtonX + 50, buttonY + 20, cancelColor);
        context.text(
            this.font,
            Component.translatable("loom-assistant.common.cancel"),
            cancelButtonX + 5,
            buttonY + 6,
            0xFFFFFFFF,
            true);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return false;
        double mouseX = event.x();
        double mouseY = event.y();

        int editorX = (this.width - EDITOR_WIDTH) / 2;
        int editorY = (this.height - EDITOR_HEIGHT) / 2;
        int buttonY = editorY + EDITOR_HEIGHT + PADDING;

        // OK button
        int okButtonX = (this.width / 2) - 60;
        if (mouseX >= okButtonX && mouseX < okButtonX + 50 && mouseY >= buttonY && mouseY < buttonY + 20) {
            renameBanner();
            return true;
        }

        // Cancel button
        int cancelButtonX = (this.width / 2) + 10;
        if (mouseX >= cancelButtonX && mouseX < cancelButtonX + 50 && mouseY >= buttonY && mouseY < buttonY + 20) {
            this.minecraft.gui.setScreen(previousScreen);
            return true;
        }

        // Click in text area
        int textStartX = editorX + PADDING;
        int textStartY = editorY + PADDING;
        if (mouseX >= textStartX
                && mouseX < textStartX + EDITOR_WIDTH - (PADDING * 2)
                && mouseY >= textStartY
                && mouseY < textStartY + 20) {
            cursorPos = textBuffer.length();
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        String text = textBuffer.toString();

        if (keyCode == 259) { // Backspace
            if (cursorPos > 0) {
                textBuffer.deleteCharAt(cursorPos - 1);
                cursorPos--;
            }
            return true;
        } else if (keyCode == 261) { // Delete
            if (cursorPos < text.length()) {
                textBuffer.deleteCharAt(cursorPos);
            }
            return true;
        } else if (keyCode == 262) { // Right arrow
            if (cursorPos < text.length()) {
                cursorPos++;
            }
            return true;
        } else if (keyCode == 263) { // Left arrow
            if (cursorPos > 0) {
                cursorPos--;
            }
            return true;
        } else if (keyCode == 257) { // Enter
            renameBanner();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char chr = (char) event.codepoint();
        if (chr >= 32 && chr <= 126) { // Printable ASCII
            textBuffer.insert(cursorPos, chr);
            cursorPos++;
            return true;
        }
        return false;
    }

    private void renameBanner() {
        String newName = textBuffer.toString().trim();
        if (newName.isEmpty()) {
            LoomAssistantMod.LOGGER.warn("Banner name cannot be empty");
            return;
        }

        BannerStorage.getInstance().renameBanner(bannerId, newName);
        LoomAssistantMod.LOGGER.info("Banner renamed to: {}", newName);
        this.minecraft.gui.setScreen(previousScreen);
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
