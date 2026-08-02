/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Registry-like holder for banner recipe categories.
 *
 * This starts as hardcoded Java data and is designed to be replaced by JSON loading later.
 */
public final class BannerRecipeCategories {
    public record Category(String id, Identifier itemId, String name) {
    }

    private static final List<Category> DEFAULT_CATEGORIES = List.of(
            new Category("letters", Identifier.withDefaultNamespace("book"), "letters"),
            new Category("flags", Identifier.withDefaultNamespace("map"), "flags"),
            new Category("logos", Identifier.withDefaultNamespace("blaze_powder"), "logos"),
            new Category("nature", Identifier.withDefaultNamespace("poppy"), "nature"),
            new Category("misc", Identifier.withDefaultNamespace("lava_bucket"), "misc"),
            new Category("test1", Identifier.withDefaultNamespace("stone"), "test1"),
            new Category("test2", Identifier.withDefaultNamespace("oak_planks"), "test2"));

    private BannerRecipeCategories() {
    }

    public static List<Category> getCategories() {
        return DEFAULT_CATEGORIES;
    }

    public static ItemStack resolveIcon(Category category) {
        if (BuiltInRegistries.ITEM.containsKey(category.itemId())) {
            return new ItemStack(BuiltInRegistries.ITEM.getValue(category.itemId()));
        }

        // Fallback for invalid/missing item ids (e.g. temporary placeholders).
        return new ItemStack(Items.BARRIER);
    }
}
