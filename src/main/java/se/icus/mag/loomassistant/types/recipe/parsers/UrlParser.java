/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types.recipe.parsers;

import se.icus.mag.loomassistant.types.recipe.BannerRecipe;

/**
 * Routes banner URLs to the appropriate provider-specific parser.
 */
public final class UrlParser {
    private UrlParser() {}

    public static String checkParse(String url) {
        if (url == null || url.isBlank()) {
            return "URL cannot be empty";
        }

        AbstractUrlBannerParser parser = selectParser(url);
        if (parser == null) {
            return "Unsupported banner URL. Supported: skinmc.net, minecraft.tools, planetminecraft.com, needcoolershoes.com, ncrs.skin";
        }

        return parser.checkParse();
    }

    public static BannerRecipe parse(String url) {
        AbstractUrlBannerParser parser = selectParser(url);
        if (parser == null) {
            return null;
        }

        return parser.parse();
    }

    private static AbstractUrlBannerParser selectParser(String url) {
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
