/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.screens;

import java.net.URI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import se.icus.mag.loomassistant.LoomScreenStateManager;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;
import se.icus.mag.loomassistant.gui.screens.packdownload.BannerPackDownloadManagementScreen;
import se.icus.mag.loomassistant.gui.screens.packselection.BannerPackSelectionScreen;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.converters.BannerRecipeCommandConverter;
import se.icus.mag.loomassistant.recipe.converters.BannerRecipeItemConverter;
import se.icus.mag.loomassistant.recipe.converters.UrlExport;
import se.icus.mag.loomassistant.recipe.converters.parsers.urlparsers.UrlParser;

public class ManageBannerRecipesScreen extends Screen {
    private static final int MAX_PANEL_W = 360;
    private static final int SCREEN_MARGIN = 6;
    private static final int PANEL_PADDING = 6;

    private static final int LABEL_H = 9;
    private static final int INPUT_H = 18;
    private static final int STATUS_H = 9;
    private static final int BTN_H = 18;
    private static final int BTN_GAP = 4;
    private static final int PREVIEW_BOX = 18;

    private static final int LINE_GAP = 2;
    private static final int SECTION_GAP = 3;

    private static final int IMPORT_BTN_W = 88;
    private static final int COPY_BTN_W = 72;
    private static final int DONE_BTN_W = 72;
    private static final int DONE_TOP_GAP = BTN_H + LINE_GAP;

    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int BG_DARK = 0xFF555555;
    private static final int BG_LIGHT = 0xFFFFFFFF;
    private static final int TEXT_COLOR = 0xFF000000;
    private static final int TEXT_DISABLED = 0xFF888888;
    private static final int OK_COLOR = 0xFF2E7D32;
    private static final int ERROR_COLOR = 0xFFB00020;

    private final Screen previousScreen;
    private final LoomScreenStateManager manager;

    private EditBox importBox;
    private EditBox exportBox;
    private Button importButton;
    private Button copyButton;
    private Button exportPlanetMcButton;
    private Button exportMinecraftToolsButton;
    private Button exportSkimMcButton;
    private Button exportNeedCoolerShoesButton;

    private BannerRecipeCommandConverter.CommandParseResult importParseResult =
            new BannerRecipeCommandConverter.CommandParseResult(null, "Invalid syntax", null);
    private boolean copiedFeedback;

    private String planetMcUrl;
    private String minecraftToolsUrl;
    private String skimMcUrl;
    private String needCoolerShoesUrl;

    public ManageBannerRecipesScreen(Screen previousScreen, LoomScreenStateManager manager) {
        super(Component.translatable("loom-assistant.screen.import_export.title"));
        this.previousScreen = previousScreen;
        this.manager = manager;
    }

    @Override
    protected void init() {
        Layout layout = layout();

        this.importBox = this.addRenderableWidget(new EditBox(
                this.font, layout.contentX(), layout.importInputY(), layout.contentW(), INPUT_H, Component.empty()));
        this.importBox.setMaxLength(2048);
        this.importBox.setResponder(this::onImportTextChanged);
        this.importBox.setFocused(true);
        this.setFocused(this.importBox);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.import_export.paste"), b -> pasteFromClipboard())
                .bounds(layout.importPasteX(), layout.importActionsY(), layout.importPasteW(), BTN_H)
                .build());

        this.importButton = this.addRenderableWidget(
                Button.builder(Component.translatable("loom-assistant.screen.import_export.import"), b -> doImport())
                        .bounds(layout.importSubmitX(), layout.importActionsY(), layout.importSubmitW(), BTN_H)
                        .build());
        this.importButton.active = false;

        this.exportBox = this.addRenderableWidget(new EditBox(
                this.font, layout.contentX(), layout.exportInputY(), layout.contentW(), INPUT_H, Component.empty()));
        this.exportBox.setMaxLength(2048);
        this.exportBox.active = false;

        this.copyButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.import_export.copy"), b -> copyToClipboard())
                .bounds(layout.exportCopyX(), layout.exportActionsY(), layout.exportCopyW(), BTN_H)
                .build());

        this.exportPlanetMcButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.import_export.export_to_planetmc"),
                        b -> openUrl(planetMcUrl))
                .bounds(layout.exportTopLeftX(), layout.exportTopLinksY(), layout.halfButtonW(), BTN_H)
                .build());

        this.exportMinecraftToolsButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.import_export.export_to_minecraft_tools"),
                        b -> openUrl(minecraftToolsUrl))
                .bounds(layout.exportTopRightX(), layout.exportTopLinksY(), layout.halfButtonW(), BTN_H)
                .build());

        this.exportSkimMcButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.import_export.export_to_skimmc"),
                        b -> openUrl(skimMcUrl))
                .bounds(layout.exportBottomLeftX(), layout.exportBottomLinksY(), layout.halfButtonW(), BTN_H)
                .build());

        this.exportNeedCoolerShoesButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.import_export.export_to_needcoolershoes"),
                        b -> openUrl(needCoolerShoesUrl))
                .bounds(layout.exportBottomRightX(), layout.exportBottomLinksY(), layout.halfButtonW(), BTN_H)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.import_export.select_packs"),
                        b -> openBannerPackManager())
                .bounds(layout.manageLeftX(), layout.manageButtonsY(), layout.halfButtonW(), BTN_H)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.import_export.download_packs"),
                        b -> openPackDownloadManager())
                .bounds(layout.manageRightX(), layout.manageButtonsY(), layout.halfButtonW(), BTN_H)
                .build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(layout.doneButtonX(), layout.doneButtonY(), layout.doneButtonW(), BTN_H)
                .build());

        updateExportWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        updateExportWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);
        Layout layout = layout();

        drawPanel(graphics, layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH());
        drawSeparator(graphics, layout.contentX(), layout.importSeparatorY(), layout.contentW());
        drawSeparator(graphics, layout.contentX(), layout.exportSeparatorY(), layout.contentW());

        graphics.text(
                this.font,
                Component.translatable("loom-assistant.screen.import_export.import_label"),
                layout.contentX(),
                layout.importLabelY(),
                TEXT_COLOR,
                false);

        String statusText = getImportStatusText();
        if (statusText != null) {
            int color = importParseResult.recipe() != null ? OK_COLOR : ERROR_COLOR;
            graphics.text(
                    this.font, Component.literal(statusText), layout.contentX(), layout.importStatusY(), color, false);
        }

        graphics.text(
                this.font,
                Component.translatable("loom-assistant.screen.import_export.export_label"),
                layout.contentX(),
                layout.exportLabelY(),
                TEXT_COLOR,
                false);

        if (copiedFeedback && hasExportTarget()) {
            String copied = Component.translatable("loom-assistant.screen.import_export.copied")
                    .getString();
            int copiedX = layout.exportCopyX() - this.font.width(copied) - BTN_GAP;
            graphics.text(
                    this.font,
                    Component.translatable("loom-assistant.screen.import_export.copied"),
                    copiedX,
                    layout.exportLabelY(),
                    OK_COLOR,
                    false);
        } else if (!hasExportTarget()) {
            int statusX = layout.contentX()
                    + this.font.width(Component.translatable("loom-assistant.screen.import_export.export_label")
                            .getString())
                    + BTN_GAP;
            graphics.text(
                    this.font,
                    Component.translatable("loom-assistant.screen.import_export.select_banner_to_export"),
                    statusX,
                    layout.exportLabelY(),
                    TEXT_DISABLED,
                    false);
        }

        graphics.text(
                this.font,
                Component.translatable("loom-assistant.screen.import_export.manage_label"),
                layout.contentX(),
                layout.manageLabelY(),
                TEXT_COLOR,
                false);

        renderImportPreview(graphics, layout.previewX(), layout.importPreviewY(), mouseX, mouseY);
        renderExportPreview(graphics, layout.previewX(), layout.exportPreviewY(), mouseX, mouseY);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderImportPreview(GuiGraphicsExtractor ctx, int x, int y, int mouseX, int mouseY) {
        BannerRecipe recipe = importParseResult.recipe();
        if (recipe == null) return;

        ItemStack stack = BannerRecipeItemConverter.toItem(this.minecraft, recipe);
        drawPreviewSlot(ctx, x, y);
        ctx.item(stack, x + 1, y + 1);
        if (isIn(mouseX, mouseY, x, y, PREVIEW_BOX, PREVIEW_BOX)) {
            ctx.setTooltipForNextFrame(this.font, Component.literal(recipe.getDisplayName()), mouseX, mouseY, null);
        }
    }

    private void renderExportPreview(GuiGraphicsExtractor ctx, int x, int y, int mouseX, int mouseY) {
        BannerRecipe recipe = manager.getEffectiveActiveBanner();
        if (recipe == null) return;

        ItemStack stack = BannerRecipeItemConverter.toItem(this.minecraft, recipe);
        drawPreviewSlot(ctx, x, y);
        ctx.item(stack, x + 1, y + 1);
        if (isIn(mouseX, mouseY, x, y, PREVIEW_BOX, PREVIEW_BOX)) {
            ctx.setTooltipForNextFrame(this.font, Component.literal(recipe.getDisplayName()), mouseX, mouseY, null);
        }
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

    private void drawSeparator(GuiGraphicsExtractor ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, BG_DARK);
    }

    private void onImportTextChanged(String text) {
        copiedFeedback = false;
        String trimmed = text == null ? "" : text.strip();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            String error = UrlParser.checkParseUrl(trimmed);
            BannerRecipe recipe = error == null ? UrlParser.parseUrl(trimmed) : null;
            importParseResult = new BannerRecipeCommandConverter.CommandParseResult(recipe, error, null);
        } else {
            importParseResult = BannerRecipeCommandConverter.parseCommandDetailed(text);
        }
        importButton.active = importParseResult.recipe() != null;
    }

    private void updateExportWidgets() {
        BannerRecipe recipe = manager.getEffectiveActiveBanner();
        String command = recipe == null ? "" : buildExportCommand(recipe);
        if (exportBox != null && !command.equals(exportBox.getValue())) {
            exportBox.setValue(command);
        }

        if (recipe == null) {
            planetMcUrl = null;
            minecraftToolsUrl = null;
            skimMcUrl = null;
            needCoolerShoesUrl = null;
        } else {
            planetMcUrl = UrlExport.toPlanetMinecraft(recipe);
            minecraftToolsUrl = UrlExport.toMinecraftTools(recipe);
            skimMcUrl = UrlExport.toSkimMc(recipe);
            needCoolerShoesUrl = UrlExport.toNeedCoolerShoes(recipe);
        }

        if (copyButton != null) copyButton.active = recipe != null;
        if (exportPlanetMcButton != null) exportPlanetMcButton.active = planetMcUrl != null;
        if (exportMinecraftToolsButton != null) exportMinecraftToolsButton.active = minecraftToolsUrl != null;
        if (exportSkimMcButton != null) exportSkimMcButton.active = skimMcUrl != null;
        if (exportNeedCoolerShoesButton != null) exportNeedCoolerShoesButton.active = needCoolerShoesUrl != null;
    }

    private String getImportStatusText() {
        if (importBox == null) return null;

        String value = importBox.getValue();
        if (value == null || value.isBlank()) return null;

        if (importParseResult.recipe() != null) {
            return Component.translatable("loom-assistant.screen.import_export.ok")
                    .getString();
        }
        if (importParseResult.errorPosition() != null) {
            return Component.translatable(
                            "loom-assistant.screen.import_export.error_at_pos", importParseResult.errorPosition())
                    .getString();
        }

        String message = importParseResult.errorMessage();
        if (message == null || message.isBlank() || "Invalid syntax".equals(message)) {
            return Component.translatable("loom-assistant.screen.import_export.invalid_syntax")
                    .getString();
        }
        return message;
    }

    private void pasteFromClipboard() {
        if (this.minecraft == null) return;

        String clipboard = this.minecraft.keyboardHandler.getClipboard();
        if (clipboard == null || clipboard.isBlank()) return;

        importBox.setValue(clipboard.trim());
        importBox.setCursorPosition(importBox.getValue().length());
    }

    private void doImport() {
        BannerRecipe recipe = importParseResult.recipe();
        if (recipe == null || this.minecraft == null) return;

        String initialName = recipe.getName();
        String initialCategory = recipe.getCategory();
        this.minecraft.gui.setScreen(new BannerDetailsScreen(
                previousScreen,
                BannerDetailsScreen.Mode.IMPORT,
                initialName,
                initialCategory,
                (name, category) -> manager.importBannerWithMetadata(recipe, name, category)));
    }

    private void copyToClipboard() {
        if (this.minecraft == null || !hasExportTarget() || exportBox == null) return;

        this.minecraft.keyboardHandler.setClipboard(exportBox.getValue());
        copiedFeedback = true;
    }

    private void openUrl(String url) {
        if (url == null || url.isBlank()) return;

        Util.getPlatform().openUri(URI.create(url));
        copiedFeedback = false;
    }

    private boolean hasExportTarget() {
        return manager.getEffectiveActiveBanner() != null;
    }

    private static String buildExportCommand(BannerRecipe recipe) {
        BannerRecipeCommandConverter converter = new BannerRecipeCommandConverter();
        return converter.fromRecipe(recipe);
    }

    private void openBannerPackManager() {
        BannerStorage storage = BannerStorage.getInstance();
        if (storage.getRepository() == null) {
            storage.load();
        }
        this.minecraft.gui.setScreen(
                new BannerPackSelectionScreen(storage.getRepository(), storage.getActivePacksConfig(), this));
    }

    private void openPackDownloadManager() {
        this.minecraft.gui.setScreen(new BannerPackDownloadManagementScreen(this));
    }

    private Layout layout() {
        int panelW = Math.min(MAX_PANEL_W, this.width - SCREEN_MARGIN * 2);
        panelW = Math.max(290, panelW);
        panelW = Math.min(panelW, this.width - 2);

        int contentW = panelW - PANEL_PADDING * 2;
        int panelH = Math.min(layoutContentHeight() + PANEL_PADDING * 2, this.height - 2);

        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        int contentX = panelX + PANEL_PADDING;

        int halfButtonW = (contentW - BTN_GAP) / 2;

        int y = panelY + PANEL_PADDING;
        int importLabelY = y;
        y += LABEL_H + LINE_GAP;
        int importInputY = y;
        y += INPUT_H + 1;
        int importStatusY = y;
        y += STATUS_H + LINE_GAP;
        int importActionsY = y;
        int importPreviewY = importActionsY;
        y += BTN_H + SECTION_GAP;
        int importSeparatorY = y;

        y += 1 + SECTION_GAP;
        int exportLabelY = y;
        y += LABEL_H + LINE_GAP;
        int exportInputY = y;
        y += INPUT_H + LINE_GAP;
        int exportActionsY = y;
        int exportPreviewY = exportActionsY;
        y += BTN_H + LINE_GAP;
        int exportTopLinksY = y;
        y += BTN_H + LINE_GAP;
        int exportBottomLinksY = y;
        y += BTN_H + SECTION_GAP;
        int exportSeparatorY = y;

        y += 1 + SECTION_GAP;
        int manageLabelY = y;
        y += LABEL_H + LINE_GAP;
        int manageButtonsY = y;
        y += BTN_H + DONE_TOP_GAP;
        int doneButtonY = y;

        int previewX = contentX;
        int importSubmitW = Math.min(IMPORT_BTN_W, Math.max(72, contentW / 3));
        int importSubmitX = contentX + contentW - importSubmitW;
        int importPasteX = previewX + PREVIEW_BOX + BTN_GAP;
        int importPasteW = Math.min(COPY_BTN_W, Math.max(64, contentW / 4));

        int exportCopyW = Math.min(COPY_BTN_W, Math.max(64, contentW / 4));
        int exportCopyX = contentX + PREVIEW_BOX + BTN_GAP;

        return new Layout(
                panelX,
                panelY,
                panelW,
                panelH,
                contentX,
                contentW,
                importLabelY,
                importInputY,
                importStatusY,
                importActionsY,
                importPreviewY,
                importPasteX,
                importPasteW,
                importSubmitX,
                importSubmitW,
                importSeparatorY,
                exportLabelY,
                exportInputY,
                exportActionsY,
                exportPreviewY,
                exportCopyX,
                exportCopyW,
                exportTopLinksY,
                exportBottomLinksY,
                exportSeparatorY,
                manageLabelY,
                manageButtonsY,
                doneButtonY,
                halfButtonW,
                previewX);
    }

    private static int layoutContentHeight() {
        return LABEL_H
                + LINE_GAP
                + INPUT_H
                + 1
                + STATUS_H
                + LINE_GAP
                + BTN_H
                + SECTION_GAP
                + 1
                + SECTION_GAP
                + LABEL_H
                + LINE_GAP
                + INPUT_H
                + LINE_GAP
                + BTN_H
                + LINE_GAP
                + BTN_H
                + LINE_GAP
                + BTN_H
                + SECTION_GAP
                + 1
                + SECTION_GAP
                + LABEL_H
                + LINE_GAP
                + BTN_H
                + DONE_TOP_GAP
                + BTN_H;
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

    private record Layout(
            int panelX,
            int panelY,
            int panelW,
            int panelH,
            int contentX,
            int contentW,
            int importLabelY,
            int importInputY,
            int importStatusY,
            int importActionsY,
            int importPreviewY,
            int importPasteX,
            int importPasteW,
            int importSubmitX,
            int importSubmitW,
            int importSeparatorY,
            int exportLabelY,
            int exportInputY,
            int exportActionsY,
            int exportPreviewY,
            int exportCopyX,
            int exportCopyW,
            int exportTopLinksY,
            int exportBottomLinksY,
            int exportSeparatorY,
            int manageLabelY,
            int manageButtonsY,
            int doneButtonY,
            int halfButtonW,
            int previewX) {
        int exportTopLeftX() {
            return contentX;
        }

        int exportTopRightX() {
            return contentX + halfButtonW + BTN_GAP;
        }

        int exportBottomLeftX() {
            return contentX;
        }

        int exportBottomRightX() {
            return contentX + halfButtonW + BTN_GAP;
        }

        int manageLeftX() {
            return contentX;
        }

        int manageRightX() {
            return contentX + halfButtonW + BTN_GAP;
        }

        int doneButtonX() {
            return contentX + (contentW - DONE_BTN_W) / 2;
        }

        int doneButtonW() {
            return DONE_BTN_W;
        }
    }
}
