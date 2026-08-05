/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types.recipe;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import se.icus.mag.loomassistant.parsers.old.BannerParserMinecraftTools;

class BannerParserMinecraftToolsTest {
    @Test
    void testExtractParameters() {
        String url =
                "https://minecraft.tools/en/banner.php?color_id_0=4&shape_id_1=28&color_id_1=15&shape_id_2=37&color_id_2=4";
        Map<String, Integer> params = BannerParserMinecraftTools.extractParameters(url);

        assertEquals(5, params.size());
        assertEquals(4, params.get("color_id_0").intValue());
        assertEquals(28, params.get("shape_id_1").intValue());
        assertEquals(15, params.get("color_id_1").intValue());
        assertEquals(37, params.get("shape_id_2").intValue());
        assertEquals(4, params.get("color_id_2").intValue());
    }

    @Test
    void testExtractParametersNoQuery() {
        String url = "https://minecraft.tools/en/banner.php";
        Map<String, Integer> params = BannerParserMinecraftTools.extractParameters(url);
        assertTrue(params.isEmpty());
    }

    @Test
    void testParseParameters() {
        Map<String, Integer> params = new java.util.HashMap<>();
        params.put("color_id_0", 4); // yellow
        params.put("shape_id_1", 28); // bricks
        params.put("color_id_1", 15); // black
        params.put("shape_id_2", 37); // skull
        params.put("color_id_2", 4); // yellow

        BannerRecipe recipe = BannerParserMinecraftTools.parseParameters(params);

        assertNotNull(recipe);
        assertEquals("blue", recipe.bannerColor());
        assertEquals(2, recipe.layers().size());

        // Check first layer
        BannerRecipeLayer layer1 = recipe.layers().get(0);
        assertEquals("minecraft:bricks", layer1.pattern().toString());
        assertEquals("white", layer1.color().getName());

        // Check second layer
        BannerRecipeLayer layer2 = recipe.layers().get(1);
        assertEquals("minecraft:half_horizontal_bottom", layer2.pattern().toString());
        assertEquals("blue", layer2.color().getName());
    }

    @Test
    void testParseUrl() {
        String url =
                "https://minecraft.tools/en/banner.php?color_id_0=4&shape_id_1=28&color_id_1=15&shape_id_2=37&color_id_2=4";
        BannerRecipe recipe = BannerParserMinecraftTools.parseUrl(url);

        assertNotNull(recipe);
        assertEquals("blue", recipe.bannerColor());
        assertEquals(2, recipe.layers().size());
    }

    @Test
    void testParseUrlNoBaseColor() {
        String url = "https://minecraft.tools/en/banner.php?shape_id_1=28&color_id_1=15";
        assertThrows(IllegalArgumentException.class, () -> BannerParserMinecraftTools.parseUrl(url));
    }

    @Test
    void testInvalidColorIndex() {
        Map<String, Integer> params = new java.util.HashMap<>();
        params.put("color_id_0", 99); // invalid

        assertThrows(IllegalArgumentException.class, () -> BannerParserMinecraftTools.parseParameters(params));
    }

    @Test
    void testInvalidPatternIndex() {
        Map<String, Integer> params = new java.util.HashMap<>();
        params.put("color_id_0", 4);
        params.put("shape_id_1", 99); // invalid
        params.put("color_id_1", 15);

        assertThrows(IllegalArgumentException.class, () -> BannerParserMinecraftTools.parseParameters(params));
    }

    @Test
    void testAllDyeColors() {
        // Test that all 16 input values map to the inverted dye colors used by minecraft.tools
        String[] colors = {
            "black", "red", "green", "brown", "blue", "purple", "cyan", "light_gray",
            "gray", "pink", "lime", "yellow", "light_blue", "magenta", "orange", "white"
        };

        for (int i = 0; i < colors.length; i++) {
            Map<String, Integer> params = new java.util.HashMap<>();
            params.put("color_id_0", i);

            BannerRecipe recipe = BannerParserMinecraftTools.parseParameters(params);
            assertEquals(colors[i], recipe.bannerColor(), "Color index " + i + " should be " + colors[i]);
        }
    }

    @Test
    void testComplexBanner() {
        // Test with the exact URL from the user
        String url =
                "https://minecraft.tools/en/banner.php?color_id_0=4&shape_id_1=28&color_id_1=15&shape_id_2=37&color_id_2=4&shape_id_3=14&color_id_3=0&shape_id_4=14&color_id_4=4&shape_id_5=26&color_id_5=0&shape_id_6=26&color_id_6=4#crafting";
        BannerRecipe recipe = BannerParserMinecraftTools.parseUrl(url);

        assertEquals("blue", recipe.bannerColor());
        assertEquals(6, recipe.layers().size());

        // Layer 1: bricks (28) - white (15)
        assertEquals("minecraft:bricks", recipe.layers().get(0).pattern().toString());
        assertEquals("white", recipe.layers().get(0).color().getName());

        // Layer 2: half horizontal bottom (37) - blue (4)
        assertEquals(
                "minecraft:half_horizontal_bottom",
                recipe.layers().get(1).pattern().toString());
        assertEquals("blue", recipe.layers().get(1).color().getName());

        // Layer 3: straight cross (14) - black (0)
        assertEquals(
                "minecraft:straight_cross", recipe.layers().get(2).pattern().toString());
        assertEquals("black", recipe.layers().get(2).color().getName());

        // Layer 4: straight cross (14) - blue (4)
        assertEquals(
                "minecraft:straight_cross", recipe.layers().get(3).pattern().toString());
        assertEquals("blue", recipe.layers().get(3).color().getName());

        // Layer 5: border (26) - black (0)
        assertEquals("minecraft:border", recipe.layers().get(4).pattern().toString());
        assertEquals("black", recipe.layers().get(4).color().getName());

        // Layer 6: border (26) - blue (4)
        assertEquals("minecraft:border", recipe.layers().get(5).pattern().toString());
        assertEquals("blue", recipe.layers().get(5).color().getName());
    }

    @Test
    void testShapeZeroIsIgnored() {
        Map<String, Integer> params = new java.util.HashMap<>();
        params.put("color_id_0", 4);
        params.put("shape_id_1", 0);
        params.put("color_id_1", 0);

        BannerRecipe recipe = BannerParserMinecraftTools.parseParameters(params);

        assertEquals("blue", recipe.bannerColor());
        assertTrue(recipe.layers().isEmpty());
    }
}
