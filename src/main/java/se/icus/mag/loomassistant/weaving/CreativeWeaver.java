/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.weaving;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.ui.extensions.PreviewExtension;

/** Weaver for creative mode: instantly adds the result to the player's inventory. */
public class CreativeWeaver extends Weaver {
    @Override
    public void weave(BannerRecipe banner) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.gameMode == null) {
            return;
        }

        ItemStack result = PreviewExtension.createBannerWithPatterns(banner);

        // Apply recipe name as custom name if it's not the unnamed placeholder.
        if (banner.description() != null
                && !banner.description().equals(BannerRecipe.DEFAULT_DESCRIPTION)
                && !banner.description().isBlank()) {
            result.set(DataComponents.CUSTOM_NAME, Component.literal(banner.description()));
        }

        var inventory = mc.player.getInventory();

        // Mirror what /give does: stack into an existing slot if possible, otherwise use free slot.
        int targetSlot = inventory.getSlotWithRemainingSpace(result);
        if (targetSlot == -1) {
            for (int i = 0; i < 9; i++) {
                if (inventory.getItem(i).isEmpty()) {
                    targetSlot = i;
                    break;
                }
            }
            if (targetSlot == -1) targetSlot = inventory.getFreeSlot();
            if (targetSlot == -1) targetSlot = inventory.getSelectedSlot();
            inventory.setItem(targetSlot, result.copy());
        } else {
            inventory.getItem(targetSlot).grow(result.getCount());
        }

        // Hotbar (0-8) maps to creative container slots 36-44; rest maps 1:1.
        int creativeSlot = targetSlot < 9 ? 36 + targetSlot : targetSlot;
        mc.gameMode.handleCreativeModeItemAdd(inventory.getItem(targetSlot).copy(), creativeSlot);
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void tick() {}

    @Override
    public boolean canWeave(BannerRecipe banner) {
        return banner != null;
    }

    @Override
    public List<String> getMissingMaterialDescriptions(BannerRecipe banner) {
        return List.of();
    }
}
