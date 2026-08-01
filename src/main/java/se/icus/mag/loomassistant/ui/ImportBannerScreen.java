/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
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
 * Screen for importing banner JSON data with a text editor.
 */
public class ImportBannerScreen extends Screen {
    private static final int EDITOR_WIDTH = 300;
    private static final int EDITOR_HEIGHT = 200;
    private static final int PADDING = 10;

    private final Screen previousScreen;
    private StringBuilder textBuffer = new StringBuilder();
    private int cursorPos = 0;
    private int scrollOffset = 0;

    public ImportBannerScreen(Screen previousScreen) {
        super(Component.literal("Import Banner"));
        this.previousScreen = previousScreen;
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
                Component.literal("Paste Banner JSON or /give Command"),
                this.width / 2 - 120,
                editorY - 20,
                0xFFFFFFFF,
                true);

        // Draw text content with scrolling
        int textStartX = editorX + PADDING;
        int textStartY = editorY + PADDING;
        int textWidth = EDITOR_WIDTH - (PADDING * 2);
        int textHeight = EDITOR_HEIGHT - (PADDING * 2) - 25;

        // Enable scissor test to clip text to editor bounds
        context.enableScissor(textStartX, textStartY, textStartX + textWidth, textStartY + textHeight);

        String text = textBuffer.toString();
        String[] lines = text.split("\n", -1);

        int lineY = textStartY - scrollOffset;
        for (int i = 0; i < lines.length; i++) {
            if (lineY > textStartY + textHeight) break;
            if (lineY + 10 > textStartY) {
                context.text(this.font, Component.literal(lines[i]), textStartX, lineY, 0xFFFFFFFF, true);
            }
            lineY += 10;
        }

        // Draw cursor
        if (cursorPos >= 0 && cursorPos <= text.length()) {
            String beforeCursor = text.substring(0, cursorPos);
            int cursorX = textStartX + this.font.width(beforeCursor.split("\n")[beforeCursor.split("\n").length - 1]);
            int cursorLineNum = beforeCursor.split("\n", -1).length - 1;
            int cursorY = textStartY + (cursorLineNum * 10) - scrollOffset;

            if (cursorY >= textStartY && cursorY <= textStartY + textHeight) {
                context.fill(cursorX, cursorY, cursorX + 1, cursorY + 10, 0xFFFFFFFF);
            }
        }

        context.disableScissor();

        // Draw buttons
        int buttonY = editorY + EDITOR_HEIGHT + PADDING;
        int buttonWidth = 50;
        int buttonSpacing = 10;

        // OK button
        int okButtonX = (this.width / 2) - buttonWidth - (buttonSpacing / 2);
        boolean okHovered =
                mouseX >= okButtonX && mouseX < okButtonX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 20;
        int okColor = okHovered ? 0xFF66BB6A : 0xFF2E7D32;
        context.fill(okButtonX, buttonY, okButtonX + buttonWidth, buttonY + 20, okColor);
        String okText = "OK";
        int okTextWidth = this.font.width(okText);
        context.text(
                this.font,
                Component.literal(okText),
                okButtonX + (buttonWidth - okTextWidth) / 2,
                buttonY + 6,
                0xFFFFFFFF,
                true);

        // Cancel button
        int cancelButtonX = (this.width / 2) + (buttonSpacing / 2);
        boolean cancelHovered = mouseX >= cancelButtonX
                && mouseX < cancelButtonX + buttonWidth
                && mouseY >= buttonY
                && mouseY < buttonY + 20;
        int cancelColor = cancelHovered ? 0xFFFF6B6B : 0xFFCC0000;
        context.fill(cancelButtonX, buttonY, cancelButtonX + buttonWidth, buttonY + 20, cancelColor);
        String cancelText = "Cancel";
        int cancelTextWidth = this.font.width(cancelText);
        context.text(
                this.font,
                Component.literal(cancelText),
                cancelButtonX + (buttonWidth - cancelTextWidth) / 2,
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
        int buttonWidth = 50;
        int buttonSpacing = 10;

        // OK button
        int okButtonX = (this.width / 2) - buttonWidth - (buttonSpacing / 2);
        if (mouseX >= okButtonX && mouseX < okButtonX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 20) {
            importBanner();
            return true;
        }

        // Cancel button
        int cancelButtonX = (this.width / 2) + (buttonSpacing / 2);
        if (mouseX >= cancelButtonX
                && mouseX < cancelButtonX + buttonWidth
                && mouseY >= buttonY
                && mouseY < buttonY + 20) {
            this.minecraft.gui.setScreen(previousScreen);
            return true;
        }

        // Click in text area
        int textStartX = editorX + PADDING;
        int textStartY = editorY + PADDING;
        int textWidth = EDITOR_WIDTH - (PADDING * 2);
        int textHeight = EDITOR_HEIGHT - (PADDING * 2) - 25;

        if (mouseX >= textStartX
                && mouseX < textStartX + textWidth
                && mouseY >= textStartY
                && mouseY < textStartY + textHeight) {
            // Set cursor position (simplified - just go to end for now)
            cursorPos = textBuffer.length();
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        String text = textBuffer.toString();

        if (event.isPaste()) {
            String clipboard = this.minecraft.keyboardHandler.getClipboard();
            if (clipboard != null && !clipboard.isEmpty()) {
                textBuffer.insert(cursorPos, clipboard);
                cursorPos += clipboard.length();
            }
            return true;
        }

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
        } else if (keyCode == 265) { // Up arrow
            // Move cursor up one line
            int lineStart = text.lastIndexOf('\n', cursorPos - 1);
            if (lineStart == -1) lineStart = 0;
            int prevLineStart = text.lastIndexOf('\n', lineStart - 1);
            if (prevLineStart == -1) prevLineStart = 0;
            int colInLine = cursorPos - lineStart - 1;
            cursorPos = Math.max(prevLineStart, prevLineStart + colInLine);
            return true;
        } else if (keyCode == 264) { // Down arrow
            // Move cursor down one line
            int lineStart = text.lastIndexOf('\n', cursorPos - 1);
            if (lineStart == -1) lineStart = 0;
            int nextLineStart = text.indexOf('\n', cursorPos);
            if (nextLineStart == -1) return true;
            int nextNextLineStart = text.indexOf('\n', nextLineStart + 1);
            if (nextNextLineStart == -1) nextNextLineStart = text.length();
            int colInLine = cursorPos - lineStart - 1;
            cursorPos = Math.min(nextNextLineStart, nextLineStart + 1 + colInLine);
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
        } else if (chr == '\n' || chr == '\r') { // Enter
            textBuffer.insert(cursorPos, '\n');
            cursorPos++;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int editorX = (this.width - EDITOR_WIDTH) / 2;
        int editorY = (this.height - EDITOR_HEIGHT) / 2;
        int textStartX = editorX + PADDING;
        int textStartY = editorY + PADDING;
        int textWidth = EDITOR_WIDTH - (PADDING * 2);
        int textHeight = EDITOR_HEIGHT - (PADDING * 2) - 25;

        // Check if mouse is over the text editor
        if (mouseX >= textStartX
                && mouseX < textStartX + textWidth
                && mouseY >= textStartY
                && mouseY < textStartY + textHeight) {
            // Scroll by 10 pixels per scroll
            scrollOffset -= (int) (verticalAmount * 10);

            // Calculate max scroll
            String text = textBuffer.toString();
            String[] lines = text.split("\n", -1);
            int totalHeight = lines.length * 10;
            int maxScroll = Math.max(0, totalHeight - textHeight);

            // Clamp scroll offset
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            return true;
        }

        return false;
    }

    private void importBanner() {
        String input = textBuffer.toString().trim();
        if (input.isEmpty()) {
            LoomAssistantMod.LOGGER.warn("Import text is empty");
            return;
        }

        BannerStorage storage = BannerStorage.getInstance();
        if (storage.importBannerFromJson(input) != null) {
            LoomAssistantMod.LOGGER.info("Banner imported successfully");
            this.minecraft.gui.setScreen(previousScreen);
        } else {
            String format = input.startsWith("/give") ? "/give command" : "JSON";
            LoomAssistantMod.LOGGER.error("Failed to import banner - invalid {}", format);
        }
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
