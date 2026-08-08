/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.weaving;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;

/**
 * Weaver for survival mode: drives the loom UI by simulating slot clicks.
 */
public class SurvivalWeaver extends Weaver {
    private static final int TICK_DELAY = 3;
    private static final int BANNER_SLOT = 0;
    private static final int DYE_SLOT = 1;
    private static final int PATTERN_SLOT = 2;
    private static final int OUTPUT_SLOT = 3;
    private static final int INVENTORY_START = 4;
    private static final int INVENTORY_END = 40;

    private final LoomMenu handler;
    private final BannerCraftabilityModel craftabilityModel;
    private AutoCraftState state = AutoCraftState.IDLE;
    private BannerRecipe targetBanner;
    private int currentLayerIndex = 0;
    private int ticksInState = 0;
    private String errorMessage = null;

    private enum AutoCraftState {
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

    public SurvivalWeaver(LoomMenu menu) {
        this.handler = menu;
        this.craftabilityModel = new BannerCraftabilityModel(menu);
    }

    @Override
    public void weave(BannerRecipe banner) {
        start(banner);
    }

    @Override
    public boolean isActive() {
        return state != AutoCraftState.IDLE && state != AutoCraftState.COMPLETE && state != AutoCraftState.ERROR;
    }

    @Override
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
            case IDLE, COMPLETE, ERROR -> {
                // handled by early return
            }
        }
    }

    @Override
    public boolean canWeave(BannerRecipe banner) {
        return craftabilityModel.canCraft(banner);
    }

    @Override
    public List<String> getMissingMaterialDescriptions(BannerRecipe banner) {
        return craftabilityModel.getMissingMaterialDescriptions(banner);
    }

    private void start(BannerRecipe banner) {
        if (banner == null) {
            error("No banner recipe selected");
            return;
        }

        this.targetBanner = banner;
        this.currentLayerIndex = 0;
        this.ticksInState = 0;
        this.errorMessage = null;

        String validationError = craftabilityModel.getValidationError(banner);
        if (validationError != null) {
            error(validationError);
            return;
        }

        this.state = AutoCraftState.CHECKING_MATERIALS;
        LoomAssistantMod.LOGGER.info("Starting auto-craft for banner with {} layers", banner.getLayers().size());
    }

    private void checkMaterials() {
        state = AutoCraftState.PLACING_BANNER;
    }

    private void placeBanner() {
        ItemStack bannerInSlot = handler.getSlot(BANNER_SLOT).getItem();
        if (!bannerInSlot.isEmpty()) {
            state = AutoCraftState.PLACING_DYE;
            return;
        }

        Item bannerItem = targetBanner.getBaseBannerItem();
        int slotId = currentLayerIndex == 0 ? findBlankBannerInInventory(bannerItem) : findItemInInventory(bannerItem);
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
            if (dyeInSlot.getItem() == dyeItem) {
                state = AutoCraftState.PLACING_PATTERN_ITEM;
                return;
            }
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
        Item patternItem = BannerCraftabilityModel.getRequiredPatternItem(patternId);
        if (patternItem == null) {
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

        List<?> patterns = handler.getSelectablePatterns();
        int patternIndex = -1;
        for (int i = 0; i < patterns.size(); i++) {
            Object pattern = patterns.get(i);
            if (pattern.toString().contains(patternId)
                    || pattern.toString().contains(patternId.replace("minecraft:", ""))) {
                patternIndex = i;
                break;
            }
        }

        if (patternIndex >= 0) {
            Minecraft client = Minecraft.getInstance();
            if (client.gameMode != null) {
                client.gameMode.handleInventoryButtonClick(handler.containerId, patternIndex);
            }
            state = AutoCraftState.WAITING_FOR_OUTPUT;
        } else {
            error("Pattern not found: " + patternId);
        }
    }

    private void waitForOutput() {
        ItemStack output = handler.getSlot(OUTPUT_SLOT).getItem();
        if (!output.isEmpty()) {
            state = AutoCraftState.TAKING_OUTPUT;
        }
    }

    private void takeOutput() {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null && client.player != null) {
            if (currentLayerIndex + 1 < targetBanner.getLayers().size()) {
                client.gameMode.handleContainerInput(handler.containerId, OUTPUT_SLOT, 0, ContainerInput.PICKUP, client.player);
                returnPatternItemsToInventory();

                ItemStack bannerSlotStack = handler.getSlot(BANNER_SLOT).getItem();
                if (!bannerSlotStack.isEmpty()) {
                    client.gameMode.handleContainerInput(
                            handler.containerId, BANNER_SLOT, 0, ContainerInput.QUICK_MOVE, client.player);
                }
                client.gameMode.handleContainerInput(handler.containerId, BANNER_SLOT, 0, ContainerInput.PICKUP, client.player);
            } else {
                client.gameMode.handleContainerInput(
                        handler.containerId, OUTPUT_SLOT, 0, ContainerInput.QUICK_MOVE, client.player);
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

            client.gameMode.handleContainerInput(handler.containerId, fromSlot, 0, ContainerInput.PICKUP, client.player);
            client.gameMode.handleContainerInput(handler.containerId, toSlot, 1, ContainerInput.PICKUP, client.player);
            client.gameMode.handleContainerInput(handler.containerId, fromSlot, 0, ContainerInput.PICKUP, client.player);
        }
    }

    private void quickMoveToInventory(int slot) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null && client.player != null) {
            client.gameMode.handleContainerInput(handler.containerId, slot, 0, ContainerInput.QUICK_MOVE, client.player);
        }
    }

    private void returnPatternItemsToInventory() {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null && client.player != null) {
            ItemStack patternSlot = handler.getSlot(PATTERN_SLOT).getItem();
            if (!patternSlot.isEmpty()) {
                client.gameMode.handleContainerInput(
                        handler.containerId, PATTERN_SLOT, 0, ContainerInput.QUICK_MOVE, client.player);
            }
        }
    }
}
