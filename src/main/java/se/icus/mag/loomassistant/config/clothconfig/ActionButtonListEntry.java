/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.config.clothconfig;

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

/**
 * Cloth config adaptation to allow a list entry with one or two buttons, for
 * putting generic actions in an AutoConfigClient.
 */
@Environment(EnvType.CLIENT)
public class ActionButtonListEntry extends TooltipListEntry<Void> {
    private static final int BUTTON_WIDTH = 170;
    private static final int BUTTON_GAP = 4;

    private final List<Button> buttons;
    private final List<GuiEventListener> children;
    private final List<NarratableEntry> narratables;

    public record ActionButtonSpec(Component label, Consumer<Screen> onPress) {}

    public ActionButtonListEntry(Component fieldName, List<ActionButtonSpec> actions) {
        super(fieldName, null, false);
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException("ActionButtonListEntry requires at least one button action");
        }

        List<Button> createdButtons = new ArrayList<>();
        for (ActionButtonSpec action : actions) {
            createdButtons.add(
                    Button.builder(action.label(), button -> action.onPress().accept(getConfigScreen()))
                            .bounds(0, 0, BUTTON_WIDTH, 20)
                            .build());
        }
        this.buttons = List.copyOf(createdButtons);

        List<GuiEventListener> childList = new ArrayList<>();
        childList.addAll(this.buttons);
        this.children = List.copyOf(childList);

        List<NarratableEntry> narratableList = new ArrayList<>();
        narratableList.addAll(this.buttons);
        this.narratables = List.copyOf(narratableList);
    }

    public ActionButtonListEntry(
            Component fieldName,
            Component buttonTextLeft,
            Consumer<Screen> onPressLeft,
            Component buttonTextRight,
            Consumer<Screen> onPressRight) {
        this(
                fieldName,
                buttonTextRight != null && onPressRight != null
                        ? List.of(
                                new ActionButtonSpec(buttonTextLeft, onPressLeft),
                                new ActionButtonSpec(buttonTextRight, onPressRight))
                        : List.of(new ActionButtonSpec(buttonTextLeft, onPressLeft)));
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
        int groupWidth =
                this.buttons.stream().mapToInt(Button::getWidth).sum() + BUTTON_GAP * (this.buttons.size() - 1);
        int buttonX = x + (entryWidth - groupWidth) / 2;
        for (Button button : this.buttons) {
            button.active = isEnabled();
            button.setPosition(buttonX, y);
            button.extractRenderState(graphics, mouseX, mouseY, delta);
            buttonX += button.getWidth() + BUTTON_GAP;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean clicked = false;
        for (Button button : this.buttons) {
            clicked = button.mouseClicked(event, doubleClick) || clicked;
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
