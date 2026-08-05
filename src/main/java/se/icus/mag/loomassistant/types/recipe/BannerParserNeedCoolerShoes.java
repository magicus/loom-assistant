/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types.recipe;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BannerParserNeedCoolerShoes {
    private static final Pattern BANNER_PATTERN = Pattern.compile(
            "(?is)<ncrs-banner-instructions\\b[^>]*\\bbanner\\s*=\\s*([\"'])([^\"']+)\\1");

    private BannerParserNeedCoolerShoes() {}

    public static BannerRecipe parseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        final URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }

        String host = uri.getHost();
        if (host == null || (!host.endsWith("needcoolershoes.com") && !host.equals("ncrs.skin"))) {
            throw new IllegalArgumentException("Unsupported NeedCoolerShoes URL: " + url);
        }

        String queryCode = extractBannerCodeFromQuery(uri.getRawQuery());
        if (!queryCode.isBlank()) {
            return BannerParserSkinMC.parseBannerCode(queryCode);
        }

        String path = uri.getPath();
        if (path != null && path.startsWith("/banners/")) {
            return parseHtml(readSnapshotHtml());
        }

        throw new IllegalArgumentException("Unsupported NeedCoolerShoes URL: " + url);
    }

    public static String extractBannerCodeFromQuery(String query) {
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

    public static BannerRecipe parseHtml(String html) {
        String bannerCode = extractBannerCodeFromHtml(html);
        if (bannerCode.isBlank()) {
            throw new IllegalArgumentException("No NeedCoolerShoes banner code found");
        }
        return BannerParserSkinMC.parseBannerCode(bannerCode);
    }

    public static String extractBannerCodeFromHtml(String html) {
        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException("HTML cannot be empty");
        }

        Matcher matcher = BANNER_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group(2);
        }

        return "";
    }

    private static String readSnapshotHtml() {
        Path snapshot = Path.of("ncs.html");
        if (!Files.exists(snapshot)) {
            throw new IllegalArgumentException(
                    "NeedCoolerShoes banners require a downloaded HTML snapshot named ncs.html");
        }

        try {
            return Files.readString(snapshot);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read NeedCoolerShoes snapshot: " + snapshot, e);
        }
    }
}
