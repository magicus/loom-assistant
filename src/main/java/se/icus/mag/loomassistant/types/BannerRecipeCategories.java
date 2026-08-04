/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Dynamic registry for banner recipe categories.
 *
 * Categories are assembled at load time from three sources (in priority order):
 *  1. The global categories.json in the loom-assistant config directory.
 *  2. Category definitions inside each banner pack's bannerpack.mcmeta.
 *  3. Implicit fallback categories auto-created for any category id used in a recipe
 *     that has no explicit definition.
 *
 * Tabs are sorted alphabetically by id.
 */
public final class BannerRecipeCategories {
    /** Hardcoded fallback category used when a recipe specifies no category. */
    public static final BannerRecipeCategory MISC =
            new BannerRecipeCategory(BannerRecipe.DEFAULT_CATEGORY, "Misc", "minecraft:lava_bucket");

    /** @deprecated Use {@link BannerRecipeCategory} directly. */
    @Deprecated
    public record Category(String id, Identifier itemId, String name) {}

    private static final Map<String, BannerRecipeCategory> registry = new LinkedHashMap<>();

    private BannerRecipeCategories() {}

    // -------------------------------------------------------------------------
    // Registry management (called by BannerStorage after loading packs)
    // -------------------------------------------------------------------------

    /** Replaces the entire registry. Entries are sorted alphabetically by id. MISC is always present. */
    public static void setCategories(Collection<BannerRecipeCategory> categories) {
        registry.clear();
        // Seed with the hardcoded fallback so it's always available.
        registry.put(MISC.id(), MISC);
        categories.stream().sorted((a, b) -> a.id().compareToIgnoreCase(b.id())).forEach(c -> registry.put(c.id(), c));
    }

    // -------------------------------------------------------------------------
    // Query API
    // -------------------------------------------------------------------------

    public static List<BannerRecipeCategory> getCategories() {
        return List.copyOf(registry.values());
    }

    public static BannerRecipeCategory get(String id) {
        return registry.getOrDefault(id, BannerRecipeCategory.fallback(id));
    }

    /** Resolves the tab icon for a category. Falls back to lava bucket on unknown item id. */
    public static ItemStack resolveIcon(BannerRecipeCategory category) {
        Identifier itemId = Identifier.tryParse(category.iconItemId());
        if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId)) {
            return new ItemStack(BuiltInRegistries.ITEM.getValue(itemId));
        }
        return new ItemStack(Items.LAVA_BUCKET);
    }

    // -------------------------------------------------------------------------
    // Legacy compat for code that still uses the old Category record
    // -------------------------------------------------------------------------

    /** @deprecated Use {@link #getCategories()} returning {@link BannerRecipeCategory}. */
    @Deprecated
    public static List<Category> getLegacyCategories() {
        return getCategories().stream()
                .map(c -> new Category(c.id(), Identifier.tryParse(c.iconItemId()), c.description()))
                .toList();
    }

    /** @deprecated Use {@link #resolveIcon(BannerRecipeCategory)}. */
    @Deprecated
    public static ItemStack resolveIcon(Category category) {
        if (category.itemId() != null && BuiltInRegistries.ITEM.containsKey(category.itemId())) {
            return new ItemStack(BuiltInRegistries.ITEM.getValue(category.itemId()));
        }
        return new ItemStack(Items.LAVA_BUCKET);
    }
}
