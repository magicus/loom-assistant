/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.extensions;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;
import se.icus.mag.loomassistant.gui.support.LoomUiStateStore;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeCategories;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;
import se.icus.mag.loomassistant.weaving.Weaver;

public class LoomScreenState {
    private static final String MODIFIED_SUFFIX_KEY = "loom-assistant.banner.modified_suffix";

    private final Minecraft minecraft;
    private final LoomMenu handler;
    private final Weaver weaver;

    private boolean panelOpen;
    private BannerRecipe activeBanner;
    private String selectedBannerId;
    private BannerRecipe activeBannerSource;
    private String activeBannerSourceId;
    private final EnumMap<DyeColor, DyeColor> persistentDyeReplacementMap = new EnumMap<>(DyeColor.class);
    private boolean persistentDyeSwitchEnabled;

    public LoomScreenState(Minecraft minecraft, LoomMenu handler) {
        this.minecraft = minecraft;
        this.handler = handler;
        this.weaver = Weaver.getWeaver(handler);
        loadPersistedState();
    }

    public boolean isPanelOpen() {
        return panelOpen;
    }

    public void setPanelOpen(boolean panelOpen) {
        this.panelOpen = panelOpen;
        LoomUiStateStore.setLoomPanelOpen(minecraft, panelOpen);
    }

    public boolean togglePanelOpen() {
        setPanelOpen(!panelOpen);
        return panelOpen;
    }

    public void tick() {
        weaver.tick();
    }

    public boolean isWeavingActive() {
        return weaver.isActive();
    }

    public boolean hasActiveBanner() {
        return activeBanner != null;
    }

    public BannerRecipe getActiveBannerRecipe() {
        return activeBanner;
    }

    public ItemStack getActiveBannerStack() {
        return activeBanner == null ? ItemStack.EMPTY : BannerRecipe.toItem(minecraft, activeBanner);
    }

    public String getActiveBannerDisplayName() {
        return effectiveActiveName();
    }

    public boolean setActiveBannerFromItemStack(ItemStack stack) {
        BannerRecipe banner = BannerRecipe.fromItem(stack);
        if (banner == null) return false;

        setActiveBannerFromSource(banner, null, true);
        return true;
    }

    public void setActiveBannerFromRecipe(BannerRecipe banner, String sourceId) {
        setActiveBannerFromSource(banner, sourceId, true);
    }

    public void loadImportedBanner(BannerRecipe imported) {
        setActiveBannerFromSource(imported, null, true);
    }

    public void clearActiveBanner() {
        activeBanner = null;
        selectedBannerId = null;
        activeBannerSource = null;
        activeBannerSourceId = null;
        persistActiveBanner();
    }

    public int getActiveBannerLayerCount() {
        return activeBanner != null ? activeBanner.getLayers().size() : 0;
    }

    public int detectCraftingProgress() {
        BannerRecipe recipe = activeBanner;
        if (recipe == null) return -1;

        ItemStack bannerInSlot = handler.getBannerSlot().getItem();
        if (bannerInSlot.isEmpty()) return -1;
        if (!(bannerInSlot.getItem() instanceof BannerItem bannerItem)) return -1;
        if (bannerItem.getColor() != recipe.getBannerColorEnum()) return -1;

        BannerPatternLayers patterns = bannerInSlot.get(DataComponents.BANNER_PATTERNS);
        List<BannerPatternLayers.Layer> currentLayers = patterns != null ? patterns.layers() : List.of();
        List<BannerRecipeLayer> recipeLayers = recipe.getLayers();

        int craftedLayerCount = currentLayers.size();
        if (craftedLayerCount >= recipeLayers.size()) return -1;

        for (int i = 0; i < craftedLayerCount; i++) {
            BannerPatternLayers.Layer current = currentLayers.get(i);
            BannerRecipeLayer expected = recipeLayers.get(i);
            if (current.color() != expected.getDyeColorEnum()) return -1;

            String currentPatternId = current.pattern()
                    .unwrapKey()
                    .map(key -> key.identifier().toString())
                    .orElse(null);
            if (currentPatternId == null) return -1;

            String expectedPatternId =
                    expected.patternId().contains(":") ? expected.patternId() : "minecraft:" + expected.patternId();
            if (!currentPatternId.equals(expectedPatternId)) return -1;
        }
        return craftedLayerCount;
    }

    public void craftActiveBanner() {
        if (activeBanner == null) return;

        BannerRecipe toWeave = activeBanner.withDescription(effectiveActiveName());
        weaver.weave(toWeave);
    }

    public boolean isActiveBannerCraftable() {
        return activeBanner != null && weaver.canWeave(activeBanner);
    }

    public String getActiveBannerMissingMaterialMessage() {
        if (activeBanner == null) {
            return Component.translatable("loom-assistant.active.select_banner").getString();
        }

        boolean survivalTooManySteps = !activeBanner.isWeavable() && isInSurvivalMode();
        List<String> missingMaterials = weaver.getMissingMaterialDescriptions(activeBanner);
        if (missingMaterials.isEmpty() && !survivalTooManySteps) return null;

        StringBuilder message = new StringBuilder();
        if (survivalTooManySteps) {
            message.append(Component.translatable("loom-assistant.active.too_many_steps")
                    .getString());
        }
        if (!missingMaterials.isEmpty()) {
            if (!message.isEmpty()) {
                message.append("\n");
            }
            message.append(Component.translatable("loom-assistant.active.missing_header")
                            .getString())
                    .append("\n")
                    .append(String.join("\n", missingMaterials));
        }
        return message.toString();
    }

    public boolean isActiveBannerWeavable() {
        return activeBanner == null || activeBanner.isWeavable();
    }

    public boolean isPersistentDyeSwitchEnabled() {
        return persistentDyeSwitchEnabled;
    }

    public Map<DyeColor, DyeColor> getPersistentDyeReplacementMapCopy() {
        return Map.copyOf(persistentDyeReplacementMap);
    }

    public List<DyeColor> getActiveBannerUsedColors() {
        if (activeBanner == null) return List.of();

        Set<DyeColor> colors = new LinkedHashSet<>();
        colors.add(activeBanner.getBaseColorEnum());
        for (BannerRecipeLayer layer : activeBanner.getLayers()) {
            colors.add(layer.getDyeColorEnum());
        }
        return List.copyOf(colors);
    }

    public Map<DyeColor, DyeColor> getInitialDyeReplacementTargets(List<DyeColor> sourceColors) {
        Map<DyeColor, DyeColor> targets = new LinkedHashMap<>();
        for (DyeColor source : sourceColors) {
            targets.put(source, persistentDyeReplacementMap.getOrDefault(source, source));
        }
        return targets;
    }

    public boolean applyDyeSwitch(Map<DyeColor, DyeColor> replacements, boolean persistent) {
        if (activeBanner == null) return false;

        BannerRecipe sourceForTransform;
        if (persistent) {
            sourceForTransform = cloneBanner(activeBannerSource != null ? activeBannerSource : activeBanner);
            activeBannerSource = cloneBanner(sourceForTransform);
            activeBannerSourceId = selectedBannerId;
        } else {
            sourceForTransform = cloneBanner(activeBanner);
            persistentDyeSwitchEnabled = false;
            persistentDyeReplacementMap.clear();
        }

        Map<DyeColor, DyeColor> normalized = normalizeReplacementMap(replacements);
        if (persistent) {
            persistentDyeReplacementMap.clear();
            persistentDyeReplacementMap.putAll(normalized);
            persistentDyeSwitchEnabled = !persistentDyeReplacementMap.isEmpty();
        }

        if (normalized.isEmpty()) return false;

        BannerRecipe transformed = applyDyeReplacementMap(sourceForTransform, normalized);
        if (transformed == null || bannersEquivalent(sourceForTransform, transformed)) return false;

        activeBanner = transformed;
        selectedBannerId = null;
        persistPersistentDyeState();
        persistActiveBanner();
        return true;
    }

    public void disablePersistentDyeSwitchAndReload() {
        if (!persistentDyeSwitchEnabled) return;

        persistentDyeSwitchEnabled = false;
        persistentDyeReplacementMap.clear();

        if (activeBannerSource != null) {
            activeBanner = cloneBanner(activeBannerSource);
            selectedBannerId = activeBannerSourceId;
        }

        persistPersistentDyeState();
        persistActiveBanner();
    }

    public boolean isActiveBannerSavable() {
        return !isActiveBannerFromWritableSource();
    }

    public boolean isActiveBannerAlreadySaved() {
        return isActiveBannerFromWritableSource();
    }

    public boolean isActiveBannerFromReadOnlySource() {
        return activeBannerSourceId != null && BannerStorage.getInstance().isRecipeReadOnly(activeBannerSourceId);
    }

    public String getActiveBannerDialogName(boolean editMode) {
        if (activeBanner == null) return BannerRecipe.getUnnamedBanner();

        if (editMode && isActiveBannerFromWritableSource()) {
            BannerRecipe source = BannerStorage.getInstance().getBannerById(activeBannerSourceId);
            if (source != null) {
                String existingName = source.getName();
                if (existingName == null || existingName.isBlank()) return BannerRecipe.getUnnamedBanner();
                return existingName;
            }
        }

        return effectiveActiveName();
    }

    public String getActiveBannerDialogCategory(boolean editMode) {
        if (activeBanner == null) return defaultSaveCategory();

        if (editMode && isActiveBannerFromWritableSource()) {
            BannerRecipe source = BannerStorage.getInstance().getBannerById(activeBannerSourceId);
            if (source != null) {
                return source.getCategory();
            }
        }

        return defaultSaveCategory();
    }

    public void applyActiveBannerMetadata(String nameInput, String categoryInput) {
        if (activeBanner == null) return;

        String name = nameInput == null || nameInput.isBlank() ? BannerRecipe.getUnnamedBanner() : nameInput.trim();
        String category =
                (categoryInput == null || categoryInput.isBlank()) ? BannerRecipe.DEFAULT_CATEGORY : categoryInput;

        if (isActiveBannerFromWritableSource()) {
            BannerStorage.getInstance().updateBannerMetadata(activeBannerSourceId, name, category);
            BannerRecipe updated = BannerStorage.getInstance().getBannerById(activeBannerSourceId);
            if (updated != null) {
                setActiveBannerFromSource(updated, updated.getId(), true);
            }
            return;
        }

        BannerRecipe toSave = cloneBanner(activeBanner).withDescription(name).withCategory(category);
        BannerRecipe created = BannerStorage.getInstance().addBanner(toSave);
        if (created != null) {
            setActiveBannerFromSource(created, created.getId(), true);
        }
    }

    private void loadPersistedState() {
        this.panelOpen = LoomUiStateStore.isLoomPanelOpen(minecraft);

        LoomUiStateStore.PersistentDyeState persistentDyeState = LoomUiStateStore.getPersistentDyeState(minecraft);
        this.persistentDyeSwitchEnabled = persistentDyeState.enabled();
        this.persistentDyeReplacementMap.clear();
        this.persistentDyeReplacementMap.putAll(persistentDyeState.replacements());

        ItemStack persistedBanner = LoomUiStateStore.getPersistedActiveBannerStack(minecraft);
        if (!persistedBanner.isEmpty()) {
            BannerRecipe recipe = BannerRecipe.fromItem(persistedBanner);
            if (recipe != null) {
                setActiveBannerFromSource(recipe, null, false);
            }
        }
    }

    private void setActiveBannerFromSource(BannerRecipe sourceBanner, String sourceId, boolean persist) {
        if (sourceBanner == null) {
            activeBanner = null;
            selectedBannerId = null;
            activeBannerSource = null;
            activeBannerSourceId = null;
            if (persist) {
                persistActiveBanner();
            }
            return;
        }

        activeBannerSource = cloneBanner(sourceBanner);
        activeBannerSourceId = sourceId;
        selectedBannerId = sourceId;

        if (persistentDyeSwitchEnabled && !persistentDyeReplacementMap.isEmpty()) {
            BannerRecipe transformed = applyDyeReplacementMap(activeBannerSource, persistentDyeReplacementMap);
            if (transformed != null && !bannersEquivalent(activeBannerSource, transformed)) {
                activeBanner = transformed;
                selectedBannerId = null;
                if (persist) {
                    persistActiveBanner();
                }
                return;
            }
        }

        activeBanner = cloneBanner(sourceBanner);
        if (persist) {
            persistActiveBanner();
        }
    }

    private void persistActiveBanner() {
        if (activeBanner == null) {
            LoomUiStateStore.setPersistedActiveBannerStack(minecraft, ItemStack.EMPTY);
            return;
        }

        BannerRecipe recipeToPersist =
                persistentDyeSwitchEnabled && activeBannerSource != null ? activeBannerSource : activeBanner;
        LoomUiStateStore.setPersistedActiveBannerStack(minecraft, BannerRecipe.toItem(minecraft, recipeToPersist));
    }

    private void persistPersistentDyeState() {
        LoomUiStateStore.setPersistentDyeState(minecraft, persistentDyeSwitchEnabled, persistentDyeReplacementMap);
    }

    private boolean isActiveBannerFromWritableSource() {
        return activeBannerSourceId != null && !BannerStorage.getInstance().isRecipeReadOnly(activeBannerSourceId);
    }

    private String effectiveActiveName() {
        if (activeBanner == null) return BannerRecipe.getUnnamedBanner();

        String base = activeBanner.getName();
        if (base == null || base.isBlank() || base.equals(BannerRecipe.DEFAULT_DESCRIPTION)) {
            return BannerRecipe.getUnnamedBanner();
        }

        if (persistentDyeSwitchEnabled) {
            return base + Component.translatable(MODIFIED_SUFFIX_KEY).getString();
        }
        return base;
    }

    private String defaultSaveCategory() {
        String selectedCategoryId = LoomUiStateStore.getSelectedCategoryId(minecraft);
        return selectedCategoryId != null ? selectedCategoryId : BannerRecipeCategories.MISC.id();
    }

    private static boolean isInSurvivalMode() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.player != null && !mc.player.hasInfiniteMaterials();
    }

    private static BannerRecipe cloneBanner(BannerRecipe source) {
        return new BannerRecipe(source.getName(), source.getBaseColorEnum(), new ArrayList<>(source.getLayers()))
                .withCategory(source.getCategory());
    }

    private static Map<DyeColor, DyeColor> normalizeReplacementMap(Map<DyeColor, DyeColor> replacements) {
        EnumMap<DyeColor, DyeColor> normalized = new EnumMap<>(DyeColor.class);
        if (replacements == null) return normalized;

        for (Map.Entry<DyeColor, DyeColor> entry : replacements.entrySet()) {
            DyeColor src = entry.getKey();
            DyeColor dst = entry.getValue();
            if (src != null && dst != null && src != dst) {
                normalized.put(src, dst);
            }
        }
        return normalized;
    }

    private static BannerRecipe applyDyeReplacementMap(BannerRecipe source, Map<DyeColor, DyeColor> replacements) {
        if (source == null) return null;

        BannerRecipe copy = cloneBanner(source);
        DyeColor newBase = replacements.getOrDefault(copy.getBaseColorEnum(), copy.getBaseColorEnum());
        copy = copy.withBannerColor(newBase.getName());

        List<BannerRecipeLayer> replacedLayers = new ArrayList<>();
        for (BannerRecipeLayer layer : copy.getLayers()) {
            DyeColor current = layer.getDyeColorEnum();
            DyeColor target = replacements.getOrDefault(current, current);
            replacedLayers.add(BannerRecipeLayer.of(layer.patternId(), target.getName()));
        }
        return copy.withLayers(replacedLayers);
    }

    private static boolean bannersEquivalent(BannerRecipe a, BannerRecipe b) {
        if (a.getBaseColorEnum() != b.getBaseColorEnum()) {
            return false;
        }
        List<BannerRecipeLayer> leftLayers = a.getLayers();
        List<BannerRecipeLayer> rightLayers = b.getLayers();
        if (leftLayers.size() != rightLayers.size()) {
            return false;
        }
        for (int i = 0; i < leftLayers.size(); i++) {
            if (!leftLayers.get(i).equals(rightLayers.get(i))) {
                return false;
            }
        }
        return true;
    }
}
