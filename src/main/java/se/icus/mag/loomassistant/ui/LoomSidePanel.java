/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.LoomMenu;
import org.lwjgl.glfw.GLFW;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.autocraft.AutoCraftStateMachine;
import se.icus.mag.loomassistant.data.BannerStorage;
import se.icus.mag.loomassistant.data.SavedBanner;

/**
 * Side panel UI for the loom screen showing saved banner patterns.
 */
public class LoomSidePanel {
    public static final int PANEL_WIDTH = 150;
    public static final int PANEL_HEIGHT = 166;
    private static final int ENTRY_HEIGHT = 24;
    private static final int HEADER_HEIGHT = 14;
    private static final int SAVE_BUTTON_HEIGHT = 16;
    private static final int BUTTON_HEIGHT = 12;
    private static final int PADDING = 4;

    private final LoomScreen screen;
    private final LoomMenu handler;
    private int x;
    private int y;
    private int scrollOffset = 0;

    private AutoCraftStateMachine autoCraft;
    private String importBuffer = "";
    private boolean showingImportPrompt = false;
    private String selectedBannerId = null;
    private Set<String> selectedBannerIds = new HashSet<>();
    private int lastClickedIndex = -1;
    private java.util.Queue<String> craftQueue = new java.util.LinkedList<>();

    public LoomSidePanel(LoomScreen screen, LoomMenu handler, int x, int y) {
        this.screen = screen;
        this.handler = handler;
        this.x = x;
        this.y = y;
        this.autoCraft = new AutoCraftStateMachine(handler);
        LoomAssistantMod.LOGGER.info("LoomSidePanel created at x={}, y={}", x, y);
    }

    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        Font textRenderer = client.font;

        // Draw panel background with gradient effect
        context.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xE8000000);

        // Draw border all the way around
        context.fill(x, y, x + 1, y + PANEL_HEIGHT, 0xFF4169E1); // Left
        context.fill(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFF4169E1); // Right
        context.fill(x, y, x + PANEL_WIDTH, y + 1, 0xFF4169E1); // Top
        context.fill(x, y + PANEL_HEIGHT - 1, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFF4169E1); // Bottom

        // Draw header with subtle background
        context.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT + PADDING + 2, 0xFF1A1A2E);
        context.text(
            textRenderer,
            Component.translatable("loom-assistant.side_panel.saved"),
            x + PADDING,
            y + PADDING,
            0xFF4169E1,
            true);

        // Draw save button
        int saveButtonY = y + HEADER_HEIGHT + PADDING;
        boolean saveHovered = isInSaveButton(mouseX, mouseY);
        int saveButtonColor = saveHovered ? 0xFF66BB6A : 0xFF2E7D32;
        context.fill(
                x + PADDING, saveButtonY, x + PANEL_WIDTH - PADDING, saveButtonY + SAVE_BUTTON_HEIGHT, saveButtonColor);
        String saveText = Component.translatable("loom-assistant.side_panel.save").getString();
        int saveTextWidth = textRenderer.width(saveText);
        context.text(
                textRenderer,
                Component.literal(saveText),
                x + PANEL_WIDTH / 2 - saveTextWidth / 2,
                saveButtonY + 3,
                0xFFFFFFFF,
                true);

        // Draw import/export buttons
        int importButtonY = saveButtonY + SAVE_BUTTON_HEIGHT + PADDING;
        int importButtonWidth = (PANEL_WIDTH - PADDING * 2 - 2) / 2;

        boolean importHovered = isInImportButton(mouseX, mouseY);
        int importButtonColor = importHovered ? 0xFF5C7CFA : 0xFF1E40AF;
        context.fill(
                x + PADDING,
                importButtonY,
                x + PADDING + importButtonWidth,
                importButtonY + BUTTON_HEIGHT,
                importButtonColor);
        String importText = Component.translatable("loom-assistant.side_panel.import").getString();
        int importTextWidth = textRenderer.width(importText);
        int importButtonCenterX = x + PADDING + importButtonWidth / 2;
        context.text(
                textRenderer,
                Component.literal(importText),
                importButtonCenterX - importTextWidth / 2,
                importButtonY + 1,
                0xFFFFFFFF,
                true);

        int exportButtonX = x + PADDING + importButtonWidth + 2;
        boolean exportHovered = isInExportButton(mouseX, mouseY);
        int exportButtonColor = exportHovered ? 0xFF5C7CFA : 0xFF1E40AF;
        context.fill(
                exportButtonX,
                importButtonY,
                x + PANEL_WIDTH - PADDING,
                importButtonY + BUTTON_HEIGHT,
                exportButtonColor);
        String exportText = Component.translatable("loom-assistant.side_panel.export").getString();
        int exportTextWidth = textRenderer.width(exportText);
        int exportButtonCenterX = exportButtonX + importButtonWidth / 2;
        context.text(
                textRenderer,
                Component.literal(exportText),
                exportButtonCenterX - exportTextWidth / 2,
                importButtonY + 1,
                0xFFFFFFFF,
                true);

        // Draw "Browse all banners" button
        int browseButtonY = importButtonY + BUTTON_HEIGHT + PADDING;
        boolean browseHovered = isInBrowseButton(mouseX, mouseY);
        int browseButtonColor = browseHovered ? 0xFF5C7CFA : 0xFF2D3A6E;
        context.fill(
                x + PADDING,
                browseButtonY,
                x + PANEL_WIDTH - PADDING,
                browseButtonY + BUTTON_HEIGHT,
                browseButtonColor);
        String browseText = Component.translatable("loom-assistant.side_panel.browse_all").getString();
        int browseTextWidth = textRenderer.width(browseText);
        context.text(
                textRenderer,
                Component.literal(browseText),
                x + PANEL_WIDTH / 2 - browseTextWidth / 2,
                browseButtonY + 1,
                0xFFCCCCFF,
                true);

        // Draw saved patterns
        List<SavedBanner> banners = BannerStorage.getInstance().getBanners();
        int listStartY = browseButtonY + BUTTON_HEIGHT + PADDING;
        int visibleHeight = PANEL_HEIGHT - (listStartY - y) - PADDING;
        int maxVisible = visibleHeight / ENTRY_HEIGHT;

        // Draw count of saved banners in header
        context.text(
                textRenderer,
            Component.translatable("loom-assistant.side_panel.count", banners.size()),
                x + PANEL_WIDTH - 22,
                y + PADDING,
                0xFFAAAAAA,
                true);

        for (int i = 0; i < maxVisible && i + scrollOffset < banners.size(); i++) {
            SavedBanner banner = banners.get(i + scrollOffset);
            int entryY = listStartY + i * ENTRY_HEIGHT;

            boolean hovered = isInPatternEntry(mouseX, mouseY, entryY);
            boolean isSelected = selectedBannerIds.contains(banner.getId());
            int bgColor = isSelected ? 0xFF4169E1 : (hovered ? 0x60FFFFFF : 0x30FFFFFF);
            context.fill(x + PADDING, entryY, x + PANEL_WIDTH - PADDING, entryY + ENTRY_HEIGHT - 2, bgColor);

            // Draw subtle border for selected items
            if (isSelected) {
                context.outline(x + PADDING, entryY, PANEL_WIDTH - PADDING * 2, ENTRY_HEIGHT - 2, 0xFF5C7CFA);
            }

            // Draw banner preview
            BannerPreviewRenderer.render(context, banner, handler, x + PADDING + 2, entryY + 2, 18);

            // Draw name (truncated to fit available space)
            String name = banner.getDisplayName();
            int maxNameWidth = PANEL_WIDTH - PADDING - 22 - 40; // Account for preview, padding, and buttons
            String displayName = name;
            while (displayName.length() > 0 && textRenderer.width(displayName) > maxNameWidth) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            if (!displayName.equals(name)) {
                displayName = displayName.substring(0, Math.max(0, displayName.length() - 2)) + "..";
            }
            context.text(textRenderer, Component.literal(displayName), x + PADDING + 22, entryY + 8, 0xFFFFFFFF, true);

            // Draw rename button
            int renameX = x + PANEL_WIDTH - PADDING - 28;
            boolean renameHovered =
                    mouseX >= renameX && mouseX < renameX + 12 && mouseY >= entryY + 4 && mouseY < entryY + 18;
            int renameColor = renameHovered ? 0xFF5C7CFA : 0xFFAAAAAA;
                context.text(
                    textRenderer,
                    Component.translatable("loom-assistant.side_panel.rename_icon"),
                    renameX + 2,
                    entryY + 7,
                    renameColor,
                    true);

            // Draw delete button
            int deleteX = x + PANEL_WIDTH - PADDING - 12;
            boolean deleteHovered =
                    mouseX >= deleteX && mouseX < deleteX + 12 && mouseY >= entryY + 4 && mouseY < entryY + 18;
            int deleteColor = deleteHovered ? 0xFFFF6B6B : 0xFFAAAAAA;
                context.text(
                    textRenderer,
                    Component.translatable("loom-assistant.side_panel.delete_icon"),
                    deleteX + 2,
                    entryY + 7,
                    deleteColor,
                    true);
        }

        // Draw craft button if any banners are selected
        if (!selectedBannerIds.isEmpty()) {
            int craftButtonY = y + PANEL_HEIGHT - BUTTON_HEIGHT - PADDING;
            boolean craftHovered = isInCraftButton(mouseX, mouseY);
            int craftButtonColor = craftHovered ? 0xFF66BB6A : 0xFF2E7D32;
            context.fill(
                    x + PADDING,
                    craftButtonY,
                    x + PANEL_WIDTH - PADDING,
                    craftButtonY + BUTTON_HEIGHT,
                    craftButtonColor);
                String craftText = selectedBannerIds.size() > 1
                    ? Component.translatable("loom-assistant.side_panel.weave_count", selectedBannerIds.size())
                        .getString()
                    : Component.translatable("loom-assistant.tooltip.weave").getString();
            int craftTextWidth = textRenderer.width(craftText);
            context.text(
                    textRenderer,
                    Component.literal(craftText),
                    x + PANEL_WIDTH / 2 - craftTextWidth / 2,
                    craftButtonY + 1,
                    0xFFFFFFFF,
                    true);
        }

        // Draw scroll indicators if needed (only if no craft button is showing)
        if (scrollOffset > 0) {
            context.text(
                    textRenderer,
                    Component.translatable("loom-assistant.common.scroll_up_symbol"),
                    x + PANEL_WIDTH / 2 - 2,
                    listStartY - 10,
                    0xFFFFFFFF,
                    true);
        }
        if (scrollOffset + maxVisible < banners.size() && selectedBannerIds.isEmpty()) {
            context.text(
                    textRenderer,
                    Component.translatable("loom-assistant.common.scroll_down_symbol"),
                    x + PANEL_WIDTH / 2 - 2,
                    y + PANEL_HEIGHT - 12,
                    0xFFFFFFFF,
                    true);
        }

        // Draw auto-craft status or error below the panel
        if (autoCraft.isActive()) {
            context.text(
                    textRenderer,
                    Component.translatable("loom-assistant.panel.weaving"),
                    x + PANEL_WIDTH / 2 - 30,
                    y + PANEL_HEIGHT + 4,
                    0xFFFFFF00,
                    true);
        } else if (autoCraft.getState() == AutoCraftStateMachine.AutoCraftState.ERROR) {
            String err = autoCraft.getErrorMessage();
            if (err == null) err = Component.translatable("loom-assistant.common.error").getString();
            int textWidth = textRenderer.width(err);
            context.text(
                    textRenderer,
                    Component.literal(err),
                    x + PANEL_WIDTH / 2 - textWidth / 2,
                    y + PANEL_HEIGHT + 4,
                    0xFFFF5555,
                    true);
        }
    }

    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent) {
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();
        int button = mouseButtonEvent.button();

        // Handle right-click (button 1) for materials screen
        if (button == 1) {
            return handleRightClick(mouseX, mouseY);
        }

        if (button != 0) return false;

        int mx = (int) mouseX;
        int my = (int) mouseY;

        LoomAssistantMod.LOGGER.info(
                "Mouse clicked at x={}, y={}, panel bounds: x={}-{}, y={}-{}",
                mx,
                my,
                x,
                x + PANEL_WIDTH,
                y,
                y + PANEL_HEIGHT);

        // Check save button
        if (isInSaveButton(mx, my)) {
            LoomAssistantMod.LOGGER.info("Save button clicked!");
            saveBannerFromOutput();
            return true;
        }

        // Check import button
        if (isInImportButton(mx, my)) {
            LoomAssistantMod.LOGGER.info("Import button clicked!");
            Minecraft.getInstance().gui.setScreen(new ImportBannerScreen(screen));
            return true;
        }

        // Check export button
        if (isInExportButton(mx, my)) {
            LoomAssistantMod.LOGGER.info("Export button clicked!");
            exportSelectedBanners();
            return true;
        }

        // Check browse button
        if (isInBrowseButton(mx, my)) {
            Minecraft.getInstance().gui.setScreen(new BannerBrowserScreen(screen));
            return true;
        }

        // Check pattern entries
        List<SavedBanner> banners = BannerStorage.getInstance().getBanners();
        int saveButtonY = y + HEADER_HEIGHT + PADDING;
        int importButtonY = saveButtonY + SAVE_BUTTON_HEIGHT + PADDING;
        int browseButtonY = importButtonY + BUTTON_HEIGHT + PADDING;
        int listStartY = browseButtonY + BUTTON_HEIGHT + PADDING;
        int visibleHeight = PANEL_HEIGHT - (listStartY - y) - PADDING;
        int maxVisible = visibleHeight / ENTRY_HEIGHT;

        // Check craft button
        if (!selectedBannerIds.isEmpty() && isInCraftButton(mx, my)) {
            craftQueue.clear();
            craftQueue.addAll(selectedBannerIds);
            if (!craftQueue.isEmpty()) {
                String nextBannerId = craftQueue.poll();
                SavedBanner selected = BannerStorage.getInstance().getBannerById(nextBannerId);
                if (selected != null) {
                    autoCraft.start(selected);
                }
            }
            return true;
        }

        // Get keyboard modifiers
        boolean isCtrlPressed =
                InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                        || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean isShiftPressed =
                InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                        || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);

        for (int i = 0; i < maxVisible && i + scrollOffset < banners.size(); i++) {
            SavedBanner banner = banners.get(i + scrollOffset);
            int entryY = listStartY + i * ENTRY_HEIGHT;

            if (isInPatternEntry(mx, my, entryY)) {
                // Check if rename button clicked
                int renameX = x + PANEL_WIDTH - PADDING - 26;
                if (mx >= renameX && mx < renameX + 10 && my >= entryY + 6 && my < entryY + 16) {
                    Minecraft.getInstance()
                            .gui
                            .setScreen(new RenameBannerScreen(screen, banner.getId(), banner.getName()));
                    return true;
                }

                // Check if delete button clicked
                int deleteX = x + PANEL_WIDTH - PADDING - 10;
                if (mx >= deleteX && mx < deleteX + 10 && my >= entryY + 6 && my < entryY + 16) {
                    BannerStorage.getInstance().removeBanner(banner.getId());
                    selectedBannerIds.remove(banner.getId());
                    return true;
                }

                // Handle selection with modifiers
                if (isCtrlPressed) {
                    // Ctrl+click: toggle individual selection
                    if (selectedBannerIds.contains(banner.getId())) {
                        selectedBannerIds.remove(banner.getId());
                    } else {
                        selectedBannerIds.add(banner.getId());
                    }
                    lastClickedIndex = i + scrollOffset;
                } else if (isShiftPressed && lastClickedIndex >= 0) {
                    // Shift+click: select range
                    int currentIndex = i + scrollOffset;
                    int start = Math.min(lastClickedIndex, currentIndex);
                    int end = Math.max(lastClickedIndex, currentIndex);
                    selectedBannerIds.clear();
                    for (int j = start; j <= end && j < banners.size(); j++) {
                        selectedBannerIds.add(banners.get(j).getId());
                    }
                } else {
                    // Regular click: single selection
                    selectedBannerIds.clear();
                    selectedBannerIds.add(banner.getId());
                    lastClickedIndex = i + scrollOffset;
                }
                return true;
            }
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isInPanel((int) mouseX, (int) mouseY)) {
            return false;
        }

        List<SavedBanner> banners = BannerStorage.getInstance().getBanners();
        int saveButtonY = y + HEADER_HEIGHT + PADDING;
        int importButtonY = saveButtonY + SAVE_BUTTON_HEIGHT + PADDING;
        int browseButtonY = importButtonY + BUTTON_HEIGHT + PADDING;
        int listStartY = browseButtonY + BUTTON_HEIGHT + PADDING;
        int visibleHeight = PANEL_HEIGHT - (listStartY - y) - PADDING;
        int maxVisible = visibleHeight / ENTRY_HEIGHT;
        int maxScroll = Math.max(0, banners.size() - maxVisible);

        scrollOffset -= (int) verticalAmount;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        return true;
    }

    public void tick() {
        autoCraft.tick();

        if (autoCraft.getState() == AutoCraftStateMachine.AutoCraftState.COMPLETE && !craftQueue.isEmpty()) {
            String nextBannerId = craftQueue.poll();
            SavedBanner selected = BannerStorage.getInstance().getBannerById(nextBannerId);
            if (selected != null) {
                autoCraft.start(selected);
            }
        }
    }

    private void saveBannerFromOutput() {
        LoomAssistantMod.LOGGER.info("Attempting to save banner");

        // Check if dye slot (slot 1) and pattern slot (slot 2) are empty
        var dyeStack = handler.getSlot(1).getItem();
        var patternStack = handler.getSlot(2).getItem();
        boolean noDyeOrPattern = dyeStack.isEmpty() && patternStack.isEmpty();

        LoomAssistantMod.LOGGER.info(
                "Dye slot empty: {}, Pattern slot empty: {}", dyeStack.isEmpty(), patternStack.isEmpty());

        // If no dye and no pattern, save from banner input slot (slot 0)
        if (noDyeOrPattern) {
            var bannerStack = handler.getSlot(0).getItem();
            LoomAssistantMod.LOGGER.info("No dye or pattern in loom, saving from banner slot");

            if (bannerStack.isEmpty()) {
                LoomAssistantMod.LOGGER.info("Banner slot is empty, nothing to save");
                return;
            }

            SavedBanner banner = BannerPreviewRenderer.extractBannerData(bannerStack);
            LoomAssistantMod.LOGGER.info("Extracted banner from input: {}", banner);

            if (banner != null) {
                BannerStorage.getInstance().addBanner(banner);
                LoomAssistantMod.LOGGER.info(
                        "Banner saved! Total banners: {}",
                        BannerStorage.getInstance().getBanners().size());
            } else {
                LoomAssistantMod.LOGGER.info("Failed to extract banner data");
            }
            return;
        }

        // Otherwise, save from output slot (slot 3)
        var outputStack = handler.getSlot(3).getItem();
        LoomAssistantMod.LOGGER.info("Output slot stack: {}, isEmpty: {}", outputStack, outputStack.isEmpty());

        if (outputStack.isEmpty()) {
            LoomAssistantMod.LOGGER.info("Output slot is empty, nothing to save");
            return;
        }

        SavedBanner banner = BannerPreviewRenderer.extractBannerData(outputStack);
        LoomAssistantMod.LOGGER.info("Extracted banner: {}", banner);

        if (banner != null) {
            BannerStorage.getInstance().addBanner(banner);
            LoomAssistantMod.LOGGER.info(
                    "Banner saved! Total banners: {}",
                    BannerStorage.getInstance().getBanners().size());
        } else {
            LoomAssistantMod.LOGGER.info("Failed to extract banner data");
        }
    }

    private boolean isInPanel(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + PANEL_WIDTH && mouseY >= y && mouseY < y + PANEL_HEIGHT;
    }

    private boolean isInSaveButton(int mouseX, int mouseY) {
        int saveButtonY = y + HEADER_HEIGHT + PADDING;
        return mouseX >= x + PADDING
                && mouseX < x + PANEL_WIDTH - PADDING
                && mouseY >= saveButtonY
                && mouseY < saveButtonY + SAVE_BUTTON_HEIGHT;
    }

    private boolean isInImportButton(int mouseX, int mouseY) {
        int saveButtonY = y + HEADER_HEIGHT + PADDING;
        int importButtonY = saveButtonY + SAVE_BUTTON_HEIGHT + PADDING;
        return mouseX >= x + PADDING
                && mouseX < x + PANEL_WIDTH / 2 - 2
                && mouseY >= importButtonY
                && mouseY < importButtonY + BUTTON_HEIGHT;
    }

    private boolean isInExportButton(int mouseX, int mouseY) {
        int saveButtonY = y + HEADER_HEIGHT + PADDING;
        int importButtonY = saveButtonY + SAVE_BUTTON_HEIGHT + PADDING;
        int exportButtonX = x + PANEL_WIDTH / 2 + 2;
        return mouseX >= exportButtonX
                && mouseX < x + PANEL_WIDTH - PADDING
                && mouseY >= importButtonY
                && mouseY < importButtonY + BUTTON_HEIGHT;
    }

    private boolean isInBrowseButton(int mouseX, int mouseY) {
        int saveButtonY = y + HEADER_HEIGHT + PADDING;
        int importButtonY = saveButtonY + SAVE_BUTTON_HEIGHT + PADDING;
        int browseButtonY = importButtonY + BUTTON_HEIGHT + PADDING;
        return mouseX >= x + PADDING
                && mouseX < x + PANEL_WIDTH - PADDING
                && mouseY >= browseButtonY
                && mouseY < browseButtonY + BUTTON_HEIGHT;
    }

    private boolean isInCraftButton(int mouseX, int mouseY) {
        int craftButtonY = y + PANEL_HEIGHT - BUTTON_HEIGHT - PADDING;
        return mouseX >= x + PADDING
                && mouseX < x + PANEL_WIDTH - PADDING
                && mouseY >= craftButtonY
                && mouseY < craftButtonY + BUTTON_HEIGHT;
    }

    private boolean isInPatternEntry(int mouseX, int mouseY, int entryY) {
        return mouseX >= x + PADDING
                && mouseX < x + PANEL_WIDTH - PADDING
                && mouseY >= entryY
                && mouseY < entryY + ENTRY_HEIGHT - 2;
    }

    private void exportSelectedBanners() {
        if (selectedBannerIds.isEmpty()) {
            LoomAssistantMod.LOGGER.info("No banners selected to export");
            return;
        }

        if (selectedBannerIds.size() == 1) {
            // Single banner: export as JSON
            String bannerId = selectedBannerIds.iterator().next();
            String json = BannerStorage.getInstance().exportBannerToJson(bannerId);

            if (json != null) {
                Minecraft.getInstance().keyboardHandler.setClipboard(json);
                LoomAssistantMod.LOGGER.info("Banner exported to clipboard");
            }
        } else {
            // Multiple banners: export as JSON array
            StringBuilder jsonArray = new StringBuilder("[");
            boolean first = true;
            for (String bannerId : selectedBannerIds) {
                String json = BannerStorage.getInstance().exportBannerToJson(bannerId);
                if (json != null) {
                    if (!first) {
                        jsonArray.append(",");
                    }
                    jsonArray.append(json);
                    first = false;
                }
            }
            jsonArray.append("]");

            Minecraft.getInstance().keyboardHandler.setClipboard(jsonArray.toString());
            LoomAssistantMod.LOGGER.info("Exported {} banners to clipboard", selectedBannerIds.size());
        }
    }

    public void importBannerFromClipboard(String jsonString) {
        SavedBanner imported = BannerStorage.getInstance().importBannerFromJson(jsonString);
        if (imported != null) {
            LoomAssistantMod.LOGGER.info("Banner imported successfully: {}", imported.getId());
        } else {
            LoomAssistantMod.LOGGER.error("Failed to import banner from JSON");
        }
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public AutoCraftStateMachine getAutoCraft() {
        return autoCraft;
    }

    private boolean handleRightClick(double mouseX, double mouseY) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        // Check pattern entries for right-click
        List<SavedBanner> banners = BannerStorage.getInstance().getBanners();
        int saveButtonY = y + HEADER_HEIGHT + PADDING;
        int importButtonY = saveButtonY + SAVE_BUTTON_HEIGHT + PADDING;
        int browseButtonY = importButtonY + BUTTON_HEIGHT + PADDING;
        int listStartY = browseButtonY + BUTTON_HEIGHT + PADDING;
        int visibleHeight = PANEL_HEIGHT - (listStartY - y) - PADDING;
        int maxVisible = visibleHeight / ENTRY_HEIGHT;

        for (int i = 0; i < maxVisible && i + scrollOffset < banners.size(); i++) {
            SavedBanner banner = banners.get(i + scrollOffset);
            int entryY = listStartY + i * ENTRY_HEIGHT;

            if (isInPatternEntry(mx, my, entryY)) {
                // Open materials screen
                Minecraft.getInstance().gui.setScreen(new BannerMaterialsScreen(screen, banner));
                return true;
            }
        }

        return false;
    }
}
