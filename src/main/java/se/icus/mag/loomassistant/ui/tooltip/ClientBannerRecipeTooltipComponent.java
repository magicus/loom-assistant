/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.jspecify.annotations.Nullable;

public class ClientBannerRecipeTooltipComponent implements ClientTooltipComponent {
    private static final int SLOT_SIZE = 16;
    private static final int COL_GAP = 2;
    private static final int ROW_GAP = 2;
    private static final int ROW_HEIGHT = SLOT_SIZE + ROW_GAP;
    private static final int TEXT_GAP = 4;
    private static final int ICON_AREA_W = SLOT_SIZE * 2 + COL_GAP;
    // Banner preview dimensions (same scale as the loom's preview pane)
    private static final int PREVIEW_W = 20;
    private static final int PREVIEW_H = 40;
    private static final int PREVIEW_GAP = 6;

    @Nullable
    private static BannerFlagModel sharedFlag;

    private final BannerRecipeTooltipComponent component;

    public ClientBannerRecipeTooltipComponent(BannerRecipeTooltipComponent component) {
        this.component = component;
    }

    /** X offset where the recipe steps icon column starts. */
    private int stepsOffsetX() {
        return component.hasPreview() ? PREVIEW_W + PREVIEW_GAP : 0;
    }

    @Override
    public int getHeight(Font font) {
        int stepsH = stepsHeight();
        return component.hasPreview() ? Math.max(PREVIEW_H, stepsH) : stepsH;
    }

    @Override
    public int getWidth(Font font) {
        int maxText = 0;
        for (BannerRecipeTooltipComponent.Row row : component.rows()) {
            maxText = Math.max(maxText, font.width(row.text()));
        }
        return stepsOffsetX() + ICON_AREA_W + TEXT_GAP + maxText;
    }

    @Override
    public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
        int textX = x + stepsOffsetX() + ICON_AREA_W + TEXT_GAP;
        int drawY = y + 4;
        for (BannerRecipeTooltipComponent.Row row : component.rows()) {
            graphics.text(font, row.text(), textX, drawY, 0xFFFFFFFF, false);
            drawY += ROW_HEIGHT;
        }
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        if (component.hasPreview()) {
            ensureFlag();
            if (sharedFlag != null) {
                DyeColor base = component.previewBaseColor();
                BannerPatternLayers patterns = component.previewPatterns();
                if (patterns == null) patterns = new BannerPatternLayers.Builder().build();
                // Align preview to the top of the tooltip image area
                int previewY = y;
                graphics.bannerPattern(sharedFlag, base, patterns, x, previewY, x + PREVIEW_W, previewY + PREVIEW_H);
            }
        }

        int iconX = x + stepsOffsetX();
        int drawY = y;
        int seed = 0;
        for (BannerRecipeTooltipComponent.Row row : component.rows()) {
            graphics.item(row.primary(), iconX, drawY, seed++);
            if (row.hasSecondary()) {
                graphics.item(row.secondary(), iconX + SLOT_SIZE + COL_GAP, drawY, seed++);
            }
            drawY += ROW_HEIGHT;
        }
    }

    private int stepsHeight() {
        int rows = component.rows().size();
        return rows == 0 ? 0 : rows * ROW_HEIGHT - ROW_GAP;
    }

    private static void ensureFlag() {
        if (sharedFlag == null) {
            try {
                ModelPart part = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
                sharedFlag = new BannerFlagModel(part);
            } catch (Exception ignored) {
                // Silently fail; preview just won't render this frame.
            }
        }
    }
}
