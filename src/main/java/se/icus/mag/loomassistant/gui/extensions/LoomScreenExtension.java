/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.extensions;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.loomassistant.gui.panel.LoomRecipePanel;

/**
 * Coordinates the LoomScreen-specific extension pieces while keeping the mixin hooks minimal.
 */
public class LoomScreenExtension {
    private static final int CONTENT_X_SHIFT = 3;
    private static final int BG_LEFT_PADDING = 19;
    private static final int PANEL_TAB_LEFT_OVERHANG = 32;
    private static final int CUSTOM_BG_WIDTH = 278;
    private static final int CUSTOM_BG_HEIGHT = 256;

    private static final Identifier BG_LOCATION =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/loom-gui.png");

    private final LoomScreen screen;

    private LoomScreenState state;
    private LoomScreenLeftBar leftBar;
    private LoomRecipePanel panel;
    private WeavingGuide weavingGuide;

    public LoomScreenExtension(LoomScreen screen) {
        this.screen = screen;
    }

    public LoomRecipePanel getPanel() {
        return panel;
    }

    public LoomScreenState getState() {
        return state;
    }

    public void onInit() {
        this.state = new LoomScreenState(screen.minecraft, screen.menu);
        this.screen.leftPos = state.isPanelOpen() ? getOpenLeftPos() : getClosedLeftPos();
        this.weavingGuide = new WeavingGuide(screen, state);
        this.leftBar = new LoomScreenLeftBar(screen, state, this::onPanelVisibilityChanged);
        this.leftBar.onInit();
        refreshPanel();
    }

    public void onExtractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        state.tick();
        if (panel != null) {
            panel.render(context, mouseX, mouseY, delta);
        }
        weavingGuide.render(context);
        leftBar.render(context, mouseX, mouseY);
    }

    public void onMouseClicked(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        if (leftBar != null && leftBar.mouseClicked(mouseButtonEvent)) {
            cir.setReturnValue(true);
            return;
        }

        if (panel != null && panel.mouseClicked(mouseButtonEvent)) {
            cir.setReturnValue(true);
        }
    }

    public void onMouseReleased(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        if (leftBar != null && leftBar.mouseReleased(mouseButtonEvent)) {
            cir.setReturnValue(true);
        }
    }

    public void onMouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount,
            CallbackInfoReturnable<Boolean> cir) {
        if (panel != null && panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
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
        screen.leftPos = state.isPanelOpen() ? getOpenLeftPos() : getClosedLeftPos();
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
        this.panel = state.isPanelOpen()
                ? new LoomRecipePanel(state, screen, screen.menu, getPanelX(), screen.topPos)
                : null;
    }
}
