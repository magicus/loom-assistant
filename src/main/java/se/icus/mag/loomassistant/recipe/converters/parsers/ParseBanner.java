/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe.converters.parsers;

import se.icus.mag.loomassistant.recipe.converters.parsers.urlparsers.UrlParser;
import se.icus.mag.loomassistant.recipe.BannerRecipe;

/**
 * Top-level entry point for banner parsing.
 *
 * <p>Supports:
 * - Minecraft /give commands and NBT data
 * - Online banner URLs (SkinMC, minecraft.tools, PlanetMinecraft, NeedCoolerShoes)
 */
public final class ParseBanner {
    private ParseBanner() {}

    /**
     * Validate that input can be parsed. Returns null if valid, error message if invalid.
     *
     * @param input the input to validate
     * @return null if valid, error message if invalid
     */
    public static String checkParse(String input) {
        if (input == null || input.isBlank()) return "Input cannot be empty";

        if (isUrl(input)) {
            return UrlParser.checkParseUrl(input);
        } else {
            return MinecraftParser.checkParse(input);
        }
    }

    /**
     * Parse a banner from input. Returns null if checkParse would have reported an error.
     *
     * @param input the input to parse
     * @return a BannerRecipe, or null if invalid
     */
    public static BannerRecipe parse(String input) {
        String error = checkParse(input);
        if (error != null) return null;

        if (isUrl(input)) {
            return UrlParser.parseUrl(input);
        } else {
            return MinecraftParser.parse(input);
        }
    }

    private static boolean isUrl(String input) {
        return input.startsWith("http://") || input.startsWith("https://");
    }
}
