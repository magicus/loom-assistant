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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.jspecify.annotations.Nullable;

public class ClientBannerRecipeTooltipComponent implements ClientTooltipComponent {
    private static final int SLOT_SIZE = 16;
    private static final int COL_GAP = 0;
    private static final int ROW_GAP = 1;
    private static final int ROW_HEIGHT = SLOT_SIZE + ROW_GAP;
    private static final int TEXT_GAP = 0;
    private static final int ICON_AREA_W = SLOT_SIZE * 2 + COL_GAP;
    // Banner preview dimensions (same scale as the loom's preview pane)
    private static final int PREVIEW_W = 20;
    private static final int PREVIEW_H = 40;
    private static final int PREVIEW_GAP = 6;
    // Pattern sprite render size – scaled up from the natural 5×10 to fill the icon row height
    private static final int PATTERN_RENDER_W = 7;
    private static final int PATTERN_RENDER_H = 14;
    // Uncraftable icon (weave icon + red slash) shown below the preview when too many steps
    private static final int UNCRAFTABLE_ICON_SIZE = 16;
    private static final int UNCRAFTABLE_GAP = 10;
    private static final Identifier WEAVE_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-weave.png");
    // First row index beyond the 6 weavable steps (row 0 = banner, rows 1-6 = steps)
    private static final int MAX_WEAVABLE_ROW = 7;

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
        int previewH = component.hasPreview()
                ? PREVIEW_H + (component.notWeavableInSurvival() ? UNCRAFTABLE_GAP + UNCRAFTABLE_ICON_SIZE : 0)
                : 0;
        return Math.max(previewH, stepsH);
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
        int indent = font.width("0. ");
        int drawY = y + 5;
        int current = component.currentRowIndex();
        for (int i = 0; i < component.rows().size(); i++) {
            BannerRecipeTooltipComponent.Row row = component.rows().get(i);
            int color;
            if (component.notWeavableInSurvival() && i >= MAX_WEAVABLE_ROW) {
                color = 0xFF888888; // beyond weavable limit
            } else if (current < 0) {
                color = 0xFFFFFFFF;
            } else if (i < current) {
                color = 0xFF888888; // done
            } else if (i == current) {
                color = 0xFFFF5555; // current step
            } else {
                color = 0xFFFFFFFF; // future
            }
            int tx = textX + (row.indented() ? indent : 0);
            graphics.text(font, row.text(), tx, drawY, color, false);
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
            // Uncraftable icon below preview when banner has too many steps in survival.
            if (component.notWeavableInSurvival()) {
                int ix = x + (PREVIEW_W - UNCRAFTABLE_ICON_SIZE) / 2;
                int iy = y + PREVIEW_H + UNCRAFTABLE_GAP;
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        WEAVE_ICON,
                        ix,
                        iy,
                        0f,
                        0f,
                        UNCRAFTABLE_ICON_SIZE,
                        UNCRAFTABLE_ICON_SIZE,
                        UNCRAFTABLE_ICON_SIZE,
                        UNCRAFTABLE_ICON_SIZE);
                for (int row = 0; row < UNCRAFTABLE_ICON_SIZE; row++) {
                    int xOff = UNCRAFTABLE_ICON_SIZE - 1 - row;
                    graphics.fill(ix + xOff - 1, iy + row, ix + xOff + 1, iy + row + 1, 0xFFFF2222);
                }
            }
        }

        int iconX = x + stepsOffsetX();
        int drawY = y;
        int seed = 0;
        for (BannerRecipeTooltipComponent.Row row : component.rows()) {
            if (row.hasPatternSprite()) {
                // Dye icon on left, pattern sprite on right
                graphics.item(row.primary(), iconX, drawY, seed++);
                renderPatternSprite(graphics, row, iconX + SLOT_SIZE + COL_GAP, drawY);
            } else {
                graphics.item(row.primary(), iconX, drawY, seed++);
                if (row.hasSecondary()) {
                    graphics.item(row.secondary(), iconX + SLOT_SIZE + COL_GAP, drawY, seed++);
                }
            }
            drawY += ROW_HEIGHT;
        }
    }

    private int stepsHeight() {
        int rows = component.rows().size();
        return rows == 0 ? 0 : rows * ROW_HEIGHT - ROW_GAP;
    }

    /**
     * Renders the loom-style pattern mini-banner (5×10) centred in the 16×16 icon slot.
     * Mirrors extractBannerOnButton in LoomScreen.
     */
    private static void renderPatternSprite(
            GuiGraphicsExtractor graphics, BannerRecipeTooltipComponent.Row row, int x, int y) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            var registry = mc.level.registryAccess().lookup(Registries.BANNER_PATTERN);
            if (registry.isEmpty()) return;
            var entryOpt = registry.get().get(row.patternSprite());
            if (entryOpt.isEmpty()) return;

            @SuppressWarnings("unchecked")
            Holder<BannerPattern> holder = (Holder<BannerPattern>) (Object) entryOpt.get();
            TextureAtlasSprite sprite = graphics.getSprite(Sheets.getBannerSprite(holder));

            // Centre the scaled mini-banner inside the 16×16 slot
            int slotCentreX = x + (SLOT_SIZE - PATTERN_RENDER_W) / 2;
            int slotCentreY = y + (SLOT_SIZE - PATTERN_RENDER_H) / 2;

            float u0 = sprite.getU0();
            float u1 = u0 + (sprite.getU1() - sprite.getU0()) * 21.0F / 64.0F;
            float vSpan = sprite.getV1() - sprite.getV0();
            float v0 = sprite.getV0() + vSpan / 64.0F;
            float v1 = v0 + vSpan * 40.0F / 64.0F;

            graphics.pose().pushMatrix();
            graphics.pose().translate(slotCentreX, slotCentreY);
            graphics.fill(0, 0, PATTERN_RENDER_W, PATTERN_RENDER_H, DyeColor.GRAY.getTextureDiffuseColor());
            graphics.blit(sprite.atlasLocation(), 0, 0, PATTERN_RENDER_W, PATTERN_RENDER_H, u0, u1, v0, v1);
            graphics.pose().popMatrix();
        } catch (Exception ignored) {
            // Fall back to nothing if registry not available.
        }
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
