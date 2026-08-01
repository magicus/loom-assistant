/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.data.BannerPatternLayer;
import se.icus.mag.loomassistant.data.SavedBanner;

/**
 * Screen showing required materials to craft a banner pattern.
 */
public class BannerMaterialsScreen extends Screen {
    private static final int CONTENT_WIDTH = 250;
    private static final int CONTENT_HEIGHT = 300;
    private static final int PADDING = 10;

    private final Screen previousScreen;
    private final SavedBanner banner;
    private int scrollOffset = 0;

    public BannerMaterialsScreen(Screen previousScreen, SavedBanner banner) {
        super(Component.literal("Banner Materials"));
        this.previousScreen = previousScreen;
        this.banner = banner;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Draw semi-transparent background
        context.fill(0, 0, this.width, this.height, 0xAA000000);

        // Draw content background
        int contentX = (this.width - CONTENT_WIDTH) / 2;
        int contentY = (this.height - CONTENT_HEIGHT) / 2;
        context.fill(contentX, contentY, contentX + CONTENT_WIDTH, contentY + CONTENT_HEIGHT, 0xFF1F1F1F);

        // Draw border
        context.fill(contentX, contentY, contentX + 1, contentY + CONTENT_HEIGHT, 0xFF4169E1); // Left
        context.fill(
                contentX + CONTENT_WIDTH - 1,
                contentY,
                contentX + CONTENT_WIDTH,
                contentY + CONTENT_HEIGHT,
                0xFF4169E1); // Right
        context.fill(contentX, contentY, contentX + CONTENT_WIDTH, contentY + 1, 0xFF4169E1); // Top
        context.fill(
                contentX,
                contentY + CONTENT_HEIGHT - 1,
                contentX + CONTENT_WIDTH,
                contentY + CONTENT_HEIGHT,
                0xFF4169E1); // Bottom

        // Draw title
        context.text(
                this.font,
                Component.literal("Required Materials"),
                contentX + PADDING,
                contentY + PADDING,
                0xFF4169E1,
                true);

        // Draw crafting order header
        context.text(
                this.font,
                Component.literal("Crafting Order:"),
                contentX + PADDING,
                contentY + PADDING + 15,
                0xFF4169E1,
                true);

        // Draw banner base color
        String baseColorName = banner.getBaseColorEnum().getSerializedName();
        ItemStack baseStack = new ItemStack(banner.getBaseBannerItem());
        context.item(baseStack, contentX + PADDING, contentY + PADDING + 28);
        context.text(
                this.font,
                Component.literal("1. " + baseColorName + " Banner"),
                contentX + PADDING + 20,
                contentY + PADDING + 31,
                0xFFFFFFFF,
                true);

        // Draw materials list with step numbers
        int materialY = contentY + PADDING + 50;
        int maxHeight = CONTENT_HEIGHT - PADDING * 2 - 50;

        List<MaterialEntry> materials = getMaterialsList();
        int stepNumber = 2;

        for (int i = scrollOffset; i < materials.size() && materialY < contentY + CONTENT_HEIGHT - 30; i++) {
            MaterialEntry entry = materials.get(i);

            // Draw item
            context.item(entry.stack, contentX + PADDING, materialY - 2);

            // Draw text with step number
            String text = stepNumber + ". " + entry.name;
            if (entry.quantity > 1) {
                text = stepNumber + ". " + entry.quantity + "x " + entry.name;
            }
            context.text(this.font, Component.literal(text), contentX + PADDING + 20, materialY, 0xFFFFFFFF, true);
            materialY += 18;
            stepNumber++;
        }

        // Draw close button
        int buttonY = contentY + CONTENT_HEIGHT - 25;
        int buttonX = (this.width / 2) - 25;
        boolean buttonHovered =
                mouseX >= buttonX && mouseX < buttonX + 50 && mouseY >= buttonY && mouseY < buttonY + 20;
        int buttonColor = buttonHovered ? 0xFF4169E1 : 0xFF1E40AF;
        context.fill(buttonX, buttonY, buttonX + 50, buttonY + 20, buttonColor);
        String closeText = "Close";
        int closeTextWidth = this.font.width(closeText);
        context.text(
                this.font,
                Component.literal(closeText),
                buttonX + (50 - closeTextWidth) / 2,
                buttonY + 6,
                0xFFFFFFFF,
                true);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int contentX = (this.width - CONTENT_WIDTH) / 2;
        int contentY = (this.height - CONTENT_HEIGHT) / 2;
        int buttonY = contentY + CONTENT_HEIGHT - 25;
        int buttonX = (this.width / 2) - 25;

        // Close button
        if (mouseX >= buttonX && mouseX < buttonX + 50 && mouseY >= buttonY && mouseY < buttonY + 20) {
            this.minecraft.gui.setScreen(previousScreen);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentX = (this.width - CONTENT_WIDTH) / 2;
        int contentY = (this.height - CONTENT_HEIGHT) / 2;
        int textStartX = contentX + PADDING;
        int textStartY = contentY + PADDING + 40;
        int textWidth = CONTENT_WIDTH - (PADDING * 2);
        int textHeight = CONTENT_HEIGHT - PADDING * 2 - 50;

        // Check if mouse is over the materials list
        if (mouseX >= textStartX
                && mouseX < textStartX + textWidth
                && mouseY >= textStartY
                && mouseY < textStartY + textHeight) {
            scrollOffset -= (int) verticalAmount;
            List<MaterialEntry> materials = getMaterialsList();
            int maxScroll = Math.max(0, materials.size() - (textHeight / 18));
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            return true;
        }

        return false;
    }

    private List<MaterialEntry> getMaterialsList() {
        List<MaterialEntry> materials = new ArrayList<>();

        // Add each layer in order with its pattern and dye
        for (BannerPatternLayer layer : banner.getLayers()) {
            String patternId = layer.patternId();
            DyeColor dyeColor = layer.getDyeColorEnum();

            // Get pattern name
            String patternName = extractPatternName(patternId);

            // Get dye color name
            String colorName = dyeColor.getSerializedName();
            String displayColor = colorName.substring(0, 1).toUpperCase() + colorName.substring(1);

            // Create display text: "Pattern Name (Dye Color)"
            String displayName = patternName + " (" + displayColor + " Dye)";

            // Check if this pattern requires an item
            ItemStack patternStack = getPatternItem(patternId);
            if (patternStack != null && !patternStack.isEmpty()) {
                // Pattern requires an item
                materials.add(new MaterialEntry(patternStack, displayName, 1));
            } else {
                // Built-in pattern - show dye instead
                ItemStack dyeStack = new ItemStack(SavedBanner.getDyeItem(dyeColor));
                materials.add(new MaterialEntry(dyeStack, displayName, 1));
            }
        }

        return materials;
    }

    private String extractPatternName(String patternId) {
        // Extract the pattern name from the ID
        String[] parts = patternId.split(":");
        String name = parts.length > 1 ? parts[1] : patternId;

        // Convert snake_case to Title Case
        String[] words = name.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return result.toString();
    }

    private ItemStack getPatternItem(String patternId) {
        // Try to find the pattern item in the registry
        try {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null) {
                var itemRegistry = client.level.registryAccess().lookup(Registries.ITEM);

                if (itemRegistry.isPresent()) {
                    Registry<net.minecraft.world.item.Item> registry =
                            (Registry<net.minecraft.world.item.Item>) itemRegistry.get();

                    // Convert pattern ID to item ID (e.g., "minecraft:globe" -> "minecraft:globe_banner_pattern")
                    String[] parts = patternId.split(":");
                    String patternName = parts.length > 1 ? parts[1] : patternId;
                    String itemId = parts[0] + ":" + patternName + "_banner_pattern";

                    Identifier itemIdentifier = Identifier.tryParse(itemId);
                    if (itemIdentifier != null && registry.containsKey(itemIdentifier)) {
                        net.minecraft.world.item.Item item = registry.getValue(itemIdentifier);
                        if (item != null) {
                            return new ItemStack(item);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LoomAssistantMod.LOGGER.debug("Failed to get pattern item for {}", patternId, e);
        }

        return ItemStack.EMPTY; // Built-in patterns don't need items
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(previousScreen);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private static class MaterialEntry {
        ItemStack stack;
        String name;
        int quantity;

        MaterialEntry(ItemStack stack, String name, int quantity) {
            this.stack = stack;
            this.name = name;
            this.quantity = quantity;
        }
    }
}
