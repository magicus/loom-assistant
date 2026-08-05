/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.parsers;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import se.icus.mag.loomassistant.parsers.old.BannerParserMinecraftTools;
import se.icus.mag.loomassistant.types.recipe.BannerRecipe;

/**
 * Parser for minecraft.tools banner URLs.
 *
 * <p>Formats:
 * - https://minecraft.tools/en/banner.php?color_id_0=4&shape_id_1=28&color_id_1=15&...
 */
public final class MinecraftToolsUrlParser extends AbstractUrlBannerParser {
    private final String url;
    private final URI uri;

    public MinecraftToolsUrlParser(String url) {
        this.url = url;
        try {
            this.uri = new URI(url);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid minecraft.tools URL: " + url, e);
        }
    }

    @Override
    protected String extractBannerCode() {
        String query = uri.getQuery();
        if (query == null || query.isBlank()) {
            return "";
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

        if (params.isEmpty()) {
            return "";
        }

        return "minecraft-tools:" + params.toString();
    }

    @Override
    protected BannerRecipe getBannerFromCode(String code) {
        if (!code.startsWith("minecraft-tools:")) {
            throw new IllegalArgumentException("Invalid minecraft.tools code");
        }

        Map<String, Integer> params = parseParams(code.substring("minecraft-tools:".length()));
        return BannerParserMinecraftTools.parseParameters(params);
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
}
