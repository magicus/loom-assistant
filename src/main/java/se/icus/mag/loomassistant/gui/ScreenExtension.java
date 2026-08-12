/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public interface ScreenExtension {
    default void init() {}

    default void removed() {}

    void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta);

    default boolean mouseClicked(MouseButtonEvent event) {
        return false;
    }

    default boolean mouseReleased(MouseButtonEvent event) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    default boolean keyPressed(KeyEvent event) {
        return false;
    }

    default boolean charTyped(CharacterEvent event) {
        return false;
    }

    default boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return false;
    }
}
