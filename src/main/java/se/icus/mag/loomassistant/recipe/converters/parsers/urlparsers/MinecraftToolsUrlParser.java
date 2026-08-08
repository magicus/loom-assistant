/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe.converters.parsers.urlparsers;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.loomassistant.recipe.converters.parsers.LegacyBannerPatterns;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;

/**
 * Parser for minecraft.tools banner URLs.
 *
 * <p>Formats:
 * - https://minecraft.tools/en/banner.php?color_id_0=4&shape_id_1=28&color_id_1=15&...
 *
 * <p>The URL format uses direct indices, but color ids are inverted in the generated command:
 * - color_id_N: Inverted dye color index (0 becomes black, 15 becomes white)
 * - shape_id_N: Site-specific banner pattern index (0-40)
 */
public final class MinecraftToolsUrlParser extends UrlParser {
    private static final String[] MINECRAFT_PATTERNS = {
        null, "mc", "bl", "br", "tl", "tr", "hh", "bs", "ts", "vh", "ls", "cs", "rs", "ms", "sc", "dls", "drs", "cr",
        "ld", "rud", "tt", "bt", "mr", "tts", "bts", "cbo", "bo", "ss", "bri", "gra", "cre", "sku", "flo", "moj", "lud",
        "rd", "gru", "hhb", "vhr", "glb", "pig"
    };

    private static final int NUM_PATTERNS = MINECRAFT_PATTERNS.length;

    private final URI uri;

    public MinecraftToolsUrlParser(String url) {
        try {
            this.uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid minecraft.tools URL: " + url, e);
        }
    }

    @Override
    protected String extractBannerCode() {
        String query = uri.getQuery();
        if (query == null || query.isBlank()) return "";

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

        if (params.isEmpty()) return "";

        return "minecraft-tools:" + params;
    }

    @Override
    protected BannerRecipe getBannerFromCode(String code) {
        if (!code.startsWith("minecraft-tools:")) throw new IllegalArgumentException("Invalid minecraft.tools code");

        Map<String, Integer> params = parseParams(code.substring("minecraft-tools:".length()));
        return parseParameters(params);
    }

    private static BannerRecipe parseParameters(Map<String, Integer> params) {
        if (params == null || params.isEmpty()) throw new IllegalArgumentException("No parameters provided");

        Integer baseColorId = params.get("color_id_0");
        if (baseColorId == null) throw new IllegalArgumentException("Missing required parameter: color_id_0");

        if (baseColorId < 0 || baseColorId > 15)
            throw new IllegalArgumentException("Invalid base color index: " + baseColorId + " (must be 0-15)");

        String bannerColor = getInvertedColorName(baseColorId);
        List<BannerRecipeLayer> layers = new ArrayList<>();
        int layerIndex = 1;

        while (params.containsKey("shape_id_" + layerIndex)) {
            Integer shapeId = params.get("shape_id_" + layerIndex);
            Integer colorId = params.get("color_id_" + layerIndex);

            if (shapeId == null) throw new IllegalArgumentException("Missing shape_id_" + layerIndex);

            if (colorId == null) throw new IllegalArgumentException("Missing color_id_" + layerIndex);

            if (shapeId < 0 || shapeId >= NUM_PATTERNS)
                throw new IllegalArgumentException("Invalid pattern index at layer "
                        + layerIndex
                        + ": "
                        + shapeId
                        + " (must be 0-"
                        + (NUM_PATTERNS - 1)
                        + ")");

            if (colorId < 0 || colorId > 15)
                throw new IllegalArgumentException(
                        "Invalid color index at layer " + layerIndex + ": " + colorId + " (must be 0-15)");

            String patternCode = MINECRAFT_PATTERNS[shapeId];
            if (patternCode == null) {
                layerIndex++;
                continue;
            }

            DyeColor dyeColor = DyeColor.byName(getInvertedColorName(colorId), DyeColor.WHITE);
            String patternId = LegacyBannerPatterns.getPatternId(patternCode);

            Identifier identifier = Identifier.tryParse(patternId);
            if (identifier == null) throw new IllegalArgumentException("Invalid pattern identifier: " + patternId);

            layers.add(new BannerRecipeLayer(identifier, dyeColor));
            layerIndex++;
        }

        return new BannerRecipe(
                "minecraft_tools_import", "Imported from minecraft.tools", null, null, bannerColor, layers);
    }

    private Map<String, Integer> parseParams(String mapStr) {
        Map<String, Integer> result = new HashMap<>();

        String content = mapStr.substring(1, mapStr.length() - 1);
        for (String entry : content.split(", ")) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                try {
                    result.put(parts[0], Integer.parseInt(parts[1]));
                } catch (NumberFormatException e) {
                    // Skip
                }
            }
        }

        return result;
    }

    private static String getInvertedColorName(int colorId) {
        int inverted = 15 - colorId;
        DyeColor dyeColor = DyeColor.byId(inverted);
        return dyeColor.getName();
    }
}
