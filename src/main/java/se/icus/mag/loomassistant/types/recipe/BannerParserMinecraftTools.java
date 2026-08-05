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
 * Parser for minecraft.tools banner editor URLs.
 * Converts URLs like
 * "https://minecraft.tools/en/banner.php?color_id_0=4&shape_id_1=28&color_id_1=15&..."
 * into BannerRecipe objects.
 *
 * <p>The URL format uses direct indices, but color ids are inverted in the generated command:
 * - color_id_N: Inverted dye color index (0 becomes black, 15 becomes white)
 * - shape_id_N: Site-specific banner pattern index (0-40)
 *
 * <p>Layer 0 is the base color (color_id_0), layers 1+ are patterns.
 */
public class BannerParserMinecraftTools {

    // minecraft.tools banner pattern order (shape_id index -> banner short code)
    private static final String[] MINECRAFT_PATTERNS = {
        null,
        "mc",
        "bl",
        "br",
        "tl",
        "tr",
        "hh",
        "bs",
        "ts",
        "vh",
        "ls",
        "cs",
        "rs",
        "ms",
        "sc",
        "dls",
        "drs",
        "cr",
        "ld",
        "rud",
        "tt",
        "bt",
        "mr",
        "tts",
        "bts",
        "cbo",
        "bo",
        "ss",
        "bri",
        "gra",
        "cre",
        "sku",
        "flo",
        "moj",
        "lud",
        "rd",
        "gru",
        "hhb",
        "vhr",
        "glb",
        "pig"
    };

    private static final Map<String, String> PATTERN_IDS = buildPatternIds();

    private static final int NUM_PATTERNS = MINECRAFT_PATTERNS.length;

    /**
     * Parse a minecraft.tools banner URL and extract parameters.
     *
     * @param url the minecraft.tools banner URL
     * @return a map of parameter names to values
     * @throws IllegalArgumentException if the URL is invalid
     */
    public static Map<String, Integer> extractParameters(String url) {
        try {
            URI uri = new URI(url);
            String query = uri.getQuery();
            if (query == null || query.isEmpty()) {
                return new HashMap<>();
            }

            query = URLDecoder.decode(query, StandardCharsets.UTF_8);

            Map<String, Integer> params = new HashMap<>();
            for (String param : query.split("&")) {
                if (param.isEmpty()) continue;
                String[] parts = param.split("=", 2);
                if (parts.length == 2 && parts[0] != null && parts[1] != null) {
                    try {
                        int value = Integer.parseInt(parts[1]);
                        params.put(parts[0], value);
                    } catch (NumberFormatException e) {
                        // Ignore non-integer values
                    }
                }
            }

            return params;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    /**
     * Parse parameters into a BannerRecipe.
     *
     * @param params the extracted parameters (color_id_N, shape_id_N)
     * @return a BannerRecipe object
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static BannerRecipe parseParameters(Map<String, Integer> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("No parameters provided");
        }

        // Get base color
        Integer baseColorId = params.get("color_id_0");
        if (baseColorId == null) {
            throw new IllegalArgumentException("Missing required parameter: color_id_0");
        }

        if (baseColorId < 0 || baseColorId > 15) {
            throw new IllegalArgumentException(
                    "Invalid base color index: " + baseColorId + " (must be 0-15)");
        }

        String bannerColor = getInvertedColorName(baseColorId);

        // Parse layers
        List<BannerRecipeLayer> layers = new ArrayList<>();
        int layerIndex = 1;

        while (params.containsKey("shape_id_" + layerIndex)) {
            Integer shapeId = params.get("shape_id_" + layerIndex);
            Integer colorId = params.get("color_id_" + layerIndex);

            if (shapeId == null) {
                throw new IllegalArgumentException("Missing shape_id_" + layerIndex);
            }

            if (colorId == null) {
                throw new IllegalArgumentException("Missing color_id_" + layerIndex);
            }

            if (shapeId < 0 || shapeId >= NUM_PATTERNS) {
                throw new IllegalArgumentException(
                        "Invalid pattern index at layer "
                                + layerIndex
                                + ": "
                                + shapeId
                                + " (must be 0-"
                                + (NUM_PATTERNS - 1)
                                + ")");
            }

            if (colorId < 0 || colorId > 15) {
                throw new IllegalArgumentException(
                        "Invalid color index at layer "
                                + layerIndex
                                + ": "
                                + colorId
                                + " (must be 0-15)");
            }

            String patternCode = MINECRAFT_PATTERNS[shapeId];
            if (patternCode == null) {
                layerIndex++;
                continue;
            }

            DyeColor dyeColor = DyeColor.byName(getInvertedColorName(colorId), DyeColor.WHITE);
            String patternId = PATTERN_IDS.get(patternCode);
            if (patternId == null) {
                throw new IllegalArgumentException("Invalid pattern code: " + patternCode);
            }

            net.minecraft.resources.Identifier identifier =
                    net.minecraft.resources.Identifier.tryParse(patternId);
            if (identifier == null) {
                throw new IllegalArgumentException("Invalid pattern identifier: " + patternId);
            }

            layers.add(new BannerRecipeLayer(identifier, dyeColor));
            layerIndex++;
        }

        return new BannerRecipe(
                "minecraft_tools_import",
                "Imported from minecraft.tools",
                null,
                null,
                bannerColor,
                layers);
    }

    /**
     * Parse a full minecraft.tools banner URL into a BannerRecipe.
     *
     * @param url the minecraft.tools banner URL
     * @return a BannerRecipe object
     * @throws IllegalArgumentException if the URL or parameters are invalid
     */
    public static BannerRecipe parseUrl(String url) {
        Map<String, Integer> params = extractParameters(url);
        if (params.isEmpty() || !params.containsKey("color_id_0")) {
            throw new IllegalArgumentException(
                    "No valid banner parameters found in URL. "
                            + "Expected color_id_0, shape_id_1, etc.: "
                            + url);
        }
        return parseParameters(params);
    }

    private static String getInvertedColorName(int colorId) {
        int inverted = 15 - colorId;
        DyeColor dyeColor = DyeColor.byId(inverted);
        return dyeColor.getName();
    }

    private static Map<String, String> buildPatternIds() {
        Map<String, String> map = new HashMap<>();
        map.put("mc", "minecraft:circle");
        map.put("bl", "minecraft:square_bottom_left");
        map.put("br", "minecraft:square_bottom_right");
        map.put("tl", "minecraft:square_top_left");
        map.put("tr", "minecraft:square_top_right");
        map.put("hh", "minecraft:half_horizontal");
        map.put("bs", "minecraft:stripe_bottom");
        map.put("ts", "minecraft:stripe_top");
        map.put("vh", "minecraft:half_vertical");
        map.put("ls", "minecraft:stripe_left");
        map.put("cs", "minecraft:stripe_center");
        map.put("rs", "minecraft:stripe_right");
        map.put("ms", "minecraft:stripe_middle");
        map.put("sc", "minecraft:straight_cross");
        map.put("dls", "minecraft:diagonal_left");
        map.put("drs", "minecraft:diagonal_right");
        map.put("cr", "minecraft:cross");
        map.put("ld", "minecraft:diagonal_up_left");
        map.put("rud", "minecraft:diagonal_up_right");
        map.put("tt", "minecraft:triangle_top");
        map.put("bt", "minecraft:triangle_bottom");
        map.put("mr", "minecraft:rhombus");
        map.put("tts", "minecraft:triangles_top");
        map.put("bts", "minecraft:triangles_bottom");
        map.put("cbo", "minecraft:curly_border");
        map.put("bo", "minecraft:border");
        map.put("ss", "minecraft:small_stripes");
        map.put("bri", "minecraft:bricks");
        map.put("gra", "minecraft:gradient");
        map.put("cre", "minecraft:creeper");
        map.put("sku", "minecraft:skull");
        map.put("flo", "minecraft:flower");
        map.put("moj", "minecraft:mojang");
        map.put("lud", "minecraft:diagonal_up_left");
        map.put("rd", "minecraft:diagonal_up_right");
        map.put("gru", "minecraft:gradient_up");
        map.put("hhb", "minecraft:half_horizontal_bottom");
        map.put("vhr", "minecraft:half_vertical_right");
        map.put("glb", "minecraft:globe");
        map.put("pig", "minecraft:piglin");
        return map;
    }
}
