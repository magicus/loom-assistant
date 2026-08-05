/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types.recipe;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BannerParserNeedCoolerShoesTest {

    @Test
    void testExtractBannerCodeFromHtml() throws IOException {
        String html = Files.readString(Path.of("ncs.html"));

        String code = BannerParserNeedCoolerShoes.extractBannerCodeFromHtml(html);

        assertEquals("paaEpzpcpiatpg", code);
    }

    @Test
    void testParseHtml() throws IOException {
        String html = Files.readString(Path.of("ncs.html"));

        BannerRecipe recipe = BannerParserNeedCoolerShoes.parseHtml(html);

        assertNotNull(recipe);
        assertEquals("white", recipe.bannerColor());
        assertEquals(6, recipe.layers().size());
    }

    @Test
    void testParseUrlUsesSnapshotHtml() {
        BannerRecipe recipe =
                BannerParserNeedCoolerShoes.parseUrl("https://needcoolershoes.com/banners/8961/~ghost");

        assertNotNull(recipe);
        assertEquals("white", recipe.bannerColor());
        assertEquals(6, recipe.layers().size());
    }

    @Test
    void testParseDirectCodeUrl() {
        BannerRecipe recipe =
                BannerParserNeedCoolerShoes.parseUrl("https://needcoolershoes.com/banner?=ealleNhEehppai");

        assertNotNull(recipe);
        assertEquals("blue", recipe.bannerColor());
        assertEquals(6, recipe.layers().size());
    }

    @Test
    void testParseNcrsShortUrl() {
        BannerRecipe recipe = BannerParserNeedCoolerShoes.parseUrl("https://ncrs.skin/b?=ealleNhEehppai");

        assertNotNull(recipe);
        assertEquals("blue", recipe.bannerColor());
        assertEquals(6, recipe.layers().size());
    }
}
