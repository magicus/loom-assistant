/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.parsers.old;

import se.icus.mag.loomassistant.types.recipe.BannerRecipe;
import se.icus.mag.loomassistant.types.recipe.BannerRecipeLayer;

/**
 * Unified banner parser that supports multiple banner URL formats.
 * Currently supported:
 * - SkinMC: https://skinmc.net/banner/editor?=paalpwpEac
 * - minecraft.tools: https://minecraft.tools/en/banner.php?color_id_0=4&shape_id_1=28&...
 * - Planet Minecraft: https://www.planetminecraft.com/banner/?e=2c729cmcf28
 * - NeedCoolerShoes: https://needcoolershoes.com/banners/8961/~ghost
 * - NeedCoolerShoes short links: https://needcoolershoes.com/banner?=ealleNhEehppai
 * - NCRS short links: https://ncrs.skin/b?=ealleNhEehppai
 */
public class BannerParser {
    /**
     * Generate a human-readable description for a banner recipe.
     *
     * @param recipe the banner recipe
     * @return a descriptive string like "Yellow banner: black bricks, white cross, ..."
     */
    public static String generateDescription(BannerRecipe recipe) {
        if (recipe.layers().isEmpty()) {
            return recipe.bannerColor() + " banner";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(capitalizeFirst(recipe.bannerColor())).append(" banner: ");

        for (int i = 0; i < recipe.layers().size(); i++) {
            BannerRecipeLayer layer = recipe.layers().get(i);
            String colorName = layer.color().getName();
            String patternName = extractPatternName(layer.pattern().toString());

            if (i > 0) {
                sb.append(", ");
            }
            sb.append(colorName).append(" ").append(patternName);
        }

        return sb.toString();
    }

    /**
     * Extract the pattern name from a full pattern identifier.
     * E.g., "minecraft:skull" → "skull"
     */
    private static String extractPatternName(String fullPattern) {
        if (fullPattern.contains(":")) {
            return fullPattern.split(":")[1];
        }
        return fullPattern;
    }

    /**
     * Capitalize first letter of a string.
     */
    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Parse a banner URL and return the banner recipe with auto-generated description.
     *
     * @param url the banner editor URL
     * @return a BannerRecipe object with auto-generated description
     */
    public static BannerRecipe parseUrlWithAutoDescription(String url) {
        BannerRecipe recipe = parseUrl(url);
        String autoDescription = generateDescription(recipe);
        return recipe.withDescription(autoDescription);
    }

    /**
     * Parse a banner URL and automatically detect which parser to use.
     *
     * @param url the banner editor URL
     * @return a BannerRecipe object
     * @throws IllegalArgumentException if the URL format is not recognized or invalid
     */
    public static BannerRecipe parseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        if (url.contains("skinmc.net")) {
            return BannerParserSkinMC.parseUrl(url);
        } else if (url.contains("minecraft.tools")) {
            return BannerParserMinecraftTools.parseUrl(url);
        } else if (url.contains("planetminecraft.com/banner/")) {
            try {
                return BannerParserPlanetMinecraft.parseUrl(url);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Please use Remix banner link", e);
            }
        } else if (url.contains("needcoolershoes.com") || url.contains("ncrs.skin")) {
            return BannerParserNeedCoolerShoes.parseUrl(url);
        } else {
            throw new IllegalArgumentException("Unsupported banner URL format. "
                    + "Supported sources: skinmc.net, minecraft.tools, planetminecraft.com, needcoolershoes.com, ncrs.skin. "
                    + "URL: "
                    + url);
        }
    }

    /**
     * Parse a banner URL and return the banner recipe with custom description and category.
     *
     * @param url the banner editor URL
     * @param description optional custom description
     * @param category optional custom category
     * @return a BannerRecipe object with updated metadata
     */
    public static BannerRecipe parseUrl(String url, String description, String category) {
        BannerRecipe recipe = parseUrl(url);
        if (description != null && !description.isBlank()) {
            recipe = recipe.withDescription(description);
        }
        if (category != null && !category.isBlank()) {
            recipe = recipe.withCategory(category);
        }
        return recipe;
    }
}
