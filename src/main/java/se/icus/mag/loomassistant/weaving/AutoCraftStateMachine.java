/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.weaving;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.types.recipe.BannerRecipe;
import se.icus.mag.loomassistant.types.recipe.BannerRecipeLayer;

public class AutoCraftStateMachine {
    private static final int TICK_DELAY = 3;
    private static final int BANNER_SLOT = 0;
    private static final int DYE_SLOT = 1;
    private static final int PATTERN_SLOT = 2;
    private static final int OUTPUT_SLOT = 3;
    private static final int INVENTORY_START = 4;
    private static final int INVENTORY_END = 40;

    private final LoomMenu handler;
    private AutoCraftState state = AutoCraftState.IDLE;
    private BannerRecipe targetBanner;
    private int currentLayerIndex = 0;
    private int ticksInState = 0;
    private String errorMessage = null;

    public enum AutoCraftState {
        IDLE,
        CHECKING_MATERIALS,
        PLACING_BANNER,
        PLACING_DYE,
        PLACING_PATTERN_ITEM,
        SELECTING_PATTERN,
        WAITING_FOR_OUTPUT,
        TAKING_OUTPUT,
        LAYER_COMPLETE,
        COMPLETE,
        ERROR
    }

    public enum MissingMaterialType {
        NONE,
        BANNER,
        DYE,
        PATTERN_TEMPLATE
    }

    private record MissingMaterial(Item item, MissingMaterialType type) {}

    private record MaterialValidationResult(String errorMessage, MissingMaterialType missingType) {}

    public AutoCraftStateMachine(LoomMenu handler) {
        this.handler = handler;
    }

    public void start(BannerRecipe banner) {
        this.targetBanner = banner;
        this.currentLayerIndex = 0;
        this.ticksInState = 0;
        this.errorMessage = null;

        String validationError = validateAllMaterials(banner);
        if (validationError != null) {
            error(validationError);
            return;
        }

        this.state = AutoCraftState.CHECKING_MATERIALS;
        LoomAssistantMod.LOGGER.info(
                "Starting auto-craft for banner with {} layers",
                banner.getLayers().size());
    }

    public void stop() {
        this.state = AutoCraftState.IDLE;
        this.targetBanner = null;
        this.currentLayerIndex = 0;
    }

    public boolean isActive() {
        return state != AutoCraftState.IDLE && state != AutoCraftState.COMPLETE && state != AutoCraftState.ERROR;
    }

    public AutoCraftState getState() {
        return state;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean canCraft(BannerRecipe banner) {
        if (banner == null) {
            return false;
        }
        return getMissingMaterialsInCraftOrder(banner).isEmpty();
    }

    public MissingMaterialType getMissingMaterialType(BannerRecipe banner) {
        if (banner == null) {
            return MissingMaterialType.NONE;
        }
        List<MissingMaterial> missingMaterials = getMissingMaterialsInCraftOrder(banner);
        if (missingMaterials.isEmpty()) {
            return MissingMaterialType.NONE;
        }
        return missingMaterials.get(0).type();
    }

    public List<String> getMissingMaterialDescriptions(BannerRecipe banner) {
        List<String> descriptions = new ArrayList<>();
        if (banner == null) {
            return descriptions;
        }

        List<MissingMaterial> missingMaterials = getMissingMaterialsInCraftOrder(banner);
        for (MissingMaterial missingMaterial : missingMaterials) {
            descriptions.add(formatMissingMaterialName(missingMaterial));
        }
        return descriptions;
    }

    public void tick() {
        if (state == AutoCraftState.IDLE || state == AutoCraftState.COMPLETE || state == AutoCraftState.ERROR) {
            return;
        }

        ticksInState++;
        if (ticksInState < TICK_DELAY) {
            return;
        }
        ticksInState = 0;

        switch (state) {
            case CHECKING_MATERIALS -> checkMaterials();
            case PLACING_BANNER -> placeBanner();
            case PLACING_DYE -> placeDye();
            case PLACING_PATTERN_ITEM -> placePatternItem();
            case SELECTING_PATTERN -> selectPattern();
            case WAITING_FOR_OUTPUT -> waitForOutput();
            case TAKING_OUTPUT -> takeOutput();
            case LAYER_COMPLETE -> advanceLayer();
        }
    }

    private void checkMaterials() {
        // All materials have been validated in start(), just proceed
        state = AutoCraftState.PLACING_BANNER;
    }

    private void placeBanner() {
        ItemStack bannerInSlot = handler.getSlot(BANNER_SLOT).getItem();

        if (!bannerInSlot.isEmpty()) {
            // Banner already in slot
            state = AutoCraftState.PLACING_DYE;
            return;
        }

        // Find banner in inventory and move it
        Item bannerItem = targetBanner.getBaseBannerItem();
        int slotId;
        if (currentLayerIndex == 0) {
            slotId = findBlankBannerInInventory(bannerItem);
        } else {
            slotId = findItemInInventory(bannerItem);
        }

        if (slotId >= 0) {
            quickMoveToSlot(slotId, BANNER_SLOT);
            state = AutoCraftState.PLACING_DYE;
        } else {
            error("Cannot find banner in inventory");
        }
    }

    private void placeDye() {
        if (currentLayerIndex >= targetBanner.getLayers().size()) {
            state = AutoCraftState.COMPLETE;
            return;
        }

        BannerRecipeLayer layer = targetBanner.getLayers().get(currentLayerIndex);
        Item dyeItem = BannerRecipe.getDyeItem(layer.getDyeColorEnum());

        ItemStack dyeInSlot = handler.getSlot(DYE_SLOT).getItem();
        if (!dyeInSlot.isEmpty()) {
            // Check if it's the right dye
            if (dyeInSlot.getItem() == dyeItem) {
                state = AutoCraftState.PLACING_PATTERN_ITEM;
                return;
            }
            // Wrong dye, need to swap it out
            quickMoveToInventory(DYE_SLOT);
        }

        int slotId = findItemInInventory(dyeItem);
        if (slotId >= 0) {
            quickMoveToSlot(slotId, DYE_SLOT);
            state = AutoCraftState.PLACING_PATTERN_ITEM;
        } else {
            error("Cannot find " + layer.getDyeColorEnum().getName() + " dye");
        }
    }

    private void placePatternItem() {
        BannerRecipeLayer layer = targetBanner.getLayers().get(currentLayerIndex);
        String patternId = layer.patternId();

        // Check if this pattern requires a pattern item
        Item patternItem = getRequiredPatternItem(patternId);
        if (patternItem == null) {
            // No pattern item needed, go straight to selecting pattern
            state = AutoCraftState.SELECTING_PATTERN;
            return;
        }

        ItemStack patternInSlot = handler.getSlot(PATTERN_SLOT).getItem();
        if (!patternInSlot.isEmpty()) {
            if (patternInSlot.getItem() == patternItem) {
                state = AutoCraftState.SELECTING_PATTERN;
                return;
            }
            quickMoveToInventory(PATTERN_SLOT);
        }

        int slotId = findItemInInventory(patternItem);
        if (slotId >= 0) {
            quickMoveToSlot(slotId, PATTERN_SLOT);
            state = AutoCraftState.SELECTING_PATTERN;
        } else {
            error("Cannot find required pattern item");
        }
    }

    private void selectPattern() {
        BannerRecipeLayer layer = targetBanner.getLayers().get(currentLayerIndex);
        String patternId = layer.patternId();

        // Find the pattern index in the loom's pattern list
        List<?> patterns = handler.getSelectablePatterns();
        int patternIndex = -1;
        for (int i = 0; i < patterns.size(); i++) {
            Object pattern = patterns.get(i);
            // Pattern entries are registry entries
            if (pattern.toString().contains(patternId)
                    || pattern.toString().contains(patternId.replace("minecraft:", ""))) {
                patternIndex = i;
                break;
            }
        }

        if (patternIndex >= 0) {
            // Click the pattern button
            Minecraft client = Minecraft.getInstance();
            if (client.gameMode != null) {
                client.gameMode.handleInventoryButtonClick(handler.containerId, patternIndex);
            }
            state = AutoCraftState.WAITING_FOR_OUTPUT;
        } else {
            // Pattern not found, might be basic pattern that's always available
            // Try to click based on pattern name
            error("Pattern not found: " + patternId);
        }
    }

    private void waitForOutput() {
        ItemStack output = handler.getSlot(OUTPUT_SLOT).getItem();
        if (!output.isEmpty()) {
            state = AutoCraftState.TAKING_OUTPUT;
        }
        // Otherwise keep waiting
    }

    private void takeOutput() {
        // Take the output
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null && client.player != null) {
            // Shift-click to move to inventory, or if more layers, we'll put back in banner slot
            if (currentLayerIndex + 1 < targetBanner.getLayers().size()) {
                // More layers to go - take output and put in banner slot
                // First pick up the output
                client.gameMode.handleContainerInput(
                        handler.containerId, OUTPUT_SLOT, 0, ContainerInput.PICKUP, client.player);
                // Return pattern items to inventory before next layer
                returnPatternItemsToInventory();
                // Then place in banner slot (clear banner slot first if needed)
                ItemStack bannerSlotStack = handler.getSlot(BANNER_SLOT).getItem();
                if (!bannerSlotStack.isEmpty()) {
                    // Banner slot should be empty after crafting, but just in case
                    client.gameMode.handleContainerInput(
                            handler.containerId, BANNER_SLOT, 0, ContainerInput.QUICK_MOVE, client.player);
                }
                client.gameMode.handleContainerInput(
                        handler.containerId, BANNER_SLOT, 0, ContainerInput.PICKUP, client.player);
            } else {
                // Last layer - move to inventory
                client.gameMode.handleContainerInput(
                        handler.containerId, OUTPUT_SLOT, 0, ContainerInput.QUICK_MOVE, client.player);
                // Return pattern items to inventory
                returnPatternItemsToInventory();
            }
        }
        state = AutoCraftState.LAYER_COMPLETE;
    }

    private void advanceLayer() {
        currentLayerIndex++;
        if (currentLayerIndex >= targetBanner.getLayers().size()) {
            state = AutoCraftState.COMPLETE;
            LoomAssistantMod.LOGGER.info("Auto-craft complete!");
        } else {
            state = AutoCraftState.CHECKING_MATERIALS;
        }
    }

    private void error(String message) {
        this.errorMessage = message;
        this.state = AutoCraftState.ERROR;
        LoomAssistantMod.LOGGER.warn("Auto-craft error: {}", message);
    }

    private int findItemInInventory(Item item) {
        for (int i = INVENTORY_START; i < INVENTORY_END; i++) {
            ItemStack stack = handler.getSlot(i).getItem();
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findBlankBannerInInventory(Item item) {
        for (int i = INVENTORY_START; i < INVENTORY_END; i++) {
            ItemStack stack = handler.getSlot(i).getItem();
            if (!stack.isEmpty() && stack.getItem() == item) {
                BannerPatternLayers patterns = stack.get(DataComponents.BANNER_PATTERNS);
                if (patterns == null || patterns.layers().isEmpty()) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void quickMoveToSlot(int fromSlot, int toSlot) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null && client.player != null) {
            ItemStack sourceStack = handler.getSlot(fromSlot).getItem();
            if (sourceStack.isEmpty()) return;

            // 1. Pick up the entire stack from source (Left Click)
            client.gameMode.handleContainerInput(
                    handler.containerId, fromSlot, 0, ContainerInput.PICKUP, client.player);

            // 2. Place ONE item into destination (Right Click)
            // Button 1 is Right Click, which places one item from the cursor stack
            client.gameMode.handleContainerInput(handler.containerId, toSlot, 1, ContainerInput.PICKUP, client.player);

            // 3. Put remaining items back into source (Left Click)
            // We do this unconditionally to ensure we don't hold onto items.
            // If we had >1 items, we are holding the rest. Clicking source puts them back.
            // If we had 1 item, we placed it. Cursor is empty. Source is empty. Clicking source does nothing.
            client.gameMode.handleContainerInput(
                    handler.containerId, fromSlot, 0, ContainerInput.PICKUP, client.player);
        }
    }

    private void quickMoveToInventory(int slot) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null && client.player != null) {
            client.gameMode.handleContainerInput(
                    handler.containerId, slot, 0, ContainerInput.QUICK_MOVE, client.player);
        }
    }

    private Item getRequiredPatternItem(String patternId) {
        // These patterns require a banner pattern item
        String lowerId = patternId.toLowerCase();
        if (lowerId.contains("globe")) return Items.GLOBE_BANNER_PATTERN;
        if (lowerId.contains("creeper")) return Items.CREEPER_BANNER_PATTERN;
        if (lowerId.contains("skull")) return Items.SKULL_BANNER_PATTERN;
        if (lowerId.contains("flower")) return Items.FLOWER_BANNER_PATTERN;
        if (lowerId.contains("mojang")) return Items.MOJANG_BANNER_PATTERN;
        if (lowerId.contains("piglin")) return Items.PIGLIN_BANNER_PATTERN;
        if (lowerId.contains("flow")) return Items.FLOW_BANNER_PATTERN;
        if (lowerId.contains("guster")) return Items.GUSTER_BANNER_PATTERN;
        return null;
    }

    /**
     * Validates that all required materials are available before starting the craft
     * Returns an error message if validation fails, null if all materials are available
     */
    private String validateAllMaterials(BannerRecipe banner) {
        return validateMaterials(banner).errorMessage();
    }

    private MaterialValidationResult validateMaterials(BannerRecipe banner) {
        List<String> missingDescriptions = getMissingMaterialDescriptions(banner);
        if (missingDescriptions.isEmpty()) {
            return new MaterialValidationResult(null, MissingMaterialType.NONE);
        }
        MissingMaterialType missingType = getMissingMaterialType(banner);
        return new MaterialValidationResult("Missing " + String.join(", ", missingDescriptions), missingType);
    }

    private List<MissingMaterial> getMissingMaterialsInCraftOrder(BannerRecipe banner) {
        List<MissingMaterial> missing = new ArrayList<>();

        Map<Item, Integer> availableCounts = getAvailableItemCounts();

        // Crafting starts with a base banner.
        ItemStack bannerSlotStack = handler.getSlot(BANNER_SLOT).getItem();
        if (bannerSlotStack.isEmpty()) {
            Item bannerItem = banner.getBaseBannerItem();
            int availableBlankBanners = countBlankBannersInInventory(bannerItem);
            if (availableBlankBanners < 1) {
                missing.add(new MissingMaterial(bannerItem, MissingMaterialType.BANNER));
            } else {
                consumeOne(availableCounts, bannerItem);
            }
        }

        for (BannerRecipeLayer layer : banner.getLayers()) {
            Item dyeItem = BannerRecipe.getDyeItem(layer.getDyeColorEnum());
            if (!consumeOne(availableCounts, dyeItem)) {
                missing.add(new MissingMaterial(dyeItem, MissingMaterialType.DYE));
            }

            Item patternItem = getRequiredPatternItem(layer.patternId());
            if (patternItem != null && !consumeOne(availableCounts, patternItem)) {
                missing.add(new MissingMaterial(patternItem, MissingMaterialType.PATTERN_TEMPLATE));
            }
        }

        return missing;
    }

    private Map<Item, Integer> getAvailableItemCounts() {
        Map<Item, Integer> available = new HashMap<>();

        for (int i = INVENTORY_START; i < INVENTORY_END; i++) {
            ItemStack stack = handler.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                available.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }

        ItemStack dyeSlotStack = handler.getSlot(DYE_SLOT).getItem();
        if (!dyeSlotStack.isEmpty()) {
            available.merge(dyeSlotStack.getItem(), dyeSlotStack.getCount(), Integer::sum);
        }

        ItemStack patternSlotStack = handler.getSlot(PATTERN_SLOT).getItem();
        if (!patternSlotStack.isEmpty()) {
            available.merge(patternSlotStack.getItem(), patternSlotStack.getCount(), Integer::sum);
        }

        return available;
    }

    private int countBlankBannersInInventory(Item bannerItem) {
        int count = 0;
        for (int i = INVENTORY_START; i < INVENTORY_END; i++) {
            ItemStack stack = handler.getSlot(i).getItem();
            if (stack.isEmpty() || stack.getItem() != bannerItem) {
                continue;
            }
            BannerPatternLayers patterns = stack.get(DataComponents.BANNER_PATTERNS);
            if (patterns == null || patterns.layers().isEmpty()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean consumeOne(Map<Item, Integer> availableCounts, Item item) {
        int available = availableCounts.getOrDefault(item, 0);
        if (available <= 0) {
            return false;
        }

        availableCounts.put(item, available - 1);
        return true;
    }

    private static String formatMissingMaterialName(MissingMaterial missingMaterial) {
        return new ItemStack(missingMaterial.item()).getHoverName().getString();
    }

    /**
     * Returns pattern items to inventory after crafting is complete
     */
    private void returnPatternItemsToInventory() {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null && client.player != null) {
            ItemStack patternSlot = handler.getSlot(PATTERN_SLOT).getItem();
            if (!patternSlot.isEmpty()) {
                // Move pattern item back to inventory
                client.gameMode.handleContainerInput(
                        handler.containerId, PATTERN_SLOT, 0, ContainerInput.QUICK_MOVE, client.player);
            }
        }
    }
}
