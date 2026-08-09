/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;

class LoomScreenStateManagerDyeSwitchTest {
    @Test
    void persistentDyeSwitchRequiresPersistedSourceId() {
        LoomScreenStateManager manager = new LoomScreenStateManager();

        BannerRecipe base = new BannerRecipe(
                "Base",
                DyeColor.WHITE,
                List.of(BannerRecipeLayer.of("minecraft:stripe_center", DyeColor.BLACK.getName())));
        manager.setActiveBannerFromRecipe(base, null);

        LoomScreenState state = extractState(manager);
        assertNull(state.getActiveBannerRecipe());

        boolean changed = manager.applyDyeSwitch(Map.of(DyeColor.WHITE, DyeColor.RED), true);

        assertFalse(changed);
        assertFalse(state.isColorReplacementEnabled());
        assertTrue(state.getColorReplacements().isEmpty());
        assertEquals(DyeColor.WHITE, manager.getEffectiveActiveBanner().getBaseColorEnum());
    }

    private static LoomScreenState extractState(LoomScreenStateManager manager) {
        try {
            Field field = LoomScreenStateManager.class.getDeclaredField("state");
            field.setAccessible(true);
            return (LoomScreenState) field.get(manager);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to access LoomScreenStateManager.state", e);
        }
    }
}
