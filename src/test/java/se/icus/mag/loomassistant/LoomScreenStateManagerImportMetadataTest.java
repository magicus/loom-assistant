/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;

class LoomScreenStateManagerImportMetadataTest {
    @Test
    void importBannerWithMetadataSavesAndSelectsImportedBanner() {
        LoomScreenStateManager manager = new LoomScreenStateManager();
        BannerRecipe imported = new BannerRecipe(
                "Incoming",
                DyeColor.WHITE,
                List.of(BannerRecipeLayer.of("minecraft:stripe_center", DyeColor.BLACK.getName())));

        manager.importBannerWithMetadata(imported, "Imported Name", "misc");

        assertTrue(manager.hasActiveBanner());
        assertNotNull(manager.getActiveBanner());
        assertNotNull(manager.getEffectiveActiveBanner());
        assertEquals("Imported Name", manager.getActiveBanner().getName());
        assertEquals("Imported Name", manager.getEffectiveActiveBanner().getName());
        assertEquals("misc", manager.getActiveBanner().getCategory());
        assertNotNull(extractState(manager).getActiveBannerRecipe());
    }

    @Test
    void importBannerWithMetadataUsesDefaultsForBlankInputs() {
        LoomScreenStateManager manager = new LoomScreenStateManager();
        BannerRecipe imported = new BannerRecipe(
                "Incoming",
                DyeColor.WHITE,
                List.of(BannerRecipeLayer.of("minecraft:stripe_center", DyeColor.BLACK.getName())));

        manager.importBannerWithMetadata(imported, "  ", "");

        assertTrue(manager.hasActiveBanner());
        assertEquals(BannerRecipe.getUnnamedBanner(), manager.getActiveBanner().getName());
        assertEquals(BannerRecipe.DEFAULT_CATEGORY, manager.getActiveBanner().getCategory());
        assertNotNull(extractState(manager).getActiveBannerRecipe());
    }

    @Test
    void importBannerWithMetadataIgnoresNullBanner() {
        LoomScreenStateManager manager = new LoomScreenStateManager();

        manager.importBannerWithMetadata(null, "Name", "misc");

        assertFalse(manager.hasActiveBanner());
        assertNull(manager.getActiveBanner());
        assertNull(manager.getEffectiveActiveBanner());
        assertNull(extractState(manager).getActiveBannerRecipe());
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
