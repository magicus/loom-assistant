/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.storage.LevelResource;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;
import se.icus.mag.loomassistant.gui.LoomScreenExtension;
import se.icus.mag.loomassistant.gui.ScreenExtension;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeCategories;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;
import se.icus.mag.loomassistant.recipe.converters.BannerRecipeItemConverter;
import se.icus.mag.loomassistant.weaving.Weaver;

public class LoomScreenStateManager {
    private static final String MODIFIED_SUFFIX_KEY = "loom-assistant.banner.modified_suffix";
    private static final String LOCAL_WORLD_KEY_PREFIX = "local:";
    private static final String MULTIPLAYER_WORLD_KEY_PREFIX = "mp:";
    private static final Gson GSON = createPersistenceGson();
    private static final Type PERSISTED_STATE_MAP_TYPE =
            new TypeToken<LinkedHashMap<String, LoomScreenState>>() {}.getType();
    private static final Path PERSISTENCE_DIR =
            FabricLoader.getInstance().getConfigDir().resolve("loom-assistant");
    private static final Path PERSISTENCE_FILE_PATH = PERSISTENCE_DIR.resolve("states.json");

    private final LoomScreenState state;
    private final Map<String, LoomScreenState> persistedStates;

    private LoomScreenExtension loomExtension;

    private LoomMenu currentMenu;
    private Weaver currentWeaver;
    private String loadedWorldKey;

    public LoomScreenStateManager() {
        this.state = new LoomScreenState();
        this.persistedStates = loadPersistedStates();
    }

    public ScreenExtension getExtension() {
        return loomExtension;
    }

    public void createExtension(LoomScreen s) {
        this.loomExtension = new LoomScreenExtension(s);
    }

    public void removeExtension() {
        this.loomExtension = null;
    }

    // ── Screen lifecycle ──────────────────────────────────────────────────────

    public void onLoomScreenOpened(LoomMenu menu) {
        this.currentMenu = menu;
        this.currentWeaver = Weaver.getWeaver(menu);
        String worldKey = currentWorldKey();
        if (!worldKey.equals(loadedWorldKey)) {
            loadedWorldKey = worldKey;
            loadPersistedState(worldKey);
        }
    }

    public void onLoomScreenClosed() {
        this.currentMenu = null;
        this.currentWeaver = null;
    }

    // ── Tick / weaving status ─────────────────────────────────────────────────

    public void tick() {
        if (currentWeaver != null) {
            currentWeaver.tick();
        }
    }

    public boolean isWeavingActive() {
        return currentWeaver != null && currentWeaver.isActive();
    }

    // ── Panel open ────────────────────────────────────────────────────────────

    public boolean isPanelOpen() {
        return state.isPanelOpen();
    }

    public boolean togglePanelOpen() {
        state.setPanelOpen(!state.isPanelOpen());
        persistCurrentWorldState();
        return state.isPanelOpen();
    }

    // ── Active banner reads ───────────────────────────────────────────────────

    public boolean hasActiveBanner() {
        return state.getActiveBanner() != null;
    }

    public BannerRecipe getActiveBannerRecipe() {
        return state.getActiveBanner();
    }

    public ItemStack getActiveBannerStack() {
        BannerRecipe banner = state.getActiveBanner();
        if (banner == null) return ItemStack.EMPTY;

        BannerRecipeItemConverter converter = new BannerRecipeItemConverter();
        return converter.fromRecipe(banner);
    }

    public String getActiveBannerDisplayName() {
        return effectiveActiveName();
    }

    public int getActiveBannerLayerCount() {
        BannerRecipe banner = state.getActiveBanner();
        return banner != null ? banner.getLayers().size() : 0;
    }

    public List<DyeColor> getActiveBannerUsedColors() {
        BannerRecipe banner = state.getActiveBanner();
        if (banner == null) return List.of();

        Set<DyeColor> colors = new LinkedHashSet<>();
        colors.add(banner.getBaseColorEnum());
        for (BannerRecipeLayer layer : banner.getLayers()) {
            colors.add(layer.getDyeColorEnum());
        }
        return List.copyOf(colors);
    }

    // ── Active banner mutations ───────────────────────────────────────────────

    public boolean setActiveBannerFromItemStack(ItemStack stack) {
        BannerRecipeItemConverter converter = new BannerRecipeItemConverter();
        BannerRecipe banner = converter.toRecipe(stack);
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
        state.setActiveBanner(null);
        state.setSelectedBannerId(null);
        state.setActiveBannerSource(null);
        state.setActiveBannerRecipe(null);
        persistCurrentWorldState();
    }

    // ── Crafting ──────────────────────────────────────────────────────────────

    public int detectCraftingProgress() {
        if (currentMenu == null) return -1;
        BannerRecipe recipe = state.getActiveBanner();
        if (recipe == null) return -1;

        ItemStack bannerInSlot = currentMenu.getBannerSlot().getItem();
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
        if (currentWeaver == null || state.getActiveBanner() == null) return;
        BannerRecipe toWeave = state.getActiveBanner().withDescription(effectiveActiveName());
        currentWeaver.weave(toWeave);
    }

    public boolean isActiveBannerCraftable() {
        return currentWeaver != null
                && state.getActiveBanner() != null
                && currentWeaver.canWeave(state.getActiveBanner());
    }

    public boolean isActiveBannerWeavable() {
        BannerRecipe banner = state.getActiveBanner();
        return banner == null || banner.isWeavable();
    }

    public String getActiveBannerMissingMaterialMessage() {
        BannerRecipe banner = state.getActiveBanner();
        if (banner == null) {
            return Component.translatable("loom-assistant.active.select_banner").getString();
        }

        boolean survivalTooManySteps = !banner.isWeavable() && isInSurvivalMode();
        List<String> missingMaterials =
                currentWeaver != null ? currentWeaver.getMissingMaterialDescriptions(banner) : List.of();
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

    // ── Dye switch ────────────────────────────────────────────────────────────

    public boolean isPersistentDyeSwitchEnabled() {
        return state.isColorReplacementEnabled();
    }

    public Map<DyeColor, DyeColor> getPersistentDyeReplacementMapCopy() {
        return Map.copyOf(state.getColorReplacements());
    }

    public Map<DyeColor, DyeColor> getInitialDyeReplacementTargets(List<DyeColor> sourceColors) {
        Map<DyeColor, DyeColor> targets = new LinkedHashMap<>();
        for (DyeColor source : sourceColors) {
            targets.put(source, state.getColorReplacements().getOrDefault(source, source));
        }
        return targets;
    }

    public boolean applyDyeSwitch(Map<DyeColor, DyeColor> replacements, boolean persistent) {
        BannerRecipe activeBanner = state.getActiveBanner();
        if (activeBanner == null) return false;

        BannerRecipe sourceForTransform;
        if (persistent) {
            BannerRecipe source = state.getActiveBannerSource() != null ? state.getActiveBannerSource() : activeBanner;
            sourceForTransform = cloneBanner(source);
            state.setActiveBannerSource(cloneBanner(sourceForTransform));
            state.setActiveBannerRecipe(state.getSelectedBannerId());
        } else {
            sourceForTransform = cloneBanner(activeBanner);
            state.setColorReplacementEnabled(false);
            state.getColorReplacements().clear();
        }

        Map<DyeColor, DyeColor> normalized = normalizeReplacementMap(replacements);
        if (persistent) {
            state.getColorReplacements().clear();
            state.getColorReplacements().putAll(normalized);
            state.setColorReplacementEnabled(
                    !state.getColorReplacements().isEmpty());
        }

        if (normalized.isEmpty()) return false;

        BannerRecipe transformed = applyDyeReplacementMap(sourceForTransform, normalized);
        if (transformed == null || bannersEquivalent(sourceForTransform, transformed)) return false;

        state.setActiveBanner(transformed);
        state.setSelectedBannerId(null);
        persistCurrentWorldState();
        return true;
    }

    public void disablePersistentDyeSwitchAndReload() {
        if (!state.isColorReplacementEnabled()) return;

        state.setColorReplacementEnabled(false);
        state.getColorReplacements().clear();

        if (state.getActiveBannerSource() != null) {
            state.setActiveBanner(cloneBanner(state.getActiveBannerSource()));
            state.setSelectedBannerId(state.getActiveBannerRecipe());
        }

        persistCurrentWorldState();
    }

    // ── Banner storage / metadata ─────────────────────────────────────────────

    public boolean isActiveBannerSavable() {
        return !isActiveBannerFromWritableSource();
    }

    public boolean isActiveBannerAlreadySaved() {
        return isActiveBannerFromWritableSource();
    }

    public boolean isActiveBannerFromReadOnlySource() {
        String sourceId = state.getActiveBannerRecipe();
        return sourceId != null && BannerStorage.getInstance().isRecipeReadOnly(sourceId);
    }

    public String getActiveBannerDialogName(boolean editMode) {
        BannerRecipe banner = state.getActiveBanner();
        if (banner == null) return BannerRecipe.getUnnamedBanner();

        if (editMode && isActiveBannerFromWritableSource()) {
            BannerRecipe source = BannerStorage.getInstance().getBannerById(state.getActiveBannerRecipe());
            if (source != null) {
                String existingName = source.getName();
                if (existingName == null || existingName.isBlank()) return BannerRecipe.getUnnamedBanner();
                return existingName;
            }
        }

        return effectiveActiveName();
    }

    public String getActiveBannerDialogCategory(boolean editMode) {
        if (state.getActiveBanner() == null) return defaultSaveCategory();

        if (editMode && isActiveBannerFromWritableSource()) {
            BannerRecipe source = BannerStorage.getInstance().getBannerById(state.getActiveBannerRecipe());
            if (source != null) {
                return source.getCategory();
            }
        }

        return defaultSaveCategory();
    }

    public void applyActiveBannerMetadata(String nameInput, String categoryInput) {
        if (state.getActiveBanner() == null) return;

        String name = nameInput == null || nameInput.isBlank() ? BannerRecipe.getUnnamedBanner() : nameInput.trim();
        String category =
                (categoryInput == null || categoryInput.isBlank()) ? BannerRecipe.DEFAULT_CATEGORY : categoryInput;

        if (isActiveBannerFromWritableSource()) {
            BannerStorage.getInstance().updateBannerMetadata(state.getActiveBannerRecipe(), name, category);
            BannerRecipe updated = BannerStorage.getInstance().getBannerById(state.getActiveBannerRecipe());
            if (updated != null) {
                setActiveBannerFromSource(updated, updated.getId(), true);
            }
            return;
        }

        BannerRecipe toSave =
                cloneBanner(state.getActiveBanner()).withDescription(name).withCategory(category);
        BannerRecipe created = BannerStorage.getInstance().addBanner(toSave);
        if (created != null) {
            setActiveBannerFromSource(created, created.getId(), true);
        }
    }

    // ── Selected category ─────────────────────────────────────────────────────

    public String getSelectedCategoryId() {
        return state.getSelectedCategory();
    }

    public void setSelectedCategoryId(String categoryId) {
        if ((state.getSelectedCategory() == null && categoryId == null)
                || (state.getSelectedCategory() != null
                        && state.getSelectedCategory().equals(categoryId))) {
            return;
        }
        state.setSelectedCategory(categoryId);
        persistCurrentWorldState();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void setActiveBannerFromSource(BannerRecipe sourceBanner, String sourceId, boolean persist) {
        if (sourceBanner == null) {
            state.setActiveBanner(null);
            state.setSelectedBannerId(null);
            state.setActiveBannerSource(null);
            state.setActiveBannerRecipe(null);
            if (persist) persistCurrentWorldState();
            return;
        }

        state.setActiveBannerSource(cloneBanner(sourceBanner));
        state.setActiveBannerRecipe(sourceId);
        state.setSelectedBannerId(sourceId);

        if (state.isColorReplacementEnabled()
                && !state.getColorReplacements().isEmpty()) {
            BannerRecipe transformed =
                    applyDyeReplacementMap(state.getActiveBannerSource(), state.getColorReplacements());
            if (transformed != null && !bannersEquivalent(state.getActiveBannerSource(), transformed)) {
                state.setActiveBanner(transformed);
                state.setSelectedBannerId(null);
                if (persist) persistCurrentWorldState();
                return;
            }
        }

        state.setActiveBanner(cloneBanner(sourceBanner));
        if (persist) persistCurrentWorldState();
    }

    private boolean isActiveBannerFromWritableSource() {
        String sourceId = state.getActiveBannerRecipe();
        return sourceId != null && !BannerStorage.getInstance().isRecipeReadOnly(sourceId);
    }

    private String effectiveActiveName() {
        BannerRecipe banner = state.getActiveBanner();
        if (banner == null) return BannerRecipe.getUnnamedBanner();

        String base = banner.getName();
        if (base == null || base.isBlank() || base.equals(BannerRecipe.DEFAULT_DESCRIPTION)) {
            return BannerRecipe.getUnnamedBanner();
        }

        if (state.isColorReplacementEnabled()) {
            return base + Component.translatable(MODIFIED_SUFFIX_KEY).getString();
        }
        return base;
    }

    private String defaultSaveCategory() {
        String cat = state.getSelectedCategory();
        return cat != null ? cat : BannerRecipeCategories.MISC.id();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private Map<String, LoomScreenState> loadPersistedStates() {
        if (!Files.exists(PERSISTENCE_FILE_PATH)) {
            return new LinkedHashMap<>();
        }

        try (Reader reader = Files.newBufferedReader(PERSISTENCE_FILE_PATH)) {
            Map<String, LoomScreenState> persistedStates = GSON.fromJson(reader, PERSISTED_STATE_MAP_TYPE);
            return persistedStates != null ? persistedStates : new LinkedHashMap<>();
        } catch (JsonSyntaxException e) {
            LoomAssistantMod.LOGGER.warn("Invalid loom state file: {}", PERSISTENCE_FILE_PATH, e);
            return new LinkedHashMap<>();
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.warn("Failed to read loom state file: {}", PERSISTENCE_FILE_PATH, e);
            return new LinkedHashMap<>();
        }
    }

    private void savePersistedStates() {
        try {
            Files.createDirectories(PERSISTENCE_FILE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PERSISTENCE_FILE_PATH)) {
                GSON.toJson(persistedStates, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save loom state file: " + PERSISTENCE_FILE_PATH, e);
        }
    }

    private void resetStateFromPersistence() {
        state.setPanelOpen(false);
        state.setActiveBanner(null);
        state.setSelectedBannerId(null);
        state.setActiveBannerSource(null);
        state.setActiveBannerRecipe(null);
        state.setSelectedCategory(null);
        state.setColorReplacementEnabled(false);
        state.getColorReplacements().clear();
    }

    private void loadPersistedState(String worldKey) {
        resetStateFromPersistence();

        LoomScreenState persistedWorldState = persistedStates.get(worldKey);
        if (persistedWorldState == null) {
            return;
        }

        state.setPanelOpen(persistedWorldState.isPanelOpen());
        state.setActiveBannerRecipe(blankToNull(persistedWorldState.getActiveBannerRecipe()));
        state.setSelectedCategory(blankToNull(persistedWorldState.getSelectedCategory()));
        state.setColorReplacements(persistedWorldState.getColorReplacements());
        state.setColorReplacementEnabled(state.getActiveBannerRecipe() != null
                && persistedWorldState.isColorReplacementEnabled()
                && !state.getColorReplacements().isEmpty());
        if (!state.isColorReplacementEnabled()) {
            state.getColorReplacements().clear();
        }

        restoreActiveBannerFromPersistedState();
    }

    private void persistCurrentWorldState() {
        if (loadedWorldKey == null) {
            LoomAssistantMod.LOGGER.warn("Cannot persist loom state before a world has been loaded");
            return;
        }

        LoomScreenState snapshot = snapshotPersistedState();
        if (snapshot.isPanelOpen()
                || blankToNull(snapshot.getSelectedCategory()) != null
                || blankToNull(snapshot.getActiveBannerRecipe()) != null
                || snapshot.isColorReplacementEnabled()
                || !snapshot.getColorReplacements().isEmpty()) {
            persistedStates.put(loadedWorldKey, snapshot);
        } else {
            persistedStates.remove(loadedWorldKey);
        }

        savePersistedStates();
    }

    private LoomScreenState snapshotPersistedState() {
        LoomScreenState snapshot = new LoomScreenState();
        snapshot.setPanelOpen(state.isPanelOpen());
        snapshot.setSelectedCategory(blankToNull(state.getSelectedCategory()));
        snapshot.setActiveBannerRecipe(blankToNull(state.getActiveBannerRecipe()));

        boolean persistableBanner = snapshot.getActiveBannerRecipe() != null;
        boolean persistentDyeEnabled = persistableBanner
                && state.isColorReplacementEnabled()
                && !state.getColorReplacements().isEmpty();
        snapshot.setColorReplacementEnabled(persistentDyeEnabled);
        if (persistentDyeEnabled) {
            snapshot.setColorReplacements(state.getColorReplacements());
        }
        return snapshot;
    }

    private void restoreActiveBannerFromPersistedState() {
        String sourceId = blankToNull(state.getActiveBannerRecipe());
        if (sourceId == null) {
            state.setActiveBanner(null);
            state.setActiveBannerSource(null);
            state.setSelectedBannerId(null);
            return;
        }

        BannerRecipe source = BannerStorage.getInstance().getBannerById(sourceId);
        if (source == null) {
            state.setActiveBanner(null);
            state.setActiveBannerSource(null);
            state.setSelectedBannerId(null);
            return;
        }

        setActiveBannerFromSource(source, sourceId, false);
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    private static boolean isInSurvivalMode() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.player != null && !mc.player.hasInfiniteMaterials();
    }

    private static BannerRecipe cloneBanner(BannerRecipe source) {
        return new BannerRecipe(source.getName(), source.getBaseColorEnum(), new ArrayList<>(source.getLayers()))
                .withCategory(source.getCategory());
    }

    private static Map<DyeColor, DyeColor> normalizeReplacementMap(Map<DyeColor, DyeColor> replacements) {
        Map<DyeColor, DyeColor> normalized = new LinkedHashMap<>();
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
        if (a.getBaseColorEnum() != b.getBaseColorEnum()) return false;
        List<BannerRecipeLayer> la = a.getLayers();
        List<BannerRecipeLayer> lb = b.getLayers();
        if (la.size() != lb.size()) return false;
        for (int i = 0; i < la.size(); i++) {
            if (!la.get(i).equals(lb.get(i))) return false;
        }
        return true;
    }

    private static String currentWorldKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return "unknown";
        IntegratedServer sp = mc.getSingleplayerServer();
        if (sp != null) {
            return localWorldKey(sp.getWorldPath(LevelResource.ROOT), FabricLoader.getInstance().getGameDir());
        }
        ServerData server = mc.getCurrentServer();
        if (server != null && server.ip != null && !server.ip.isBlank()) {
            return MULTIPLAYER_WORLD_KEY_PREFIX + server.ip.toLowerCase();
        }
        return "unknown";
    }

    static String localWorldKey(Path worldRoot, Path gameDir) {
        Path normalizedWorldRoot = worldRoot.toAbsolutePath().normalize();
        Path normalizedGameDir = gameDir.toAbsolutePath().normalize();
        Path normalizedSavesDir = normalizedGameDir.resolve("saves").normalize();

        if (normalizedWorldRoot.startsWith(normalizedSavesDir)) {
            return LOCAL_WORLD_KEY_PREFIX + pathKeySuffix(normalizedSavesDir.relativize(normalizedWorldRoot));
        }
        if (normalizedWorldRoot.startsWith(normalizedGameDir)) {
            return LOCAL_WORLD_KEY_PREFIX + pathKeySuffix(normalizedGameDir.relativize(normalizedWorldRoot));
        }

        Path fileName = normalizedWorldRoot.getFileName();
        return LOCAL_WORLD_KEY_PREFIX + (fileName != null ? fileName : normalizedWorldRoot);
    }

    private static String pathKeySuffix(Path path) {
        StringBuilder builder = new StringBuilder();
        for (Path part : path) {
            if (builder.length() > 0) {
                builder.append('/');
            }
            builder.append(part);
        }
        return builder.toString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    static Gson createPersistenceGson() {
        return new GsonBuilder()
                .registerTypeAdapter(DyeColor.class, new DyeColorAdapter())
                .enableComplexMapKeySerialization()
                .setPrettyPrinting()
                .create();
    }

    static final class DyeColorAdapter extends TypeAdapter<DyeColor> {
        @Override
        public void write(JsonWriter out, DyeColor value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.name().toLowerCase(Locale.ROOT));
        }

        @Override
        public DyeColor read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return DyeColor.valueOf(in.nextString().toUpperCase(Locale.ROOT));
        }
    }
}
