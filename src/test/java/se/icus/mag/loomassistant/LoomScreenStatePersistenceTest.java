/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

class LoomScreenStatePersistenceTest {
    private static final Gson GSON = LoomScreenStateManager.createPersistenceGson();

    @Test
    void writesPersistedFieldsWithRequestedNames() {
        LoomScreenState state = new LoomScreenState();
        state.setActiveBannerRecipe("local:test-pack/banner");
        state.setColorReplacementEnabled(true);
        state.setColorReplacements(Map.of(DyeColor.LIGHT_BLUE, DyeColor.LIME));

        JsonObject json = JsonParser.parseString(GSON.toJson(state)).getAsJsonObject();

        assertEquals("local:test-pack/banner", json.get("activeBannerRecipe").getAsString());
        assertTrue(json.getAsJsonObject("colorReplacements").has("light_blue"));
        assertEquals(
                "lime", json.getAsJsonObject("colorReplacements").get("light_blue").getAsString());
        assertTrue(json.get("colorReplacementEnabled").getAsBoolean());
        assertFalse(json.has("activeBannerSourceId"));
        assertFalse(json.has("persistentDyeReplacementMap"));
        assertFalse(json.has("persistentDyeSwitchEnabled"));
    }

    @Test
    void derivesLocalWorldKeyFromInstanceSavePath() {
        Path gameDir = Path.of("/instance");
        Path worldDir = gameDir.resolve("saves").resolve("New World");

        assertEquals("local:New World", LoomScreenStateManager.localWorldKey(worldDir, gameDir));
    }
}
