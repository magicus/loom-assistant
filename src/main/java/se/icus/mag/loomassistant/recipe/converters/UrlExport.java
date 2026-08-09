/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe.converters;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;
import se.icus.mag.loomassistant.recipe.converters.parsers.LegacyBannerPatterns;

/**
 * Temporary URL export utility extracted from ManageBannerRecipesScreen.
 *
 * <p>Kept as an intermediate step so UI logic stays clean while future refactoring can decide final
 * ownership and API.
 */
public final class UrlExport {
    private static final String PMC_ALPHABET = "123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0_-";
    private static final String SKINMC_BASE64_DICT = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/";

    private static final String[] SKINMC_PATTERNS = {
        "base", "bl", "bo", "br", "bri", "bs", "bt", "bts", "cbo", "cr", "cre", "cs", "dls", "drs", "flo", "gra", "hh",
        "ld", "ls", "mc", "moj", "mr", "ms", "rd", "rs", "sc", "sku", "ss", "tl", "tr", "ts", "tt", "tts", "vh", "lud",
        "rud", "gru", "hhb", "vhr", "glb", "pig", "flow", "guster"
    };

    private static final String[] MINECRAFT_TOOLS_PATTERNS = {
        null, "mc", "bl", "br", "tl", "tr", "hh", "bs", "ts", "vh", "ls", "cs", "rs", "ms", "sc", "dls", "drs", "cr",
        "ld", "rud", "tt", "bt", "mr", "tts", "bts", "cbo", "bo", "ss", "bri", "gra", "cre", "sku", "flo", "moj", "lud",
        "rd", "gru", "hhb", "vhr", "glb", "pig"
    };

    private static final Map<String, Integer> PLANET_PATTERN_INDEX_BY_ID = Map.ofEntries(
            Map.entry("minecraft:border", 2),
            Map.entry("minecraft:bricks", 3),
            Map.entry("minecraft:circle", 4),
            Map.entry("minecraft:creeper", 5),
            Map.entry("minecraft:cross", 6),
            Map.entry("minecraft:curly_border", 7),
            Map.entry("minecraft:diagonal_left", 8),
            Map.entry("minecraft:diagonal_right", 9),
            Map.entry("minecraft:flower", 10),
            Map.entry("minecraft:gradient", 11),
            Map.entry("minecraft:half_horizontal", 12),
            Map.entry("minecraft:half_vertical", 13),
            Map.entry("minecraft:mojang", 14),
            Map.entry("minecraft:rhombus", 15),
            Map.entry("minecraft:skull", 16),
            Map.entry("minecraft:small_stripes", 17),
            Map.entry("minecraft:square_bottom_left", 18),
            Map.entry("minecraft:square_bottom_right", 19),
            Map.entry("minecraft:square_top_left", 20),
            Map.entry("minecraft:square_top_right", 21),
            Map.entry("minecraft:straight_cross", 22),
            Map.entry("minecraft:stripe_bottom", 23),
            Map.entry("minecraft:stripe_center", 24),
            Map.entry("minecraft:stripe_downleft", 25),
            Map.entry("minecraft:stripe_downright", 26),
            Map.entry("minecraft:stripe_left", 27),
            Map.entry("minecraft:stripe_middle", 28),
            Map.entry("minecraft:stripe_right", 29),
            Map.entry("minecraft:stripe_top", 30),
            Map.entry("minecraft:triangles_bottom", 31),
            Map.entry("minecraft:triangles_top", 32),
            Map.entry("minecraft:triangle_bottom", 33),
            Map.entry("minecraft:triangle_top", 34),
            Map.entry("minecraft:diagonal_up_left", 35),
            Map.entry("minecraft:diagonal_up_right", 36),
            Map.entry("minecraft:gradient_up", 37),
            Map.entry("minecraft:half_horizontal_bottom", 38),
            Map.entry("minecraft:half_vertical_right", 39),
            Map.entry("minecraft:globe", 40),
            Map.entry("minecraft:piglin", 41),
            Map.entry("minecraft:flow", 42),
            Map.entry("minecraft:guster", 43));

    private static final Map<String, Integer> SKINMC_PATTERN_INDEX_BY_ID = buildSkinMcPatternIndex();
    private static final Map<String, Integer> MINECRAFT_TOOLS_PATTERN_INDEX_BY_ID = buildMinecraftToolsPatternIndex();

    private UrlExport() {}

    public static String toPlanetMinecraft(BannerRecipe recipe) {
        if (recipe == null) return null;

        StringBuilder code = new StringBuilder();
        int baseColorIndex = providerColorIndex(recipe.getBannerColorEnum());
        if (baseColorIndex < 0 || baseColorIndex >= PMC_ALPHABET.length()) return null;
        code.append(PMC_ALPHABET.charAt(baseColorIndex));

        for (BannerRecipeLayer layer : recipe.getLayers()) {
            Integer patternIndex = PLANET_PATTERN_INDEX_BY_ID.get(
                    normalizePatternId(layer.pattern().toString()));
            if (patternIndex == null || patternIndex < 0 || patternIndex >= PMC_ALPHABET.length()) return null;

            int colorIndex = providerColorIndex(layer.getDyeColorEnum());
            if (colorIndex < 0 || colorIndex >= PMC_ALPHABET.length()) return null;

            code.append(PMC_ALPHABET.charAt(colorIndex));
            code.append(PMC_ALPHABET.charAt(patternIndex));
        }

        return "https://www.planetminecraft.com/banner/?b=" + code;
    }

    public static String toMinecraftTools(BannerRecipe recipe) {
        if (recipe == null) return null;

        StringBuilder url = new StringBuilder("https://minecraft.tools/en/banner.php?");
        url.append("color_id_0=").append(providerColorIndex(recipe.getBannerColorEnum()));

        for (int i = 0; i < recipe.getLayers().size(); i++) {
            BannerRecipeLayer layer = recipe.getLayers().get(i);
            Integer patternIndex = MINECRAFT_TOOLS_PATTERN_INDEX_BY_ID.get(
                    normalizePatternId(layer.pattern().toString()));
            if (patternIndex == null) return null;

            int layerNo = i + 1;
            url.append("&shape_id_").append(layerNo).append('=').append(patternIndex);
            url.append("&color_id_").append(layerNo).append('=').append(providerColorIndex(layer.getDyeColorEnum()));
        }

        return url.toString();
    }

    public static String toSkimMc(BannerRecipe recipe) {
        String code = buildSkinMcCode(recipe);
        return code == null ? null : "https://skinmc.net/banner/editor?=" + code;
    }

    public static String toNeedCoolerShoes(BannerRecipe recipe) {
        String code = buildSkinMcCode(recipe);
        return code == null ? null : "https://needcoolershoes.com/banner?=" + code;
    }

    private static String buildSkinMcCode(BannerRecipe recipe) {
        if (recipe == null) return null;

        StringBuilder code = new StringBuilder();
        String basePair = encodeSkinMcPair(0, providerColorIndex(recipe.getBannerColorEnum()));
        if (basePair == null) return null;
        code.append(basePair);

        for (BannerRecipeLayer layer : recipe.getLayers()) {
            Integer patternIndex = SKINMC_PATTERN_INDEX_BY_ID.get(
                    normalizePatternId(layer.pattern().toString()));
            if (patternIndex == null) return null;

            String pair = encodeSkinMcPair(patternIndex, providerColorIndex(layer.getDyeColorEnum()));
            if (pair == null) return null;
            code.append(pair);
        }

        return code.toString();
    }

    private static String encodeSkinMcPair(int patternIndex, int colorIndex) {
        if (patternIndex < 0 || colorIndex < 0 || colorIndex > 15) return null;

        int encodedHigh = ((patternIndex >> 6) << 4) | colorIndex;
        int encodedLow = patternIndex & 63;
        if (encodedHigh < 0 || encodedHigh >= SKINMC_BASE64_DICT.length()) return null;
        if (encodedLow < 0 || encodedLow >= SKINMC_BASE64_DICT.length()) return null;

        return "" + SKINMC_BASE64_DICT.charAt(encodedHigh) + SKINMC_BASE64_DICT.charAt(encodedLow);
    }

    private static int providerColorIndex(DyeColor color) {
        return 15 - color.getId();
    }

    private static String normalizePatternId(String patternId) {
        if (patternId == null || patternId.isBlank()) return "";
        return patternId.contains(":") ? patternId : "minecraft:" + patternId;
    }

    private static Map<String, Integer> buildSkinMcPatternIndex() {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < SKINMC_PATTERNS.length; i++) {
            map.put(LegacyBannerPatterns.getPatternId(SKINMC_PATTERNS[i]), i);
        }
        return Map.copyOf(map);
    }

    private static Map<String, Integer> buildMinecraftToolsPatternIndex() {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < MINECRAFT_TOOLS_PATTERNS.length; i++) {
            String patternCode = MINECRAFT_TOOLS_PATTERNS[i];
            if (patternCode == null) continue;
            map.put(LegacyBannerPatterns.getPatternId(patternCode), i);
        }
        return Map.copyOf(map);
    }
}
