/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types.recipe;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import se.icus.mag.loomassistant.parsers.old.BannerParser;

class BannerParserPlanetMinecraftTest {
    @Test
    void testParsePlanetMinecraftRemixUrlE() {
        BannerRecipe recipe = BannerParser.parseUrl("https://www.planetminecraft.com/banner/?e=2c729cmcf28");

        assertEquals("red", recipe.bannerColor());
        assertEquals(5, recipe.layers().size());
    }

    @Test
    void testParsePlanetMinecraftRemixUrlB() {
        BannerRecipe recipe = BannerParser.parseUrl("https://www.planetminecraft.com/banner/?b=2c729cmcf28");

        assertEquals("red", recipe.bannerColor());
        assertEquals(5, recipe.layers().size());
    }

    @Test
    void testParsePlanetMinecraftCanonicalUrlFailsWithRemixHint() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> BannerParser.parseUrl(
                        "https://www.planetminecraft.com/banner/communist-flag-hammer-and-sickle/"));

        assertEquals("Please use Remix banner link", ex.getMessage());
    }
}
