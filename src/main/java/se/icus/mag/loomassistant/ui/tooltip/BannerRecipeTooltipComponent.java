/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui.tooltip;

import java.util.List;
import net.minecraft.network.chat.Component;
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

    public BannerRecipeTooltipComponent(List<Row> rows) {
        this(rows, null, null);
    }

    public BannerRecipeTooltipComponent(
            List<Row> rows, @Nullable DyeColor previewBaseColor, @Nullable BannerPatternLayers previewPatterns) {
        this.rows = rows.stream().map(Row::copy).toList();
        this.previewBaseColor = previewBaseColor;
        this.previewPatterns = previewPatterns;
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

    public record Row(ItemStack primary, ItemStack secondary, Component text) {
        public static Row single(ItemStack primary, Component text) {
            return new Row(primary, ItemStack.EMPTY, text);
        }

        public static Row pair(ItemStack primary, ItemStack secondary, Component text) {
            return new Row(primary, secondary, text);
        }

        public Row copy() {
            return new Row(this.primary.copy(), this.secondary.copy(), this.text.copy());
        }

        public boolean hasSecondary() {
            return !this.secondary.isEmpty();
        }
    }
}
