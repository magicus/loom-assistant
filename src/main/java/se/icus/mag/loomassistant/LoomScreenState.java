/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.loomassistant.recipe.BannerRecipe;

public class LoomScreenState {
    private boolean panelOpen;
    private transient BannerRecipe activeBanner;
    private transient String selectedBannerId;
    private transient BannerRecipe activeBannerSource;
    private String activeBannerRecipe;
    private String selectedCategory;
    private Map<DyeColor, DyeColor> colorReplacements = new HashMap<>();
    private boolean colorReplacementEnabled;

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

    public String getActiveBannerRecipe() {
        return activeBannerRecipe;
    }

    public void setActiveBannerRecipe(String activeBannerRecipe) {
        this.activeBannerRecipe = activeBannerRecipe;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(String selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    public boolean isColorReplacementEnabled() {
        return colorReplacementEnabled;
    }

    public void setColorReplacementEnabled(boolean colorReplacementEnabled) {
        this.colorReplacementEnabled = colorReplacementEnabled;
    }

    public Map<DyeColor, DyeColor> getColorReplacements() {
        if (colorReplacements == null) {
            colorReplacements = new HashMap<>();
        }
        return colorReplacements;
    }

    public void setColorReplacements(Map<DyeColor, DyeColor> colorReplacements) {
        this.colorReplacements =
                colorReplacements != null ? new HashMap<>(colorReplacements) : new HashMap<>();
    }
}
