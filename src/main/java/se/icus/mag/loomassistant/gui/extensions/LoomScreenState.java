/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.extensions;

import java.util.EnumMap;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.loomassistant.recipe.BannerRecipe;

public class LoomScreenState {
    private boolean panelOpen;
    private BannerRecipe activeBanner;
    private String selectedBannerId;
    private BannerRecipe activeBannerSource;
    private String activeBannerSourceId;
    private String selectedCategoryId;
    private final EnumMap<DyeColor, DyeColor> persistentDyeReplacementMap = new EnumMap<>(DyeColor.class);
    private boolean persistentDyeSwitchEnabled;

    public boolean isPanelOpen() {
        return panelOpen;
    }

    public void setPanelOpen(boolean panelOpen) {
        this.panelOpen = panelOpen;
    }

    public BannerRecipe getActiveBanner() {
        return activeBanner;
    }

    public void setActiveBanner(BannerRecipe activeBanner) {
        this.activeBanner = activeBanner;
    }

    public String getSelectedBannerId() {
        return selectedBannerId;
    }

    public void setSelectedBannerId(String selectedBannerId) {
        this.selectedBannerId = selectedBannerId;
    }

    public BannerRecipe getActiveBannerSource() {
        return activeBannerSource;
    }

    public void setActiveBannerSource(BannerRecipe activeBannerSource) {
        this.activeBannerSource = activeBannerSource;
    }

    public String getActiveBannerSourceId() {
        return activeBannerSourceId;
    }

    public void setActiveBannerSourceId(String activeBannerSourceId) {
        this.activeBannerSourceId = activeBannerSourceId;
    }

    public String getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(String selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }

    public boolean isPersistentDyeSwitchEnabled() {
        return persistentDyeSwitchEnabled;
    }

    public void setPersistentDyeSwitchEnabled(boolean persistentDyeSwitchEnabled) {
        this.persistentDyeSwitchEnabled = persistentDyeSwitchEnabled;
    }

    public EnumMap<DyeColor, DyeColor> getPersistentDyeReplacementMap() {
        return persistentDyeReplacementMap;
    }
}
