/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.parsers.urlparsers;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import se.icus.mag.loomassistant.recipe.BannerRecipe;

/**
 * Parser for SkinMC banner URLs.
 *
 * <p>Formats:
 * - Editor link: https://skinmc.net/banner/editor?=BANNERCODE
 */
public final class SkinMcUrlParser extends UrlParser {
    private final URI uri;

    public SkinMcUrlParser(String url) {
        try {
            this.uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SkinMC URL: " + url, e);
        }
    }

    @Override
    protected String extractBannerCode() {
        String path = uri.getPath();

        if (path != null && !"/banner/editor".equals(path)) {
            if (path.matches("^/banner/[^/]+$")) {
                throw new IllegalArgumentException("Please Use Edit design link");
            }
        }

        String query = uri.getQuery();
        if (query == null || query.isBlank()) {
            return "";
        }

        query = URLDecoder.decode(query, StandardCharsets.UTF_8);

        for (String param : query.split("&")) {
            if (param.startsWith("=") && param.length() > 1) {
                return param.substring(1);
            }
        }

        for (String param : query.split("&")) {
            if (param.isEmpty()) continue;
            String[] parts = param.split("=", 2);
            if (parts.length == 2 && parts[1] != null && !parts[1].isEmpty()) {
                return parts[1];
            }
        }

        return "";
    }

    @Override
    protected BannerRecipe getBannerFromCode(String code) {
        return SkinMcBannerCodeParser.parseBannerCode(code);
    }
}
