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
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.LoomScreenStateManager;
import se.icus.mag.loomassistant.gui.panel.LoomRecipePanel;

public class LoomScreenExtension {
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

    public LoomScreenExtension(LoomScreen screen) {
        this.screen = screen;
        this.manager = LoomAssistantMod.getLoomManager();
    }

    public LoomRecipePanel getPanel() {
        return panel;
    }

    public void onInit() {
        manager.onLoomScreenOpened(screen.menu);
        this.screen.leftPos = manager.isPanelOpen() ? getOpenLeftPos() : getClosedLeftPos();
        this.weavingGuide = new WeavingGuide(screen, manager);
        this.leftBar = new LoomScreenLeftBar(screen, manager, this::onPanelVisibilityChanged);
        this.leftBar.onInit();
        refreshPanel();
    }

    public void onRemoved() {
        manager.onLoomScreenClosed();
    }

    public void onExtractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        manager.tick();
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
    }
}
