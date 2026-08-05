/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types.recipe;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BannerParserPlanetMinecraft {
    private static final String ALPHABET = "123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0_-";

    private static final String[] COLORS = {
        "black", "red", "green", "brown", "blue", "purple", "cyan", "light_gray",
        "gray", "pink", "lime", "yellow", "light_blue", "magenta", "orange", "white"
    };

    private static final Map<Integer, String> PATTERNS = Map.ofEntries(
            Map.entry(2, "border"),
            Map.entry(3, "bricks"),
            Map.entry(4, "circle"),
            Map.entry(5, "creeper"),
            Map.entry(6, "cross"),
            Map.entry(7, "curly_border"),
            Map.entry(8, "diagonal_left"),
            Map.entry(9, "diagonal_right"),
            Map.entry(10, "flower"),
            Map.entry(11, "gradient"),
            Map.entry(12, "half_horizontal"),
            Map.entry(13, "half_vertical"),
            Map.entry(14, "mojang"),
            Map.entry(15, "rhombus"),
            Map.entry(16, "skull"),
            Map.entry(17, "small_stripes"),
            Map.entry(18, "square_bottom_left"),
            Map.entry(19, "square_bottom_right"),
            Map.entry(20, "square_top_left"),
            Map.entry(21, "square_top_right"),
            Map.entry(22, "straight_cross"),
            Map.entry(23, "stripe_bottom"),
            Map.entry(24, "stripe_center"),
            Map.entry(25, "stripe_downleft"),
            Map.entry(26, "stripe_downright"),
            Map.entry(27, "stripe_left"),
            Map.entry(28, "stripe_middle"),
            Map.entry(29, "stripe_right"),
            Map.entry(30, "stripe_top"),
            Map.entry(31, "triangles_bottom"),
            Map.entry(32, "triangles_top"),
            Map.entry(33, "triangle_bottom"),
            Map.entry(34, "triangle_top"),
            Map.entry(35, "diagonal_up_left"),
            Map.entry(36, "diagonal_up_right"),
            Map.entry(37, "gradient_up"),
            Map.entry(38, "half_horizontal_bottom"),
            Map.entry(39, "half_vertical_right"),
            Map.entry(40, "globe"),
            Map.entry(41, "piglin"),
            Map.entry(42, "flow"),
            Map.entry(43, "guster"));

    public static BannerRecipe parseUrl(String url) {
        String code = extractCode(url);
        return parseCode(code);
    }

    public static String extractCode(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        if (!url.contains("://")) {
            return url.trim();
        }

        try {
            URI uri = new URI(url);
            String query = uri.getQuery();
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("No PMC banner code found in URL: " + url);
            }

            query = URLDecoder.decode(query, StandardCharsets.UTF_8);
            for (String part : query.split("&")) {
                if (part.startsWith("b=")) return part.substring(2);
                if (part.startsWith("e=")) return part.substring(2);
            }

            throw new IllegalArgumentException("No PMC banner code found in URL: " + url);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    public static BannerRecipe parseCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("PMC banner code cannot be empty");
        }
        code = code.trim();

        // 1 base char + 2 chars per layer
        if (code.length() < 3 || code.length() % 2 == 0) {
            throw new IllegalArgumentException("Invalid PMC banner code length: " + code.length());
        }

        int baseId = decodeChar(code.charAt(0));
        if (baseId < 0 || baseId > 15) {
            throw new IllegalArgumentException("Invalid base color id: " + baseId);
        }

        String bannerColor = COLORS[baseId];
        List<BannerRecipeLayer> layers = new ArrayList<>();

        for (int i = 1; i < code.length(); i += 2) {
            int colorId = decodeChar(code.charAt(i));
            int patternIdx = decodeChar(code.charAt(i + 1));

            String pattern = PATTERNS.get(patternIdx);
            if (pattern == null) {
                throw new IllegalArgumentException("Unsupported PMC pattern index: " + patternIdx);
            }
            if (colorId < 0 || colorId > 15) {
                throw new IllegalArgumentException("Invalid layer color id: " + colorId);
            }

            layers.add(BannerRecipeLayer.of("minecraft:" + pattern, COLORS[colorId]));
        }

        return new BannerRecipe(
                "planetminecraft_import", "Imported from PlanetMinecraft", null, null, bannerColor, layers);
    }

    private static int decodeChar(char c) {
        int idx = ALPHABET.indexOf(c);
        if (idx < 0) {
            throw new IllegalArgumentException("Invalid PMC code character: " + c);
        }
        return idx;
    }
}
