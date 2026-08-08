/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.screens.colorswitch;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.gui.extensions.LoomScreenExtension;
import se.icus.mag.loomassistant.gui.panel.LoomRecipePanel;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.util.DyeColorSorting;
import se.icus.mag.loomassistant.util.MathUtils;

public class BannerColorSwitchScreen extends Screen {
    // -------------------------------------------------------------------------
    // Layout – fixed 3-column, 3-row grid (column-major slot order)
    //   col 0: slots 0-2    col 1: slots 3-5    col 2: slot 6 / OK / Cancel
    // -------------------------------------------------------------------------

    private static final int MAX_SLOTS = 7;
    private static final int LAYOUT_COLS = 3;
    private static final int LAYOUT_ROWS = 3;

    private static final int ROW_H = 24;
    private static final int ICON_SIZE = 16;
    private static final int ARROW_W = 7;
    private static final int ARROW_H = 11;
    public static final int TARGET_BTN_W = 20;
    public static final int TARGET_BTN_H = 20;
    private static final int PAD = 10;
    private static final int COL_GAP = 14;
    private static final int TITLE_H = 24;

    // Width of a single slot column: [icon][gap][arrow][gap][target]
    private static final int SLOT_W = ICON_SIZE + 6 + ARROW_W + 6 + TARGET_BTN_W; // 60

    private static final int PANEL_W = PAD + LAYOUT_COLS * SLOT_W + (LAYOUT_COLS - 1) * COL_GAP + PAD;
    private static final int PANEL_H = TITLE_H + LAYOUT_ROWS * ROW_H + PAD;

    // Color picker popup
    private static final int PICKER_COLS = 4;
    private static final int PICKER_CELL = 20;
    private static final int PICKER_PAD = 6;
    private static final int PICKER_GRID_W = PICKER_COLS * PICKER_CELL;
    private static final int PICKER_GRID_ROWS = (int) Math.ceil(16.0 / PICKER_COLS);
    private static final int PICKER_W = PICKER_PAD + PICKER_GRID_W + PICKER_PAD;
    private static final int PICKER_H = PICKER_PAD + PICKER_GRID_ROWS * PICKER_CELL + PICKER_PAD;

    // Vanilla loom-gray background + bevel colors
    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int BG_DARK = 0xFF555555;
    private static final int BG_LIGHT = 0xFFFFFFFF;
    private static final int INACTIVE_COLOR = 0xFF999999;

    private static final Identifier REPLACE_COLOR_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/change_color.png");
    public static final Identifier DYE_OUTLINE_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/dye_outline.png");

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final Screen previousScreen;
    private final LoomRecipePanel panel;
    public final List<DyeColor> sourceColors;
    public final EnumMap<DyeColor, DyeColor> targets = new EnumMap<>(DyeColor.class);

    private DyeColor[] pickerColors; // resolved from config on init
    private DyeColor pickerOpenFor = null;
    private int pickerX, pickerY;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public BannerColorSwitchScreen(Screen previousScreen, LoomRecipePanel panel) {
        super(Component.translatable("loom-assistant.screen.color_switch.title"));
        this.previousScreen = previousScreen;
        this.panel = panel;
        this.sourceColors = panel.getActiveBannerUsedColors();
    }

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    @Override
    protected void init() {
        pickerColors = DyeColorSorting.sorted(LoomAssistantMod.getConfig().getColorSortOrder());

        Map<DyeColor, DyeColor> initialTargets = panel.getInitialDyeReplacementTargets(sourceColors);
        for (DyeColor source : sourceColors) {
            targets.put(source, initialTargets.getOrDefault(source, source));
        }

        int px = panelLeft();
        int py = panelTop();

        // Target-color buttons for active slots only
        for (int slotIdx = 0; slotIdx < sourceColors.size(); slotIdx++) {
            DyeColor source = sourceColors.get(slotIdx);
            int col = slotIdx / LAYOUT_ROWS;
            int row = slotIdx % LAYOUT_ROWS;
            int btnX = slotX(px, col) + ICON_SIZE + 6 + ARROW_W + 6;
            int btnY = slotY(py, row) + (ROW_H - TARGET_BTN_H) / 2;
            int btnCenterX = btnX + TARGET_BTN_W / 2;
            int btnCenterY = btnY + TARGET_BTN_H / 2;
            this.addRenderableWidget(new ColorSelectButton(this, btnX, btnY, source, btnCenterX, btnCenterY, slotIdx));
        }

        // OK and Cancel in column 2, rows 1 and 2
        int okCancelX = slotX(px, 2);

        // Disabled placeholder buttons for inactive slots
        for (int slotIdx = sourceColors.size(); slotIdx < MAX_SLOTS; slotIdx++) {
            int col = slotIdx / LAYOUT_ROWS;
            int row = slotIdx % LAYOUT_ROWS;
            if (col == 2 && row >= 1) continue;
            int btnX = slotX(px, col) + ICON_SIZE + 6 + ARROW_W + 6;
            int btnY = slotY(py, row) + (ROW_H - TARGET_BTN_H) / 2;
            Button btn = this.addRenderableWidget(new DisabledColorSelectButton(btnX, btnY));
            btn.active = false;
        }
        int okY = slotY(py, 1) + (ROW_H - 20) / 2;
        int cancelY = slotY(py, 2) + (ROW_H - 20) / 2;

        this.addRenderableWidget(
                Button.builder(Component.translatable("loom-assistant.common.ok"), button -> applyAndClose())
                        .bounds(okCancelX, okY, SLOT_W, 20)
                        .build());

        this.addRenderableWidget(
                Button.builder(Component.translatable("loom-assistant.common.cancel"), button -> this.onClose())
                        .bounds(okCancelX, cancelY, SLOT_W, 20)
                        .build());
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        int px = panelLeft();
        int py = panelTop();
        drawPanel(graphics, px, py, PANEL_W, PANEL_H);

        graphics.text(this.font, this.title, px + PAD, py + 8, 0xFF000000, false);

        for (int slotIdx = 0; slotIdx < MAX_SLOTS; slotIdx++) {
            int col = slotIdx / LAYOUT_ROWS;
            int row = slotIdx % LAYOUT_ROWS;

            // Skip the OK/Cancel area (col 2, rows 1-2)
            if (col == 2 && row >= 1) continue;

            int sx = slotX(px, col);
            int sy = slotY(py, row);
            int centerY = sy + ROW_H / 2;

            boolean active = slotIdx < sourceColors.size();

            if (active) {
                DyeColor source = sourceColors.get(slotIdx);
                int iconY = centerY - ICON_SIZE / 2;
                graphics.fakeItem(dyeStack(source), sx, iconY);

                int arrowGap = ICON_SIZE + 6 + ARROW_W + 6 + TARGET_BTN_W;
                int arrowX = sx + ICON_SIZE + (arrowGap - ICON_SIZE - TARGET_BTN_W) / 2 - ARROW_W / 2;
                int arrowY = centerY - ARROW_H / 2;
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        REPLACE_COLOR_ICON,
                        arrowX,
                        arrowY,
                        0f,
                        0f,
                        ARROW_W,
                        ARROW_H,
                        ARROW_W,
                        ARROW_H);

                if (MathUtils.isIn(mouseX, mouseY, sx, iconY, ICON_SIZE, ICON_SIZE)) {
                    graphics.setTooltipForNextFrame(
                            this.font, List.of(dyeStack(source).getHoverName()), Optional.empty(), mouseX, mouseY);
                }
            } else {
                // Inactive source icon placeholder
                int iconY = centerY - ICON_SIZE / 2;
                graphics.blit(RenderPipelines.GUI_TEXTURED, DYE_OUTLINE_ICON, sx, iconY, 0f, 0f, 16, 16, 16, 16);

                // Arrow (same icon as active slots, same centred position)
                int gapTotal = 6 + ARROW_W + 6;
                int arrowX = sx + ICON_SIZE + gapTotal / 2 - ARROW_W / 2;
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        REPLACE_COLOR_ICON,
                        arrowX,
                        centerY - ARROW_H / 2,
                        0f,
                        0f,
                        ARROW_W,
                        ARROW_H,
                        ARROW_W,
                        ARROW_H);
                // (target button rendered by disabled widget above)
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if (pickerOpenFor != null) {
            drawPicker(graphics, mouseX, mouseY);
        }
    }

    private void drawPicker(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        drawPanel(ctx, pickerX, pickerY, PICKER_W, PICKER_H);

        int gridX = pickerX + PICKER_PAD;
        int gridY = pickerY + PICKER_PAD;
        DyeColor selected = targets.getOrDefault(pickerOpenFor, pickerOpenFor);

        ctx.fill(gridX, gridY, gridX + PICKER_GRID_W, gridY + PICKER_GRID_ROWS * PICKER_CELL, BG_COLOR);

        for (int i = 0; i < pickerColors.length; i++) {
            DyeColor color = pickerColors[i];
            int col = i % PICKER_COLS;
            int row = i / PICKER_COLS;
            int cellX = gridX + col * PICKER_CELL;
            int cellY = gridY + row * PICKER_CELL;

            boolean isSelected = (color == selected);
            boolean isHover = MathUtils.isIn(mouseX, mouseY, cellX, cellY, PICKER_CELL, PICKER_CELL);

            int backgroundColor = getBackgroundColor(isSelected, isHover);
            ctx.fill(cellX + 1, cellY + 1, cellX + PICKER_CELL - 1, cellY + PICKER_CELL - 1, backgroundColor);
            ctx.fakeItem(dyeStack(color), cellX + 2, cellY + 2);

            if (isHover) {
                ctx.setTooltipForNextFrame(
                        this.font, List.of(dyeStack(color).getHoverName()), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    private static int getBackgroundColor(boolean isSel, boolean hover) {
        int bgCol;
        if (isSel) {
            bgCol = 0xFF4477CC;
        } else {
            if (hover) {
                bgCol = 0xFFAAAAAA;
            } else {
                bgCol = 0xFF888888;
            }
        }
        return bgCol;
    }

    private void drawPanel(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, BG_COLOR);
        ctx.fill(x, y, x + w, y + 1, BG_LIGHT);
        ctx.fill(x, y, x + 1, y + h, BG_LIGHT);
        ctx.fill(x, y + h - 1, x + w, y + h, BG_DARK);
        ctx.fill(x + w - 1, y, x + w, y + h, BG_DARK);
    }

    // -------------------------------------------------------------------------
    // Mouse input
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x();
        int my = (int) event.y();

        if (pickerOpenFor == null) return super.mouseClicked(event, doubleClick);

        if (!handlePickerClick(mx, my)) {
            pickerOpenFor = null;
        }
        return true;
    }

    private boolean handlePickerClick(int mx, int my) {
        int gridX = pickerX + PICKER_PAD;
        int gridY = pickerY + PICKER_PAD;

        for (int i = 0; i < pickerColors.length; i++) {
            int col = i % PICKER_COLS;
            int row = i / PICKER_COLS;
            int cellX = gridX + col * PICKER_CELL;
            int cellY = gridY + row * PICKER_CELL;

            if (MathUtils.isIn(mx, my, cellX, cellY, PICKER_CELL, PICKER_CELL)) {
                targets.put(pickerOpenFor, pickerColors[i]);
                pickerOpenFor = null;
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Keyboard
    // -------------------------------------------------------------------------

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (pickerOpenFor == null) {
                this.onClose();
            } else {
                pickerOpenFor = null;
            }
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            applyAndClose();
            return true;
        }

        return super.keyPressed(event);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    // btnCenterX/Y is the pixel center of the target button that was clicked.
    public void openPicker(DyeColor source, int btnCenterX, int btnCenterY) {
        pickerOpenFor = source;
        DyeColor selected = targets.getOrDefault(source, source);
        int selIdx = MathUtils.indexOf(pickerColors, selected);
        int selCol = selIdx % PICKER_COLS;
        int selRow = selIdx / PICKER_COLS;
        int gridOffX = PICKER_PAD + selCol * PICKER_CELL + PICKER_CELL / 2;
        int gridOffY = PICKER_PAD + selRow * PICKER_CELL + PICKER_CELL / 2;
        pickerX = Math.clamp(btnCenterX - gridOffX, 4, this.width - PICKER_W - 4);
        pickerY = Math.clamp(btnCenterY - gridOffY, 4, this.height - PICKER_H - 4);
    }

    private void applyAndClose() {
        boolean changed = panel.applyDyeSwitch(targets, true);
        LoomScreenExtension ext = LoomAssistantMod.getExtension(previousScreen);
        if (ext != null) {
            if (changed) ext.setPendingActiveBannerStack(panel.getActiveBannerStack());
            ext.setPersistentDyeSwitchState(
                    panel.isPersistentDyeSwitchEnabled(), panel.getPersistentDyeReplacementMapCopy());
        }
        this.minecraft.gui.setScreen(previousScreen);
    }

    private int panelLeft() {
        return (this.width - PANEL_W) / 2;
    }

    private int panelTop() {
        return (this.height - PANEL_H) / 2;
    }

    // X of a slot column (0-based)
    private static int slotX(int panelX, int col) {
        return panelX + PAD + col * (SLOT_W + COL_GAP);
    }

    // Y of a slot row (0-based) within the panel
    private static int slotY(int panelY, int row) {
        return panelY + TITLE_H + row * ROW_H;
    }

    public static ItemStack dyeStack(DyeColor color) {
        return new ItemStack(BannerRecipe.getDyeItem(color));
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(previousScreen);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
