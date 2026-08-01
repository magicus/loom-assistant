/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.mixin;

import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LoomScreen.class)
public interface LoomScreenAccessor {
    @Accessor("bannerStack")
    ItemStack getBanner();

    @Accessor("dyeStack")
    ItemStack getDye();

    @Accessor("patternStack")
    ItemStack getPattern();

    @Accessor("displayPatterns")
    boolean getCanApplyDyePattern();

    @Accessor("hasMaxPatterns")
    boolean getHasTooManyPatterns();

    @Accessor("scrollOffs")
    float getScrollPosition();

    @Accessor("scrollOffs")
    void setScrollPosition(float position);

    @Accessor("resultBannerPatterns")
    BannerPatternLayers getBannerPatterns();
}
