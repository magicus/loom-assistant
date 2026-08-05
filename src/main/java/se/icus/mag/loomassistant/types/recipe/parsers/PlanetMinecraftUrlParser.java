/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types.recipe.parsers;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import se.icus.mag.loomassistant.types.recipe.BannerParserPlanetMinecraft;
import se.icus.mag.loomassistant.types.recipe.BannerRecipe;

/**
 * Parser for PlanetMinecraft banner URLs.
 *
 * <p>Formats accepted:
 * - Remix link: https://www.planetminecraft.com/banner/?e=2c729cmcf28
 * - Remix link: https://www.planetminecraft.com/banner/?b=2c729cmcf28
 *
 * <p>Gallery page links (e.g., https://www.planetminecraft.com/banner/union-sovietic-flag/)
 * are rejected with a message directing users to use the "Remix Banner" link instead.
 */
public final class PlanetMinecraftUrlParser extends AbstractUrlBannerParser {
    private final String url;
    private final URI uri;

    public PlanetMinecraftUrlParser(String url) {
        this.url = url;
        try {
            this.uri = new URI(url);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid PlanetMinecraft URL: " + url, e);
        }
    }

    @Override
    protected String extractBannerCode() {
        String path = uri.getPath();

        if (path != null && path.startsWith("/banner/") && !path.equals("/banner/")) {
            throw new IllegalArgumentException("Please use Remix Banner link");
        }

        String query = uri.getQuery();
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("No PlanetMinecraft banner code found");
        }

        query = URLDecoder.decode(query, StandardCharsets.UTF_8);

        for (String part : query.split("&")) {
            if (part.startsWith("b=")) return part.substring(2);
            if (part.startsWith("e=")) return part.substring(2);
        }

        throw new IllegalArgumentException("No PlanetMinecraft banner code found");
    }

    @Override
    protected BannerRecipe getBannerFromCode(String code) {
        return BannerParserPlanetMinecraft.parseCode(code);
    }
}
