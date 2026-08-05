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

public final class BannerRecipeTooltipComponent implements TooltipComponent {
    private final List<Row> rows;

    private final DyeColor previewBaseColor;

    private final BannerPatternLayers previewPatterns;

    /** Row index to highlight as the current crafting step (-1 = no highlighting). */
    private final int currentRowIndex;

    /** True when the banner has more than 6 layers and the player is in survival mode. */
    private final boolean notWeavableInSurvival;

    public BannerRecipeTooltipComponent(
            List<Row> rows,
            DyeColor previewBaseColor,
            BannerPatternLayers previewPatterns,
            int currentRowIndex,
            boolean notWeavableInSurvival) {
        this.rows = rows.stream().map(Row::copy).toList();
        this.previewBaseColor = previewBaseColor;
        this.previewPatterns = previewPatterns;
        this.currentRowIndex = currentRowIndex;
        this.notWeavableInSurvival = notWeavableInSurvival;
    }

    public int currentRowIndex() {
        return currentRowIndex;
    }

    public boolean notWeavableInSurvival() {
        return notWeavableInSurvival;
    }

    public List<Row> rows() {
        return this.rows;
    }

    public boolean hasPreview() {
        return previewBaseColor != null;
    }

    public DyeColor previewBaseColor() {
        return previewBaseColor;
    }

    public BannerPatternLayers previewPatterns() {
        return previewPatterns;
    }

    public record Row(
            ItemStack primary, ItemStack secondary, Component text, Identifier patternSprite, boolean indented) {
        public static Row singleIndented(ItemStack primary, Component text) {
            return new Row(primary, ItemStack.EMPTY, text, null, true);
        }

        public static Row pair(ItemStack primary, ItemStack secondary, Component text) {
            return new Row(primary, secondary, text, null, false);
        }

        /** Row that renders a loom pattern sprite on the right with dye item on the left. */
        public static Row withPattern(ItemStack dye, Identifier patternId, Component text) {
            return new Row(dye, ItemStack.EMPTY, text, patternId, false);
        }

        public Row copy() {
            return new Row(
                    this.primary.copy(), this.secondary.copy(), this.text.copy(), this.patternSprite, this.indented);
        }

        public boolean hasSecondary() {
            return !this.secondary.isEmpty();
        }

        public boolean hasPatternSprite() {
            return patternSprite != null;
        }
    }
}
