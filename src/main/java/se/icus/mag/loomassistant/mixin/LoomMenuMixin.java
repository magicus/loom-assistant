/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.mixin;

import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoomMenu.class)
public class LoomMenuMixin {
    @Shadow
    @Final
    private Slot bannerSlot;

    @Shadow
    @Final
    private DataSlot selectedBannerPatternIndex;

    @Unique
    private ItemStack lastBanner = ItemStack.EMPTY;

    /**
     * Vanilla has a bug where it doesn't clear the selected pattern when the
     * banner in the loom changes. This causes the previous step's pattern
     * selection to carry over to the next step. We need to fix this, otherwise
     * it would break our weaving guide.
     */
    @Inject(method = "slotsChanged", at = @At("HEAD"))
    private void clearSelectionOnBannerChange(Container container, CallbackInfo ci) {
        ItemStack current = bannerSlot.getItem();
        if (!sameEffectiveBanner(current, lastBanner)) {
            selectedBannerPatternIndex.set(-1);
        }
        lastBanner = current.copy();
    }

    @Unique
    private static boolean sameEffectiveBanner(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() != b.isEmpty()) return false;
        if (a.getItem() != b.getItem()) return false;
        BannerPatternLayers patternsA = a.get(DataComponents.BANNER_PATTERNS);
        BannerPatternLayers patternsB = b.get(DataComponents.BANNER_PATTERNS);
        return Objects.equals(patternsA, patternsB);
    }
}
