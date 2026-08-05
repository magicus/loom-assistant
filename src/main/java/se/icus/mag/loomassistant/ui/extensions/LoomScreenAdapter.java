/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui.extensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.world.inventory.LoomMenu;

/** Provides LoomScreenExtension live access to protected AbstractContainerScreen fields. */
public interface LoomScreenAdapter {
    int loomassistant$getLeftPos();

    void loomassistant$setLeftPos(int x);

    int loomassistant$getTopPos();

    int loomassistant$getImageWidth();

    int loomassistant$getImageHeight();

    int loomassistant$getScreenWidth();

    Font loomassistant$getFont();

    Minecraft loomassistant$getMinecraft();

    LoomMenu loomassistant$getMenu();

    LoomScreen loomassistant$asLoomScreen();

    <T extends AbstractWidget> T loomassistant$addWidget(T widget);
}
