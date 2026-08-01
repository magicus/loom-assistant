/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.mixin;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.loomassistant.LoomPanelHost;

@Mixin(ContainerEventHandler.class)
public interface AbstractContainerEventHandlerInputMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof LoomScreen)) {
            return;
        }

        if ((Object) this instanceof LoomPanelHost host
                && host.loomassistant$getPanel() != null
                && host.loomassistant$getPanel().charTyped(event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseDragged(
            MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof LoomScreen)) {
            return;
        }

        if ((Object) this instanceof LoomPanelHost host
                && host.loomassistant$getPanel() != null
                && host.loomassistant$getPanel().mouseDragged(event, dx, dy)) {
            cir.setReturnValue(true);
        }
    }
}
