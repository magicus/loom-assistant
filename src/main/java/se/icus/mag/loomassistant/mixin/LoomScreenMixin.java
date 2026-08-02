/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.loomassistant.LoomPanelHost;
import se.icus.mag.loomassistant.autocraft.AutoCraftStateMachine;
import se.icus.mag.loomassistant.data.SavedBanner;
import se.icus.mag.loomassistant.ui.BannerPreviewRenderer;
import se.icus.mag.loomassistant.ui.BannerRecipeImportExportScreen;
import se.icus.mag.loomassistant.ui.BannerSaveEditScreen;
import se.icus.mag.loomassistant.ui.LoomPanel;
import se.icus.mag.loomassistant.ui.LoomUiStateStore;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> implements LoomPanelHost {
    @Unique
    private static final int LOOMASSISTANT_CONTENT_X_SHIFT = 3;
    @Unique
    private static final int LOOMASSISTANT_BG_LEFT_PADDING = 19;
    @Unique
    private static final int LOOMASSISTANT_PANEL_TAB_LEFT_OVERHANG = 32;
    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_BUTTON_X = 3;
    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_RECIPE_Y = 5;
    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_ACTIVE_SLOT_Y = LOOMASSISTANT_LEFT_STRIP_RECIPE_Y + 42;
    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_CRAFT_Y = LOOMASSISTANT_LEFT_STRIP_ACTIVE_SLOT_Y + 22;
    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_SAVE_EDIT_Y = LOOMASSISTANT_LEFT_STRIP_CRAFT_Y + 20;
    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_IMPORT_EXPORT_BOTTOM_MARGIN = 6;
    @Unique
    private static final int LOOMASSISTANT_ACTIVE_SLOT_W = 20;
    @Unique
    private static final int LOOMASSISTANT_ACTIVE_SLOT_H = 18;
    @Unique
    private static final int LOOMASSISTANT_CUSTOM_BG_WIDTH = 278;
    @Unique
    private static final int LOOMASSISTANT_CUSTOM_BG_HEIGHT = 256;
    @Unique
    private static final Identifier LOOMASSISTANT_BG_LOCATION = Identifier.fromNamespaceAndPath("loom-assistant", "textures/loom.png");
        @Unique
        private static final SoundEvent LOOMASSISTANT_ACTIVE_SLOT_SET_SOUND =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("loom-assistant", "ui.active_slot_set"));
        @Unique
        private static final SoundEvent LOOMASSISTANT_ACTIVE_SLOT_CLEAR_SOUND =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("loom-assistant", "ui.active_slot_clear"));
            @Unique
            private static final Component LOOMASSISTANT_SAVE_TOOLTIP =
                Component.translatable("loom-assistant.tooltip.save");
            @Unique
                private static final Component LOOMASSISTANT_WEAVE_TOOLTIP =
                    Component.translatable("loom-assistant.tooltip.weave");
            @Unique
            private static final Component LOOMASSISTANT_EDIT_TOOLTIP =
                Component.translatable("loom-assistant.tooltip.edit");
            @Unique
            private static final Component LOOMASSISTANT_IMPORT_EXPORT_TOOLTIP =
                Component.translatable("loom-assistant.tooltip.import_export");
    @Unique
    private boolean loomassistant$panelOpen = false;
    @Unique
    private LoomPanel loomassistant$panel;
    @Unique
    private ImageButton loomassistant$recipeBookButton;
    @Unique
    private Button loomassistant$saveButton;
    @Unique
    private Button loomassistant$craftButton;
    @Unique
    private Button loomassistant$importExportButton;
    @Unique
    private ItemStack loomassistant$activeBannerStack = ItemStack.EMPTY;
    @Unique
    private ItemStack loomassistant$pendingActiveBannerStack = ItemStack.EMPTY;
    @Unique
    private AutoCraftStateMachine loomassistant$craftabilityProbe;

    public LoomScreenMixin(LoomMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void loomassistant$onInit(CallbackInfo ci) {
        this.loomassistant$panelOpen = LoomUiStateStore.isLoomPanelOpen(this.minecraft);
        this.leftPos = loomassistant$panelOpen ? loomassistant$getOpenLeftPos() : loomassistant$getClosedLeftPos();
        this.loomassistant$craftabilityProbe = new AutoCraftStateMachine(this.menu);

        this.loomassistant$recipeBookButton = this.addRenderableWidget(new ImageButton(
            this.loomassistant$getLeftStripButtonX(),
            this.topPos + LOOMASSISTANT_LEFT_STRIP_RECIPE_Y,
            20,
            18,
            RecipeBookComponent.RECIPE_BUTTON_SPRITES,
            button -> {
                loomassistant$panelOpen = !loomassistant$panelOpen;
                LoomUiStateStore.setLoomPanelOpen(this.minecraft, loomassistant$panelOpen);
                this.leftPos = loomassistant$panelOpen ? loomassistant$getOpenLeftPos() : loomassistant$getClosedLeftPos();
                loomassistant$refreshControls();
                loomassistant$refreshPanel();
            }));

        this.loomassistant$saveButton = this.addRenderableWidget(new Button.Plain(
            this.loomassistant$getLeftStripButtonX(),
            this.topPos + LOOMASSISTANT_LEFT_STRIP_SAVE_EDIT_Y,
            20,
            18,
            Component.empty(),
            button -> {
                if (loomassistant$panel != null && loomassistant$panel.hasActiveBanner()) {
                    this.minecraft.gui.setScreen(new BannerSaveEditScreen(
                            (LoomScreen) (Object) this,
                            loomassistant$panel,
                            loomassistant$showEditOnSaveButton()));
                }
            },
            defaultNarrationSupplier -> defaultNarrationSupplier.get()) {
            @Override
            public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
                this.extractDefaultSprite(graphics);
                ItemStack icon = loomassistant$showEditOnSaveButton()
                        ? Items.WRITABLE_BOOK.getDefaultInstance()
                        : Items.CHEST.getDefaultInstance();
                graphics.fakeItem(icon, this.getX() + 2, this.getY() + 1);
            }
        });

        this.loomassistant$craftButton = this.addRenderableWidget(new Button.Plain(
            this.loomassistant$getLeftStripButtonX(),
            this.topPos + LOOMASSISTANT_LEFT_STRIP_CRAFT_Y,
            20,
            18,
            Component.empty(),
            button -> {
                if (loomassistant$panel != null) {
                    loomassistant$panel.craftSelectedBanner();
                }
            },
            defaultNarrationSupplier -> defaultNarrationSupplier.get()) {
            @Override
            public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
                this.extractDefaultSprite(graphics);
                graphics.fakeItem(Items.LOOM.getDefaultInstance(), this.getX() + 2, this.getY() + 1);
            }
        });

        this.loomassistant$importExportButton = this.addRenderableWidget(new Button.Plain(
            this.loomassistant$getLeftStripButtonX(),
            this.loomassistant$getImportExportButtonY(),
            20,
            18,
            Component.empty(),
            button -> this.minecraft.gui.setScreen(new BannerRecipeImportExportScreen((LoomScreen) (Object) this)),
            defaultNarrationSupplier -> defaultNarrationSupplier.get()) {
            @Override
            public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
                this.extractDefaultSprite(graphics);
                graphics.fakeItem(Items.OAK_BOAT.getDefaultInstance(), this.getX() + 2, this.getY() + 1);
            }
        });

        this.loomassistant$craftButton.active = false;
        this.loomassistant$saveButton.active = false;
        this.loomassistant$saveButton.visible = false;

        loomassistant$refreshPanel();
    }

        @Unique
        private void loomassistant$refreshControls() {
        if (this.loomassistant$recipeBookButton != null) {
            this.loomassistant$recipeBookButton.setPosition(
                    this.loomassistant$getLeftStripButtonX(), this.topPos + LOOMASSISTANT_LEFT_STRIP_RECIPE_Y);
        }
        if (this.loomassistant$saveButton != null) {
            this.loomassistant$saveButton.setPosition(
                    this.loomassistant$getLeftStripButtonX(), this.topPos + LOOMASSISTANT_LEFT_STRIP_SAVE_EDIT_Y);
        }
        if (this.loomassistant$craftButton != null) {
            this.loomassistant$craftButton.setPosition(
                    this.loomassistant$getLeftStripButtonX(), this.topPos + LOOMASSISTANT_LEFT_STRIP_CRAFT_Y);
        }
        if (this.loomassistant$importExportButton != null) {
            this.loomassistant$importExportButton.setPosition(
                    this.loomassistant$getLeftStripButtonX(), this.loomassistant$getImportExportButtonY());
        }
    }

    @Unique
    private int loomassistant$getImportExportButtonY() {
        return this.topPos + this.imageHeight - 18 - LOOMASSISTANT_LEFT_STRIP_IMPORT_EXPORT_BOTTOM_MARGIN;
    }

    @Unique
    private int loomassistant$getClosedLeftPos() {
        int guiExtraLeft = LOOMASSISTANT_BG_LEFT_PADDING + LOOMASSISTANT_CONTENT_X_SHIFT;
        int visualGuiWidth = this.imageWidth + guiExtraLeft;
        int visualLeft = (this.width - visualGuiWidth) / 2;
        return visualLeft + guiExtraLeft;
    }

    @Unique
    private int loomassistant$getOpenLeftPos() {
        int leftExtensionWithoutTabs = LoomPanel.PANEL_WIDTH + 5 + LOOMASSISTANT_BG_LEFT_PADDING;

        // Center panel + loom as a combined area (tabs excluded from centering).
        int centeredAreaWidth = this.imageWidth + leftExtensionWithoutTabs;
        int centeredAreaLeft = (this.width - centeredAreaWidth) / 2;
        int leftPos = centeredAreaLeft + leftExtensionWithoutTabs;

        // If tabs would go off-screen on the left, shift everything right just enough.
        int panelLeft = leftPos - leftExtensionWithoutTabs;
        int tabLeft = panelLeft - LOOMASSISTANT_PANEL_TAB_LEFT_OVERHANG;
        if (tabLeft < 0) {
            leftPos += -tabLeft;
        }

        return leftPos;
    }

    @Unique
    private int loomassistant$getPanelX() {
        return this.leftPos - LoomPanel.PANEL_WIDTH - 5 - LOOMASSISTANT_BG_LEFT_PADDING;
    }

    @Unique
    private void loomassistant$refreshPanel() {
        this.loomassistant$panel = loomassistant$panelOpen
                ? new LoomPanel((LoomScreen) (Object) this, this.menu, loomassistant$getPanelX(), this.topPos)
                : null;
    }

    @Unique
    private int loomassistant$getLeftStripButtonX() {
        return this.leftPos - LOOMASSISTANT_BG_LEFT_PADDING + LOOMASSISTANT_LEFT_STRIP_BUTTON_X;
    }

    @Unique
    private boolean loomassistant$isInActiveSlot(int mouseX, int mouseY) {
        int slotX = this.loomassistant$getLeftStripButtonX();
        int slotY = this.topPos + LOOMASSISTANT_LEFT_STRIP_ACTIVE_SLOT_Y;
        return mouseX >= slotX
                && mouseX < slotX + LOOMASSISTANT_ACTIVE_SLOT_W
                && mouseY >= slotY
                && mouseY < slotY + LOOMASSISTANT_ACTIVE_SLOT_H;
    }

    @Redirect(
            method = "extractBackground",
            at = @At(
                    value = "INVOKE",
                target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private void loomassistant$drawCustomLoomBackground(
            GuiGraphicsExtractor graphics,
            RenderPipeline renderPipeline,
            Identifier originalTexture,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int textureWidth,
            int textureHeight) {
        graphics.blit(
            renderPipeline,
            LOOMASSISTANT_BG_LOCATION,
                x - LOOMASSISTANT_BG_LEFT_PADDING - LOOMASSISTANT_CONTENT_X_SHIFT,
            y,
            u,
            v,
                width + LOOMASSISTANT_BG_LEFT_PADDING + LOOMASSISTANT_CONTENT_X_SHIFT,
            height,
            LOOMASSISTANT_CUSTOM_BG_WIDTH,
            LOOMASSISTANT_CUSTOM_BG_HEIGHT);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void loomassistant$onExtractBackground(
            GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        boolean hasActiveBanner = false;
        boolean canCraftActiveBanner = false;
        String craftDisabledMessage = null;
        if (loomassistant$panel != null) {
            if (!loomassistant$pendingActiveBannerStack.isEmpty()) {
                loomassistant$panel.setActiveBannerFromItemStack(loomassistant$pendingActiveBannerStack);
                loomassistant$pendingActiveBannerStack = ItemStack.EMPTY;
            }
            loomassistant$panel.tick();
            loomassistant$activeBannerStack = loomassistant$panel.getActiveBannerStack();
            hasActiveBanner = !loomassistant$activeBannerStack.isEmpty();
            canCraftActiveBanner = loomassistant$panel.isActiveBannerCraftable();
            if (!canCraftActiveBanner) {
                craftDisabledMessage = loomassistant$panel.getActiveBannerMissingMaterialMessage();
            }
            if (this.loomassistant$saveButton != null) {
                this.loomassistant$saveButton.active = loomassistant$panel.hasActiveBanner();
                this.loomassistant$saveButton.visible = loomassistant$panel.hasActiveBanner();
            }
            loomassistant$panel.render(context, mouseX, mouseY, delta);
        } else if (this.loomassistant$saveButton != null) {
            this.loomassistant$saveButton.active = false;
            this.loomassistant$saveButton.visible = false;
            hasActiveBanner = !loomassistant$activeBannerStack.isEmpty();
            if (hasActiveBanner && loomassistant$craftabilityProbe != null) {
                SavedBanner activeBanner = BannerPreviewRenderer.extractBannerData(loomassistant$activeBannerStack);
                if (activeBanner != null) {
                    canCraftActiveBanner = loomassistant$craftabilityProbe.canCraft(activeBanner);
                    if (!canCraftActiveBanner) {
                        craftDisabledMessage = loomassistant$buildMissingMaterialsMessage(
                                activeBanner, loomassistant$craftabilityProbe);
                    }
                }
            }
            this.loomassistant$saveButton.visible = hasActiveBanner;
            this.loomassistant$saveButton.active = hasActiveBanner;
        }

        if (this.loomassistant$craftButton != null) {
            this.loomassistant$craftButton.active = hasActiveBanner && canCraftActiveBanner;
        }

        if (this.loomassistant$saveButton != null
                && loomassistant$isMouseOverWidget(this.loomassistant$saveButton, mouseX, mouseY)) {
            Component tooltip = loomassistant$showEditOnSaveButton() ? LOOMASSISTANT_EDIT_TOOLTIP : LOOMASSISTANT_SAVE_TOOLTIP;
            loomassistant$setSingleLineTooltip(context, tooltip, mouseX, mouseY);
        }
        if (this.loomassistant$craftButton != null
                && loomassistant$isMouseOverWidget(this.loomassistant$craftButton, mouseX, mouseY)) {
            List<Component> tooltipLines = new ArrayList<>();
            tooltipLines.add(LOOMASSISTANT_WEAVE_TOOLTIP);

            if (!this.loomassistant$craftButton.active && hasActiveBanner) {
                String reason = craftDisabledMessage;
                if (reason != null && !reason.isBlank()) {
                    tooltipLines.add(Component.empty());
                    tooltipLines.addAll(reason.lines().map(Component::literal).toList());
                }
            }

            context.setTooltipForNextFrame(this.font, tooltipLines, Optional.empty(), mouseX, mouseY);
        }
        if (this.loomassistant$importExportButton != null
                && loomassistant$isMouseOverWidget(this.loomassistant$importExportButton, mouseX, mouseY)) {
            loomassistant$setSingleLineTooltip(context, LOOMASSISTANT_IMPORT_EXPORT_TOOLTIP, mouseX, mouseY);
        }

        if (!loomassistant$activeBannerStack.isEmpty()) {
            context.fakeItem(
                    loomassistant$activeBannerStack,
                    this.loomassistant$getLeftStripButtonX() + 2,
                    this.topPos + LOOMASSISTANT_LEFT_STRIP_ACTIVE_SLOT_Y + 1);

            if (loomassistant$isInActiveSlot(mouseX, mouseY)) {
                if (loomassistant$panel != null) {
                    loomassistant$panel.setActiveBannerTooltip(context, mouseX, mouseY);
                } else {
                    SavedBanner activeBanner = BannerPreviewRenderer.extractBannerData(loomassistant$activeBannerStack);
                    LoomPanel.setBannerTooltip(context, activeBanner, mouseX, mouseY);
                }
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseClicked(
            MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (loomassistant$isInActiveSlot((int) mouseButtonEvent.x(), (int) mouseButtonEvent.y())) {
            ItemStack carried = this.menu.getCarried();
            if (!carried.isEmpty()) {
                if (loomassistant$panel != null) {
                    if (loomassistant$panel.setActiveBannerFromItemStack(carried)) {
                        loomassistant$activeBannerStack = loomassistant$panel.getActiveBannerStack();
                        loomassistant$playActiveSlotSetSound();
                    }
                } else {
                    loomassistant$pendingActiveBannerStack = carried.copyWithCount(1);
                    loomassistant$activeBannerStack = loomassistant$pendingActiveBannerStack.copy();
                    loomassistant$playActiveSlotSetSound();
                }
                cir.setReturnValue(true);
                return;
            }

            if (!loomassistant$activeBannerStack.isEmpty()) {
                if (loomassistant$panel != null) {
                    loomassistant$panel.clearSelectedBanner();
                }
                loomassistant$pendingActiveBannerStack = ItemStack.EMPTY;
                loomassistant$activeBannerStack = ItemStack.EMPTY;
                loomassistant$playActiveSlotClearSound();
                cir.setReturnValue(true);
                return;
            }
        }

        if (loomassistant$panel != null && loomassistant$panel.mouseClicked(mouseButtonEvent)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseReleased(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        if (mouseButtonEvent.button() != 0) {
            return;
        }

        if (loomassistant$isInActiveSlot((int) mouseButtonEvent.x(), (int) mouseButtonEvent.y())
                && !this.menu.getCarried().isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount,
            CallbackInfoReturnable<Boolean> cir) {
        if (loomassistant$panel != null
                && loomassistant$panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public LoomPanel loomassistant$getPanel() {
        return loomassistant$panel;
    }

    @Unique
    private static boolean loomassistant$isMouseOverWidget(AbstractWidget widget, int mouseX, int mouseY) {
        return mouseX >= widget.getX()
                && mouseX < widget.getX() + widget.getWidth()
                && mouseY >= widget.getY()
                && mouseY < widget.getY() + widget.getHeight();
    }

    @Unique
    private void loomassistant$setSingleLineTooltip(
            GuiGraphicsExtractor context, Component text, int mouseX, int mouseY) {
        context.setTooltipForNextFrame(this.font, List.of(text), Optional.empty(), mouseX, mouseY);
    }

    @Unique
    private boolean loomassistant$showEditOnSaveButton() {
        return loomassistant$panel != null
                && loomassistant$panel.hasActiveBanner()
                && !loomassistant$panel.isActiveBannerSavable();
    }

    @Unique
    private static String loomassistant$buildMissingMaterialsMessage(
            SavedBanner banner, AutoCraftStateMachine autoCraft) {
        List<String> missingMaterials = autoCraft.getMissingMaterialDescriptions(banner);
        if (missingMaterials.isEmpty()) {
            return null;
        }
        return Component.translatable("loom-assistant.active.missing_header").getString()
                + "\n"
                + String.join("\n", missingMaterials);
    }

    @Unique
    private void loomassistant$playActiveSlotSetSound() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player
                    .level()
                    .playLocalSound(
                            this.minecraft.player.getX(),
                            this.minecraft.player.getY(),
                            this.minecraft.player.getZ(),
                            LOOMASSISTANT_ACTIVE_SLOT_SET_SOUND,
                            SoundSource.PLAYERS,
                            0.42F,
                            1.0F,
                            false);
        }
    }

    @Unique
    private void loomassistant$playActiveSlotClearSound() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player
                    .level()
                    .playLocalSound(
                            this.minecraft.player.getX(),
                            this.minecraft.player.getY(),
                            this.minecraft.player.getZ(),
                            LOOMASSISTANT_ACTIVE_SLOT_CLEAR_SOUND,
                            SoundSource.PLAYERS,
                            0.42F,
                            1.0F,
                            false);
        }
    }
}
