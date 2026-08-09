/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BannerDetailsScreenTest {
    @Test
    void panelHeightReflectsReadOnlyEditMode() {
        assertEquals(150, BannerDetailsScreen.panelHeightForMode(BannerDetailsScreen.Mode.EDIT_READONLY));
        assertEquals(130, BannerDetailsScreen.panelHeightForMode(BannerDetailsScreen.Mode.EDIT));
        assertEquals(130, BannerDetailsScreen.panelHeightForMode(BannerDetailsScreen.Mode.SAVE));
        assertEquals(130, BannerDetailsScreen.panelHeightForMode(BannerDetailsScreen.Mode.IMPORT));
    }

    @Test
    void modeMapsToCorrectTitleTranslationKey() {
        assertEquals(
                "loom-assistant.screen.save_edit.title_edit",
                BannerDetailsScreen.titleTranslationKeyForMode(BannerDetailsScreen.Mode.EDIT));
        assertEquals(
                "loom-assistant.screen.save_edit.title_edit",
                BannerDetailsScreen.titleTranslationKeyForMode(BannerDetailsScreen.Mode.EDIT_READONLY));
        assertEquals(
                "loom-assistant.screen.save_edit.title_save",
                BannerDetailsScreen.titleTranslationKeyForMode(BannerDetailsScreen.Mode.SAVE));
        assertEquals(
                "loom-assistant.screen.save_edit.title_save",
                BannerDetailsScreen.titleTranslationKeyForMode(BannerDetailsScreen.Mode.IMPORT));
    }

    @Test
    void modeMapsToCorrectConfirmTranslationKey() {
        assertEquals(
                "loom-assistant.tooltip.edit_recipe",
                BannerDetailsScreen.confirmTranslationKeyForMode(BannerDetailsScreen.Mode.EDIT));
        assertEquals(
                "loom-assistant.tooltip.edit_recipe",
                BannerDetailsScreen.confirmTranslationKeyForMode(BannerDetailsScreen.Mode.EDIT_READONLY));
        assertEquals(
                "loom-assistant.tooltip.add_recipe",
                BannerDetailsScreen.confirmTranslationKeyForMode(BannerDetailsScreen.Mode.SAVE));
        assertEquals(
                "loom-assistant.tooltip.add_recipe",
                BannerDetailsScreen.confirmTranslationKeyForMode(BannerDetailsScreen.Mode.IMPORT));
    }
}
