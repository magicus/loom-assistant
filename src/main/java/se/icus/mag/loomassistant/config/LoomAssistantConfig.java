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

@Config(name = LoomAssistantMod.MOD_ID)
public class LoomAssistantConfig implements ConfigData {
    public enum ColorSortOrder {
        RAINBOW,
        VANILLA
    }

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    ColorSortOrder colorSortOrder = ColorSortOrder.RAINBOW;

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public BannerPackRepoSettings bannerPackRepo = new BannerPackRepoSettings();

    public ColorSortOrder getColorSortOrder() {
        return colorSortOrder;
    }

    public BannerPackRepoSettings getBannerPackRepo() {
        return bannerPackRepo;
    }

    public static class BannerPackRepoSettings {
        public String repoIndexUrl =
                "https://raw.githubusercontent.com/magicus/banner-recipe-database/refs/heads/main/bannerpack-index-v1.json";

        @ConfigEntry.Gui.Tooltip(count = 1)
        public String autoInstallPackIds = "categories,numbers";

        @ConfigButtons({
            @ConfigButtons.ButtonAction(
                    screenClass = "se.icus.mag.loomassistant.gui.screens.packselection.BannerPackSelectionScreen",
                    buttonLabelKey = "loom-assistant.screen.import_export.select_packs"),
            @ConfigButtons.ButtonAction(
                    screenClass =
                            "se.icus.mag.loomassistant.gui.screens.packdownload.BannerPackDownloadManagementScreen",
                    buttonLabelKey = "loom-assistant.screen.import_export.download_packs")
        })
        @ConfigEntry.Gui.Excluded
        public boolean activateAfterDownload = true;

        public List<String> getAutoInstallPackIdList() {
            return Arrays.stream(autoInstallPackIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }
}
