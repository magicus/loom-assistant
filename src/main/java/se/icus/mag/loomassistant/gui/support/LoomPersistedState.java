/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.support;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LoomPersistedState {
    private Map<String, WorldState> worlds = new LinkedHashMap<>();

    public Map<String, WorldState> getWorlds() {
        if (worlds == null) {
            worlds = new LinkedHashMap<>();
        }
        return worlds;
    }

    public WorldState getWorld(String worldKey) {
        return getWorlds().get(worldKey);
    }

    public WorldState getOrCreateWorld(String worldKey) {
        return getWorlds().computeIfAbsent(worldKey, ignored -> new WorldState());
    }

    public static final class WorldState {
        private boolean panelOpen;
        private String activeBannerJson;
        private String selectedCategoryId;
        private boolean persistentDyeSwitchEnabled;
        private Map<String, String> persistentDyeReplacements = new LinkedHashMap<>();

        public boolean isPanelOpen() {
            return panelOpen;
        }

        public void setPanelOpen(boolean panelOpen) {
            this.panelOpen = panelOpen;
        }

        public String getActiveBannerJson() {
            return activeBannerJson;
        }

        public void setActiveBannerJson(String activeBannerJson) {
            this.activeBannerJson = activeBannerJson;
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

        public Map<String, String> getPersistentDyeReplacements() {
            if (persistentDyeReplacements == null) {
                persistentDyeReplacements = new LinkedHashMap<>();
            }
            return persistentDyeReplacements;
        }

        public void setPersistentDyeReplacements(Map<String, String> persistentDyeReplacements) {
            this.persistentDyeReplacements = persistentDyeReplacements != null
                    ? new LinkedHashMap<>(persistentDyeReplacements)
                    : new LinkedHashMap<>();
        }

        public boolean isEmpty() {
            return !panelOpen
                    && (activeBannerJson == null || activeBannerJson.isBlank())
                    && (selectedCategoryId == null || selectedCategoryId.isBlank())
                    && !persistentDyeSwitchEnabled
                    && getPersistentDyeReplacements().isEmpty();
        }
    }
}
