/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.data.BannerPatternLayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import se.icus.mag.loomassistant.config.LoomAssistantConfig;
import se.icus.mag.loomassistant.data.BannerStorage;
import se.icus.mag.loomassistant.data.SavedBanner;

/**
 * Full-screen browser for viewing and acting on saved banner recipes.
 * Displays a pack list on the left (currently only "root"), a banner list in the center,
 * and a details/actions panel on the right.
 */
public class BannerBrowserScreen extends Screen {
    private static final int PANEL_PACK_W = 90;
    private static final int PANEL_BANNER_W = 150;
    private static final int PANEL_DESC_W = 140;
    private static final int TOTAL_W = PANEL_PACK_W + PANEL_BANNER_W + PANEL_DESC_W;
    private static final int TOTAL_H = 240;
    private static final int HEADER_H = 14;
    private static final int ENTRY_H = 20;
    private static final int BTN_H = 14;
    private static final int PADDING = 5;
    private static final int PANEL_EXPLORER_W = PANEL_PACK_W + PANEL_BANNER_W;
    private static final int GRID_COLS = 4;
    private static final int CELL_H = 40;

    // Colors
    private static final int COL_BG = 0xFF1A1A2E;
    private static final int COL_HEADER = 0xFF16213E;
    private static final int COL_BORDER = 0xFF4169E1;
    private static final int COL_ACCENT = 0xFF4169E1;
    private static final int COL_SEL = 0xFF4169E1;
    private static final int COL_HOVER = 0x40FFFFFF;
    private static final int COL_STRIPE = 0x10FFFFFF;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_TEXT_DIM = 0xFFAAAAAA;
    private static final int COL_BTN = 0xFF1E40AF;
    private static final int COL_BTN_HOVER = 0xFF5C7CFA;
    private static final int COL_BTN_PRIMARY = 0xFF2E7D32;
    private static final int COL_BTN_PRIMARY_HOVER = 0xFF66BB6A;

    private final Screen previousScreen;

    // Selection / scroll state
    private String currentPackId = null; // null = root view, string = inside that pack
    private String selectedBannerId = null;
    private int gridScrollOffset = 0;

    // Double-click detection
    private String lastClickedBannerId = null;
    private long lastClickTime = 0;

    public BannerBrowserScreen(Screen previousScreen) {
        super(Component.translatable("loom-assistant.screen.banner_browser.title"));
        this.previousScreen = previousScreen;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        int cx = contentX();
        int cy = contentY();

        // Dim world behind the screen
        ctx.fill(0, 0, this.width, this.height, 0xAA000000);

        // Main panel
        ctx.fill(cx, cy, cx + TOTAL_W, cy + TOTAL_H, COL_BG);
        ctx.outline(cx, cy, TOTAL_W, TOTAL_H, COL_BORDER);

        // Divider between explorer and details panels
        ctx.fill(cx + PANEL_EXPLORER_W, cy, cx + PANEL_EXPLORER_W + 1, cy + TOTAL_H, COL_BORDER);

        renderExplorerPanel(ctx, cx, cy, mouseX, mouseY);
        renderDescPanel(ctx, cx, cy, mouseX, mouseY);

        super.extractRenderState(ctx, mouseX, mouseY, delta);
    }

    private void renderExplorerPanel(GuiGraphicsExtractor ctx, int cx, int cy, int mx, int my) {
        int ew = PANEL_EXPLORER_W;
        int innerW = ew - PADDING * 2;
        int cellW = innerW / GRID_COLS;

        // Header
        ctx.fill(cx, cy, cx + ew, cy + HEADER_H, COL_HEADER);
        if (currentPackId == null) {
            List<SavedBanner> all = BannerStorage.getInstance().getBanners();
            ctx.text(
                    this.font,
                    Component.translatable("loom-assistant.screen.banner_browser.banners_count", all.size()),
                    cx + PADDING,
                    cy + 3,
                    COL_ACCENT,
                    true);
        } else {
            boolean backHov = isInBackButton(mx, my, cx, cy);
            ctx.text(
                    this.font,
                    Component.translatable("loom-assistant.common.back_symbol"),
                    cx + PADDING,
                    cy + 3,
                    backHov ? COL_TEXT : COL_ACCENT,
                    true);
            ctx.text(this.font, Component.literal(currentPackId), cx + PADDING + 12, cy + 3, COL_TEXT, true);
        }

        List<SavedBanner> items = getGridItems();
        int totalRows = (items.size() + GRID_COLS - 1) / GRID_COLS;
        int gridStartY = cy + HEADER_H;
        int maxVisRows = (TOTAL_H - HEADER_H) / CELL_H;

        for (int row = gridScrollOffset; row < Math.min(gridScrollOffset + maxVisRows, totalRows); row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int idx = row * GRID_COLS + col;
                if (idx >= items.size()) break;
                SavedBanner banner = items.get(idx);
                int cellX = cx + PADDING + col * cellW;
                int cellY = gridStartY + (row - gridScrollOffset) * CELL_H;

                boolean hovered = mx >= cellX && mx < cellX + cellW && my >= cellY && my < cellY + CELL_H;
                boolean selected = banner.getId().equals(selectedBannerId);
                ctx.fill(
                        cellX, cellY, cellX + cellW, cellY + CELL_H, selected ? COL_SEL : (hovered ? COL_HOVER : 0));

                BannerPreviewRenderer.render(ctx, banner, null, cellX + (cellW - 16) / 2, cellY + 6, 16);

                String label = truncate(banner.getDisplayName(), cellW - 4);
                int lw = this.font.width(label);
                ctx.text(
                        this.font,
                        Component.literal(label),
                        cellX + (cellW - lw) / 2,
                        cellY + 25,
                        COL_TEXT_DIM,
                        true);
            }
        }

        if (gridScrollOffset > 0) {
            ctx.text(
                    this.font,
                    Component.translatable("loom-assistant.common.scroll_up_symbol"),
                    cx + ew - 10,
                    gridStartY + 2,
                    COL_TEXT_DIM,
                    true);
        }
        int maxScroll = Math.max(0, totalRows - maxVisRows);
        if (gridScrollOffset < maxScroll) {
            ctx.text(
                    this.font,
                    Component.translatable("loom-assistant.common.scroll_down_symbol"),
                    cx + ew - 10,
                    cy + TOTAL_H - 10,
                    COL_TEXT_DIM,
                    true);
        }
    }

    private boolean isInBackButton(int mx, int my, int cx, int cy) {
        return currentPackId != null && mx >= cx && mx < cx + 20 && my >= cy && my < cy + HEADER_H;
    }

    private List<SavedBanner> getGridItems() {
        // TODO: filter by currentPackId when packs are implemented
        return BannerStorage.getInstance().getBanners();
    }

    private void renderDescPanel(GuiGraphicsExtractor ctx, int cx, int cy, int mx, int my) {
        int dx = cx + PANEL_PACK_W + PANEL_BANNER_W + 1;
        int dy = cy;
        int dw = PANEL_DESC_W - 1;

        // Header
        ctx.fill(dx, dy, dx + dw, dy + HEADER_H, COL_HEADER);
        ctx.text(
                this.font,
                Component.translatable("loom-assistant.screen.banner_browser.details"),
                dx + PADDING,
                dy + 3,
                COL_ACCENT,
                true);

        SavedBanner sel = getSelectedBanner();
        if (sel == null) {
            String hint = Component.translatable("loom-assistant.panel.select_banner").getString();
            int hw = this.font.width(hint);
            ctx.text(this.font, Component.literal(hint), dx + (dw - hw) / 2, dy + TOTAL_H / 2, COL_TEXT_DIM, true);
            return;
        }

        int ty = dy + HEADER_H + PADDING;

        // Banner preview centered
        BannerPreviewRenderer.render(ctx, sel, null, dx + (dw - 16) / 2, ty, 16);
        ty += 20;

        // Name (centered, bold-ish via shadow)
        String name = truncate(sel.getDisplayName(), dw - PADDING * 2);
        int nw = this.font.width(name);
        ctx.text(this.font, Component.literal(name), dx + (dw - nw) / 2, ty, COL_TEXT, true);
        ty += 11;

        // Action buttons anchored to the bottom
        int btnW = dw - PADDING * 2;
        int bbx = dx + PADDING;
        int bottomY = dy + TOTAL_H - PADDING;
        int numButtons = 4;
        int startBtnY = bottomY - numButtons * (BTN_H + 3) + 3;

        // Materials list fills the space between metadata and buttons
        renderMaterialsList(ctx, sel, dx, ty, dw, startBtnY - ty - PADDING);

        renderButton(ctx, mx, my, bbx, startBtnY, btnW, BTN_H, defaultActionLabel(), true);
    renderButton(
        ctx,
        mx,
        my,
        bbx,
        startBtnY + (BTN_H + 3),
        btnW,
        BTN_H,
        Component.translatable("loom-assistant.tooltip.weave").getString(),
        false);
    renderButton(
        ctx,
        mx,
        my,
        bbx,
        startBtnY + (BTN_H + 3) * 2,
        btnW,
        BTN_H,
        Component.translatable("loom-assistant.screen.banner_browser.edit_soon").getString(),
        false);
    renderButton(
        ctx,
        mx,
        my,
        bbx,
        startBtnY + (BTN_H + 3) * 3,
        btnW,
        BTN_H,
        Component.translatable("loom-assistant.screen.banner_browser.change_colors_soon").getString(),
        false);
    }

    private void renderMaterialsList(
            GuiGraphicsExtractor ctx, SavedBanner sel, int dx, int ty, int dw, int availH) {
        int rowH = 18;
        int iconW = 16;
        int lx = dx + PADDING;
        int maxRows = availH / rowH;

        // Row 1: base banner — icon + i18n name aligned with layer text column
        ctx.item(new ItemStack(sel.getBaseBannerItem()), lx, ty + 1);
        String colorKey = "block.minecraft." + sel.getBaseColorEnum().getSerializedName() + "_banner";
        String baseName = Language.getInstance().getOrDefault(colorKey);
        int layerTextX = lx + (iconW + 2) * 2;
        int layerTextMaxW = dw - PADDING * 2 - (iconW + 2) * 2;
        ctx.text(
                this.font,
                Component.literal(truncate(baseName, layerTextMaxW)),
                layerTextX,
                ty + 5,
                COL_TEXT_DIM,
                true);
        ty += rowH;

        List<BannerPatternLayer> layers = sel.getLayers();
        int visible = Math.min(layers.size(), Math.max(0, maxRows - 1));

        for (int i = 0; i < visible; i++) {
            BannerPatternLayer layer = layers.get(i);

            // Pattern preview: white banner with only this layer
            BannerPreviewRenderer.render(
                    ctx, new SavedBanner(null, DyeColor.WHITE, List.of(layer)), null, lx, ty + 1, 16);

            ctx.item(new ItemStack(SavedBanner.getDyeItem(layer.getDyeColorEnum())), lx + iconW + 2, ty + 1);

            String label = getPatternDisplayName(layer);
            ctx.text(
                    this.font,
                    Component.literal(truncate(label, layerTextMaxW)),
                    layerTextX,
                    ty + 5,
                    COL_TEXT_DIM,
                    true);
            ty += rowH;
        }

        if (visible < layers.size()) {
            ctx.text(
                    this.font,
                Component.translatable(
                    "loom-assistant.screen.banner_browser.more_count", layers.size() - visible),
                    lx,
                    ty,
                    COL_TEXT_DIM,
                    true);
        }
    }

    @SuppressWarnings("unchecked")
    private String getPatternDisplayName(BannerPatternLayer layer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            try {
                var regOpt = mc.level.registryAccess().lookup(Registries.BANNER_PATTERN);
                if (regOpt.isPresent()) {
                    Identifier id = Identifier.tryParse(layer.patternId());
                    if (id != null) {
                        var entry = regOpt.get().get(id);
                        if (entry.isPresent()) {
                            net.minecraft.world.level.block.entity.BannerPattern pattern =
                                    (net.minecraft.world.level.block.entity.BannerPattern) entry.get().value();
                            String key = pattern.translationKey() + "." + layer.getDyeColorEnum().getName();
                            return Component.translatable(key).getString();
                        }
                    }
                }
            } catch (Exception e) {
                // fall through
            }
        }
        return toDisplayName(layer.getDyeColorEnum().getSerializedName()) + " " + toDisplayName(layer.patternId());
    }

    private static String toDisplayName(String patternId) {
        String raw = patternId.contains(":") ? patternId.split(":", 2)[1] : patternId;
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split("_")) {
            if (!sb.isEmpty()) sb.append(' ');
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private void renderButton(
            GuiGraphicsExtractor ctx, int mx, int my, int bx, int by, int bw, int bh, String label, boolean primary) {
        boolean hovered = mx >= bx && mx < bx + bw && my >= by && my < by + bh;
        int color = primary ? (hovered ? COL_BTN_PRIMARY_HOVER : COL_BTN_PRIMARY) : (hovered ? COL_BTN_HOVER : COL_BTN);
        ctx.fill(bx, by, bx + bw, by + bh, color);
        String text = truncate(label, bw - PADDING * 2);
        int tw = this.font.width(text);
        ctx.text(this.font, Component.literal(text), bx + (bw - tw) / 2, by + (bh - 7) / 2, COL_TEXT, true);
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return false;
        double mx = event.x();
        double my = event.y();
        int cx = contentX();
        int cy = contentY();

        // Back button (only when inside a pack)
        if (currentPackId != null && isInBackButton((int) mx, (int) my, cx, cy)) {
            currentPackId = null;
            selectedBannerId = null;
            gridScrollOffset = 0;
            return true;
        }

        // Explorer grid cells
        int innerW = PANEL_EXPLORER_W - PADDING * 2;
        int cellW = innerW / GRID_COLS;
        int gridStartY = cy + HEADER_H;
        List<SavedBanner> items = getGridItems();
        int totalRows = (items.size() + GRID_COLS - 1) / GRID_COLS;
        int maxVisRows = (TOTAL_H - HEADER_H) / CELL_H;

        for (int row = gridScrollOffset; row < Math.min(gridScrollOffset + maxVisRows, totalRows); row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int idx = row * GRID_COLS + col;
                if (idx >= items.size()) break;
                SavedBanner banner = items.get(idx);
                int cellX = cx + PADDING + col * cellW;
                int cellY = gridStartY + (row - gridScrollOffset) * CELL_H;
                if (mx >= cellX && mx < cellX + cellW && my >= cellY && my < cellY + CELL_H) {
                    long now = System.currentTimeMillis();
                    boolean isDoubleClick =
                            banner.getId().equals(lastClickedBannerId) && (now - lastClickTime) < 400;
                    selectedBannerId = banner.getId();
                    lastClickedBannerId = banner.getId();
                    lastClickTime = now;
                    if (isDoubleClick || doubleClick) {
                        handleDefaultAction(banner);
                    }
                    return true;
                }
            }
        }

        // Description panel buttons
        SavedBanner sel = getSelectedBanner();
        if (sel != null) {
            int dx = cx + PANEL_EXPLORER_W + 1;
            int dw = PANEL_DESC_W - 1;
            int bbx = dx + PADDING;
            int btnW = dw - PADDING * 2;
            int bottomY = cy + TOTAL_H - PADDING;
            int startBtnY = bottomY - 4 * (BTN_H + 3) + 3;

            if (isInBtn(mx, my, bbx, startBtnY, btnW, BTN_H)) {
                handleDefaultAction(sel);
                return true;
            }
            if (isInBtn(mx, my, bbx, startBtnY + (BTN_H + 3), btnW, BTN_H)) {
                handleCraft(sel);
                return true;
            }
            // Edit / Change colors are stubs – consume the click silently
            if (isInBtn(mx, my, bbx, startBtnY + (BTN_H + 3) * 2, btnW, BTN_H)) return true;
            if (isInBtn(mx, my, bbx, startBtnY + (BTN_H + 3) * 3, btnW, BTN_H)) return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int cx = contentX();
        int cy = contentY();

        if (mx >= cx && mx < cx + PANEL_EXPLORER_W && my >= cy && my < cy + TOTAL_H) {
            List<SavedBanner> items = getGridItems();
            int totalRows = (items.size() + GRID_COLS - 1) / GRID_COLS;
            int maxVisRows = (TOTAL_H - HEADER_H) / CELL_H;
            int maxScroll = Math.max(0, totalRows - maxVisRows);
            gridScrollOffset = Math.max(0, Math.min(gridScrollOffset - (int) vAmt, maxScroll));
            return true;
        }
        return super.mouseScrolled(mx, my, hAmt, vAmt);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.gui.setScreen(previousScreen);
            return true;
        }
        return super.keyPressed(event);
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    private void handleDefaultAction(SavedBanner banner) {
        try {
            LoomAssistantConfig cfg = LoomAssistantMod.getConfig();
            switch (cfg.getDefaultAction()) {
                case ENQUEUE -> handleEnqueue(banner);
                case CRAFT -> handleCraft(banner);
                case SHOW -> handleShow(banner);
            }
        } catch (Exception e) {
            handleEnqueue(banner);
        }
    }

    private void handleEnqueue(SavedBanner banner) {
        // TODO: wire up to AutoCraftStateMachine queue in a later step
        this.minecraft.gui.setScreen(previousScreen);
    }

    private void handleCraft(SavedBanner banner) {
        // TODO: trigger auto-craft in a later step
        this.minecraft.gui.setScreen(previousScreen);
    }

    @SuppressWarnings("unused")
    private void handleShow(SavedBanner banner) {
        // Materials are shown inline in the details panel; nothing to navigate to.
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int contentX() {
        return (this.width - TOTAL_W) / 2;
    }

    private int contentY() {
        return (this.height - TOTAL_H) / 2;
    }

    private SavedBanner getSelectedBanner() {
        if (selectedBannerId == null) return null;
        return BannerStorage.getInstance().getBanners().stream()
                .filter(b -> b.getId().equals(selectedBannerId))
                .findFirst()
                .orElse(null);
    }

    private boolean isInBtn(double mx, double my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private String defaultActionLabel() {
        try {
            LoomAssistantConfig cfg = LoomAssistantMod.getConfig();
            return switch (cfg.getDefaultAction()) {
                case ENQUEUE -> Component.translatable("loom-assistant.action.enqueue_default").getString();
                case CRAFT -> Component.translatable("loom-assistant.action.weave_default").getString();
                case SHOW -> Component.translatable("loom-assistant.action.show_default").getString();
            };
        } catch (Exception e) {
            return Component.translatable("loom-assistant.action.enqueue_default").getString();
        }
    }

    private String truncate(String text, int maxW) {
        if (this.font.width(text) <= maxW) return text;
        while (!text.isEmpty() && this.font.width(text + "..") > maxW) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "..";
    }
}
