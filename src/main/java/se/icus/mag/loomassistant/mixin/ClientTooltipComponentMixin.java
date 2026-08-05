/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.loomassistant.ui.tooltip.BannerRecipeTooltipComponent;
import se.icus.mag.loomassistant.ui.tooltip.ClientBannerRecipeTooltipComponent;

@Mixin(ClientTooltipComponent.class)
public interface ClientTooltipComponentMixin {
    @Inject(
            method =
                    "create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
            at = @At("HEAD"),
            cancellable = true)
    private static void createCustomTooltip(
            TooltipComponent component, CallbackInfoReturnable<ClientTooltipComponent> cir) {
        if (!(component instanceof BannerRecipeTooltipComponent bannerRecipeTooltipComponent)) return;

        cir.setReturnValue(new ClientBannerRecipeTooltipComponent(bannerRecipeTooltipComponent));
    }
}
