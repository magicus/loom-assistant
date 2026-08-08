/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.gui.ScreenExtension;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenKeyPressedMixin {
    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        if (!((Object) this instanceof LoomScreen)) return;

        ScreenExtension extension = LoomAssistantMod.getLoomManager().getExtension();
        if (extension != null) {
            extension.removed();
        }
        LoomAssistantMod.getLoomManager().setExtension(null);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof LoomScreen)) return;

        ScreenExtension extension = LoomAssistantMod.getLoomManager().getExtension();
        if (extension == null) return;

        if (extension.keyPressed(event)) {
            cir.setReturnValue(true);
        }
    }
}
