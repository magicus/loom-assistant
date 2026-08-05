/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types.recipe;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import se.icus.mag.loomassistant.parsers.old.BannerParser;

class BannerParserTest {
    @Test
    void testParseSkinMcUrl() {
        String url = "https://skinmc.net/banner/editor?=paalpwpEac";
        BannerRecipe recipe = BannerParser.parseUrl(url);

        assertNotNull(recipe);
        assertEquals("white", recipe.bannerColor());
        assertEquals(4, recipe.layers().size());
    }

    @Test
    void testParseSkinMcDirectBannerUrlFailsWithEditDesignHint() {
        String url = "https://skinmc.net/banner/18d1d0c1-08fd-46a6-abfd-1766ebc47f26";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BannerParser.parseUrl(url));
        assertEquals("Please Use Edit design link", ex.getMessage());
    }

    @Test
    void testParseMinecraftToolsUrl() {
        String url =
                "https://minecraft.tools/en/banner.php?color_id_0=4&shape_id_1=28&color_id_1=15&shape_id_2=37&color_id_2=4";
        BannerRecipe recipe = BannerParser.parseUrl(url);

        assertNotNull(recipe);
        assertEquals("blue", recipe.bannerColor());
        assertEquals(2, recipe.layers().size());
    }

    @Test
    void testParsePlanetMinecraftRemixUrlE() {
        String url = "https://www.planetminecraft.com/banner/?e=2c729cmcf28";
        BannerRecipe recipe = BannerParser.parseUrl(url);

        assertNotNull(recipe);
        assertEquals("red", recipe.bannerColor());
        assertEquals(5, recipe.layers().size());
    }

    @Test
    void testParsePlanetMinecraftRemixUrlB() {
        String url = "https://www.planetminecraft.com/banner/?b=2c729cmcf28";
        BannerRecipe recipe = BannerParser.parseUrl(url);

        assertNotNull(recipe);
        assertEquals("red", recipe.bannerColor());
        assertEquals(5, recipe.layers().size());
    }

    @Test
    void testParsePlanetMinecraftCanonicalUrlFailsWithRemixHint() {
        String url = "https://www.planetminecraft.com/banner/communist-flag-hammer-and-sickle/";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BannerParser.parseUrl(url));
        assertEquals("Please use Remix banner link", ex.getMessage());
    }

    @Test
    void testParseNeedCoolerShoesUrl() {
        BannerRecipe recipe = BannerParser.parseUrl("https://needcoolershoes.com/banners/8961/~ghost");

        assertNotNull(recipe);
        assertEquals("white", recipe.bannerColor());
        assertEquals(6, recipe.layers().size());
    }

    @Test
    void testParseNeedCoolerShoesDirectCodeUrls() {
        BannerRecipe recipe1 = BannerParser.parseUrl("https://needcoolershoes.com/banner?=ealleNhEehppai");
        BannerRecipe recipe2 = BannerParser.parseUrl("https://ncrs.skin/b?=ealleNhEehppai");

        assertNotNull(recipe1);
        assertNotNull(recipe2);
        assertEquals("blue", recipe1.bannerColor());
        assertEquals("blue", recipe2.bannerColor());
        assertEquals(6, recipe1.layers().size());
        assertEquals(6, recipe2.layers().size());
    }

    @Test
    void testParseUrlWithCustomDescription() {
        String url = "https://skinmc.net/banner/editor?=paalpwpEac";
        BannerRecipe recipe = BannerParser.parseUrl(url, "My Custom Banner", "flags");

        assertEquals("My Custom Banner", recipe.description());
        assertEquals("flags", recipe.category());
    }

    @Test
    void testParseUrlWithNullDescription() {
        String url = "https://skinmc.net/banner/editor?=paalpwpEac";
        BannerRecipe recipe = BannerParser.parseUrl(url, null, null);

        assertNotNull(recipe.description()); // Should use default
    }

    @Test
    void testUnsupportedUrl() {
        String url = "https://unknown-banner-tool.com/editor?code=xyz";
        assertThrows(IllegalArgumentException.class, () -> BannerParser.parseUrl(url));
    }

    @Test
    void testEmptyUrl() {
        assertThrows(IllegalArgumentException.class, () -> BannerParser.parseUrl(""));
    }

    @Test
    void testNullUrl() {
        assertThrows(IllegalArgumentException.class, () -> BannerParser.parseUrl(null));
    }

    @Test
    void testGenerateDescription() {
        String url =
                "https://minecraft.tools/en/banner.php?color_id_0=4&shape_id_1=28&color_id_1=15&shape_id_2=37&color_id_2=4";
        BannerRecipe recipe = BannerParser.parseUrl(url);

        String description = BannerParser.generateDescription(recipe);
        assertEquals("Blue banner: white bricks, blue half_horizontal_bottom", description);
    }

    @Test
    void testParseUrlWithAutoDescription() {
        String url = "https://minecraft.tools/en/banner.php?color_id_0=4&shape_id_1=28&color_id_1=15";
        BannerRecipe recipe = BannerParser.parseUrlWithAutoDescription(url);

        assertNotNull(recipe.description());
        assertTrue(recipe.description().contains("Blue banner"));
        assertTrue(recipe.description().contains("white bricks"));
    }

    @Test
    void testBothFormats() {
        // Test that both formats work interchangeably through unified parser
        String skinmcUrl = "https://skinmc.net/banner/editor?=paal";
        String toolsUrl = "https://minecraft.tools/en/banner.php?color_id_0=15&shape_id_1=11&color_id_1=0";

        BannerRecipe recipe1 = BannerParser.parseUrl(skinmcUrl);
        BannerRecipe recipe2 = BannerParser.parseUrl(toolsUrl);

        // Both should succeed
        assertNotNull(recipe1);
        assertNotNull(recipe2);

        // Both should have base color
        assertNotNull(recipe1.bannerColor());
        assertNotNull(recipe2.bannerColor());

        // Both should generate descriptions
        String desc1 = BannerParser.generateDescription(recipe1);
        String desc2 = BannerParser.generateDescription(recipe2);
        assertNotNull(desc1);
        assertNotNull(desc2);
    }
}
