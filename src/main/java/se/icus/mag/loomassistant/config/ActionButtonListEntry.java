/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.config;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class ActionButtonListEntry extends TooltipListEntry<Void> {
    private static final int BUTTON_WIDTH = 170;

    private final Button actionButton;
    private final List<GuiEventListener> children;
    private final List<NarratableEntry> narratables;

    public ActionButtonListEntry(Component fieldName, Component buttonText, Consumer<Screen> onPress) {
        super(fieldName, null, false);
        this.actionButton = Button.builder(buttonText, button -> onPress.accept(getConfigScreen()))
                .bounds(0, 0, BUTTON_WIDTH, 20)
                .build();
        this.children = List.of(this.actionButton);
        this.narratables = List.of(this.actionButton);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int index,
            int y,
            int x,
            int entryWidth,
            int entryHeight,
            int mouseX,
            int mouseY,
            boolean isHovered,
            float delta) {
        super.extractRenderState(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        this.actionButton.active = isEnabled();
        this.actionButton.setPosition(x + (entryWidth - this.actionButton.getWidth()) / 2, y);
        this.actionButton.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return this.actionButton.mouseClicked(event, doubleClick) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public Optional<Void> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return children;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return narratables;
    }
}
