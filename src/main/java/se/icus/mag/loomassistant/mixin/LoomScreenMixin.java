/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import se.icus.mag.loomassistant.ui.LoomPanel;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> implements LoomPanelHost {
    @Unique
    private static final int LOOMASSISTANT_CONTENT_X_SHIFT = 3;
    @Unique
    private static final int LOOMASSISTANT_BG_LEFT_PADDING = 19;
    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_BUTTON_X = 3;
    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_RECIPE_Y = 5;
    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_SAVE_Y = 25;
    @Unique
    private static final int LOOMASSISTANT_CUSTOM_BG_WIDTH = 278;
    @Unique
    private static final int LOOMASSISTANT_CUSTOM_BG_HEIGHT = 256;
    @Unique
    private static final Identifier LOOMASSISTANT_BG_LOCATION = Identifier.fromNamespaceAndPath("loom-assistant", "textures/loom.png");
    @Unique
    private static boolean loomassistant$panelOpen = false;
    @Unique
    private LoomPanel loomassistant$panel;
    @Unique
    private ImageButton loomassistant$recipeBookButton;
    @Unique
    private Button loomassistant$saveButton;

    public LoomScreenMixin(LoomMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void loomassistant$onInit(CallbackInfo ci) {
        if (loomassistant$panelOpen) {
            this.leftPos = loomassistant$getOpenLeftPos();
        }

        this.loomassistant$recipeBookButton = this.addRenderableWidget(new ImageButton(
            this.loomassistant$getLeftStripButtonX(),
            this.topPos + LOOMASSISTANT_LEFT_STRIP_RECIPE_Y,
            20,
            18,
            RecipeBookComponent.RECIPE_BUTTON_SPRITES,
            button -> {
                loomassistant$panelOpen = !loomassistant$panelOpen;
                this.leftPos = loomassistant$panelOpen ? loomassistant$getOpenLeftPos() : loomassistant$getClosedLeftPos();
                loomassistant$refreshControls();
                loomassistant$refreshPanel();
            }));

        this.loomassistant$saveButton = this.addRenderableWidget(new Button.Plain(
            this.loomassistant$getLeftStripButtonX(),
            this.topPos + LOOMASSISTANT_LEFT_STRIP_SAVE_Y,
            20,
            18,
            Component.empty(),
            button -> LoomPanel.saveBannerFromOutput(this.menu),
            defaultNarrationSupplier -> defaultNarrationSupplier.get()) {
            @Override
            public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
                this.extractDefaultSprite(graphics);
                graphics.fakeItem(Items.CHEST.getDefaultInstance(), this.getX() + 2, this.getY() + 1);
            }
        });

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
                    this.loomassistant$getLeftStripButtonX(), this.topPos + LOOMASSISTANT_LEFT_STRIP_SAVE_Y);
        }
    }

    @Unique
    private int loomassistant$getClosedLeftPos() {
        return (this.width - this.imageWidth) / 2;
    }

    @Unique
    private int loomassistant$getOpenLeftPos() {
        return loomassistant$getClosedLeftPos() + (LoomPanel.PANEL_WIDTH + 5 + LOOMASSISTANT_BG_LEFT_PADDING) / 2;
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
        if (loomassistant$panel != null) {
            loomassistant$panel.tick();
            loomassistant$panel.render(context, mouseX, mouseY, delta);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseClicked(
            MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (loomassistant$panel != null && loomassistant$panel.mouseClicked(mouseButtonEvent)) {
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
}
