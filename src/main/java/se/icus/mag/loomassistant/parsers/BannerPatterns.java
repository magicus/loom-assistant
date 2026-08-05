/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.parsers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared banner pattern mappings used across all banner parsers.
 * Maps between different provider-specific pattern codes and Minecraft pattern IDs.
 */
public final class BannerPatterns {
    private static final Map<String, String> PATTERN_IDS = buildPatternIds();

    private BannerPatterns() {}

    /**
     * Get the Minecraft pattern ID for a given provider-specific pattern code.
     *
     * @param patternCode the provider-specific pattern code (e.g., "drs", "cr")
     * @return the Minecraft pattern ID (e.g., "minecraft:stripe_downright")
     */
    public static String getPatternId(String patternCode) {
        return PATTERN_IDS.getOrDefault(patternCode, "minecraft:" + patternCode);
    }

    /**
     * Get an unmodifiable map of all pattern mappings.
     *
     * @return a map from pattern codes to Minecraft IDs
     */
    public static Map<String, String> getPatternMap() {
        return Collections.unmodifiableMap(PATTERN_IDS);
    }

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
}
