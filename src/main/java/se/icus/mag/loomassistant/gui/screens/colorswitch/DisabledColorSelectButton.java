/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.screens.colorswitch;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public class DisabledColorSelectButton extends Button.Plain {
    public DisabledColorSelectButton(int btnX, int btnY) {
        super(
                btnX,
                btnY,
                BannerColorSwitchScreen.TARGET_BTN_W,
                BannerColorSwitchScreen.TARGET_BTN_H,
                Component.empty(),
                button -> {},
                Supplier::get);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractDefaultSprite(graphics);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BannerColorSwitchScreen.DYE_OUTLINE_ICON,
                this.getX() + 2,
                this.getY() + 2,
                0f,
                0f,
                16,
                16,
                16,
                16);
    }
}
