/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types.recipe;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BannerParserSkinMCTest {

    @Test
    void testExtractBannerCode() {
        String url = "https://skinmc.net/banner/editor?=paalpwpEac";
        String code = BannerParserSkinMC.extractBannerCode(url);
        assertEquals("paalpwpEac", code);
    }

    @Test
    void testExtractBannerCodeWithMultipleParams() {
        String url = "https://skinmc.net/banner/editor?foo=bar&=paalpwpEac&baz=qux";
        String code = BannerParserSkinMC.extractBannerCode(url);
        assertEquals("paalpwpEac", code);
    }

    @Test
    void testExtractBannerCodeNoQuery() {
        String url = "https://skinmc.net/banner/editor";
        String code = BannerParserSkinMC.extractBannerCode(url);
        assertEquals("", code);
    }

    @Test
    void testDirectBannerLinkIsRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                BannerParserSkinMC.extractBannerCode(
                        "https://skinmc.net/banner/18d1d0c1-08fd-46a6-abfd-1766ebc47f26"));

        assertEquals("Please Use Edit design link", ex.getMessage());
    }

    @Test
    void testParseBannerCode() {
        // Code: paalpwpEac
        // Should decode to:
        // pa = white base
        // al = black cs (Pale)
        // pw = white ms (Fess)
        // pE = white ts (Chief)
        // ac = black bo (Bordure)
        BannerRecipe recipe = BannerParserSkinMC.parseBannerCode("paalpwpEac");

        assertNotNull(recipe);
        assertEquals("white", recipe.bannerColor());
        assertEquals(4, recipe.layers().size());

        // Check first layer
        BannerRecipeLayer layer1 = recipe.layers().get(0);
        assertEquals("minecraft:stripe_center", layer1.pattern().toString());

        // Check second layer
        BannerRecipeLayer layer2 = recipe.layers().get(1);
        assertEquals("minecraft:stripe_middle", layer2.pattern().toString());
    }

    @Test
    void testParseUrl() {
        String url = "https://skinmc.net/banner/editor?=paalpwpEac";
        BannerRecipe recipe = BannerParserSkinMC.parseUrl(url);

        assertNotNull(recipe);
        assertEquals("white", recipe.bannerColor());
        assertEquals(4, recipe.layers().size());
    }

    @Test
    void testInvalidEmptyCode() {
        assertThrows(IllegalArgumentException.class, () -> BannerParserSkinMC.parseBannerCode(""));
    }

    @Test
    void testInvalidOddLengthCode() {
        assertThrows(
                IllegalArgumentException.class, () -> BannerParserSkinMC.parseBannerCode("paa"));
    }

    @Test
    void testInvalidCharacterInCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> BannerParserSkinMC.parseBannerCode("pa!!"));
    }

    @Test
    void testParseUrlNoCode() {
        String url = "https://skinmc.net/banner/editor";
        assertThrows(IllegalArgumentException.class, () -> BannerParserSkinMC.parseUrl(url));
    }

    @Test
    void testMultipleBanners() {
        // Test parsing multiple different banner codes
        BannerRecipe recipe1 = BannerParserSkinMC.parseBannerCode("paalpwpEac");
        BannerRecipe recipe2 = BannerParserSkinMC.parseBannerCode("paal");  // white base + black cs

        assertEquals("white", recipe1.bannerColor());
        assertEquals(4, recipe1.layers().size());

        assertEquals("white", recipe2.bannerColor());
        assertEquals(1, recipe2.layers().size());
        assertEquals("minecraft:stripe_center", recipe2.layers().get(0).pattern().toString());
    }
}
