/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types.recipe;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.DyeColor;

/**
 * Parser for SkinMC banner editor URLs.
 * Converts URLs like "https://skinmc.net/banner/editor?=paalpwpEac" into BannerRecipe objects.
 *
 * <p>The banner code is base64-like encoded with a custom alphabet:
 * "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/"
 *
 * <p>Each pair of characters encodes a banner layer:
 * - First character's lower 4 bits = dye color index
 * - Combined bits = pattern index
 */
public class BannerParserSkinMC {
    private static final String BASE64_DICT = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/";

    // All available banner patterns (from window.BANNER_CONFIG.patterns)
    private static final String[] PATTERNS = {
        "base", "bl", "bo", "br", "bri", "bs", "bt", "bts", "cbo", "cr", "cre", "cs", "dls",
        "drs", "flo", "gra", "hh", "ld", "ls", "mc", "moj", "mr", "ms", "rd", "rs", "sc",
        "sku", "ss", "tl", "tr", "ts", "tt", "tts", "vh", "lud", "rud", "gru", "hhb", "vhr",
        "glb", "pig", "flow", "guster"
    };

    // Minecraft pattern IDs (namespace:pattern_name)
    private static final Map<String, String> PATTERN_IDS = buildPatternIds();

    // Dye color index to name mapping
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

    private static Map<String, String> buildPatternIds() {
        Map<String, String> map = new HashMap<>();
        map.put("bs", "minecraft:stripe_bottom");
        map.put("ts", "minecraft:stripe_top");
        map.put("ls", "minecraft:stripe_left");
        map.put("rs", "minecraft:stripe_right");
        map.put("cs", "minecraft:stripe_center");
        map.put("ms", "minecraft:stripe_middle");
        map.put("drs", "minecraft:stripe_downright");
        map.put("dls", "minecraft:stripe_downleft");
        map.put("ss", "minecraft:small_stripes");
        map.put("cr", "minecraft:cross");
        map.put("sc", "minecraft:straight_cross");
        map.put("ld", "minecraft:diagonal_left");
        map.put("rud", "minecraft:diagonal_right");
        map.put("lud", "minecraft:diagonal_up_left");
        map.put("rd", "minecraft:diagonal_up_right");
        map.put("vh", "minecraft:half_vertical");
        map.put("vhr", "minecraft:half_vertical_right");
        map.put("hh", "minecraft:half_horizontal");
        map.put("hhb", "minecraft:half_horizontal_bottom");
        map.put("bl", "minecraft:square_bottom_left");
        map.put("br", "minecraft:square_bottom_right");
        map.put("tl", "minecraft:square_top_left");
        map.put("tr", "minecraft:square_top_right");
        map.put("bt", "minecraft:triangle_bottom");
        map.put("tt", "minecraft:triangle_top");
        map.put("bts", "minecraft:triangles_bottom");
        map.put("tts", "minecraft:triangles_top");
        map.put("mc", "minecraft:circle");
        map.put("mr", "minecraft:rhombus");
        map.put("bo", "minecraft:border");
        map.put("cbo", "minecraft:curly_border");
        map.put("bri", "minecraft:bricks");
        map.put("gra", "minecraft:gradient");
        map.put("gru", "minecraft:gradient_up");
        map.put("cre", "minecraft:creeper");
        map.put("sku", "minecraft:skull");
        map.put("flo", "minecraft:flower");
        map.put("moj", "minecraft:mojang");
        map.put("glb", "minecraft:globe");
        map.put("pig", "minecraft:piglin");
        map.put("flow", "minecraft:flow");
        map.put("guster", "minecraft:guster");
        return map;
    }

    /**
     * Parse a SkinMC banner editor URL and extract the banner code.
     *
     * @param url the SkinMC banner editor URL
     * @return the extracted banner code, or empty string if not found
     * @throws IllegalArgumentException if the URL is invalid
     */
    public static String extractBannerCode(String url) {
        final URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }

        if (isDirectBannerLink(uri.getPath())) {
            throw new IllegalArgumentException("Please Use Edit design link");
        }

        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            return "";
        }

        try {
            query = URLDecoder.decode(query, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }

        // First, try to find the "=CODE" format (special case for SkinMC)
        for (String param : query.split("&")) {
            if (param.startsWith("=") && param.length() > 1) {
                return param.substring(1);
            }
        }

        // Fallback: look for any parameter with a value
        for (String param : query.split("&")) {
            if (param.isEmpty()) continue;
            String[] parts = param.split("=", 2);
            if (parts.length == 2 && parts[1] != null && !parts[1].isEmpty()) {
                return parts[1];
            }
        }

        return "";
    }

    /**
     * Decode a single pair of characters from the banner code.
     *
     * @param pair a two-character string
     * @return DecodedPair with color index and pattern name
     * @throws IllegalArgumentException if the pair is invalid
     */
    private static DecodedPair decodePair(String pair) {
        if (pair == null || pair.length() != 2) {
            throw new IllegalArgumentException("Pair must be exactly 2 characters, got: " + pair);
        }

        int e = BASE64_DICT.indexOf(pair.charAt(0));
        int a = BASE64_DICT.indexOf(pair.charAt(1));

        if (e < 0 || a < 0) {
            throw new IllegalArgumentException("Invalid character in pair: " + pair + " (not in base64 alphabet)");
        }

        int colorIndex = e & 15; // Lower 4 bits = color index
        int patternIndex = (e >> 4) << 6 | a; // Upper bits + second char = pattern index

        if (patternIndex >= PATTERNS.length) {
            throw new IllegalArgumentException("Invalid pattern index: " + patternIndex + " for pair: " + pair);
        }

        String pattern = PATTERNS[patternIndex];
        String colorName = colorIndex < DYE_COLORS.length ? DYE_COLORS[colorIndex] : "white";

        return new DecodedPair(pattern, colorName, colorIndex);
    }

    /**
     * Parse a banner code into a BannerRecipe.
     *
     * @param bannerCode the banner code (e.g., "paalpwpEac")
     * @return a BannerRecipe object
     * @throws IllegalArgumentException if the code is invalid or empty
     */
    public static BannerRecipe parseBannerCode(String bannerCode) {
        if (bannerCode == null || bannerCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Banner code cannot be empty");
        }

        // Split code into pairs
        String normalized = bannerCode.trim();
        if (normalized.length() % 2 != 0) {
            throw new IllegalArgumentException(
                    "Banner code must have even number of characters, got: " + normalized.length());
        }

        List<DecodedPair> decoded = new ArrayList<>();
        for (int i = 0; i < normalized.length(); i += 2) {
            String pair = normalized.substring(i, i + 2);
            decoded.add(decodePair(pair));
        }

        if (decoded.isEmpty()) {
            throw new IllegalArgumentException("Banner code produced no layers");
        }

        // First pair is the base color and base pattern
        DecodedPair base = decoded.get(0);
        String bannerColor = base.colorName();

        // Rest are layer patterns
        List<BannerRecipeLayer> layers = new ArrayList<>();
        for (int i = 1; i < decoded.size(); i++) {
            DecodedPair layer = decoded.get(i);
            String patternId = PATTERN_IDS.getOrDefault(layer.pattern(), "minecraft:" + layer.pattern());
            DyeColor color = DyeColor.byName(layer.colorName(), DyeColor.WHITE);
            layers.add(new BannerRecipeLayer(net.minecraft.resources.Identifier.tryParse(patternId), color));
        }

        return new BannerRecipe("skinmc_import", "Imported from SkinMC", null, null, bannerColor, layers);
    }

    /**
     * Parse a full SkinMC banner editor URL into a BannerRecipe.
     *
     * @param url the SkinMC banner editor URL
     * @return a BannerRecipe object
     * @throws IllegalArgumentException if the URL or banner code is invalid
     */
    public static BannerRecipe parseUrl(String url) {
        String bannerCode = extractBannerCode(url);
        if (bannerCode.isEmpty()) {
            throw new IllegalArgumentException("No banner code found in URL: " + url);
        }
        return parseBannerCode(bannerCode);
    }

    private static boolean isDirectBannerLink(String path) {
        if (path == null) {
            return false;
        }

        String normalizedPath = path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
        return normalizedPath.matches("^/banner/[^/]+$") && !"/banner/editor".equals(normalizedPath);
    }

    /**
     * Internal class to hold decoded pair data.
     */
    private record DecodedPair(String pattern, String colorName, int colorIndex) {}
}
