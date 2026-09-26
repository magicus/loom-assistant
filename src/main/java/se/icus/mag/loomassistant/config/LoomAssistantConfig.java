/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.config;

import java.util.Arrays;
import java.util.List;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.config.clothconfig.ConfigButtons;
import se.icus.mag.loomassistant.gui.screens.packdownload.BannerPackDownloadManagementScreen;
import se.icus.mag.loomassistant.gui.screens.packselection.BannerPackSelectionScreen;

@Config(name = LoomAssistantMod.MOD_ID)
public class LoomAssistantConfig implements ConfigData {
    public enum ColorSortOrder {
        RAINBOW,
        VANILLA
    }

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public final ColorSortOrder colorSortOrder = ColorSortOrder.RAINBOW;

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public final BannerPackRepoSettings bannerPackRepo = new BannerPackRepoSettings();

    public static class BannerPackRepoSettings {
        public String repoIndexUrl =
                "https://raw.githubusercontent.com/magicus/banner-recipe-database/refs/heads/main/bannerpack-index-v1.json";

        @ConfigEntry.Gui.Tooltip()
        private String autoInstallPackIds = "categories,numbers";

        public List<String> getAutoInstallPackIdList() {
            return Arrays.stream(autoInstallPackIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        @ConfigButtons({
            @ConfigButtons.ButtonAction(
                    screenClass = BannerPackSelectionScreen.class,
                    buttonLabelKey = "loom-assistant.screen.import_export.select_packs"),
            @ConfigButtons.ButtonAction(
                    screenClass = BannerPackDownloadManagementScreen.class,
                    buttonLabelKey = "loom-assistant.screen.import_export.download_packs")
        })
        @ConfigEntry.Gui.Excluded
        public boolean activateAfterDownload = true;
    }
}
