/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.weaving;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;

/** Computes craftability and missing materials for loom banner recipes. */
public class BannerCraftabilityModel {
    private static final int BANNER_SLOT = 0;
    private static final int DYE_SLOT = 1;
    private static final int PATTERN_SLOT = 2;
    private static final int INVENTORY_START = 4;
    private static final int INVENTORY_END = 40;

    private final LoomMenu handler;

    public enum MissingMaterialType {
        NONE,
        BANNER,
        DYE,
        PATTERN_TEMPLATE
    }

    private record MissingMaterial(Item item, MissingMaterialType type) {}

    public BannerCraftabilityModel(LoomMenu handler) {
        this.handler = handler;
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

    public String getValidationError(BannerRecipe banner) {
        List<String> missingDescriptions = getMissingMaterialDescriptions(banner);
        if (missingDescriptions.isEmpty()) {
            return null;
        }
        return "Missing " + String.join(", ", missingDescriptions);
    }

    public static Item getRequiredPatternItem(String patternId) {
        // These patterns require a banner pattern item.
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
}
