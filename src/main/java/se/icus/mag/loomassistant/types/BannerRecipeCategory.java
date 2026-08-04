/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types;

/**
 * A named banner recipe category with a human-readable description and an icon item id.
 * <p>
 * The {@code iconItemId} is a vanilla item identifier (e.g. {@code "minecraft:book"}) used to
 * render the tab icon. If the item is unknown the tab falls back to a lava bucket.
 */
public record BannerRecipeCategory(String id, String description, String iconItemId) {
    /** Fallback used when a recipe references an unknown category id. */
    public static BannerRecipeCategory fallback(String id) {
        return new BannerRecipeCategory(id, id, "minecraft:lava_bucket");
    }

    /** Returns a copy with description and icon taken from {@code override} when present. */
    public BannerRecipeCategory mergedWith(BannerRecipeCategory override) {
        if (override == null) return this;
        String desc = override.description() != null && !override.description().isBlank()
                ? override.description()
                : this.description;
        String icon = override.iconItemId() != null && !override.iconItemId().isBlank()
                ? override.iconItemId()
                : this.iconItemId;
        return new BannerRecipeCategory(id, desc, icon);
    }
}
