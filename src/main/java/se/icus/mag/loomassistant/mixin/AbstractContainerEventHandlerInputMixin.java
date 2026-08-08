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
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.gui.extensions.LoomScreenExtension;
import se.icus.mag.loomassistant.gui.panel.LoomRecipePanel;

@Mixin(ContainerEventHandler.class)
public interface AbstractContainerEventHandlerInputMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!(this instanceof LoomScreen)) return;

        LoomScreenExtension extension = LoomAssistantMod.getLoomManager().getExtension();
        LoomRecipePanel panel = extension != null ? extension.getPanel() : null;
        if (panel == null) return;

        if (panel.charTyped(event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (!(this instanceof LoomScreen)) return;

        LoomScreenExtension extension = LoomAssistantMod.getLoomManager().getExtension();
        LoomRecipePanel panel = extension != null ? extension.getPanel() : null;
        if (panel == null) return;

        if (panel.mouseDragged(event, dx, dy)) {
            cir.setReturnValue(true);
        }
    }
}
