/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.parsers.urlparsers;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.loomassistant.parsers.LegacyBannerPatterns;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;

/**
 * Shared utility for parsing SkinMC-format banner codes.
 * Used by both SkinMcUrlParser and NeedCoolershoesUrlParser since they use the same encoding.
 */
public final class SkinMcBannerCodeParser {
    private static final String BASE64_DICT = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/";

    private static final String[] PATTERNS = {
        "base", "bl", "bo", "br", "bri", "bs", "bt", "bts", "cbo", "cr", "cre", "cs", "dls",
        "drs", "flo", "gra", "hh", "ld", "ls", "mc", "moj", "mr", "ms", "rd", "rs", "sc",
        "sku", "ss", "tl", "tr", "ts", "tt", "tts", "vh", "lud", "rud", "gru", "hhb", "vhr",
        "glb", "pig", "flow", "guster"
    };

    private static final String[] DYE_COLORS = {
        "black",
        "red",
        "green",
        "brown",
        "blue",
        "purple",
        "cyan",
        "gray",
        "dark_gray",
        "pink",
        "lime",
        "yellow",
        "light_blue",
        "magenta",
        "orange",
        "white"
    };

    private SkinMcBannerCodeParser() {}

    /**
     * Parse a SkinMC-format banner code into a BannerRecipe.
     *
     * @param bannerCode the banner code (e.g., "paalpwpEac")
     * @return a BannerRecipe object
     * @throws IllegalArgumentException if the code is invalid or empty
     */
    public static BannerRecipe parseBannerCode(String bannerCode) {
        if (bannerCode == null || bannerCode.trim().isEmpty())
            throw new IllegalArgumentException("Banner code cannot be empty");

        String normalized = bannerCode.trim();
        if (normalized.length() % 2 != 0)
            throw new IllegalArgumentException(
                    "Banner code must have even number of characters, got: " + normalized.length());

        List<DecodedPair> decoded = new ArrayList<>();
        for (int i = 0; i < normalized.length(); i += 2) {
            String pair = normalized.substring(i, i + 2);
            decoded.add(decodePair(pair));
        }

        if (decoded.isEmpty()) throw new IllegalArgumentException("Banner code produced no layers");

        DecodedPair base = decoded.getFirst();
        String bannerColor = base.colorName();

        List<BannerRecipeLayer> layers = new ArrayList<>();
        for (int i = 1; i < decoded.size(); i++) {
            DecodedPair layer = decoded.get(i);
            String patternId = LegacyBannerPatterns.getPatternId(layer.pattern());
            DyeColor color = DyeColor.byName(layer.colorName(), DyeColor.WHITE);
            layers.add(new BannerRecipeLayer(Identifier.tryParse(patternId), color));
        }

        return new BannerRecipe("skinmc_import", "Imported from SkinMC", null, null, bannerColor, layers);
    }

    private static DecodedPair decodePair(String pair) {
        if (pair == null || pair.length() != 2)
            throw new IllegalArgumentException("Pair must be exactly 2 characters, got: " + pair);

        int e = BASE64_DICT.indexOf(pair.charAt(0));
        int a = BASE64_DICT.indexOf(pair.charAt(1));

        if (e < 0 || a < 0)
            throw new IllegalArgumentException("Invalid character in pair: " + pair + " (not in base64 alphabet)");

        int colorIndex = e & 15;
        int patternIndex = (e >> 4) << 6 | a;

        if (patternIndex >= PATTERNS.length)
            throw new IllegalArgumentException("Invalid pattern index: " + patternIndex + " for pair: " + pair);

        String pattern = PATTERNS[patternIndex];
        String colorName = colorIndex < DYE_COLORS.length ? DYE_COLORS[colorIndex] : "white";

        return new DecodedPair(pattern, colorName, colorIndex);
    }

    private record DecodedPair(String pattern, String colorName, int colorIndex) {}
}
