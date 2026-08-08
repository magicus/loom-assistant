/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.gui.extensions.LoomScreenExtension;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        LoomScreenExtension extension = new LoomScreenExtension((LoomScreen) (Object) this);
        LoomAssistantMod.getLoomManager().setExtension(extension);
        extension.onInit();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        LoomScreenExtension extension = LoomAssistantMod.getLoomManager().getExtension();
        if (extension != null) {
            extension.onRemoved();
        }
        LoomAssistantMod.getLoomManager().setExtension(null);
    }

    @Redirect(
            method = "extractBackground",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private void drawCustomLoomBackground(
            GuiGraphicsExtractor graphics,
            RenderPipeline renderPipeline,
            Identifier texture,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int textureWidth,
            int textureHeight) {
        LoomScreenExtension extension = LoomAssistantMod.getLoomManager().getExtension();
        if (extension == null) return;

        extension.drawCustomBackground(
                graphics, renderPipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void onExtractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        LoomScreenExtension extension = LoomAssistantMod.getLoomManager().getExtension();
        if (extension == null) return;

        extension.extractBackground(graphics, mouseX, mouseY, delta);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(
            MouseButtonEvent mouseButtonEvent, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        LoomScreenExtension extension = LoomAssistantMod.getLoomManager().getExtension();
        if (extension == null) return;

        extension.onMouseClicked(mouseButtonEvent, cir);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        LoomScreenExtension extension = LoomAssistantMod.getLoomManager().getExtension();
        if (extension == null) return;

        extension.onMouseReleased(mouseButtonEvent, cir);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(
            double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        LoomScreenExtension extension = LoomAssistantMod.getLoomManager().getExtension();
        if (extension == null) return;

        extension.onMouseScrolled(mouseX, mouseY, scrollX, scrollY, cir);
    }
}
