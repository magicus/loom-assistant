/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.LoomScreenStateManager;
import se.icus.mag.loomassistant.gui.extensions.LoomRecipePanel;
import se.icus.mag.loomassistant.gui.extensions.LoomScreenLeftBar;
import se.icus.mag.loomassistant.gui.extensions.WeavingGuide;

public class LoomScreenExtension implements ScreenExtension {
    private static final int CONTENT_X_SHIFT = 3;
    private static final int BG_LEFT_PADDING = 19;
    private static final int PANEL_TAB_LEFT_OVERHANG = 32;
    private static final int CUSTOM_BG_WIDTH = 278;
    private static final int CUSTOM_BG_HEIGHT = 256;

    private static final Identifier BG_LOCATION =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/loom-gui.png");

    private final LoomScreen screen;
    private final LoomScreenStateManager manager;

    private LoomScreenLeftBar leftBar;
    private LoomRecipePanel panel;
    private WeavingGuide weavingGuide;
    private List<ScreenExtension> widgets;

    public LoomScreenExtension(LoomScreen screen) {
        this.screen = screen;
        this.manager = LoomAssistantMod.getLoomManager();
        this.widgets = List.of();
    }

    public LoomRecipePanel getPanel() {
        return panel;
    }

    public void init() {
        manager.onLoomScreenOpened(screen.menu);
        this.screen.leftPos = manager.isPanelOpen() ? getOpenLeftPos() : getClosedLeftPos();
        this.weavingGuide = new WeavingGuide(screen, manager);
        this.leftBar = new LoomScreenLeftBar(screen, manager, this::onPanelVisibilityChanged);
        refreshPanel();

        rebuildWidgetList();

        for (ScreenExtension widget : widgets) {
            widget.init();
        }
    }

    public void removed() {
        for (ScreenExtension widget : widgets) {
            widget.removed();
        }
        manager.onLoomScreenClosed();
    }

    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        manager.tick();
        for (ScreenExtension widget : widgets) {
            widget.extractBackground(context, mouseX, mouseY, delta);
        }
    }

    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent) {
        for (ScreenExtension widget : widgets) {
            if (widget.mouseClicked(mouseButtonEvent)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        for (ScreenExtension widget : widgets) {
            if (widget.mouseReleased(mouseButtonEvent)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (ScreenExtension widget : widgets) {
            if (widget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        for (ScreenExtension widget : widgets) {
            if (widget.keyPressed(event)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        for (ScreenExtension widget : widgets) {
            if (widget.charTyped(event)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        for (ScreenExtension widget : widgets) {
            if (widget.mouseDragged(event, dx, dy)) {
                return true;
            }
        }
        return false;
    }

    public void drawCustomBackground(
            GuiGraphicsExtractor graphics,
            RenderPipeline renderPipeline,
            Identifier originalTexture,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int textureWidth,
            int textureHeight) {
        graphics.blit(
                renderPipeline,
                BG_LOCATION,
                x - BG_LEFT_PADDING - CONTENT_X_SHIFT,
                y,
                u,
                v,
                width + BG_LEFT_PADDING + CONTENT_X_SHIFT,
                height,
                CUSTOM_BG_WIDTH,
                CUSTOM_BG_HEIGHT);
    }

    private void onPanelVisibilityChanged() {
        screen.leftPos = manager.isPanelOpen() ? getOpenLeftPos() : getClosedLeftPos();
        if (leftBar != null) {
            leftBar.refreshLayout();
        }
        refreshPanel();
    }

    private int getClosedLeftPos() {
        int guiExtraLeft = BG_LEFT_PADDING + CONTENT_X_SHIFT;
        int visualGuiWidth = screen.imageWidth + guiExtraLeft;
        int visualLeft = (screen.width - visualGuiWidth) / 2;
        return visualLeft + guiExtraLeft;
    }

    private int getOpenLeftPos() {
        int leftExtensionWithoutTabs = LoomRecipePanel.PANEL_WIDTH + 5 + BG_LEFT_PADDING;
        int centeredAreaWidth = screen.imageWidth + leftExtensionWithoutTabs;
        int centeredAreaLeft = (screen.width - centeredAreaWidth) / 2;
        int leftPos = centeredAreaLeft + leftExtensionWithoutTabs;

        int panelLeft = leftPos - leftExtensionWithoutTabs;
        int tabLeft = panelLeft - PANEL_TAB_LEFT_OVERHANG;
        if (tabLeft < 0) {
            leftPos -= tabLeft;
        }
        return leftPos;
    }

    private int getPanelX() {
        return screen.leftPos - LoomRecipePanel.PANEL_WIDTH - 5 - BG_LEFT_PADDING;
    }

    private void refreshPanel() {
        this.panel = manager.isPanelOpen()
                ? new LoomRecipePanel(manager, screen, screen.menu, getPanelX(), screen.topPos)
                : null;
        rebuildWidgetList();
    }

    private void rebuildWidgetList() {
        List<ScreenExtension> updated = new ArrayList<>(3);
        if (panel != null) {
            updated.add(panel);
        }
        if (weavingGuide != null) {
            updated.add(weavingGuide);
        }
        if (leftBar != null) {
            updated.add(leftBar);
        }
        widgets = List.copyOf(updated);
    }
}
