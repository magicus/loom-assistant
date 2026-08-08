/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.extensions;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class ScreenExtensionWidget {
    public abstract void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta);
}
