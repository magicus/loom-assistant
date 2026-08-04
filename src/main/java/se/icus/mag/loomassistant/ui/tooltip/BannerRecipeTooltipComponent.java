/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui.tooltip;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.jspecify.annotations.Nullable;

public final class BannerRecipeTooltipComponent implements TooltipComponent {
    private final List<Row> rows;

    @Nullable
    private final DyeColor previewBaseColor;

    @Nullable
    private final BannerPatternLayers previewPatterns;

    /** Row index to highlight as the current crafting step (-1 = no highlighting). */
    private final int currentRowIndex;

    public BannerRecipeTooltipComponent(List<Row> rows) {
        this(rows, null, null, -1);
    }

    public BannerRecipeTooltipComponent(
            List<Row> rows, @Nullable DyeColor previewBaseColor, @Nullable BannerPatternLayers previewPatterns) {
        this(rows, previewBaseColor, previewPatterns, -1);
    }

    public BannerRecipeTooltipComponent(
            List<Row> rows,
            @Nullable DyeColor previewBaseColor,
            @Nullable BannerPatternLayers previewPatterns,
            int currentRowIndex) {
        this.rows = rows.stream().map(Row::copy).toList();
        this.previewBaseColor = previewBaseColor;
        this.previewPatterns = previewPatterns;
        this.currentRowIndex = currentRowIndex;
    }

    public int currentRowIndex() {
        return currentRowIndex;
    }

    public List<Row> rows() {
        return this.rows;
    }

    public boolean hasPreview() {
        return previewBaseColor != null;
    }

    public @Nullable DyeColor previewBaseColor() {
        return previewBaseColor;
    }

    public @Nullable BannerPatternLayers previewPatterns() {
        return previewPatterns;
    }

    public record Row(ItemStack primary, ItemStack secondary, Component text, @Nullable Identifier patternSprite) {
        public static Row single(ItemStack primary, Component text) {
            return new Row(primary, ItemStack.EMPTY, text, null);
        }

        public static Row pair(ItemStack primary, ItemStack secondary, Component text) {
            return new Row(primary, secondary, text, null);
        }

        /** Row that renders a loom pattern sprite on the right with dye item on the left. */
        public static Row withPattern(ItemStack dye, Identifier patternId, Component text) {
            return new Row(dye, ItemStack.EMPTY, text, patternId);
        }

        public Row copy() {
            return new Row(this.primary.copy(), this.secondary.copy(), this.text.copy(), this.patternSprite);
        }

        public boolean hasSecondary() {
            return !this.secondary.isEmpty();
        }

        public boolean hasPatternSprite() {
            return patternSprite != null;
        }
    }
}
