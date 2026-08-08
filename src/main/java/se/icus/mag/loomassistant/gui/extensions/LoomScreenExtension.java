/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.extensions;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.loomassistant.gui.panel.LoomRecipePanel;
import se.icus.mag.loomassistant.gui.screens.BannerRecipeImportExportScreen;
import se.icus.mag.loomassistant.gui.screens.BannerSaveEditScreen;
import se.icus.mag.loomassistant.gui.screens.colorswitch.BannerColorSwitchScreen;
import se.icus.mag.loomassistant.gui.support.LoomUiStateStore;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeJsonConverter;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;
import se.icus.mag.loomassistant.weaving.BannerCraftabilityModel;

/**
 * Contains all extension logic for LoomScreen. LoomScreenMixin holds only minimal mixin hooks that delegate here.
 */
public class LoomScreenExtension {
    private static final int CONTENT_X_SHIFT = 3;
    private static final int BG_LEFT_PADDING = 19;

    private static final int PANEL_TAB_LEFT_OVERHANG = 32;
    private static final int LEFT_STRIP_BUTTON_X = 3;
    private static final int LEFT_STRIP_RECIPE_Y = 5;
    private static final int LEFT_STRIP_ACTIVE_SLOT_Y = LEFT_STRIP_RECIPE_Y + 42;
    private static final int LEFT_STRIP_CRAFT_Y = LEFT_STRIP_ACTIVE_SLOT_Y + 22;
    private static final int LEFT_STRIP_SAVE_EDIT_Y = LEFT_STRIP_CRAFT_Y + 20;
    private static final int LEFT_STRIP_COLOR_Y = LEFT_STRIP_SAVE_EDIT_Y + 20;
    private static final int LEFT_STRIP_IMPORT_EXPORT_BOTTOM_MARGIN = 6;
    private static final int ACTIVE_SLOT_W = 20;
    private static final int ACTIVE_SLOT_H = 18;
    private static final int CUSTOM_BG_WIDTH = 278;
    private static final int CUSTOM_BG_HEIGHT = 256;

    private static final Identifier BG_LOCATION =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/loom-gui.png");

    private static final Identifier RECIPE_WEAVE_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-weave.png");

    private static final Identifier RECIPE_ADD_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-add.png");

    private static final Identifier RECIPE_EDIT_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-edit.png");

    private static final Identifier RECIPE_SWAP_COLORS_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-swap-colors.png");

    private static final Identifier RECIPE_IMPORT_EXPORT_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-import-export.png");

    private static final SoundEvent ACTIVE_SLOT_SET_SOUND = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath("loom-assistant", "ui.active_slot_set"));

    private static final SoundEvent ACTIVE_SLOT_CLEAR_SOUND = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath("loom-assistant", "ui.active_slot_clear"));

    private static final Component SAVE_TOOLTIP = Component.translatable("loom-assistant.tooltip.add_recipe");
    private static final Component WEAVE_TOOLTIP = Component.translatable("loom-assistant.tooltip.weave");
    private static final Component EDIT_TOOLTIP = Component.translatable("loom-assistant.tooltip.edit_recipe");
    private static final Component IMPORT_EXPORT_TOOLTIP =
            Component.translatable("loom-assistant.tooltip.import_export_recipes");
    private static final Component CHANGE_COLORS_TOOLTIP =
            Component.translatable("loom-assistant.tooltip.replace_colors");

    // ── screen reference ─────────────────────────────────────────────────────
    private final LoomScreen screen;

    // ── state ─────────────────────────────────────────────────────────────────
    private boolean panelOpen = false;
    private LoomRecipePanel panel;
    private ImageButton recipeBookButton;
    private Button saveButton;
    private Button craftButton;
    private Button colorButton;
    private Button importExportButton;
    private ItemStack activeBannerStack = ItemStack.EMPTY;
    private ItemStack pendingActiveBannerStack = ItemStack.EMPTY;
    private final Map<DyeColor, DyeColor> persistentDyeMap = new EnumMap<>(DyeColor.class);
    private boolean persistentDyeSwitchEnabled = false;
    private String lastPersistedActiveBannerJson = null;
    private final Map<DyeColor, DyeColor> lastPersistedDyeMap = new EnumMap<>(DyeColor.class);
    private boolean lastPersistedDyeEnabled = false;
    private BannerCraftabilityModel craftabilityProbe;

    public LoomScreenExtension(LoomScreen screen) {
        this.screen = screen;
    }

    // ── public accessors used by LoomPanelHost / LoomActiveBannerHost impls ──

    public LoomRecipePanel getPanel() {
        return panel;
    }

    public void setPendingActiveBannerStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            this.pendingActiveBannerStack = ItemStack.EMPTY;
            this.activeBannerStack = ItemStack.EMPTY;
            LoomUiStateStore.setPersistedActiveBannerStack(screen.minecraft, ItemStack.EMPTY);
            this.lastPersistedActiveBannerJson = null;
            return;
        }
        this.pendingActiveBannerStack = stack.copyWithCount(1);
        this.activeBannerStack = this.pendingActiveBannerStack.copy();
        LoomUiStateStore.setPersistedActiveBannerStack(screen.minecraft, this.activeBannerStack);
        BannerRecipe recipe = BannerRecipe.fromItem(this.activeBannerStack);
        if (recipe == null) {
            this.lastPersistedActiveBannerJson = null;
        } else {
            BannerRecipeJsonConverter converter = new BannerRecipeJsonConverter();
            this.lastPersistedActiveBannerJson = converter.fromRecipe(recipe);
        }
    }

    public void setPersistentDyeSwitchState(boolean enabled, Map<DyeColor, DyeColor> replacements) {
        this.persistentDyeSwitchEnabled = enabled;
        this.persistentDyeMap.clear();
        if (replacements != null) {
            for (Map.Entry<DyeColor, DyeColor> entry : replacements.entrySet()) {
                DyeColor src = entry.getKey();
                DyeColor dst = entry.getValue();
                if (src != null && dst != null && src != dst) {
                    this.persistentDyeMap.put(src, dst);
                }
            }
        }

        LoomUiStateStore.setPersistentDyeState(
                screen.minecraft, this.persistentDyeSwitchEnabled, this.persistentDyeMap);

        this.lastPersistedDyeEnabled = this.persistentDyeSwitchEnabled;
        this.lastPersistedDyeMap.clear();
        this.lastPersistedDyeMap.putAll(this.persistentDyeMap);

        if (this.panel != null) {
            this.panel.restorePersistentDyeSwitchState(
                    this.persistentDyeSwitchEnabled, Map.copyOf(this.persistentDyeMap));
        }
    }

    // ── mixin hook handlers ───────────────────────────────────────────────────

    public void onInit() {
        this.panelOpen = LoomUiStateStore.isLoomPanelOpen(screen.minecraft);
        screen.leftPos = panelOpen ? getOpenLeftPos() : getClosedLeftPos();
        this.craftabilityProbe = new BannerCraftabilityModel(screen.menu);

        ModelPart flagPart = screen.minecraft.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
        BannerFlagModel previewFlag = new BannerFlagModel(flagPart);

        LoomUiStateStore.PersistentDyeState persistentDyeState =
                LoomUiStateStore.getPersistentDyeState(screen.minecraft);
        this.persistentDyeSwitchEnabled = persistentDyeState.enabled();
        this.persistentDyeMap.clear();
        this.persistentDyeMap.putAll(persistentDyeState.replacements());
        this.lastPersistedDyeEnabled = this.persistentDyeSwitchEnabled;
        this.lastPersistedDyeMap.clear();
        this.lastPersistedDyeMap.putAll(this.persistentDyeMap);

        this.pendingActiveBannerStack = LoomUiStateStore.getPersistedActiveBannerStack(screen.minecraft);
        if (this.pendingActiveBannerStack.isEmpty()) {
            this.activeBannerStack = ItemStack.EMPTY;
            this.lastPersistedActiveBannerJson = null;
        } else {
            this.activeBannerStack = this.pendingActiveBannerStack.copy();
            BannerRecipe persistedRecipe = BannerRecipe.fromItem(this.activeBannerStack);
            if (persistedRecipe == null) {
                this.lastPersistedActiveBannerJson = null;
            } else {
                BannerRecipeJsonConverter converter = new BannerRecipeJsonConverter();
                this.lastPersistedActiveBannerJson = converter.fromRecipe(persistedRecipe);
            }
        }

        this.recipeBookButton = screen.addRenderableWidget(new ImageButton(
                getLeftStripButtonX(),
                screen.topPos + LEFT_STRIP_RECIPE_Y,
                20,
                18,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES,
                button -> {
                    panelOpen = !panelOpen;
                    LoomUiStateStore.setLoomPanelOpen(screen.minecraft, panelOpen);
                    screen.leftPos = panelOpen ? getOpenLeftPos() : getClosedLeftPos();
                    refreshControls();
                    refreshPanel();
                }));

        this.saveButton = screen.addRenderableWidget(new SaveEditButton());

        this.craftButton = screen.addRenderableWidget(new WeaveButton());

        this.importExportButton = screen.addRenderableWidget(new ImportExportButton());

        this.colorButton = screen.addRenderableWidget(new ReplaceColorButton());
        this.colorButton.setOverrideRenderHighlightedSprite(
                () -> (panel != null && panel.isPersistentDyeSwitchEnabled()) || this.colorButton.isHoveredOrFocused());

        this.craftButton.active = false;
        this.saveButton.active = false;
        this.saveButton.visible = true;
        this.colorButton.active = false;
        this.colorButton.visible = true;

        refreshPanel();
    }

    public void onExtractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        boolean hasActiveBanner = false;
        boolean canCraftActiveBanner = false;
        String craftDisabledMessage = null;
        if (panel != null) {
            if (!pendingActiveBannerStack.isEmpty()) {
                panel.setActiveBannerFromItemStack(pendingActiveBannerStack);
                pendingActiveBannerStack = ItemStack.EMPTY;
            } else if (panel.getActiveBannerStack().isEmpty() && !activeBannerStack.isEmpty()) {
                panel.setActiveBannerFromItemStack(activeBannerStack);
            }
            panel.tick();
            persistentDyeSwitchEnabled = panel.isPersistentDyeSwitchEnabled();
            persistentDyeMap.clear();
            persistentDyeMap.putAll(panel.getPersistentDyeReplacementMapCopy());
            activeBannerStack = panel.getActiveBannerStack();
            syncPerWorldUiState();
            hasActiveBanner = !activeBannerStack.isEmpty();
            canCraftActiveBanner = panel.isActiveBannerCraftable();
            if (!canCraftActiveBanner) {
                craftDisabledMessage = panel.getActiveBannerMissingMaterialMessage();
            }
            if (this.saveButton != null) {
                this.saveButton.active = panel.hasActiveBanner();
                this.saveButton.visible = true;
            }
            if (this.colorButton != null) {
                this.colorButton.active = panel.hasActiveBanner();
                this.colorButton.visible = true;
            }
            panel.render(context, mouseX, mouseY, delta);
        } else if (this.saveButton != null) {
            this.saveButton.active = false;
            this.saveButton.visible = true;
            hasActiveBanner = !activeBannerStack.isEmpty();
            if (hasActiveBanner && craftabilityProbe != null) {
                Minecraft mc = screen.minecraft;
                if (mc != null && mc.player != null && mc.player.hasInfiniteMaterials()) {
                    canCraftActiveBanner = true;
                    craftDisabledMessage = null;
                } else {
                    BannerRecipe activeBanner = BannerRecipe.fromItem(activeBannerStack);
                    if (activeBanner != null) {
                        canCraftActiveBanner = craftabilityProbe.canCraft(activeBanner);
                        if (!canCraftActiveBanner) {
                            craftDisabledMessage = buildMissingMaterialsMessage(activeBanner, craftabilityProbe);
                        }
                    }
                }
            }
            this.saveButton.active = hasActiveBanner;
            if (this.colorButton != null) {
                this.colorButton.active = false;
                this.colorButton.visible = true;
            }
        }

        if (this.craftButton != null) {
            this.craftButton.active = hasActiveBanner && canCraftActiveBanner;
        }

        if (this.saveButton != null && isMouseOverWidget(this.saveButton, mouseX, mouseY)) {
            Component tooltip = shouldUseEditIconOnSaveButton() ? EDIT_TOOLTIP : SAVE_TOOLTIP;
            setSingleLineTooltip(context, tooltip, mouseX, mouseY);
        }
        if (this.craftButton != null && isMouseOverWidget(this.craftButton, mouseX, mouseY)) {
            List<Component> tooltipLines = new ArrayList<>();
            tooltipLines.add(WEAVE_TOOLTIP);
            if (!this.craftButton.active && hasActiveBanner) {
                String reason = craftDisabledMessage;
                if (reason != null && !reason.isBlank()) {
                    tooltipLines.add(Component.empty());
                    tooltipLines.addAll(reason.lines().map(Component::literal).toList());
                }
            }
            context.setTooltipForNextFrame(screen.font, tooltipLines, Optional.empty(), mouseX, mouseY);
        }
        if (this.importExportButton != null && isMouseOverWidget(this.importExportButton, mouseX, mouseY)) {
            setSingleLineTooltip(context, IMPORT_EXPORT_TOOLTIP, mouseX, mouseY);
        }
        if (this.colorButton != null && isMouseOverWidget(this.colorButton, mouseX, mouseY)) {
            setSingleLineTooltip(context, CHANGE_COLORS_TOOLTIP, mouseX, mouseY);
        }

        if (!activeBannerStack.isEmpty()) {
            context.fakeItem(
                    activeBannerStack, getLeftStripButtonX() + 2, screen.topPos + LEFT_STRIP_ACTIVE_SLOT_Y + 1);

            if (panel != null) {
                boolean survivalNotWeavable = !panel.isActiveBannerWeavable()
                        && screen.minecraft.player != null
                        && !screen.minecraft.player.hasInfiniteMaterials();
                if (!survivalNotWeavable) {
                    int progress = panel.detectCraftingProgress();
                    int total = panel.getActiveBannerLayerCount();
                    if (progress >= 0 && total > 0) {
                        String badge = (progress + 1) + "/" + total;
                        int badgeW = screen.font.width(badge);
                        int badgeX = getLeftStripButtonX() + (20 - badgeW) / 2;
                        int badgeY = screen.topPos + LEFT_STRIP_RECIPE_Y + 22;
                        context.text(screen.font, badge, badgeX, badgeY, 0xFFFFFFFF, true);
                    }
                }
            }

            if (isInActiveSlot(mouseX, mouseY)) {
                if (panel != null) {
                    panel.setActiveBannerTooltip(context, mouseX, mouseY);
                } else {
                    BannerRecipe activeBanner = BannerRecipe.fromItem(activeBannerStack);
                    LoomRecipePanel.setBannerTooltip(context, activeBanner, mouseX, mouseY);
                }
            }
        }

        if (panel != null) {
            extractGuideRender(context);
        }
    }

    public void extractGuideRender(GuiGraphicsExtractor context) {
        Minecraft mc = screen.minecraft;
        boolean survivalNotWeavable =
                !panel.isActiveBannerWeavable() && mc.player != null && !mc.player.hasInfiniteMaterials();
        if (!survivalNotWeavable) {
            int progress = panel.detectCraftingProgress();
            if (progress >= 0) {
                if (screen.menu.getResultSlot().getItem().isEmpty()) {
                    renderNextStepHint(context, progress);
                }
                renderOutputSlotBorder(context, progress);
            }
        } else if (screen.menu.getResultSlot().getItem().isEmpty()) {
            renderUncraftablePreview(context);
        }
    }

    public void onMouseClicked(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        if (mouseButtonEvent.button() == 1 && isShiftHeld()) {
            int mx = (int) mouseButtonEvent.x();
            int my = (int) mouseButtonEvent.y();
            int leftPos = screen.leftPos;
            int topPos = screen.topPos;
            for (Slot slot : screen.menu.slots) {
                if (mx >= leftPos + slot.x
                        && mx < leftPos + slot.x + 16
                        && my >= topPos + slot.y
                        && my < topPos + slot.y + 16) {
                    ItemStack stack = slot.getItem();
                    if (!stack.isEmpty() && stack.getItem() instanceof BannerItem) {
                        if (panel != null) {
                            if (panel.setActiveBannerFromItemStack(stack)) {
                                activeBannerStack = panel.getActiveBannerStack();
                            }
                        } else {
                            pendingActiveBannerStack = stack.copyWithCount(1);
                            activeBannerStack = pendingActiveBannerStack.copy();
                        }
                        playActiveSlotSetSound();
                        cir.setReturnValue(true);
                        return;
                    }
                    break;
                }
            }
        }

        if (isInActiveSlot((int) mouseButtonEvent.x(), (int) mouseButtonEvent.y())) {
            ItemStack carried = screen.menu.getCarried();
            if (!carried.isEmpty()) {
                if (panel != null) {
                    if (panel.setActiveBannerFromItemStack(carried)) {
                        activeBannerStack = panel.getActiveBannerStack();
                        playActiveSlotSetSound();
                    }
                } else {
                    pendingActiveBannerStack = carried.copyWithCount(1);
                    activeBannerStack = pendingActiveBannerStack.copy();
                    playActiveSlotSetSound();
                }
                cir.setReturnValue(true);
                return;
            }

            if (!activeBannerStack.isEmpty()) {
                if (panel != null) {
                    panel.clearSelectedBanner();
                }
                pendingActiveBannerStack = ItemStack.EMPTY;
                activeBannerStack = ItemStack.EMPTY;
                playActiveSlotClearSound();
                cir.setReturnValue(true);
                return;
            }
        }

        if (panel != null && panel.mouseClicked(mouseButtonEvent)) {
            cir.setReturnValue(true);
        }
    }

    public void onMouseReleased(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        if (mouseButtonEvent.button() != 0) return;

        if (isInActiveSlot((int) mouseButtonEvent.x(), (int) mouseButtonEvent.y())
                && !screen.menu.getCarried().isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    public void onMouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount,
            CallbackInfoReturnable<Boolean> cir) {
        if (panel != null && panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    // ── layout helpers ────────────────────────────────────────────────────────

    private void refreshControls() {
        if (this.recipeBookButton != null) {
            this.recipeBookButton.setPosition(getLeftStripButtonX(), screen.topPos + LEFT_STRIP_RECIPE_Y);
        }
        if (this.saveButton != null) {
            this.saveButton.setPosition(getLeftStripButtonX(), screen.topPos + LEFT_STRIP_SAVE_EDIT_Y);
        }
        if (this.craftButton != null) {
            this.craftButton.setPosition(getLeftStripButtonX(), screen.topPos + LEFT_STRIP_CRAFT_Y);
        }
        if (this.colorButton != null) {
            this.colorButton.setPosition(getLeftStripButtonX(), screen.topPos + LEFT_STRIP_COLOR_Y);
        }
        if (this.importExportButton != null) {
            this.importExportButton.setPosition(getLeftStripButtonX(), getImportExportButtonY());
        }
    }

    private int getImportExportButtonY() {
        return screen.topPos + screen.imageHeight - 18 - LEFT_STRIP_IMPORT_EXPORT_BOTTOM_MARGIN;
    }

    private int getClosedLeftPos() {
        int guiExtraLeft = BG_LEFT_PADDING + CONTENT_X_SHIFT;
        int visualGuiWidth = screen.imageWidth + guiExtraLeft;
        int visualLeft = (screen.width - visualGuiWidth) / 2;
        return visualLeft + guiExtraLeft;
    }

    private int getOpenLeftPos() {
        int leftExtensionWithoutTabs = LoomRecipePanel.PANEL_WIDTH + 5 + BG_LEFT_PADDING;
        int centeredAreaWidth = screen.imageWidth + leftExtensionWithoutTabs;
        int centeredAreaLeft = (screen.width - centeredAreaWidth) / 2;
        int leftPos = centeredAreaLeft + leftExtensionWithoutTabs;

        int panelLeft = leftPos - leftExtensionWithoutTabs;
        int tabLeft = panelLeft - PANEL_TAB_LEFT_OVERHANG;
        if (tabLeft < 0) {
            leftPos -= tabLeft;
        }
        return leftPos;
    }

    private int getPanelX() {
        return screen.leftPos - LoomRecipePanel.PANEL_WIDTH - 5 - BG_LEFT_PADDING;
    }

    private void refreshPanel() {
        this.panel = panelOpen ? new LoomRecipePanel(this, screen, screen.menu, getPanelX(), screen.topPos) : null;
        if (this.panel != null) {
            this.panel.restorePersistentDyeSwitchState(persistentDyeSwitchEnabled, Map.copyOf(persistentDyeMap));
        }
    }

    private int getLeftStripButtonX() {
        return screen.leftPos - BG_LEFT_PADDING + LEFT_STRIP_BUTTON_X;
    }

    private boolean isInActiveSlot(int mouseX, int mouseY) {
        int slotX = getLeftStripButtonX();
        int slotY = screen.topPos + LEFT_STRIP_ACTIVE_SLOT_Y;
        return mouseX >= slotX && mouseX < slotX + ACTIVE_SLOT_W && mouseY >= slotY && mouseY < slotY + ACTIVE_SLOT_H;
    }

    // ── rendering helpers ─────────────────────────────────────────────────────

    private static boolean isMouseOverWidget(AbstractWidget widget, int mouseX, int mouseY) {
        return mouseX >= widget.getX()
                && mouseX < widget.getX() + widget.getWidth()
                && mouseY >= widget.getY()
                && mouseY < widget.getY() + widget.getHeight();
    }

    private void setSingleLineTooltip(GuiGraphicsExtractor context, Component text, int mouseX, int mouseY) {
        context.setTooltipForNextFrame(screen.font, List.of(text), Optional.empty(), mouseX, mouseY);
    }

    private boolean showEditOnSaveButton() {
        return panel != null && panel.hasActiveBanner() && !panel.isActiveBannerSavable();
    }

    private boolean shouldUseEditIconOnSaveButton() {
        if (panel == null || !panel.hasActiveBanner()) return true;

        return showEditOnSaveButton();
    }

    private void drawWeaveWithSlash(GuiGraphicsExtractor ctx, int ix, int iy, int size) {
        ctx.blit(RenderPipelines.GUI_TEXTURED, RECIPE_WEAVE_ICON, ix, iy, 0f, 0f, size, size, size, size);
        for (int row = 0; row < size; row++) {
            int xOff = size - 1 - row;
            ctx.fill(ix + xOff - 1, iy + row, ix + xOff + 1, iy + row + 1, 0xFFFF2222);
        }
    }

    private void renderUncraftableBadge(GuiGraphicsExtractor ctx) {
        int iconSize = 12;
        int bx = getLeftStripButtonX() + (20 - iconSize) / 2;
        int by = screen.topPos + LEFT_STRIP_RECIPE_Y + 23;
        drawWeaveWithSlash(ctx, bx, by, iconSize);
    }

    private void renderUncraftablePreview(GuiGraphicsExtractor ctx) {
        int iconSize = 16;
        int px = screen.leftPos + 141 + (20 - iconSize) / 2;
        int py = screen.topPos + 8 + (40 - iconSize) / 2 + 6;
        drawWeaveWithSlash(ctx, px, py, iconSize);
    }

    private void renderNextStepHint(GuiGraphicsExtractor context, int nextLayerIndex) {
        if (panel == null) return;
        BannerRecipe recipe = BannerRecipe.fromItem(activeBannerStack);
        if (recipe == null || nextLayerIndex >= recipe.getLayers().size()) return;

        BannerRecipeLayer layer = recipe.getLayers().get(nextLayerIndex);
        int px = screen.leftPos + 141;
        int py = screen.topPos + 8;

        ItemStack dye = new ItemStack(BannerRecipe.getDyeItem(layer.getDyeColorEnum()));
        context.fakeItem(dye, px + 2, py + 2);

        try {
            Identifier patId = Identifier.tryParse(layer.patternId());
            if (patId != null && screen.minecraft.level != null) {
                Optional<Registry<BannerPattern>> reg =
                        screen.minecraft.level.registryAccess().lookup(Registries.BANNER_PATTERN);
                if (reg.isPresent()) {
                    Optional<Holder.Reference<BannerPattern>> entry = reg.get().get(patId);
                    if (entry.isPresent()) {
                        Holder<BannerPattern> holder = entry.get();
                        TextureAtlasSprite sprite = context.getSprite(Sheets.getBannerSprite(holder));
                        float u0 = sprite.getU0();
                        float u1 = u0 + (sprite.getU1() - u0) * 21.0F / 64.0F;
                        float vSpan = sprite.getV1() - sprite.getV0();
                        float v0 = sprite.getV0() + vSpan / 64.0F;
                        float v1 = v0 + vSpan * 40.0F / 64.0F;
                        context.pose().pushMatrix();
                        context.pose().translate(px + 3, py + 22);
                        context.fill(0, 0, 7, 14, DyeColor.GRAY.getTextureDiffuseColor());
                        context.blit(sprite.atlasLocation(), 0, 0, 7, 14, u0, u1, v0, v1);
                        context.pose().popMatrix();
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void renderOutputSlotBorder(GuiGraphicsExtractor context, int progress) {
        int rx = screen.leftPos + 143;
        int ry = screen.topPos + 57;
        ItemStack result = screen.menu.getResultSlot().getItem();
        if (result.isEmpty()) return;

        boolean correct = WeavingGuide.resultMatchesExpected(activeBannerStack, result, progress);
        int color = correct ? 0xFF44FF44 : 0xFFFF4444;
        context.fill(rx - 1, ry - 1, rx + 17, ry, color);
        context.fill(rx - 1, ry + 16, rx + 17, ry + 17, color);
        context.fill(rx - 1, ry, rx, ry + 16, color);
        context.fill(rx + 16, ry, rx + 17, ry + 16, color);
    }

    // ── sound & misc ──────────────────────────────────────────────────────────

    private void syncPerWorldUiState() {
        BannerRecipe activeRecipe = BannerRecipe.fromItem(this.activeBannerStack);
        String currentActiveJson;
        if (activeRecipe == null) {
            currentActiveJson = null;
        } else {
            BannerRecipeJsonConverter converter = new BannerRecipeJsonConverter();
            currentActiveJson = converter.fromRecipe(activeRecipe);
        }
        boolean activeChanged = !Objects.equals(currentActiveJson, this.lastPersistedActiveBannerJson);
        if (activeChanged) {
            LoomUiStateStore.setPersistedActiveBannerStack(screen.minecraft, this.activeBannerStack);
            this.lastPersistedActiveBannerJson = currentActiveJson;
        }

        boolean dyeChanged = this.lastPersistedDyeEnabled != this.persistentDyeSwitchEnabled
                || !this.lastPersistedDyeMap.equals(this.persistentDyeMap);
        if (dyeChanged) {
            LoomUiStateStore.setPersistentDyeState(
                    screen.minecraft, this.persistentDyeSwitchEnabled, this.persistentDyeMap);
            this.lastPersistedDyeEnabled = this.persistentDyeSwitchEnabled;
            this.lastPersistedDyeMap.clear();
            this.lastPersistedDyeMap.putAll(this.persistentDyeMap);
        }
    }

    private static String buildMissingMaterialsMessage(BannerRecipe banner, BannerCraftabilityModel autoCraft) {
        List<String> missingMaterials = autoCraft.getMissingMaterialDescriptions(banner);
        if (missingMaterials.isEmpty()) return null;

        return Component.translatable("loom-assistant.active.missing_header").getString()
                + "\n"
                + String.join("\n", missingMaterials);
    }

    private boolean isShiftHeld() {
        Window win = screen.minecraft.getWindow();
        return InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void playActiveSlotSetSound() {
        Minecraft mc = screen.minecraft;
        if (mc != null && mc.player != null) {
            mc.player
                    .level()
                    .playLocalSound(
                            mc.player.getX(),
                            mc.player.getY(),
                            mc.player.getZ(),
                            ACTIVE_SLOT_SET_SOUND,
                            SoundSource.PLAYERS,
                            0.42F,
                            1.0F,
                            false);
        }
    }

    private void playActiveSlotClearSound() {
        Minecraft mc = screen.minecraft;
        if (mc != null && mc.player != null) {
            mc.player
                    .level()
                    .playLocalSound(
                            mc.player.getX(),
                            mc.player.getY(),
                            mc.player.getZ(),
                            ACTIVE_SLOT_CLEAR_SOUND,
                            SoundSource.PLAYERS,
                            0.42F,
                            1.0F,
                            false);
        }
    }

    /**
     * Called by the mixin @Redirect to draw the custom background texture.
     */
    public void drawCustomBackground(
            GuiGraphicsExtractor graphics,
            RenderPipeline renderPipeline,
            Identifier originalTexture,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int textureWidth,
            int textureHeight) {
        graphics.blit(
                renderPipeline,
                BG_LOCATION,
                x - BG_LEFT_PADDING - CONTENT_X_SHIFT,
                y,
                u,
                v,
                width + BG_LEFT_PADDING + CONTENT_X_SHIFT,
                height,
                CUSTOM_BG_WIDTH,
                CUSTOM_BG_HEIGHT);
    }

    private class ReplaceColorButton extends Button.Plain {
        protected ReplaceColorButton() {
            super(
                    LoomScreenExtension.this.getLeftStripButtonX(),
                    LoomScreenExtension.this.screen.topPos + LoomScreenExtension.LEFT_STRIP_COLOR_Y,
                    20,
                    18,
                    Component.empty(),
                    button -> {
                        if (LoomScreenExtension.this.panel == null || !LoomScreenExtension.this.panel.hasActiveBanner())
                            return;

                        if (LoomScreenExtension.this.panel.isPersistentDyeSwitchEnabled()) {
                            LoomScreenExtension.this.panel.disablePersistentDyeSwitchAndReload();
                        } else {
                            LoomScreenExtension.this.screen.minecraft.gui.setScreen(new BannerColorSwitchScreen(
                                    LoomScreenExtension.this.screen, LoomScreenExtension.this.panel));
                        }
                    },
                    Supplier::get);
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            this.extractDefaultSprite(graphics);
            boolean persistent = panel != null && panel.isPersistentDyeSwitchEnabled();
            int iconOffset = persistent ? 1 : 0;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    RECIPE_SWAP_COLORS_ICON,
                    this.getX() + 2 + iconOffset,
                    this.getY() + 1 + iconOffset,
                    0.0F,
                    0.0F,
                    16,
                    16,
                    16,
                    16);
            if (panel != null && panel.isPersistentDyeSwitchEnabled()) {
                graphics.fill(
                        this.getX() + 1,
                        this.getY() + 1,
                        this.getX() + this.getWidth() - 1,
                        this.getY() + 2,
                        0x55000000);
            }
        }
    }

    private class SaveEditButton extends Button.Plain {
        protected SaveEditButton() {
            super(
                    LoomScreenExtension.this.getLeftStripButtonX(),
                    LoomScreenExtension.this.screen.topPos + LoomScreenExtension.LEFT_STRIP_SAVE_EDIT_Y,
                    20,
                    18,
                    Component.empty(),
                    button -> {
                        if (LoomScreenExtension.this.panel != null
                                && LoomScreenExtension.this.panel.hasActiveBanner()) {
                            LoomScreenExtension.this.screen.minecraft.gui.setScreen(new BannerSaveEditScreen(
                                    LoomScreenExtension.this.screen,
                                    LoomScreenExtension.this.panel,
                                    LoomScreenExtension.this.showEditOnSaveButton()));
                        }
                    },
                    Supplier::get);
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            this.extractDefaultSprite(graphics);
            Identifier icon = shouldUseEditIconOnSaveButton() ? RECIPE_EDIT_ICON : RECIPE_ADD_ICON;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, icon, this.getX() + 2, this.getY() + 1, 0.0F, 0.0F, 16, 16, 16, 16);
        }
    }

    private class WeaveButton extends Button.Plain {
        protected WeaveButton() {
            super(
                    LoomScreenExtension.this.getLeftStripButtonX(),
                    LoomScreenExtension.this.screen.topPos + LoomScreenExtension.LEFT_STRIP_CRAFT_Y,
                    20,
                    18,
                    Component.empty(),
                    button -> {
                        if (LoomScreenExtension.this.panel != null) {
                            LoomScreenExtension.this.panel.craftSelectedBanner();
                        }
                    },
                    Supplier::get);
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            this.extractDefaultSprite(graphics);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    RECIPE_WEAVE_ICON,
                    this.getX() + 2,
                    this.getY() + 1,
                    0.0F,
                    0.0F,
                    16,
                    16,
                    16,
                    16);
        }
    }

    private class ImportExportButton extends Button.Plain {
        protected ImportExportButton() {
            super(
                    LoomScreenExtension.this.getLeftStripButtonX(),
                    LoomScreenExtension.this.getImportExportButtonY(),
                    20,
                    18,
                    Component.empty(),
                    button ->
                            LoomScreenExtension.this.screen.minecraft.gui.setScreen(new BannerRecipeImportExportScreen(
                                    LoomScreenExtension.this.screen, LoomScreenExtension.this.panel)),
                    Supplier::get);
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            this.extractDefaultSprite(graphics);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    RECIPE_IMPORT_EXPORT_ICON,
                    this.getX() + 2,
                    this.getY() + 1,
                    0.0F,
                    0.0F,
                    16,
                    16,
                    16,
                    16);
        }
    }
}
