/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import se.icus.mag.loomassistant.recipe.BannerRecipe;

class ParseBannerTest {
    @Test
    void testCheckParseSkinMcUrl() {
        String error = ParseBanner.checkParse("https://skinmc.net/banner/editor?=paalpwpEac");
        assertNull(error);
    }

    @Test
    void testCheckParseMinecraftToolsUrl() {
        String error = ParseBanner.checkParse(
                "https://minecraft.tools/en/banner.php?color_id_0=4&shape_id_1=28&color_id_1=15");
        assertNull(error);
    }

    @Test
    void testCheckParsePlanetMinecraftUrl() {
        String error = ParseBanner.checkParse("https://www.planetminecraft.com/banner/?e=2c729cmcf28");
        assertNull(error);
    }

    @Test
    void testCheckParsePlanetMinecraftPageLink() {
        String error = ParseBanner.checkParse("https://www.planetminecraft.com/banner/union-sovietic-flag/");
        assertNotNull(error);
        assertTrue(error.contains("Remix Banner"));
    }

    @Test
    void testCheckParseNeedCoolershoesDirectUrl() {
        String error = ParseBanner.checkParse("https://needcoolershoes.com/banner?=ealleNhEehppai");
        assertNull(error);
    }

    @Test
    void testCheckParseNeedCoolershoesPageLink() {
        String error = ParseBanner.checkParse("https://needcoolershoes.com/banners/8961/~ghost");
        assertNotNull(error);
        assertTrue(error.contains("Open in Editor"));
    }

    @Test
    void testCheckParseNeedCoolershoesShortLink() {
        String error = ParseBanner.checkParse("https://ncrs.skin/b?=ealleNhEehppai");
        assertNull(error);
    }

    @Test
    void testCheckParseInvalidUrl() {
        String error = ParseBanner.checkParse("https://unknown.com/banner?code=xyz");
        assertNotNull(error);
        assertTrue(error.contains("Unsupported"));
    }

    @Test
    void testCheckParseEmpty() {
        String error = ParseBanner.checkParse("");
        assertNotNull(error);
        assertTrue(error.contains("empty"));
    }

    @Test
    void testParseSkinMcUrl() {
        BannerRecipe recipe = ParseBanner.parse("https://skinmc.net/banner/editor?=paalpwpEac");
        assertNotNull(recipe);
        assertEquals("white", recipe.bannerColor());
        assertEquals(4, recipe.layers().size());
    }

    @Test
    void testParseSkinMcDirectBannerLink() {
        String error = ParseBanner.checkParse("https://skinmc.net/banner/18d1d0c1-08fd-46a6-abfd-1766ebc47f26");
        assertNotNull(error);
        assertTrue(error.contains("Edit design"));
    }

    @Test
    void testParseInvalidReturnsNull() {
        BannerRecipe recipe = ParseBanner.parse("https://unknown.com/banner?code=xyz");
        assertNull(recipe);
    }
}
