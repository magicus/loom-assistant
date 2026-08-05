/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui.extensions;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.loomassistant.types.recipe.BannerRecipe;
import se.icus.mag.loomassistant.ui.LoomRecipePanel;
import se.icus.mag.loomassistant.ui.screens.BannerColorSwitchScreen;
import se.icus.mag.loomassistant.ui.screens.BannerRecipeImportExportScreen;
import se.icus.mag.loomassistant.ui.screens.BannerSaveEditScreen;
import se.icus.mag.loomassistant.ui.support.LoomUiStateStore;
import se.icus.mag.loomassistant.weaving.AutoCraftStateMachine;

/**
 * Contains all extension logic for LoomScreen. Receives live access to the screen via
 * LoomScreenAdapter. LoomScreenMixin holds only minimal mixin hooks that delegate here.
 */
public class LoomScreenExtension {
    public static final int CONTENT_X_SHIFT = 3;
    public static final int BG_LEFT_PADDING = 19;

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
    public static final int CUSTOM_BG_WIDTH = 278;
    public static final int CUSTOM_BG_HEIGHT = 256;

    public static final Identifier BG_LOCATION =
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

    // ── adapter ───────────────────────────────────────────────────────────────
    private final LoomScreenAdapter adapter;

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
    private final EnumMap<DyeColor, DyeColor> persistentDyeMap = new EnumMap<>(DyeColor.class);
    private boolean persistentDyeSwitchEnabled = false;
    private String lastPersistedActiveBannerJson = null;
    private final EnumMap<DyeColor, DyeColor> lastPersistedDyeMap = new EnumMap<>(DyeColor.class);
    private boolean lastPersistedDyeEnabled = false;
    private AutoCraftStateMachine craftabilityProbe;
    private BannerFlagModel previewFlag;

    public LoomScreenExtension(LoomScreenAdapter adapter) {
        this.adapter = adapter;
    }

    // ── public accessors used by LoomPanelHost / LoomActiveBannerHost impls ──

    public LoomRecipePanel getPanel() {
        return panel;
    }

    public void setPendingActiveBannerStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            this.pendingActiveBannerStack = ItemStack.EMPTY;
            this.activeBannerStack = ItemStack.EMPTY;
            LoomUiStateStore.setPersistedActiveBannerStack(adapter.loomassistant$getMinecraft(), ItemStack.EMPTY);
            this.lastPersistedActiveBannerJson = null;
            return;
        }
        this.pendingActiveBannerStack = stack.copyWithCount(1);
        this.activeBannerStack = this.pendingActiveBannerStack.copy();
        LoomUiStateStore.setPersistedActiveBannerStack(adapter.loomassistant$getMinecraft(), this.activeBannerStack);
        BannerRecipe recipe = BannerRecipe.fromItem(this.activeBannerStack);
        this.lastPersistedActiveBannerJson = recipe == null ? null : recipe.toJson();
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
                adapter.loomassistant$getMinecraft(), this.persistentDyeSwitchEnabled, this.persistentDyeMap);

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
        this.panelOpen = LoomUiStateStore.isLoomPanelOpen(adapter.loomassistant$getMinecraft());
        adapter.loomassistant$setLeftPos(panelOpen ? getOpenLeftPos() : getClosedLeftPos());
        this.craftabilityProbe = new AutoCraftStateMachine(adapter.loomassistant$getMenu());

        ModelPart flagPart =
                adapter.loomassistant$getMinecraft().getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
        this.previewFlag = new BannerFlagModel(flagPart);

        LoomUiStateStore.PersistentDyeState persistentDyeState =
                LoomUiStateStore.getPersistentDyeState(adapter.loomassistant$getMinecraft());
        this.persistentDyeSwitchEnabled = persistentDyeState.enabled();
        this.persistentDyeMap.clear();
        this.persistentDyeMap.putAll(persistentDyeState.replacements());
        this.lastPersistedDyeEnabled = this.persistentDyeSwitchEnabled;
        this.lastPersistedDyeMap.clear();
        this.lastPersistedDyeMap.putAll(this.persistentDyeMap);

        this.pendingActiveBannerStack =
                LoomUiStateStore.getPersistedActiveBannerStack(adapter.loomassistant$getMinecraft());
        if (!this.pendingActiveBannerStack.isEmpty()) {
            this.activeBannerStack = this.pendingActiveBannerStack.copy();
            BannerRecipe persistedRecipe = BannerRecipe.fromItem(this.activeBannerStack);
            this.lastPersistedActiveBannerJson = persistedRecipe == null ? null : persistedRecipe.toJson();
        } else {
            this.activeBannerStack = ItemStack.EMPTY;
            this.lastPersistedActiveBannerJson = null;
        }

        this.recipeBookButton = adapter.loomassistant$addWidget(new ImageButton(
                getLeftStripButtonX(),
                adapter.loomassistant$getTopPos() + LEFT_STRIP_RECIPE_Y,
                20,
                18,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES,
                button -> {
                    panelOpen = !panelOpen;
                    LoomUiStateStore.setLoomPanelOpen(adapter.loomassistant$getMinecraft(), panelOpen);
                    adapter.loomassistant$setLeftPos(panelOpen ? getOpenLeftPos() : getClosedLeftPos());
                    refreshControls();
                    refreshPanel();
                }));

        this.saveButton = adapter.loomassistant$addWidget(
                new Button.Plain(
                        getLeftStripButtonX(),
                        adapter.loomassistant$getTopPos() + LEFT_STRIP_SAVE_EDIT_Y,
                        20,
                        18,
                        Component.empty(),
                        button -> {
                            if (panel != null && panel.hasActiveBanner()) {
                                adapter.loomassistant$getMinecraft()
                                        .gui
                                        .setScreen(new BannerSaveEditScreen(
                                                adapter.loomassistant$asLoomScreen(), panel, showEditOnSaveButton()));
                            }
                        },
                        defaultNarrationSupplier -> defaultNarrationSupplier.get()) {
                    @Override
                    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
                        this.extractDefaultSprite(graphics);
                        Identifier icon = shouldUseEditIconOnSaveButton() ? RECIPE_EDIT_ICON : RECIPE_ADD_ICON;
                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                icon,
                                this.getX() + 2,
                                this.getY() + 1,
                                0.0F,
                                0.0F,
                                16,
                                16,
                                16,
                                16);
                    }
                });

        this.craftButton = adapter.loomassistant$addWidget(
                new Button.Plain(
                        getLeftStripButtonX(),
                        adapter.loomassistant$getTopPos() + LEFT_STRIP_CRAFT_Y,
                        20,
                        18,
                        Component.empty(),
                        button -> {
                            if (panel != null) {
                                panel.craftSelectedBanner();
                            }
                        },
                        defaultNarrationSupplier -> defaultNarrationSupplier.get()) {
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
                });

        this.importExportButton = adapter.loomassistant$addWidget(
                new Button.Plain(
                        getLeftStripButtonX(),
                        getImportExportButtonY(),
                        20,
                        18,
                        Component.empty(),
                        button -> adapter.loomassistant$getMinecraft()
                                .gui
                                .setScreen(new BannerRecipeImportExportScreen(
                                        adapter.loomassistant$asLoomScreen(), panel)),
                        defaultNarrationSupplier -> defaultNarrationSupplier.get()) {
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
                });

        this.colorButton = adapter.loomassistant$addWidget(
                new Button.Plain(
                        getLeftStripButtonX(),
                        adapter.loomassistant$getTopPos() + LEFT_STRIP_COLOR_Y,
                        20,
                        18,
                        Component.empty(),
                        button -> {
                            if (panel == null || !panel.hasActiveBanner()) {
                                return;
                            }
                            if (panel.isPersistentDyeSwitchEnabled()) {
                                panel.disablePersistentDyeSwitchAndReload();
                                return;
                            }
                            adapter.loomassistant$getMinecraft()
                                    .gui
                                    .setScreen(
                                            new BannerColorSwitchScreen(adapter.loomassistant$asLoomScreen(), panel));
                        },
                        defaultNarrationSupplier -> defaultNarrationSupplier.get()) {
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
                });
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
                var mc = adapter.loomassistant$getMinecraft();
                if (mc != null && mc.player != null && mc.player.hasInfiniteMaterials()) {
                    canCraftActiveBanner = true;
                    craftDisabledMessage = null;
                } else {
                    BannerRecipe activeBanner = PreviewExtension.extractBannerData(activeBannerStack);
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
            context.setTooltipForNextFrame(
                    adapter.loomassistant$getFont(), tooltipLines, Optional.empty(), mouseX, mouseY);
        }
        if (this.importExportButton != null && isMouseOverWidget(this.importExportButton, mouseX, mouseY)) {
            setSingleLineTooltip(context, IMPORT_EXPORT_TOOLTIP, mouseX, mouseY);
        }
        if (this.colorButton != null && isMouseOverWidget(this.colorButton, mouseX, mouseY)) {
            setSingleLineTooltip(context, CHANGE_COLORS_TOOLTIP, mouseX, mouseY);
        }

        if (!activeBannerStack.isEmpty()) {
            context.fakeItem(
                    activeBannerStack,
                    getLeftStripButtonX() + 2,
                    adapter.loomassistant$getTopPos() + LEFT_STRIP_ACTIVE_SLOT_Y + 1);

            if (panel != null) {
                boolean survivalNotWeavable = !panel.isActiveBannerWeavable()
                        && adapter.loomassistant$getMinecraft().player != null
                        && !adapter.loomassistant$getMinecraft().player.hasInfiniteMaterials();
                if (!survivalNotWeavable) {
                    int progress = panel.detectCraftingProgress();
                    int total = panel.getActiveBannerLayerCount();
                    if (progress >= 0 && total > 0) {
                        String badge = (progress + 1) + "/" + total;
                        int badgeW = adapter.loomassistant$getFont().width(badge);
                        int badgeX = getLeftStripButtonX() + (20 - badgeW) / 2;
                        int badgeY = adapter.loomassistant$getTopPos() + LEFT_STRIP_RECIPE_Y + 22;
                        context.text(adapter.loomassistant$getFont(), badge, badgeX, badgeY, 0xFFFFFFFF, true);
                    }
                }
            }

            if (isInActiveSlot(mouseX, mouseY)) {
                if (panel != null) {
                    panel.setActiveBannerTooltip(context, mouseX, mouseY);
                } else {
                    BannerRecipe activeBanner = PreviewExtension.extractBannerData(activeBannerStack);
                    LoomRecipePanel.setBannerTooltip(context, activeBanner, mouseX, mouseY);
                }
            }
        }

        if (panel != null) {
            var mc = adapter.loomassistant$getMinecraft();
            boolean survivalNotWeavable =
                    !panel.isActiveBannerWeavable() && mc.player != null && !mc.player.hasInfiniteMaterials();
            if (!survivalNotWeavable) {
                int progress = panel.detectCraftingProgress();
                if (progress >= 0) {
                    if (adapter.loomassistant$getMenu()
                            .getResultSlot()
                            .getItem()
                            .isEmpty()) {
                        renderNextStepHint(context, progress);
                    }
                    renderOutputSlotBorder(context, progress);
                }
            } else if (adapter.loomassistant$getMenu().getResultSlot().getItem().isEmpty()) {
                renderUncraftablePreview(context);
            }
        }
    }

    public void onMouseClicked(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        if (mouseButtonEvent.button() == 1 && isShiftHeld()) {
            int mx = (int) mouseButtonEvent.x();
            int my = (int) mouseButtonEvent.y();
            int leftPos = adapter.loomassistant$getLeftPos();
            int topPos = adapter.loomassistant$getTopPos();
            for (net.minecraft.world.inventory.Slot slot : adapter.loomassistant$getMenu().slots) {
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
            ItemStack carried = adapter.loomassistant$getMenu().getCarried();
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
        if (mouseButtonEvent.button() != 0) {
            return;
        }
        if (isInActiveSlot((int) mouseButtonEvent.x(), (int) mouseButtonEvent.y())
                && !adapter.loomassistant$getMenu().getCarried().isEmpty()) {
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
            this.recipeBookButton.setPosition(
                    getLeftStripButtonX(), adapter.loomassistant$getTopPos() + LEFT_STRIP_RECIPE_Y);
        }
        if (this.saveButton != null) {
            this.saveButton.setPosition(
                    getLeftStripButtonX(), adapter.loomassistant$getTopPos() + LEFT_STRIP_SAVE_EDIT_Y);
        }
        if (this.craftButton != null) {
            this.craftButton.setPosition(getLeftStripButtonX(), adapter.loomassistant$getTopPos() + LEFT_STRIP_CRAFT_Y);
        }
        if (this.colorButton != null) {
            this.colorButton.setPosition(getLeftStripButtonX(), adapter.loomassistant$getTopPos() + LEFT_STRIP_COLOR_Y);
        }
        if (this.importExportButton != null) {
            this.importExportButton.setPosition(getLeftStripButtonX(), getImportExportButtonY());
        }
    }

    private int getImportExportButtonY() {
        return adapter.loomassistant$getTopPos()
                + adapter.loomassistant$getImageHeight()
                - 18
                - LEFT_STRIP_IMPORT_EXPORT_BOTTOM_MARGIN;
    }

    private int getClosedLeftPos() {
        int guiExtraLeft = BG_LEFT_PADDING + CONTENT_X_SHIFT;
        int visualGuiWidth = adapter.loomassistant$getImageWidth() + guiExtraLeft;
        int visualLeft = (adapter.loomassistant$getScreenWidth() - visualGuiWidth) / 2;
        return visualLeft + guiExtraLeft;
    }

    private int getOpenLeftPos() {
        int leftExtensionWithoutTabs = LoomRecipePanel.PANEL_WIDTH + 5 + BG_LEFT_PADDING;
        int centeredAreaWidth = adapter.loomassistant$getImageWidth() + leftExtensionWithoutTabs;
        int centeredAreaLeft = (adapter.loomassistant$getScreenWidth() - centeredAreaWidth) / 2;
        int leftPos = centeredAreaLeft + leftExtensionWithoutTabs;

        int panelLeft = leftPos - leftExtensionWithoutTabs;
        int tabLeft = panelLeft - PANEL_TAB_LEFT_OVERHANG;
        if (tabLeft < 0) {
            leftPos += -tabLeft;
        }
        return leftPos;
    }

    private int getPanelX() {
        return adapter.loomassistant$getLeftPos() - LoomRecipePanel.PANEL_WIDTH - 5 - BG_LEFT_PADDING;
    }

    private void refreshPanel() {
        this.panel = panelOpen
                ? new LoomRecipePanel(
                        adapter.loomassistant$asLoomScreen(),
                        adapter.loomassistant$getMenu(),
                        getPanelX(),
                        adapter.loomassistant$getTopPos())
                : null;
        if (this.panel != null) {
            this.panel.restorePersistentDyeSwitchState(persistentDyeSwitchEnabled, Map.copyOf(persistentDyeMap));
        }
    }

    private int getLeftStripButtonX() {
        return adapter.loomassistant$getLeftPos() - BG_LEFT_PADDING + LEFT_STRIP_BUTTON_X;
    }

    private boolean isInActiveSlot(int mouseX, int mouseY) {
        int slotX = getLeftStripButtonX();
        int slotY = adapter.loomassistant$getTopPos() + LEFT_STRIP_ACTIVE_SLOT_Y;
        return mouseX >= slotX && mouseX < slotX + ACTIVE_SLOT_W && mouseY >= slotY && mouseY < slotY + ACTIVE_SLOT_H;
    }

    // ── rendering helpers ─────────────────────────────────────────────────────

    private static boolean isMouseOverWidget(
            net.minecraft.client.gui.components.AbstractWidget widget, int mouseX, int mouseY) {
        return mouseX >= widget.getX()
                && mouseX < widget.getX() + widget.getWidth()
                && mouseY >= widget.getY()
                && mouseY < widget.getY() + widget.getHeight();
    }

    private void setSingleLineTooltip(GuiGraphicsExtractor context, Component text, int mouseX, int mouseY) {
        context.setTooltipForNextFrame(
                adapter.loomassistant$getFont(), List.of(text), Optional.empty(), mouseX, mouseY);
    }

    private boolean showEditOnSaveButton() {
        return panel != null && panel.hasActiveBanner() && !panel.isActiveBannerSavable();
    }

    private boolean shouldUseEditIconOnSaveButton() {
        if (panel == null || !panel.hasActiveBanner()) {
            return true;
        }
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
        int by = adapter.loomassistant$getTopPos() + LEFT_STRIP_RECIPE_Y + 23;
        drawWeaveWithSlash(ctx, bx, by, iconSize);
    }

    private void renderUncraftablePreview(GuiGraphicsExtractor ctx) {
        int iconSize = 16;
        int px = adapter.loomassistant$getLeftPos() + 141 + (20 - iconSize) / 2;
        int py = adapter.loomassistant$getTopPos() + 8 + (40 - iconSize) / 2 + 6;
        drawWeaveWithSlash(ctx, px, py, iconSize);
    }

    private void renderNextStepHint(GuiGraphicsExtractor context, int nextLayerIndex) {
        if (panel == null) return;
        BannerRecipe recipe = PreviewExtension.extractBannerData(activeBannerStack);
        if (recipe == null || nextLayerIndex >= recipe.getLayers().size()) return;

        var layer = recipe.getLayers().get(nextLayerIndex);
        int px = adapter.loomassistant$getLeftPos() + 141;
        int py = adapter.loomassistant$getTopPos() + 8;

        ItemStack dye = new ItemStack((net.minecraft.world.item.Item) BannerRecipe.getDyeItem(layer.getDyeColorEnum()));
        context.fakeItem(dye, px + 2, py + 2);

        try {
            Identifier patId = Identifier.tryParse(layer.patternId());
            if (patId != null && adapter.loomassistant$getMinecraft().level != null) {
                var reg = adapter.loomassistant$getMinecraft()
                        .level
                        .registryAccess()
                        .lookup(Registries.BANNER_PATTERN);
                if (reg.isPresent()) {
                    var entry = reg.get().get(patId);
                    if (entry.isPresent()) {
                        @SuppressWarnings("unchecked")
                        Holder<BannerPattern> holder = (Holder<BannerPattern>) (Object) entry.get();
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
        } catch (Exception ignored) {
        }
    }

    private void renderOutputSlotBorder(GuiGraphicsExtractor context, int progress) {
        int rx = adapter.loomassistant$getLeftPos() + 143;
        int ry = adapter.loomassistant$getTopPos() + 57;
        ItemStack result = adapter.loomassistant$getMenu().getResultSlot().getItem();
        if (result.isEmpty()) return;

        boolean correct = resultMatchesExpected(result, progress);
        int color = correct ? 0xFF44FF44 : 0xFFFF4444;
        context.fill(rx - 1, ry - 1, rx + 17, ry, color);
        context.fill(rx - 1, ry + 16, rx + 17, ry + 17, color);
        context.fill(rx - 1, ry, rx, ry + 16, color);
        context.fill(rx + 16, ry, rx + 17, ry + 16, color);
    }

    private boolean resultMatchesExpected(ItemStack result, int nextLayerIndex) {
        if (!(result.getItem() instanceof BannerItem bannerItem)) return false;
        BannerRecipe recipe = PreviewExtension.extractBannerData(activeBannerStack);
        if (recipe == null || nextLayerIndex >= recipe.getLayers().size()) return false;
        if (bannerItem.getColor() != recipe.getBannerColorEnum()) return false;

        BannerPatternLayers layers = result.get(DataComponents.BANNER_PATTERNS);
        if (layers == null) return false;
        int expected = nextLayerIndex + 1;
        if (layers.layers().size() != expected) return false;

        for (int i = 0; i < expected; i++) {
            var cur = layers.layers().get(i);
            var exp = recipe.getLayers().get(i);
            if (cur.color() != exp.getDyeColorEnum()) return false;
            String curId = cur.pattern()
                    .unwrapKey()
                    .map(k -> k.identifier().toString())
                    .orElse(null);
            if (curId == null) return false;
            String expId = exp.patternId().contains(":") ? exp.patternId() : "minecraft:" + exp.patternId();
            if (!curId.equals(expId)) return false;
        }
        return true;
    }

    // ── sound & misc ──────────────────────────────────────────────────────────

    private void syncPerWorldUiState() {
        BannerRecipe activeRecipe = BannerRecipe.fromItem(this.activeBannerStack);
        String currentActiveJson = activeRecipe == null ? null : activeRecipe.toJson();
        boolean activeChanged = currentActiveJson == null
                ? this.lastPersistedActiveBannerJson != null
                : !currentActiveJson.equals(this.lastPersistedActiveBannerJson);
        if (activeChanged) {
            LoomUiStateStore.setPersistedActiveBannerStack(
                    adapter.loomassistant$getMinecraft(), this.activeBannerStack);
            this.lastPersistedActiveBannerJson = currentActiveJson;
        }

        boolean dyeChanged = this.lastPersistedDyeEnabled != this.persistentDyeSwitchEnabled
                || !this.lastPersistedDyeMap.equals(this.persistentDyeMap);
        if (dyeChanged) {
            LoomUiStateStore.setPersistentDyeState(
                    adapter.loomassistant$getMinecraft(), this.persistentDyeSwitchEnabled, this.persistentDyeMap);
            this.lastPersistedDyeEnabled = this.persistentDyeSwitchEnabled;
            this.lastPersistedDyeMap.clear();
            this.lastPersistedDyeMap.putAll(this.persistentDyeMap);
        }
    }

    private static String buildMissingMaterialsMessage(BannerRecipe banner, AutoCraftStateMachine autoCraft) {
        List<String> missingMaterials = autoCraft.getMissingMaterialDescriptions(banner);
        if (missingMaterials.isEmpty()) {
            return null;
        }
        return Component.translatable("loom-assistant.active.missing_header").getString()
                + "\n"
                + String.join("\n", missingMaterials);
    }

    private boolean isShiftHeld() {
        var win = adapter.loomassistant$getMinecraft().getWindow();
        return InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void playActiveSlotSetSound() {
        var mc = adapter.loomassistant$getMinecraft();
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
        var mc = adapter.loomassistant$getMinecraft();
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

    /** Called by the mixin @Redirect to draw the custom background texture. */
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
}
