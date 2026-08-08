/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.screens.colorswitch;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

public class ColorSelectButton extends Button.Plain {
    private final BannerColorSwitchScreen bannerColorSwitchScreen;
    private final int fi;

    public ColorSelectButton(
            BannerColorSwitchScreen bannerColorSwitchScreen,
            int btnX,
            int btnY,
            DyeColor source,
            int btnCenterX,
            int btnCenterY,
            int fi) {
        super(
                btnX,
                btnY,
                BannerColorSwitchScreen.TARGET_BTN_W,
                BannerColorSwitchScreen.TARGET_BTN_H,
                Component.empty(),
                button -> bannerColorSwitchScreen.openPicker(source, btnCenterX, btnCenterY),
                Supplier::get);
        this.bannerColorSwitchScreen = bannerColorSwitchScreen;
        this.fi = fi;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractDefaultSprite(graphics);
        DyeColor target = bannerColorSwitchScreen.targets.getOrDefault(
                bannerColorSwitchScreen.sourceColors.get(fi), bannerColorSwitchScreen.sourceColors.get(fi));
        graphics.fakeItem(BannerColorSwitchScreen.dyeStack(target), this.getX() + 2, this.getY() + 2);
    }
}
