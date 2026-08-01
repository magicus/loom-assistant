/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public class ClientBannerRecipeTooltipComponent implements ClientTooltipComponent {
    private static final int SLOT_SIZE = 16;
    private static final int COL_GAP = 2;
    private static final int ROW_GAP = 2;
    private static final int ROW_HEIGHT = SLOT_SIZE + ROW_GAP;
    private static final int TEXT_GAP = 4;
    private static final int ICON_AREA_WIDTH = SLOT_SIZE * 2 + COL_GAP;
    private static final int TEXT_X = ICON_AREA_WIDTH + TEXT_GAP;

    private final BannerRecipeTooltipComponent component;

    public ClientBannerRecipeTooltipComponent(BannerRecipeTooltipComponent component) {
        this.component = component;
    }

    @Override
    public int getHeight(Font font) {
        int rows = this.component.rows().size();
        if (rows == 0) {
            return 0;
        }
        return rows * ROW_HEIGHT - ROW_GAP;
    }

    @Override
    public int getWidth(Font font) {
        int maxTextWidth = 0;
        for (BannerRecipeTooltipComponent.Row row : this.component.rows()) {
            maxTextWidth = Math.max(maxTextWidth, font.width(row.text()));
        }
        return TEXT_X + maxTextWidth;
    }

    @Override
    public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
        int drawY = y + 4;
        for (BannerRecipeTooltipComponent.Row row : this.component.rows()) {
            graphics.text(font, row.text(), x + TEXT_X, drawY, 0xFFFFFFFF, false);
            drawY += ROW_HEIGHT;
        }
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int drawY = y;
        int seed = 0;
        for (BannerRecipeTooltipComponent.Row row : this.component.rows()) {
            graphics.item(row.primary(), x, drawY, seed++);
            if (row.hasSecondary()) {
                graphics.item(row.secondary(), x + SLOT_SIZE + COL_GAP, drawY, seed++);
            }
            drawY += ROW_HEIGHT;
        }
    }
}
