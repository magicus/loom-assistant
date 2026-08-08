/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.screens.packselection;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class HeaderEntry extends BannerPackListEntry {
    private final Font font;
    private final Component text;

    public HeaderEntry(Font font, Component text) {
        this.font = font;
        this.text = text;
    }

    @Override
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        graphics.centeredText(
                this.font, this.text, this.getContentX() + this.getWidth() / 2, this.getContentYMiddle() - 9 / 2, -1);
    }

    @Override
    public Component getNarration() {
        return this.text;
    }

    @Override
    public String getPackId() {
        return "";
    }
}
