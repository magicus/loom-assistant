/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.config;

import java.util.ArrayList;
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
    private static final int BUTTON_GAP = 4;

    private final Button leftButton;
    private final Button rightButton;
    private final List<GuiEventListener> children;
    private final List<NarratableEntry> narratables;

    public ActionButtonListEntry(Component fieldName, Component buttonText, Consumer<Screen> onPress) {
        this(fieldName, buttonText, onPress, null, null);
    }

    public ActionButtonListEntry(
            Component fieldName,
            Component leftButtonText,
            Consumer<Screen> leftOnPress,
            Component rightButtonText,
            Consumer<Screen> rightOnPress) {
        super(fieldName, null, false);
        this.leftButton = Button.builder(leftButtonText, button -> leftOnPress.accept(getConfigScreen()))
                .bounds(0, 0, BUTTON_WIDTH, 20)
                .build();

        if (rightButtonText != null && rightOnPress != null) {
            this.rightButton = Button.builder(rightButtonText, button -> rightOnPress.accept(getConfigScreen()))
                    .bounds(0, 0, BUTTON_WIDTH, 20)
                    .build();
        } else {
            this.rightButton = null;
        }

        List<GuiEventListener> childList = new ArrayList<>();
        childList.add(this.leftButton);
        if (this.rightButton != null) {
            childList.add(this.rightButton);
        }
        this.children = List.copyOf(childList);

        List<NarratableEntry> narratableList = new ArrayList<>();
        narratableList.add(this.leftButton);
        if (this.rightButton != null) {
            narratableList.add(this.rightButton);
        }
        this.narratables = List.copyOf(narratableList);
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
        this.leftButton.active = isEnabled();
        if (this.rightButton != null) {
            this.rightButton.active = isEnabled();
            int groupWidth = this.leftButton.getWidth() + BUTTON_GAP + this.rightButton.getWidth();
            int leftX = x + (entryWidth - groupWidth) / 2;
            this.leftButton.setPosition(leftX, y);
            this.rightButton.setPosition(leftX + this.leftButton.getWidth() + BUTTON_GAP, y);
            this.leftButton.extractRenderState(graphics, mouseX, mouseY, delta);
            this.rightButton.extractRenderState(graphics, mouseX, mouseY, delta);
        } else {
            this.leftButton.setPosition(x + (entryWidth - this.leftButton.getWidth()) / 2, y);
            this.leftButton.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean clicked = this.leftButton.mouseClicked(event, doubleClick);
        if (this.rightButton != null) {
            clicked = this.rightButton.mouseClicked(event, doubleClick) || clicked;
        }
        return clicked || super.mouseClicked(event, doubleClick);
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
