/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import se.icus.mag.loomassistant.types.recipe.BannerRecipe;

/**
 * Combined import/export screen for /give commands.
 * Import is on top; export is underneath and disabled when no active banner exists.
 */
public class BannerRecipeImportExportScreen extends Screen {
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 236;
    private static final int PAD = 10;
    private static final int HEADER_H = 18;
    private static final int SECTION_H = 90;
    private static final int SECTION_GAP = 8;
    private static final int PREVIEW_SIZE = 18;
    private static final int PREVIEW_BOX = 28;
    private static final int INPUT_H = 18;
    private static final int BTN_H = 20;
    private static final int BTN_W = 72;

    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int BG_DARK = 0xFF555555;
    private static final int BG_LIGHT = 0xFFFFFFFF;
    private static final int TEXT_COLOR = 0xFF000000;
    private static final int TEXT_DIM = 0xFF555555;
    private static final int TEXT_DISABLED = 0xFF888888;
    private static final int OK_COLOR = 0xFF2E7D32;
    private static final int ERROR_COLOR = 0xFFB00020;

    private final Screen previousScreen;
    private final LoomPanel loomPanel;

    private EditBox importBox;
    private EditBox exportBox;
    private Button pasteButton;
    private Button importButton;
    private Button copyButton;

    private BannerRecipe.CommandParseResult importParseResult = new BannerRecipe.CommandParseResult(
            null, "Invalid syntax", null);
    private boolean copiedFeedback;

    public BannerRecipeImportExportScreen(Screen previousScreen, LoomPanel loomPanel) {
        super(Component.translatable("loom-assistant.screen.import_export.title"));
        this.previousScreen = previousScreen;
        this.loomPanel = loomPanel;
    }

    private int panelX() {
        return (this.width - PANEL_W) / 2;
    }

    private int panelY() {
        return (this.height - PANEL_H) / 2;
    }

    private int importY() {
        return panelY() + HEADER_H + PAD;
    }

    private int exportY() {
        return importY() + SECTION_H + SECTION_GAP;
    }

    @Override
    protected void init() {
        int px = panelX();

        this.importBox = this.addRenderableWidget(new EditBox(
                this.font,
                px + PAD,
                importY() + 38,
                PANEL_W - PAD * 2,
                INPUT_H,
                Component.empty()));
        this.importBox.setMaxLength(2048);
        this.importBox.setResponder(this::onImportTextChanged);
        this.importBox.setFocused(true);
        this.setFocused(this.importBox);

        int buttonY = importY() + SECTION_H - BTN_H - 6;
        this.pasteButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.import_export.paste"),
                        b -> pasteFromClipboard())
                .bounds(px + PAD, buttonY, BTN_W, BTN_H)
                .build());

        this.importButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.import_export.import"),
                        b -> doImport())
                .bounds(px + PANEL_W - PAD - BTN_W, buttonY, BTN_W, BTN_H)
                .build());
        this.importButton.active = false;

        this.exportBox = this.addRenderableWidget(new EditBox(
                this.font,
                px + PAD + PREVIEW_BOX + 8,
                exportY() + 38,
                PANEL_W - (PAD * 2 + PREVIEW_BOX + 8),
                INPUT_H,
                Component.empty()));
        this.exportBox.setMaxLength(2048);
        this.exportBox.active = false;
        this.exportBox.setValue(buildExportCommand());

        this.copyButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.import_export.copy"),
                        b -> copyToClipboard())
                .bounds(px + PANEL_W - PAD - BTN_W, exportY() + SECTION_H - BTN_H - 6, BTN_W, BTN_H)
                .build());
        this.copyButton.active = hasExportTarget();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        int px = panelX();
        int py = panelY();
        int iy = importY();
        int ey = exportY();

        drawPanel(ctx, px, py, PANEL_W, PANEL_H);
        ctx.text(this.font, this.title, px + PAD, py + 7, TEXT_COLOR, false);

        drawSectionFrame(ctx, px + PAD, iy, PANEL_W - PAD * 2, SECTION_H);
        drawSectionFrame(ctx, px + PAD, ey, PANEL_W - PAD * 2, SECTION_H);

        ctx.text(
                this.font,
                Component.translatable("loom-assistant.screen.import_export.import_label"),
                px + PAD + 4,
                iy + 6,
                TEXT_COLOR,
                false);
        ctx.text(
                this.font,
                Component.translatable("loom-assistant.screen.import_export.export_label"),
                px + PAD + 4,
                ey + 6,
                TEXT_COLOR,
                false);

        ctx.text(
                this.font,
                Component.translatable("loom-assistant.screen.import_export.import_command_label"),
                px + PAD,
                iy + 22,
                TEXT_DIM,
                false);
        ctx.text(
                this.font,
                Component.translatable("loom-assistant.screen.import_export.export_command_label"),
                px + PAD + PREVIEW_BOX + 8,
                ey + 22,
                TEXT_DIM,
                false);

        renderImportPreview(ctx, px + PAD, iy + 58, mouseX, mouseY);
        renderExportPreview(ctx, px + PAD, ey + 58, mouseX, mouseY);

        String statusText = getImportStatusText();
        if (statusText != null) {
            int color = importParseResult.recipe() != null ? OK_COLOR : ERROR_COLOR;
            ctx.text(this.font, Component.literal(statusText), px + PAD + PREVIEW_BOX + 8, iy + 60, color, false);
        }

        if (!hasExportTarget()) {
            ctx.text(
                    this.font,
                    Component.translatable("loom-assistant.screen.import_export.select_banner_to_export"),
                    px + PAD + PREVIEW_BOX + 8,
                    ey + 60,
                    TEXT_DISABLED,
                    false);
        } else if (copiedFeedback) {
            ctx.text(
                    this.font,
                    Component.translatable("loom-assistant.screen.import_export.copied"),
                    px + PAD + PREVIEW_BOX + 8,
                    ey + 60,
                    OK_COLOR,
                    false);
        }

        super.extractRenderState(ctx, mouseX, mouseY, delta);
    }

    private void renderImportPreview(GuiGraphicsExtractor ctx, int x, int y, int mouseX, int mouseY) {
        BannerRecipe recipe = importParseResult.recipe();
        if (recipe == null) {
            drawPreviewSlot(ctx, x, y);
            return;
        }

        ItemStack stack = BannerPreviewRenderer.createBannerWithPatterns(recipe);
        ctx.item(stack, x + 6, y + 6);
        if (isIn(mouseX, mouseY, x, y, PREVIEW_BOX, PREVIEW_BOX)) {
            ctx.setTooltipForNextFrame(this.font, Component.literal(recipe.getDisplayName()), mouseX, mouseY, null);
        }
        ctx.text(this.font, Component.literal(recipe.getDisplayName()), x + PREVIEW_BOX + 8, y + 5, TEXT_COLOR, false);
    }

    private void renderExportPreview(GuiGraphicsExtractor ctx, int x, int y, int mouseX, int mouseY) {
        BannerRecipe recipe = loomPanel != null ? loomPanel.getActiveBannerRecipe() : null;
        if (recipe == null) {
            drawPreviewSlot(ctx, x, y);
            return;
        }

        ItemStack stack = BannerPreviewRenderer.createBannerWithPatterns(recipe);
        ctx.item(stack, x + 6, y + 6);
        if (isIn(mouseX, mouseY, x, y, PREVIEW_BOX, PREVIEW_BOX)) {
            ctx.setTooltipForNextFrame(this.font, Component.literal(recipe.getDisplayName()), mouseX, mouseY, null);
        }
        ctx.text(this.font, Component.literal(recipe.getDisplayName()), x + PREVIEW_BOX + 8, y + 5, TEXT_COLOR, false);
    }

    private void drawPreviewSlot(GuiGraphicsExtractor ctx, int x, int y) {
        ctx.fill(x, y, x + PREVIEW_BOX, y + PREVIEW_BOX, BG_COLOR);
        ctx.fill(x, y, x + PREVIEW_BOX, y + 1, BG_LIGHT);
        ctx.fill(x, y, x + 1, y + PREVIEW_BOX, BG_LIGHT);
        ctx.fill(x, y + PREVIEW_BOX - 1, x + PREVIEW_BOX, y + PREVIEW_BOX, BG_DARK);
        ctx.fill(x + PREVIEW_BOX - 1, y, x + PREVIEW_BOX, y + PREVIEW_BOX, BG_DARK);
    }

    private void drawPanel(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, BG_COLOR);
        ctx.fill(x, y, x + w, y + 1, BG_LIGHT);
        ctx.fill(x, y, x + 1, y + h, BG_LIGHT);
        ctx.fill(x, y + h - 1, x + w, y + h, BG_DARK);
        ctx.fill(x + w - 1, y, x + w, y + h, BG_DARK);
    }

    private void drawSectionFrame(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        drawPanel(ctx, x, y, w, h);
    }

    private void onImportTextChanged(String text) {
        copiedFeedback = false;
        importParseResult = BannerRecipe.parseCommandDetailed(text);
        importButton.active = importParseResult.recipe() != null;
    }

    private String getImportStatusText() {
        if (importBox == null) {
            return null;
        }
        String value = importBox.getValue();
        if (value == null || value.isBlank()) {
            return null;
        }
        if (importParseResult.recipe() != null) {
            return "OK";
        }
        if (importParseResult.errorPosition() != null) {
            return "Error at pos " + importParseResult.errorPosition();
        }
        return importParseResult.errorMessage() != null ? importParseResult.errorMessage() : "Invalid syntax";
    }

    private void pasteFromClipboard() {
        if (this.minecraft == null) {
            return;
        }
        String clipboard = this.minecraft.keyboardHandler.getClipboard();
        if (clipboard == null || clipboard.isBlank()) {
            return;
        }
        importBox.setValue(clipboard.trim());
        importBox.setCursorPosition(importBox.getValue().length());
    }

    private void doImport() {
        BannerRecipe recipe = importParseResult.recipe();
        if (recipe == null || loomPanel == null || this.minecraft == null) {
            return;
        }
        loomPanel.loadImportedBanner(recipe);
        this.minecraft.gui.setScreen(new BannerSaveEditScreen(previousScreen, loomPanel, false));
    }

    private boolean hasExportTarget() {
        return loomPanel != null && loomPanel.getActiveBannerRecipe() != null;
    }

    private String buildExportCommand() {
        BannerRecipe recipe = loomPanel != null ? loomPanel.getActiveBannerRecipe() : null;
        return recipe == null ? "" : recipe.toCommand();
    }

    private void copyToClipboard() {
        if (this.minecraft == null || !hasExportTarget()) {
            return;
        }
        this.minecraft.keyboardHandler.setClipboard(buildExportCommand());
        copiedFeedback = true;
    }

    private static boolean isIn(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
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
