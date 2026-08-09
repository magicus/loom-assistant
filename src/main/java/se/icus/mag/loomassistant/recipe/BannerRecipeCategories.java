/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe;

import com.mojang.brigadier.StringReader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import se.icus.mag.loomassistant.LoomAssistantMod;

/**
 * Dynamic registry for banner recipe categories.
 * <p>
 * Categories are assembled at load time from three sources (in priority order):
 * 1. The global categories.json in the loom-assistant config directory.
 * 2. Category definitions inside each banner pack's bannerpack.mcmeta.
 * 3. Implicit fallback categories auto-created for any category id used in a recipe
 * that has no explicit definition.
 * <p>
 * Tabs are sorted alphabetically by id.
 */
public final class BannerRecipeCategories {
    /**
     * Hardcoded fallback category used when a recipe specifies no category.
     */
    public static final BannerRecipeCategory MISC =
            new BannerRecipeCategory(BannerRecipe.DEFAULT_CATEGORY, "Misc", "minecraft:lava_bucket");

    /**
     * @deprecated Use {@link BannerRecipeCategory} directly.
     */
    @Deprecated
    public record Category(String id, Identifier itemId, String name) {}

    private static final Map<String, BannerRecipeCategory> registry = new LinkedHashMap<>();
    /**
     * locale code → (category id → localized name)
     */
    private static final Map<String, Map<String, String>> translations = new LinkedHashMap<>();

    private BannerRecipeCategories() {}

    // -------------------------------------------------------------------------
    // Registry management (called by BannerStorage after loading packs)
    // -------------------------------------------------------------------------

    /**
     * Replaces the entire registry. Entries are sorted alphabetically by id. MISC is always present.
     */
    public static void setCategories(Collection<BannerRecipeCategory> categories) {
        registry.clear();
        // Seed with the hardcoded fallback so it's always available.
        registry.put(MISC.id(), MISC);
        categories.stream().sorted((a, b) -> a.id().compareToIgnoreCase(b.id())).forEach(c -> registry.put(c.id(), c));
    }

    /**
     * Replaces all locale translations collected from packs.
     */
    public static void setTranslations(Map<String, Map<String, String>> allTranslations) {
        translations.clear();
        translations.putAll(allTranslations);
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

    /**
     * Returns the category name localized for the current game language.
     * Falls back to description field, then the id itself.
     */
    public static String getLocalizedDescription(Minecraft mc, String catId) {
        String locale = currentLocale(mc);
        Map<String, String> localeMap = translations.get(locale);
        if (localeMap != null && localeMap.containsKey(catId)) {
            return localeMap.get(catId);
        }
        // Fallback: try stripping country variant (e.g. "en_gb" → "en")
        String lang = locale.split("_")[0];
        for (Map.Entry<String, Map<String, String>> e : translations.entrySet()) {
            if (e.getKey().startsWith(lang) && e.getValue().containsKey(catId)) {
                return e.getValue().get(catId);
            }
        }
        BannerRecipeCategory cat = registry.get(catId);
        return cat != null ? cat.description() : catId;
    }

    private static String currentLocale(Minecraft mc) {
        if (mc.options == null) return "en_us";

        return mc.options.languageCode.toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves the tab icon for a category from iconItemId.
     * Supports:
     * - Simple item id: "minecraft:book"
     * - Item with components: "minecraft:blue_banner[banner_patterns=[{\"pattern\":\"straight_cross\",\"color\":\"yellow\"}]]"
     * Falls back to lava bucket on parse error or unknown item id.
     */
    public static ItemStack resolveIcon(BannerRecipeCategory category) {
        String iconStr = category.iconItemId();
        if (iconStr == null || iconStr.isBlank()) {
            return new ItemStack(Items.LAVA_BUCKET);
        }

        try {
            ItemParser itemParser = new ItemParser(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            return itemParser.parse(new StringReader(iconStr)).createItemStack(1);
        } catch (RuntimeException | com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            LoomAssistantMod.LOGGER.debug("Failed to parse category icon as vanilla item input: {}", iconStr, e);
        }

        return new ItemStack(Items.LAVA_BUCKET);
    }

    // -------------------------------------------------------------------------
    // Legacy compat for code that still uses the old Category record
    // -------------------------------------------------------------------------

    /**
     * @deprecated Use {@link #getCategories()} returning {@link BannerRecipeCategory}.
     */
    @Deprecated
    public static List<Category> getLegacyCategories() {
        return getCategories().stream()
                .map(c -> new Category(c.id(), Identifier.tryParse(c.iconItemId()), c.description()))
                .toList();
    }

    /**
     * @deprecated Use {@link #resolveIcon(BannerRecipeCategory)}.
     */
    @Deprecated
    public static ItemStack resolveIcon(Category category) {
        if (category.itemId() != null && BuiltInRegistries.ITEM.containsKey(category.itemId())) {
            return new ItemStack(BuiltInRegistries.ITEM.getValue(category.itemId()));
        }
        return new ItemStack(Items.LAVA_BUCKET);
    }
}
