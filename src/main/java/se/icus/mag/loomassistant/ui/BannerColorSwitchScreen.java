/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import se.icus.mag.loomassistant.LoomActiveBannerHost;
import se.icus.mag.loomassistant.data.SavedBanner;

public class BannerColorSwitchScreen extends Screen {

    // -------------------------------------------------------------------------
    // Layout
    // -------------------------------------------------------------------------

    private static final int ROW_H = 22;
    private static final int ICON_SIZE = 16;
    private static final int ARROW_W = 12;
    private static final int ARROW_H = 17;
    private static final int TARGET_BTN_W = 20;
    private static final int TARGET_BTN_H = 20;
    private static final int PAD = 10;
    // Row content: [16 icon] [6 gap] [12 arrow] [6 gap] [20 target]
    private static final int ROW_CONTENT_W = ICON_SIZE + 6 + ARROW_W + 6 + TARGET_BTN_W;
    private static final int PANEL_W = PAD + ROW_CONTENT_W + PAD;

    // Color picker popup
    private static final int PICKER_COLS = 4;
    private static final int PICKER_CELL = 20; // includes 1px border each side
    private static final int PICKER_CLOSE_SIZE = 10;
    private static final int PICKER_PAD = 6;
    private static final int PICKER_GRID_W = PICKER_COLS * PICKER_CELL;
    private static final int PICKER_GRID_ROWS = (int) Math.ceil(16.0 / PICKER_COLS);
    private static final int PICKER_W = PICKER_PAD + PICKER_GRID_W + PICKER_PAD;
    private static final int PICKER_H = PICKER_PAD + PICKER_CLOSE_SIZE + 4 + PICKER_GRID_ROWS * PICKER_CELL + PICKER_PAD;

    // Background colour matching vanilla loom GUI
    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int BG_DARK  = 0xFF555555;
    private static final int BG_LIGHT = 0xFFFFFFFF;

    private static final Identifier REPLACE_COLOR_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/replace_color.png");

    private static final DyeColor[] ALL_COLORS = DyeColor.values();

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final Screen previousScreen;
    private final LoomPanel panel;
    private final List<DyeColor> sourceColors;
    private final EnumMap<DyeColor, DyeColor> targets = new EnumMap<>(DyeColor.class);
    private boolean persistent;
    private Button persistentButton;

    /** Which source color's picker is currently open (null = none). */
    private DyeColor pickerOpenFor = null;
    /** Top-left of the picker popup. */
    private int pickerX, pickerY;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public BannerColorSwitchScreen(Screen previousScreen, LoomPanel panel) {
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
        Map<DyeColor, DyeColor> initialTargets = panel.getInitialDyeReplacementTargets(sourceColors);
        for (DyeColor source : sourceColors) {
            targets.put(source, initialTargets.getOrDefault(source, source));
        }

        int panelH = panelHeight();
        int px = panelLeft();
        int py = (this.height - panelH) / 2;

        // Target-color buttons, one per row
        for (int i = 0; i < sourceColors.size(); i++) {
            DyeColor source = sourceColors.get(i);
            int btnX = px + PAD + ICON_SIZE + 6 + ARROW_W + 6;
            int btnY = py + titleAreaH() + i * ROW_H + (ROW_H - TARGET_BTN_H) / 2;
            int fi = i;
            this.addRenderableWidget(new Button.Plain(btnX, btnY, TARGET_BTN_W, TARGET_BTN_H,
                    Component.empty(),
                    button -> openPicker(source, btnX, btnY + TARGET_BTN_H + 2),
                    s -> s.get()) {
                @Override
                public void extractContents(GuiGraphicsExtractor g, int mx, int my, float a) {
                    this.extractDefaultSprite(g);
                    DyeColor target = targets.getOrDefault(sourceColors.get(fi), sourceColors.get(fi));
                    g.fakeItem(dyeStack(target), this.getX() + 2, this.getY() + 2);
                }
            });
        }

        int bottomY = py + panelH - 28;

        persistentButton = this.addRenderableWidget(Button.builder(persistentLabel(),
                button -> { persistent = !persistent; button.setMessage(persistentLabel()); })
                .bounds(px + PAD, bottomY, PANEL_W - PAD * 2, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("loom-assistant.common.ok"), button -> applyAndClose())
                .bounds(px + PAD, bottomY + 22, (PANEL_W - PAD * 2 - 4) / 2, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("loom-assistant.common.cancel"), button -> this.onClose())
                .bounds(px + PAD + (PANEL_W - PAD * 2 - 4) / 2 + 4, bottomY + 22, (PANEL_W - PAD * 2 - 4) / 2, 20)
                .build());
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        // Dim world behind dialog
        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        int panelH = panelHeight();
        int px = panelLeft();
        int py = (this.height - panelH) / 2;

        // Panel background (vanilla loom grey tone with simple bevel)
        drawPanel(ctx, px, py, PANEL_W, panelH);

        ctx.text(this.font, this.title, px + PAD, py + 8, 0xFF000000, false);

        int rowsY = py + titleAreaH();
        for (int i = 0; i < sourceColors.size(); i++) {
            DyeColor source = sourceColors.get(i);
            int rowY = rowsY + i * ROW_H;
            int centerY = rowY + ROW_H / 2;

            // Source dye icon
            int iconX = px + PAD;
            int iconY = centerY - ICON_SIZE / 2;
            ctx.fakeItem(dyeStack(source), iconX, iconY);

            // Arrow image, vertically centred
            int arrowX = px + PAD + ICON_SIZE + 6;
            int arrowY = centerY - ARROW_H / 2;
            ctx.blit(RenderPipelines.GUI_TEXTURED, REPLACE_COLOR_ICON,
                    arrowX, arrowY, 0f, 0f, ARROW_W, ARROW_H, ARROW_W, ARROW_H);

            // Tooltip for source when hovered
            if (mouseX >= iconX && mouseX < iconX + ICON_SIZE && mouseY >= iconY && mouseY < iconY + ICON_SIZE) {
                ctx.setTooltipForNextFrame(this.font, List.of(dyeStack(source).getHoverName()), Optional.empty(), mouseX, mouseY);
            }
        }

        super.extractRenderState(ctx, mouseX, mouseY, delta);

        // Color picker overlay (drawn on top of buttons)
        if (pickerOpenFor != null) {
            drawPicker(ctx, mouseX, mouseY);
        }
    }

    private void drawPicker(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        drawPanel(ctx, pickerX, pickerY, PICKER_W, PICKER_H);

        // Close button (red X)
        int closeX = pickerX + PICKER_W - PICKER_PAD - PICKER_CLOSE_SIZE;
        int closeY = pickerY + PICKER_PAD;
        boolean closeHover = isIn(mouseX, mouseY, closeX, closeY, PICKER_CLOSE_SIZE, PICKER_CLOSE_SIZE);
        ctx.fill(closeX, closeY, closeX + PICKER_CLOSE_SIZE, closeY + PICKER_CLOSE_SIZE,
                closeHover ? 0xFFFF4444 : 0xFFCC2222);
        ctx.text(this.font, Component.literal("x"), closeX + 2, closeY + 1, 0xFFFFFFFF, false);

        int gridX = pickerX + PICKER_PAD;
        int gridY = pickerY + PICKER_PAD + PICKER_CLOSE_SIZE + 4;
        DyeColor selected = targets.getOrDefault(pickerOpenFor, pickerOpenFor);

        for (int i = 0; i < ALL_COLORS.length; i++) {
            DyeColor color = ALL_COLORS[i];
            int col = i % PICKER_COLS;
            int row = i / PICKER_COLS;
            int cellX = gridX + col * PICKER_CELL;
            int cellY = gridY + row * PICKER_CELL;

            boolean isSel = color == selected;
            boolean hover = isIn(mouseX, mouseY, cellX, cellY, PICKER_CELL, PICKER_CELL);

            int bgCol = isSel ? 0xFF4477CC : (hover ? 0xFFAAAAAA : 0xFF888888);
            ctx.fill(cellX, cellY, cellX + PICKER_CELL, cellY + PICKER_CELL, bgCol);
            ctx.fakeItem(dyeStack(color), cellX + 2, cellY + 2);

            if (hover) {
                ctx.setTooltipForNextFrame(this.font, List.of(dyeStack(color).getHoverName()), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    private void drawPanel(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, BG_COLOR);
        // Simple inset bevel (vanilla style)
        ctx.fill(x,         y,         x + w,     y + 1,     BG_LIGHT);
        ctx.fill(x,         y,         x + 1,     y + h,     BG_LIGHT);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     BG_DARK);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     BG_DARK);
    }

    // -------------------------------------------------------------------------
    // Mouse input
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x();
        int my = (int) event.y();

        if (pickerOpenFor != null) {
            if (handlePickerClick(mx, my)) return true;
            // Click outside picker closes it
            pickerOpenFor = null;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private boolean handlePickerClick(int mx, int my) {
        // Close button
        int closeX = pickerX + PICKER_W - PICKER_PAD - PICKER_CLOSE_SIZE;
        int closeY = pickerY + PICKER_PAD;
        if (isIn(mx, my, closeX, closeY, PICKER_CLOSE_SIZE, PICKER_CLOSE_SIZE)) {
            pickerOpenFor = null;
            return true;
        }

        // Color grid
        int gridX = pickerX + PICKER_PAD;
        int gridY = pickerY + PICKER_PAD + PICKER_CLOSE_SIZE + 4;
        for (int i = 0; i < ALL_COLORS.length; i++) {
            int col = i % PICKER_COLS;
            int row = i / PICKER_COLS;
            int cellX = gridX + col * PICKER_CELL;
            int cellY = gridY + row * PICKER_CELL;
            if (isIn(mx, my, cellX, cellY, PICKER_CELL, PICKER_CELL)) {
                targets.put(pickerOpenFor, ALL_COLORS[i]);
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
            if (pickerOpenFor != null) { pickerOpenFor = null; return true; }
            this.onClose();
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

    private void openPicker(DyeColor source, int btnX, int preferY) {
        pickerOpenFor = source;
        // Clamp picker so it stays on screen
        pickerX = Math.min(btnX, this.width - PICKER_W - 4);
        pickerY = Math.min(preferY, this.height - PICKER_H - 4);
        pickerX = Math.max(4, pickerX);
        pickerY = Math.max(4, pickerY);
    }

    private void applyAndClose() {
        boolean changed = panel.applyDyeSwitch(targets, persistent);
        if (changed && previousScreen instanceof LoomActiveBannerHost host) {
            host.loomassistant$setPendingActiveBannerStack(panel.getActiveBannerStack());
        }
        if (previousScreen instanceof LoomActiveBannerHost host) {
            host.loomassistant$setPersistentDyeSwitchState(
                    panel.isPersistentDyeSwitchEnabled(),
                    panel.getPersistentDyeReplacementMapCopy());
        }
        this.minecraft.gui.setScreen(previousScreen);
    }

    private int panelLeft() {
        return (this.width - PANEL_W) / 2;
    }

    private int titleAreaH() {
        return 24;
    }

    private int panelHeight() {
        int rowsH = sourceColors.size() * ROW_H;
        int bottomH = 28 + 22 + 4; // persistent button + ok/cancel + gap
        return titleAreaH() + rowsH + 8 + bottomH;
    }

    private static ItemStack dyeStack(DyeColor color) {
        return new ItemStack((Item) SavedBanner.getDyeItem(color));
    }

    private static boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private Component persistentLabel() {
        String prefix = persistent ? "[x] " : "[ ] ";
        return Component.literal(prefix).append(Component.translatable("loom-assistant.screen.color_switch.persistent"));
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
