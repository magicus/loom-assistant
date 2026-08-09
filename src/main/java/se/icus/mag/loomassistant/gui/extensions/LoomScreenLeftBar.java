/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.extensions;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import se.icus.mag.loomassistant.LoomScreenStateManager;
import se.icus.mag.loomassistant.gui.ScreenExtension;
import se.icus.mag.loomassistant.gui.screens.BannerDetailsScreen;
import se.icus.mag.loomassistant.gui.screens.ManageBannerRecipesScreen;
import se.icus.mag.loomassistant.gui.screens.colorswitch.BannerColorSwitchScreen;

public class LoomScreenLeftBar implements ScreenExtension {
    private static final int BG_LEFT_PADDING = 19;
    private static final int LEFT_STRIP_BUTTON_X = 3;
    private static final int LEFT_STRIP_RECIPE_Y = 5;
    private static final int LEFT_STRIP_ACTIVE_SLOT_Y = LEFT_STRIP_RECIPE_Y + 42;
    private static final int LEFT_STRIP_CRAFT_Y = LEFT_STRIP_ACTIVE_SLOT_Y + 22;
    private static final int LEFT_STRIP_SAVE_EDIT_Y = LEFT_STRIP_CRAFT_Y + 20;
    private static final int LEFT_STRIP_COLOR_Y = LEFT_STRIP_SAVE_EDIT_Y + 20;
    private static final int LEFT_STRIP_IMPORT_EXPORT_BOTTOM_MARGIN = 6;
    private static final int ACTIVE_SLOT_W = 20;
    private static final int ACTIVE_SLOT_H = 18;

    private static final Identifier RECIPE_WEAVE_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-weave.png");
    private static final Identifier RECIPE_ADD_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-add.png");
    private static final Identifier RECIPE_EDIT_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-edit.png");
    private static final Identifier RECIPE_SWAP_COLORS_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-swap-colors.png");
    private static final Identifier RECIPE_IMPORT_EXPORT_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-import-export.png");

    private static final SoundEvent ACTIVE_SLOT_SET_SOUND = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath("loom-assistant", "ui.active_slot_set"));
    private static final SoundEvent ACTIVE_SLOT_CLEAR_SOUND = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath("loom-assistant", "ui.active_slot_clear"));

    private static final Component SAVE_TOOLTIP = Component.translatable("loom-assistant.tooltip.add_recipe");
    private static final Component WEAVE_TOOLTIP = Component.translatable("loom-assistant.tooltip.weave");
    private static final Component EDIT_TOOLTIP = Component.translatable("loom-assistant.tooltip.edit_recipe");
    private static final Component IMPORT_EXPORT_TOOLTIP =
            Component.translatable("loom-assistant.tooltip.import_export_recipes");
    private static final Component CHANGE_COLORS_TOOLTIP =
            Component.translatable("loom-assistant.tooltip.replace_colors");

    private final LoomScreen screen;
    private final LoomScreenStateManager manager;
    private final Runnable panelVisibilityChangedCallback;

    private ImageButton recipeBookButton;
    private Button saveButton;
    private Button craftButton;
    private Button colorButton;
    private Button importExportButton;

    public LoomScreenLeftBar(
            LoomScreen screen, LoomScreenStateManager manager, Runnable panelVisibilityChangedCallback) {
        this.screen = screen;
        this.manager = manager;
        this.panelVisibilityChangedCallback = panelVisibilityChangedCallback;
    }

    public void init() {
        this.recipeBookButton = screen.addRenderableWidget(new ImageButton(
                getLeftStripButtonX(),
                screen.topPos + LEFT_STRIP_RECIPE_Y,
                20,
                18,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES,
                button -> {
                    manager.togglePanelOpen();
                    panelVisibilityChangedCallback.run();
                }));

        this.saveButton = screen.addRenderableWidget(new SaveEditButton());
        this.craftButton = screen.addRenderableWidget(new WeaveButton());
        this.craftButton.setOverrideRenderHighlightedSprite(this.craftButton::isHovered);
        this.importExportButton = screen.addRenderableWidget(new ImportExportButton());
        this.colorButton = screen.addRenderableWidget(new ReplaceColorButton());
        this.colorButton.setOverrideRenderHighlightedSprite(this.colorButton::isHovered);

        refreshLayout();
        updateButtonState();
    }

    public void refreshLayout() {
        if (recipeBookButton != null) {
            recipeBookButton.setPosition(getLeftStripButtonX(), screen.topPos + LEFT_STRIP_RECIPE_Y);
        }
        if (saveButton != null) {
            saveButton.setPosition(getLeftStripButtonX(), screen.topPos + LEFT_STRIP_SAVE_EDIT_Y + 1);
        }
        if (craftButton != null) {
            craftButton.setPosition(getLeftStripButtonX(), screen.topPos + LEFT_STRIP_CRAFT_Y - 1);
        }
        if (colorButton != null) {
            colorButton.setPosition(getLeftStripButtonX(), screen.topPos + LEFT_STRIP_COLOR_Y + 3);
        }
        if (importExportButton != null) {
            importExportButton.setPosition(getLeftStripButtonX(), getImportExportButtonY());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        boolean hasActiveBanner = manager.hasActiveBanner();
        boolean canCraftEffectiveActiveBanner = hasActiveBanner && manager.isEffectiveActiveBannerCraftable();
        String craftDisabledMessage = hasActiveBanner && !canCraftEffectiveActiveBanner
                ? manager.getEffectiveActiveBannerMissingMaterialMessage()
                : null;

        updateButtonState();

        if (saveButton != null && isMouseOverWidget(saveButton, mouseX, mouseY)) {
            Component tooltip = manager.isActiveBannerAlreadySaved() ? EDIT_TOOLTIP : SAVE_TOOLTIP;
            setSingleLineTooltip(context, tooltip, mouseX, mouseY);
        }
        if (craftButton != null && isMouseOverWidget(craftButton, mouseX, mouseY)) {
            List<Component> tooltipLines = new ArrayList<>();
            tooltipLines.add(WEAVE_TOOLTIP);
            if (!craftButton.active
                    && hasActiveBanner
                    && craftDisabledMessage != null
                    && !craftDisabledMessage.isBlank()) {
                tooltipLines.add(Component.empty());
                tooltipLines.addAll(
                        craftDisabledMessage.lines().map(Component::literal).toList());
            }
            context.setTooltipForNextFrame(screen.font, tooltipLines, Optional.empty(), mouseX, mouseY);
        }
        if (importExportButton != null && isMouseOverWidget(importExportButton, mouseX, mouseY)) {
            setSingleLineTooltip(context, IMPORT_EXPORT_TOOLTIP, mouseX, mouseY);
        }
        if (colorButton != null && isMouseOverWidget(colorButton, mouseX, mouseY)) {
            setSingleLineTooltip(context, CHANGE_COLORS_TOOLTIP, mouseX, mouseY);
        }

        ItemStack effectiveActiveBannerStack = manager.getEffectiveActiveBannerStack();
        if (effectiveActiveBannerStack.isEmpty()) return;

        context.fakeItem(
                effectiveActiveBannerStack, getLeftStripButtonX() + 2, screen.topPos + LEFT_STRIP_ACTIVE_SLOT_Y + 1);

        if (!isSurvivalNotWeavable()) {
            int progress = manager.detectCraftingProgress();
            int total = manager.getEffectiveActiveBannerLayerCount();
            if (progress >= 0 && total > 0) {
                String badge = (progress + 1) + "/" + total;
                int badgeWidth = screen.font.width(badge);
                int badgeX = getLeftStripButtonX() + (20 - badgeWidth) / 2;
                int badgeY = screen.topPos + LEFT_STRIP_ACTIVE_SLOT_Y - 12;
                context.text(screen.font, badge, badgeX, badgeY, 0xFFFFFFFF, true);
            }
        }

        if (isInActiveSlot(mouseX, mouseY) && manager.getEffectiveActiveBanner() != null) {
            int progress = manager.detectCraftingProgress();
            int currentRowIndex = progress >= 0 ? progress + 1 : -1;
            LoomRecipePanel.setBannerTooltip(
                    context,
                    screen.minecraft,
                    manager.getEffectiveActiveBannerDisplayName(),
                    manager.getEffectiveActiveBanner(),
                    currentRowIndex,
                    mouseX,
                    mouseY);
        }
    }

    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.button() == 1 && isShiftHeld()) {
            int mouseX = (int) mouseButtonEvent.x();
            int mouseY = (int) mouseButtonEvent.y();
            int leftPos = screen.leftPos;
            int topPos = screen.topPos;

            for (Slot slot : screen.menu.slots) {
                if (mouseX >= leftPos + slot.x
                        && mouseX < leftPos + slot.x + 16
                        && mouseY >= topPos + slot.y
                        && mouseY < topPos + slot.y + 16) {
                    ItemStack stack = slot.getItem();
                    if (!stack.isEmpty()
                            && stack.getItem() instanceof BannerItem
                            && manager.setActiveBannerFromItemStack(stack)) {
                        playActiveSlotSetSound();
                        return true;
                    }
                    break;
                }
            }
        }

        if (!isInActiveSlot((int) mouseButtonEvent.x(), (int) mouseButtonEvent.y())) {
            return false;
        }

        ItemStack carried = screen.menu.getCarried();
        if (!carried.isEmpty() && manager.setActiveBannerFromItemStack(carried)) {
            playActiveSlotSetSound();
            return true;
        }

        if (manager.hasActiveBanner()) {
            manager.clearActiveBanner();
            playActiveSlotClearSound();
            return true;
        }

        return false;
    }

    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        return mouseButtonEvent.button() == 0
                && isInActiveSlot((int) mouseButtonEvent.x(), (int) mouseButtonEvent.y())
                && !screen.menu.getCarried().isEmpty();
    }

    private void updateButtonState() {
        boolean hasActiveBanner = manager.hasActiveBanner();
        if (saveButton != null) {
            saveButton.active = hasActiveBanner;
            saveButton.visible = true;
        }
        if (craftButton != null) {
            craftButton.active = hasActiveBanner && manager.isEffectiveActiveBannerCraftable();
        }
        if (colorButton != null) {
            colorButton.active = hasActiveBanner;
            colorButton.visible = true;
        }
    }

    private boolean isSurvivalNotWeavable() {
        LocalPlayer player = screen.minecraft.player;
        return !manager.isEffectiveActiveBannerWeavable() && !player.hasInfiniteMaterials();
    }

    private int getLeftStripButtonX() {
        return screen.leftPos - BG_LEFT_PADDING + LEFT_STRIP_BUTTON_X;
    }

    private int getImportExportButtonY() {
        return screen.topPos + screen.imageHeight - 20 - LEFT_STRIP_IMPORT_EXPORT_BOTTOM_MARGIN;
    }

    private boolean isInActiveSlot(int mouseX, int mouseY) {
        int slotX = getLeftStripButtonX();
        int slotY = screen.topPos + LEFT_STRIP_ACTIVE_SLOT_Y;
        return mouseX >= slotX && mouseX < slotX + ACTIVE_SLOT_W && mouseY >= slotY && mouseY < slotY + ACTIVE_SLOT_H;
    }

    private static boolean isMouseOverWidget(AbstractWidget widget, int mouseX, int mouseY) {
        return mouseX >= widget.getX()
                && mouseX < widget.getX() + widget.getWidth()
                && mouseY >= widget.getY()
                && mouseY < widget.getY() + widget.getHeight();
    }

    private void setSingleLineTooltip(GuiGraphicsExtractor context, Component text, int mouseX, int mouseY) {
        context.setTooltipForNextFrame(screen.font, List.of(text), Optional.empty(), mouseX, mouseY);
    }

    private boolean isShiftHeld() {
        Window window = screen.minecraft.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void playActiveSlotSetSound() {
        Minecraft mc = screen.minecraft;
        mc.player
                .level()
                .playLocalSound(
                        mc.player.getX(),
                        mc.player.getY(),
                        mc.player.getZ(),
                        ACTIVE_SLOT_SET_SOUND,
                        SoundSource.PLAYERS,
                        0.42F,
                        1.0F,
                        false);
    }

    private void playActiveSlotClearSound() {
        Minecraft mc = screen.minecraft;
        mc.player
                .level()
                .playLocalSound(
                        mc.player.getX(),
                        mc.player.getY(),
                        mc.player.getZ(),
                        ACTIVE_SLOT_CLEAR_SOUND,
                        SoundSource.PLAYERS,
                        0.42F,
                        1.0F,
                        false);
    }

    private class ReplaceColorButton extends Button.Plain {
        protected ReplaceColorButton() {
            super(
                    LoomScreenLeftBar.this.getLeftStripButtonX(),
                    LoomScreenLeftBar.this.screen.topPos + LEFT_STRIP_COLOR_Y + 3,
                    20,
                    20,
                    Component.empty(),
                    button -> {
                        if (!manager.hasActiveBanner()) return;

                        if (manager.isPersistentDyeSwitchEnabled()) {
                            manager.disablePersistentDyeSwitchAndReload();
                        } else {
                            screen.minecraft.gui.setScreen(new BannerColorSwitchScreen(screen, manager));
                        }
                    },
                    Supplier::get);
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            boolean persistent = manager.isPersistentDyeSwitchEnabled();
            if (persistent) {
                // Rotate 180° to flip highlight/shadow, giving a pressed-in look
                float cx = this.getX() + this.getWidth() / 2.0f;
                float cy = this.getY() + this.getHeight() / 2.0f;
                graphics.pose().pushMatrix();
                graphics.pose().translate(cx, cy);
                graphics.pose().rotate((float) Math.PI);
                graphics.pose().translate(-cx, -cy);
                this.extractDefaultSprite(graphics);
                graphics.pose().popMatrix();
                graphics.fill(
                        this.getX() + 1,
                        this.getY() + 1,
                        this.getX() + this.getWidth() - 1,
                        this.getY() + this.getHeight() - 1,
                        0xAA8A92C5);
            } else {
                this.extractDefaultSprite(graphics);
            }
            int iconShift = persistent ? 1 : 0;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    RECIPE_SWAP_COLORS_ICON,
                    this.getX() + 2 + iconShift,
                    this.getY() + 2 + iconShift,
                    0.0F,
                    0.0F,
                    16,
                    16,
                    16,
                    16);
        }
    }

    private class SaveEditButton extends Button.Plain {
        protected SaveEditButton() {
            super(
                    LoomScreenLeftBar.this.getLeftStripButtonX(),
                    LoomScreenLeftBar.this.screen.topPos + LEFT_STRIP_SAVE_EDIT_Y + 1,
                    20,
                    20,
                    Component.empty(),
                    button -> {
                        if (manager.hasActiveBanner()) {
                            BannerDetailsScreen.Mode mode;
                            if (manager.isActiveBannerAlreadySaved()) {
                                mode = manager.isActiveBannerFromReadOnlySource()
                                        ? BannerDetailsScreen.Mode.EDIT_READONLY
                                        : BannerDetailsScreen.Mode.EDIT;
                            } else {
                                mode = BannerDetailsScreen.Mode.SAVE;
                            }

                            boolean loadFromPersistedSource = mode == BannerDetailsScreen.Mode.EDIT
                                    || mode == BannerDetailsScreen.Mode.EDIT_READONLY;
                            String initialName = manager.getActiveBannerDialogName(loadFromPersistedSource);
                            String initialCategory = manager.getActiveBannerDialogCategory(loadFromPersistedSource);

                            screen.minecraft.gui.setScreen(new BannerDetailsScreen(
                                    screen, mode, initialName, initialCategory, manager::applyActiveBannerMetadata));
                        }
                    },
                    Supplier::get);
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            this.extractDefaultSprite(graphics);
            Identifier icon = manager.isActiveBannerAlreadySaved() ? RECIPE_EDIT_ICON : RECIPE_ADD_ICON;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, icon, this.getX() + 2, this.getY() + 2, 0.0F, 0.0F, 16, 16, 16, 16);
        }
    }

    private class WeaveButton extends Button.Plain {
        protected WeaveButton() {
            super(
                    LoomScreenLeftBar.this.getLeftStripButtonX(),
                    LoomScreenLeftBar.this.screen.topPos + LEFT_STRIP_CRAFT_Y - 1,
                    20,
                    20,
                    Component.empty(),
                    button -> manager.craftActiveBanner(),
                    Supplier::get);
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            this.extractDefaultSprite(graphics);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    RECIPE_WEAVE_ICON,
                    this.getX() + 2,
                    this.getY() + 2,
                    0.0F,
                    0.0F,
                    16,
                    16,
                    16,
                    16);
        }
    }

    private class ImportExportButton extends Button.Plain {
        protected ImportExportButton() {
            super(
                    LoomScreenLeftBar.this.getLeftStripButtonX(),
                    LoomScreenLeftBar.this.getImportExportButtonY(),
                    20,
                    20,
                    Component.empty(),
                    button -> screen.minecraft.gui.setScreen(new ManageBannerRecipesScreen(screen, manager)),
                    Supplier::get);
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            this.extractDefaultSprite(graphics);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    RECIPE_IMPORT_EXPORT_ICON,
                    this.getX() + 2,
                    this.getY() + 2,
                    0.0F,
                    0.0F,
                    16,
                    16,
                    16,
                    16);
        }
    }
}
