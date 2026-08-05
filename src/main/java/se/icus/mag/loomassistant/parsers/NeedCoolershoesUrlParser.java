/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.parsers;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import se.icus.mag.loomassistant.parsers.old.BannerParserSkinMC;
import se.icus.mag.loomassistant.types.recipe.BannerRecipe;

/**
 * Parser for NeedCoolerShoes banner URLs.
 *
 * <p>Formats accepted:
 * - Direct code link: https://needcoolershoes.com/banner?=BANNERCODE
 * - Short link: https://ncrs.skin/b?=BANNERCODE
 *
 * <p>Page links (e.g., https://needcoolershoes.com/banners/8961/~ghost) are rejected
 * with a message directing users to use the "Open in Editor" link instead.
 */
public final class NeedCoolershoesUrlParser extends AbstractUrlBannerParser {
    private static final Pattern PAGE_LINK_PATTERN = Pattern.compile("/banners/\\d+/~\\w+$");

    private final String url;
    private final URI uri;

    public NeedCoolershoesUrlParser(String url) {
        this.url = url;
        try {
            this.uri = new URI(url);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid NeedCoolerShoes URL: " + url, e);
        }
    }

    @Override
    protected String extractBannerCode() {
        String host = uri.getHost();
        if (host == null || (!host.endsWith("needcoolershoes.com") && !host.equals("ncrs.skin"))) {
            throw new IllegalArgumentException("Unsupported NeedCoolerShoes URL");
        }

        String path = uri.getPath();
        if (path != null && PAGE_LINK_PATTERN.matcher(path).find()) {
            throw new IllegalArgumentException("Please use Open in Editor link");
        }

        String queryCode = extractBannerCodeFromQuery(uri.getRawQuery());
        if (!queryCode.isBlank()) {
            return queryCode;
        }

        throw new IllegalArgumentException("No banner code found in NeedCoolerShoes URL");
    }

    @Override
    protected BannerRecipe getBannerFromCode(String code) {
        return BannerParserSkinMC.parseBannerCode(code);
    }

    private String extractBannerCodeFromQuery(String query) {
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
}
