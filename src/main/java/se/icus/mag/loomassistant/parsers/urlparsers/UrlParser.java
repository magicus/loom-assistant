/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.parsers.urlparsers;

import se.icus.mag.loomassistant.recipe.BannerRecipe;

/**
 * Abstract base class for URL-based banner parsers.
 *
 * <p>Each provider (SkinMC, minecraft.tools, PlanetMinecraft, NeedCoolerShoes) extracts banner
 * codes differently. Subclasses implement extractBannerCode() and getBannerFromCode().
 *
 * <p>This class also provides static routing methods to select the appropriate parser for a given
 * URL.
 */
public abstract class UrlParser {
    /**
     * Extract the banner code from this URL. Must be implemented by subclasses.
     *
     * @return the banner code, or null/empty if not found
     * @throws IllegalArgumentException if URL format is invalid
     */
    protected abstract String extractBannerCode() throws IllegalArgumentException;

    /**
     * Build a BannerRecipe from the banner code. Must be implemented by subclasses.
     *
     * @param code the banner code
     * @return a BannerRecipe
     * @throws IllegalArgumentException if code is invalid
     */
    protected abstract BannerRecipe getBannerFromCode(String code) throws IllegalArgumentException;

    /**
     * Validate that this parser can handle the URL.
     *
     * @return null if valid, error message if invalid
     */
    public String checkParse() {
        try {
            String code = extractBannerCode();
            if (code == null || code.isBlank()) {
                return "No banner code found";
            }
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /**
     * Parse the URL into a BannerRecipe.
     *
     * @return a BannerRecipe, or null if validation failed
     */
    public BannerRecipe parse() {
        String error = checkParse();
        if (error != null) {
            return null;
        }

        try {
            String code = extractBannerCode();
            return getBannerFromCode(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ========== Static routing methods ==========

    /**
     * Check if a URL can be parsed.
     *
     * @param url the banner URL
     * @return null if valid, error message if invalid
     */
    public static String checkParseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "URL cannot be empty";
        }

        UrlParser parser = selectParser(url);
        if (parser == null) {
            return "Unsupported banner URL. Supported: skinmc.net, minecraft.tools, planetminecraft.com, needcoolershoes.com, ncrs.skin";
        }

        return parser.checkParse();
    }

    /**
     * Parse a banner URL into a BannerRecipe.
     *
     * @param url the banner URL
     * @return a BannerRecipe, or null if parsing failed
     */
    public static BannerRecipe parseUrl(String url) {
        UrlParser parser = selectParser(url);
        if (parser == null) {
            return null;
        }

        return parser.parse();
    }

    /**
     * Select the appropriate parser for a given URL.
     *
     * @param url the banner URL
     * @return an appropriate parser, or null if no parser supports this URL
     */
    private static UrlParser selectParser(String url) {
        if (url.contains("skinmc.net")) {
            return new SkinMcUrlParser(url);
        } else if (url.contains("minecraft.tools")) {
            return new MinecraftToolsUrlParser(url);
        } else if (url.contains("planetminecraft.com/banner/")) {
            return new PlanetMinecraftUrlParser(url);
        } else if (url.contains("needcoolershoes.com") || url.contains("ncrs.skin")) {
            return new NeedCoolershoesUrlParser(url);
        }

        return null;
    }
}
