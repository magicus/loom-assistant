/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.ui.extensions.LoomScreenExtension;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu> {
    @Unique
    private LoomScreenExtension extension;

    public LoomScreenMixin(LoomMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    // ── mixin hooks ───────────────────────────────────────────────────────────

    @Inject(method = "init", at = @At("TAIL"))
    private void loomassistant$onInit(CallbackInfo ci) {
        this.extension = new LoomScreenExtension(this);
        this.extension.onInit();
        LoomAssistantMod.registerExtension(this, this.extension);
    }

    @Redirect(
            method = "extractBackground",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
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
        this.extension.drawCustomBackground(
                graphics, renderPipeline, originalTexture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void loomassistant$onExtractBackground(
            GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.extension.onExtractBackground(context, mouseX, mouseY, delta);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseClicked(
            MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        this.extension.onMouseClicked(mouseButtonEvent, cir);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseReleased(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        this.extension.onMouseReleased(mouseButtonEvent, cir);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount,
            CallbackInfoReturnable<Boolean> cir) {
        this.extension.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount, cir);
    }
}
