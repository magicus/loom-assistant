/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import se.icus.mag.loomassistant.storage.ActivePacksConfig;
import se.icus.mag.loomassistant.storage.BannerPackRepository;
import se.icus.mag.loomassistant.types.bannerpack.BannerPack;

/**
 * Screen for managing which banner packs are active.
 * Similar to Vanilla's resource pack screen, shows available packs on the left
 * and active packs on the right, with buttons to move between them.
 */
public class BannerPackManagerScreen extends Screen {
    private static final int PANEL_W = 160;
    private static final int PANEL_H = 200;
    private static final int PADDING = 5;
    private static final int HEADER_H = 14;
    private static final int ENTRY_H = 20;
    private static final int BTN_W = 30;
    private static final int BTN_H = 14;

    // Colors
    private static final int COL_BG = 0xFF1A1A2E;
    private static final int COL_HEADER = 0xFF16213E;
    private static final int COL_BORDER = 0xFF4169E1;
    private static final int COL_ACCENT = 0xFF4169E1;
    private static final int COL_SEL = 0xFF4169E1;
    private static final int COL_HOVER = 0x40FFFFFF;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_TEXT_DIM = 0xFFAAAAAA;
    private static final int COL_BTN = 0xFF1E40AF;
    private static final int COL_BTN_HOVER = 0xFF5C7CFA;
    private static final int COL_DISABLED = 0xFF505050;

    private final Screen previousScreen;
    private final BannerPackRepository packRepository;
    private final ActivePacksConfig activePacksConfig;

    // State
    private String selectedAvailableId = null;
    private String selectedActiveId = null;
    private int availableScrollOffset = 0;
    private int activeScrollOffset = 0;

    public BannerPackManagerScreen(
            Screen previousScreen, BannerPackRepository packRepository, ActivePacksConfig activePacksConfig) {
        super(Component.literal("Banner Pack Manager"));
        this.previousScreen = previousScreen;
        this.packRepository = packRepository;
        this.activePacksConfig = activePacksConfig;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        int cx = contentX();
        int cy = contentY();

        // Dim world behind
        ctx.fill(0, 0, this.width, this.height, 0xAA000000);

        // Background
        int totalW = PANEL_W * 2 + 50 + PADDING * 4;
        ctx.fill(cx, cy, cx + totalW, cy + PANEL_H + 60, COL_BG);
        ctx.outline(cx, cy, totalW, PANEL_H + 60, COL_BORDER);

        // Render available packs (left panel)
        renderAvailablePacksPanel(ctx, cx, cy, mouseX, mouseY);

        // Render buttons (middle)
        renderButtons(ctx, cx + PANEL_W + PADDING, cy, mouseX, mouseY);

        // Render active packs (right panel)
        renderActivePacksPanel(ctx, cx + PANEL_W + 50, cy, mouseX, mouseY);

        // Render close button
        renderCloseButton(ctx, cx + totalW - 20, cy + 5, mouseX, mouseY);

        super.extractRenderState(ctx, mouseX, mouseY, delta);
    }

    private void renderAvailablePacksPanel(GuiGraphicsExtractor ctx, int cx, int cy, int mx, int my) {
        int px = cx + PADDING;
        int py = cy + PADDING;

        // Header
        ctx.fill(px, py, px + PANEL_W, py + HEADER_H, COL_HEADER);
        ctx.text(this.font, Component.literal("Available"), px + 2, py + 3, COL_ACCENT, true);

        List<String> available = getAvailablePacks();
        renderPackList(
                ctx,
                px,
                py + HEADER_H,
                PANEL_W,
                PANEL_H - HEADER_H,
                available,
                selectedAvailableId,
                availableScrollOffset,
                true,
                mx,
                my);
    }

    private void renderActivePacksPanel(GuiGraphicsExtractor ctx, int cx, int cy, int mx, int my) {
        int px = cx + PADDING;
        int py = cy + PADDING;

        // Header
        ctx.fill(px, py, px + PANEL_W, py + HEADER_H, COL_HEADER);
        ctx.text(this.font, Component.literal("Active"), px + 2, py + 3, COL_ACCENT, true);

        List<String> active = activePacksConfig.getActivePacks();
        renderPackList(
                ctx,
                px,
                py + HEADER_H,
                PANEL_W,
                PANEL_H - HEADER_H,
                active,
                selectedActiveId,
                activeScrollOffset,
                false,
                mx,
                my);
    }

    private void renderPackList(
            GuiGraphicsExtractor ctx,
            int x,
            int y,
            int w,
            int h,
            List<String> packs,
            String selected,
            int scrollOffset,
            boolean isAvailable,
            int mx,
            int my) {
        int visibleRows = h / ENTRY_H;
        int totalRows = packs.size();

        // Background
        ctx.fill(x, y, x + w, y + h, 0xFF000000);
        ctx.outline(x, y, w, h, COL_BORDER);

        for (int i = 0; i < visibleRows && scrollOffset + i < totalRows; i++) {
            int idx = scrollOffset + i;
            String packId = packs.get(idx);
            int entryY = y + i * ENTRY_H;

            // Check if hovered
            boolean hovered = mx >= x && mx < x + w && my >= entryY && my < entryY + ENTRY_H;
            boolean isSelected = packId.equals(selected);
            boolean isLocal = packId.equals(BannerPackRepository.LOCAL_PACK_ID);

            // Background
            int bgColor = isSelected ? COL_SEL : (hovered ? COL_HOVER : 0);
            if (bgColor != 0) {
                ctx.fill(x, entryY, x + w, entryY + ENTRY_H, bgColor);
            }

            // Text
            BannerPack pack = packRepository.getPack(packId);
            String displayName = pack != null ? pack.getMetadata().description() : packId;
            if (isLocal) {
                displayName = displayName + " *";
            }
            String label = truncate(displayName, w - 6);
            ctx.text(this.font, Component.literal(label), x + 3, entryY + 5, COL_TEXT, true);
        }

        // Scroll indicators
        if (scrollOffset > 0) {
            ctx.text(this.font, Component.literal("▲"), x + w - 8, y + 2, COL_TEXT_DIM, true);
        }
        if (scrollOffset + visibleRows < totalRows) {
            ctx.text(this.font, Component.literal("▼"), x + w - 8, y + h - 8, COL_TEXT_DIM, true);
        }
    }

    private void renderButtons(GuiGraphicsExtractor ctx, int x, int y, int mx, int my) {
        // Right arrow button (move to active)
        boolean rightHovered = isInRightButton(mx, my, x, y);
        int rightColor = rightHovered && selectedAvailableId != null
                ? COL_BTN_HOVER
                : (selectedAvailableId != null ? COL_BTN : COL_DISABLED);
        ctx.fill(x, y + 30, x + BTN_W, y + 30 + BTN_H, rightColor);
        ctx.text(this.font, Component.literal("→"), x + 10, y + 35, 0xFFFFFFFF, true);

        // Left arrow button (move to available)
        boolean leftHovered = isInLeftButton(mx, my, x, y);
        int leftColor =
                leftHovered && selectedActiveId != null && !BannerPackRepository.LOCAL_PACK_ID.equals(selectedActiveId)
                        ? COL_BTN_HOVER
                        : (selectedActiveId != null && !BannerPackRepository.LOCAL_PACK_ID.equals(selectedActiveId)
                                ? COL_BTN
                                : COL_DISABLED);
        ctx.fill(x, y + 50, x + BTN_W, y + 50 + BTN_H, leftColor);
        ctx.text(this.font, Component.literal("←"), x + 10, y + 55, 0xFFFFFFFF, true);
    }

    private void renderCloseButton(GuiGraphicsExtractor ctx, int x, int y, int mx, int my) {
        boolean hovered = mx >= x && mx < x + 20 && my >= y && my < y + 14;
        ctx.fill(x, y, x + 20, y + 14, hovered ? COL_BTN_HOVER : COL_BTN);
        ctx.text(this.font, Component.literal("Close"), x + 2, y + 3, 0xFFFFFFFF, true);
    }

    private List<String> getAvailablePacks() {
        List<String> available = new ArrayList<>();
        List<String> active = activePacksConfig.getActivePacks();

        for (String packId : packRepository.getPacks().keySet()) {
            if (!active.contains(packId)) {
                available.add(packId);
            }
        }

        return available;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        // Handle scroll events for both panels
        if (mouseX < contentX() + PANEL_W + PADDING * 2) {
            // Left panel
            if (vAmt > 0) {
                availableScrollOffset = Math.max(0, availableScrollOffset - 1);
            } else {
                availableScrollOffset++;
            }
        } else if (mouseX > contentX() + PANEL_W + 50) {
            // Right panel
            if (vAmt > 0) {
                activeScrollOffset = Math.max(0, activeScrollOffset - 1);
            } else {
                activeScrollOffset++;
            }
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = event.button();
        if (button != 0) return false;

        double mouseX = event.x();
        double mouseY = event.y();

        int cx = contentX();
        int cy = contentY();

        // Check close button
        if (isInCloseButton((int) mouseX, (int) mouseY, cx + PANEL_W * 2 + 50 + PADDING * 4 - 20, cy + 5)) {
            this.minecraft.gui.setScreen(previousScreen);
            return true;
        }

        // Check right button (move to active)
        if (isInRightButton((int) mouseX, (int) mouseY, cx + PANEL_W + PADDING, cy) && selectedAvailableId != null) {
            activePacksConfig.enablePack(selectedAvailableId);
            selectedAvailableId = null;
            return true;
        }

        // Check left button (move to available)
        if (isInLeftButton((int) mouseX, (int) mouseY, cx + PANEL_W + PADDING, cy)
                && selectedActiveId != null
                && !BannerPackRepository.LOCAL_PACK_ID.equals(selectedActiveId)) {
            activePacksConfig.disablePack(selectedActiveId);
            selectedActiveId = null;
            return true;
        }

        // Check left panel click
        if (mouseX >= cx + PADDING
                && mouseX < cx + PADDING + PANEL_W
                && mouseY >= cy + PADDING + HEADER_H
                && mouseY < cy + PADDING + PANEL_H) {
            int row = (int) ((mouseY - cy - PADDING - HEADER_H) / ENTRY_H) + availableScrollOffset;
            List<String> available = getAvailablePacks();
            if (row >= 0 && row < available.size()) {
                selectedAvailableId = available.get(row);
                selectedActiveId = null;
            }
            return true;
        }

        // Check right panel click
        if (mouseX >= cx + PANEL_W + 50
                && mouseX < cx + PANEL_W + 50 + PANEL_W
                && mouseY >= cy + PADDING + HEADER_H
                && mouseY < cy + PADDING + PANEL_H) {
            int row = (int) ((mouseY - cy - PADDING - HEADER_H) / ENTRY_H) + activeScrollOffset;
            List<String> active = activePacksConfig.getActivePacks();
            if (row >= 0 && row < active.size()) {
                selectedActiveId = active.get(row);
                selectedAvailableId = null;
            }
            return true;
        }

        return false;
    }

    private boolean isInCloseButton(int mx, int my, int x, int y) {
        return mx >= x && mx < x + 20 && my >= y && my < y + 14;
    }

    private boolean isInRightButton(int mx, int my, int x, int y) {
        return mx >= x && mx < x + BTN_W && my >= y + 30 && my < y + 30 + BTN_H;
    }

    private boolean isInLeftButton(int mx, int my, int x, int y) {
        return mx >= x && mx < x + BTN_W && my >= y + 50 && my < y + 50 + BTN_H;
    }

    private String truncate(String text, int maxWidth) {
        while (text.length() > 0 && this.font.width(text) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private int contentX() {
        return (this.width - (PANEL_W * 2 + 50 + PADDING * 4)) / 2;
    }

    private int contentY() {
        return (this.height - (PANEL_H + 60)) / 2;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(previousScreen);
    }
}
