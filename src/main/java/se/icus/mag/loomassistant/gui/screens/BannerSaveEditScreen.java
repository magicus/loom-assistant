/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.screens;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import se.icus.mag.loomassistant.gui.panel.LoomRecipePanel;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeCategories;
import se.icus.mag.loomassistant.recipe.BannerRecipeCategory;

/**
 * Simple modal for naming a banner and selecting category before save/edit.
 */
public class BannerSaveEditScreen extends Screen {
    private static final int PANEL_W = 250;
    private static final int PANEL_H_BASE = 130;
    private static final int NOTICE_EXTRA_H = 20;

    private final Screen previousScreen;
    private final LoomRecipePanel panel;
    private final boolean editMode;
    private final boolean readOnlySource;
    private final List<BannerRecipeCategory> categories;

    private EditBox nameBox;
    private String selectedCategoryId;

    public BannerSaveEditScreen(Screen previousScreen, LoomRecipePanel panel, boolean editMode) {
        super(Component.translatable(
                editMode
                        ? "loom-assistant.screen.save_edit.title_edit"
                        : "loom-assistant.screen.save_edit.title_save"));
        this.previousScreen = previousScreen;
        this.panel = panel;
        this.editMode = editMode;
        this.readOnlySource = editMode && panel.isActiveBannerFromReadOnlySource();
        this.categories = BannerRecipeCategories.getCategories();
    }

    private int panelH() {
        return readOnlySource ? PANEL_H_BASE + NOTICE_EXTRA_H : PANEL_H_BASE;
    }

    @Override
    protected void init() {
        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - panelH()) / 2;

        String initialName = panel.getActiveBannerDialogName(editMode);
        this.selectedCategoryId = normalizeCategory(panel.getActiveBannerDialogCategory(editMode));

        this.nameBox = this.addRenderableWidget(new EditBox(
                this.font,
                x + 16,
                y + 38,
                PANEL_W - 32,
                18,
                Component.translatable("loom-assistant.screen.save_edit.name")));
        this.nameBox.setMaxLength(64);
        this.nameBox.setValue(initialName);
        this.nameBox.setFocused(true);
        this.setFocused(this.nameBox);

        if (editMode) {
            this.nameBox.setCursorPosition(initialName.length());
            this.nameBox.setHighlightPos(initialName.length());
        } else {
            this.nameBox.setCursorPosition(initialName.length());
            this.nameBox.setHighlightPos(0);
        }

        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleCategory(-1))
                .bounds(x + 16, y + 74, 20, 18)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleCategory(1))
                .bounds(x + PANEL_W - 36, y + 74, 20, 18)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable(
                                editMode ? "loom-assistant.tooltip.edit_recipe" : "loom-assistant.tooltip.add_recipe"),
                        button -> applyAndClose())
                .bounds(x + 16, y + 102, 104, 20)
                .build());

        this.addRenderableWidget(
                Button.builder(Component.translatable("loom-assistant.common.cancel"), button -> this.onClose())
                        .bounds(x + PANEL_W - 120, y + 102, 104, 20)
                        .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - panelH()) / 2;

        graphics.fill(x, y, x + PANEL_W, y + panelH(), 0xFF222222);
        graphics.outline(x, y, PANEL_W, panelH(), 0xFFFFFFFF);

        graphics.text(this.font, this.title, x + 16, y + 12, 0xFFFFFFFF, false);
        graphics.text(
                this.font,
                Component.translatable("loom-assistant.screen.save_edit.name"),
                x + 16,
                y + 28,
                0xFFDDDDDD,
                false);
        graphics.text(
                this.font,
                Component.translatable("loom-assistant.screen.save_edit.category"),
                x + 16,
                y + 64,
                0xFFDDDDDD,
                false);

        Component category = Component.literal(BannerRecipeCategories.getLocalizedDescription(selectedCategoryId));
        int categoryX = x + (PANEL_W - this.font.width(category)) / 2;
        graphics.text(this.font, category, categoryX, y + 79, 0xFFFFFFFF, false);

        if (readOnlySource) {
            graphics.text(
                    this.font,
                    Component.translatable("loom-assistant.screen.save_edit.read_only_notice"),
                    x + 16,
                    y + PANEL_H_BASE + 4,
                    0xFFFFAA44,
                    false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            applyAndClose();
            return true;
        }
        if (this.nameBox != null && this.nameBox.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.nameBox != null && this.nameBox.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    private void cycleCategory(int delta) {
        if (categories.isEmpty()) {
            selectedCategoryId = BannerRecipe.DEFAULT_CATEGORY;
            return;
        }

        int idx = indexOfCategory(selectedCategoryId);
        if (idx < 0) {
            idx = indexOfCategory(BannerRecipe.DEFAULT_CATEGORY);
        }
        if (idx < 0) {
            idx = 0;
        }

        idx = (idx + delta + categories.size()) % categories.size();
        selectedCategoryId = categories.get(idx).id();
    }

    private int indexOfCategory(String id) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeCategory(String id) {
        if (id == null || id.isBlank()) {
            return BannerRecipe.DEFAULT_CATEGORY;
        }
        return indexOfCategory(id) >= 0 ? id : BannerRecipe.DEFAULT_CATEGORY;
    }

    private void applyAndClose() {
        String name = this.nameBox == null ? "" : this.nameBox.getValue();
        panel.applyActiveBannerMetadata(name, selectedCategoryId);
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
