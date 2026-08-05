/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types.recipe.parsers;

import se.icus.mag.loomassistant.types.recipe.BannerRecipe;

/**
 * Abstract base for URL-based banner parsers.
 *
 * <p>Each provider (SkinMC, minecraft.tools, etc.) extracts banner codes differently.
 * Subclasses implement extractBannerCode() and getBannerFromCode().
 */
public abstract class AbstractUrlBannerParser {
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
}
