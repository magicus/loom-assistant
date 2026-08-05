/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.loomassistant.LoomActiveBannerHost;
import se.icus.mag.loomassistant.LoomPanelHost;
import se.icus.mag.loomassistant.types.recipe.BannerRecipe;
import se.icus.mag.loomassistant.ui.BannerColorSwitchScreen;
import se.icus.mag.loomassistant.ui.BannerPreviewRenderer;
import se.icus.mag.loomassistant.ui.BannerRecipeImportExportScreen;
import se.icus.mag.loomassistant.ui.BannerSaveEditScreen;
import se.icus.mag.loomassistant.ui.LoomPanel;
import se.icus.mag.loomassistant.ui.LoomUiStateStore;
import se.icus.mag.loomassistant.weaving.AutoCraftStateMachine;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu>
        implements LoomPanelHost, LoomActiveBannerHost {
    @Unique
    private static final int LOOMASSISTANT_CONTENT_X_SHIFT = 3;

    @Unique
    private static final int LOOMASSISTANT_BG_LEFT_PADDING = 19;

    @Unique
    private static final int LOOMASSISTANT_PANEL_TAB_LEFT_OVERHANG = 32;

    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_BUTTON_X = 3;

    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_RECIPE_Y = 5;

    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_ACTIVE_SLOT_Y = LOOMASSISTANT_LEFT_STRIP_RECIPE_Y + 42;

    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_CRAFT_Y = LOOMASSISTANT_LEFT_STRIP_ACTIVE_SLOT_Y + 22;

    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_SAVE_EDIT_Y = LOOMASSISTANT_LEFT_STRIP_CRAFT_Y + 20;

    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_COLOR_Y = LOOMASSISTANT_LEFT_STRIP_SAVE_EDIT_Y + 20;

    @Unique
    private static final int LOOMASSISTANT_LEFT_STRIP_IMPORT_EXPORT_BOTTOM_MARGIN = 6;

    @Unique
    private static final int LOOMASSISTANT_ACTIVE_SLOT_W = 20;

    @Unique
    private static final int LOOMASSISTANT_ACTIVE_SLOT_H = 18;

    @Unique
    private static final int LOOMASSISTANT_CUSTOM_BG_WIDTH = 278;

    @Unique
    private static final int LOOMASSISTANT_CUSTOM_BG_HEIGHT = 256;

    @Unique
    private static final Identifier LOOMASSISTANT_BG_LOCATION =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/loom-gui.png");

    @Unique
    private static final Identifier LOOMASSISTANT_RECIPE_WEAVE_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-weave.png");

    @Unique
    private static final Identifier LOOMASSISTANT_RECIPE_ADD_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-add.png");

    @Unique
    private static final Identifier LOOMASSISTANT_RECIPE_EDIT_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-edit.png");

    @Unique
    private static final Identifier LOOMASSISTANT_RECIPE_SWAP_COLORS_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-swap-colors.png");

    @Unique
    private static final Identifier LOOMASSISTANT_RECIPE_IMPORT_EXPORT_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-import-export.png");

    @Unique
    private static final SoundEvent LOOMASSISTANT_ACTIVE_SLOT_SET_SOUND = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath("loom-assistant", "ui.active_slot_set"));

    @Unique
    private static final SoundEvent LOOMASSISTANT_ACTIVE_SLOT_CLEAR_SOUND = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath("loom-assistant", "ui.active_slot_clear"));

    @Unique
    private static final Component LOOMASSISTANT_SAVE_TOOLTIP =
            Component.translatable("loom-assistant.tooltip.add_recipe");

    @Unique
    private static final Component LOOMASSISTANT_WEAVE_TOOLTIP = Component.translatable("loom-assistant.tooltip.weave");

    @Unique
    private static final Component LOOMASSISTANT_EDIT_TOOLTIP =
            Component.translatable("loom-assistant.tooltip.edit_recipe");

    @Unique
    private static final Component LOOMASSISTANT_IMPORT_EXPORT_TOOLTIP =
            Component.translatable("loom-assistant.tooltip.import_export_recipes");

    @Unique
    private static final Component LOOMASSISTANT_CHANGE_COLORS_TOOLTIP =
            Component.translatable("loom-assistant.tooltip.replace_colors");

    @Unique
    private boolean loomassistant$panelOpen = false;

    @Unique
    private LoomPanel loomassistant$panel;

    @Unique
    private ImageButton loomassistant$recipeBookButton;

    @Unique
    private Button loomassistant$saveButton;

    @Unique
    private Button loomassistant$craftButton;

    @Unique
    private Button loomassistant$colorButton;

    @Unique
    private Button loomassistant$importExportButton;

    @Unique
    private ItemStack loomassistant$activeBannerStack = ItemStack.EMPTY;

    @Unique
    private ItemStack loomassistant$pendingActiveBannerStack = ItemStack.EMPTY;

    @Unique
    private final EnumMap<DyeColor, DyeColor> loomassistant$persistentDyeMap = new EnumMap<>(DyeColor.class);

    @Unique
    private boolean loomassistant$persistentDyeSwitchEnabled = false;

    @Unique
    private String loomassistant$lastPersistedActiveBannerJson = null;

    @Unique
    private final EnumMap<DyeColor, DyeColor> loomassistant$lastPersistedDyeMap = new EnumMap<>(DyeColor.class);

    @Unique
    private boolean loomassistant$lastPersistedDyeEnabled = false;

    @Unique
    private AutoCraftStateMachine loomassistant$craftabilityProbe;

    @Unique
    private BannerFlagModel loomassistant$previewFlag;

    public LoomScreenMixin(LoomMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void loomassistant$onInit(CallbackInfo ci) {
        this.loomassistant$panelOpen = LoomUiStateStore.isLoomPanelOpen(this.minecraft);
        this.leftPos = loomassistant$panelOpen ? loomassistant$getOpenLeftPos() : loomassistant$getClosedLeftPos();
        this.loomassistant$craftabilityProbe = new AutoCraftStateMachine(this.menu);

        ModelPart flagPart = this.minecraft.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
        this.loomassistant$previewFlag = new BannerFlagModel(flagPart);

        LoomUiStateStore.PersistentDyeState persistentDyeState = LoomUiStateStore.getPersistentDyeState(this.minecraft);
        this.loomassistant$persistentDyeSwitchEnabled = persistentDyeState.enabled();
        this.loomassistant$persistentDyeMap.clear();
        this.loomassistant$persistentDyeMap.putAll(persistentDyeState.replacements());
        this.loomassistant$lastPersistedDyeEnabled = this.loomassistant$persistentDyeSwitchEnabled;
        this.loomassistant$lastPersistedDyeMap.clear();
        this.loomassistant$lastPersistedDyeMap.putAll(this.loomassistant$persistentDyeMap);

        this.loomassistant$pendingActiveBannerStack = LoomUiStateStore.getPersistedActiveBannerStack(this.minecraft);
        if (!this.loomassistant$pendingActiveBannerStack.isEmpty()) {
            this.loomassistant$activeBannerStack = this.loomassistant$pendingActiveBannerStack.copy();
            BannerRecipe persistedRecipe = BannerRecipe.fromItem(this.loomassistant$activeBannerStack);
            this.loomassistant$lastPersistedActiveBannerJson =
                    persistedRecipe == null ? null : persistedRecipe.toJson();
        } else {
            this.loomassistant$activeBannerStack = ItemStack.EMPTY;
            this.loomassistant$lastPersistedActiveBannerJson = null;
        }

        this.loomassistant$recipeBookButton = this.addRenderableWidget(new ImageButton(
                this.loomassistant$getLeftStripButtonX(),
                this.topPos + LOOMASSISTANT_LEFT_STRIP_RECIPE_Y,
                20,
                18,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES,
                button -> {
                    loomassistant$panelOpen = !loomassistant$panelOpen;
                    LoomUiStateStore.setLoomPanelOpen(this.minecraft, loomassistant$panelOpen);
                    this.leftPos =
                            loomassistant$panelOpen ? loomassistant$getOpenLeftPos() : loomassistant$getClosedLeftPos();
                    loomassistant$refreshControls();
                    loomassistant$refreshPanel();
                }));

        this.loomassistant$saveButton = this.addRenderableWidget(
                new Button.Plain(
                        this.loomassistant$getLeftStripButtonX(),
                        this.topPos + LOOMASSISTANT_LEFT_STRIP_SAVE_EDIT_Y,
                        20,
                        18,
                        Component.empty(),
                        button -> {
                            if (loomassistant$panel != null && loomassistant$panel.hasActiveBanner()) {
                                this.minecraft.gui.setScreen(new BannerSaveEditScreen(
                                        (LoomScreen) (Object) this,
                                        loomassistant$panel,
                                        loomassistant$showEditOnSaveButton()));
                            }
                        },
                        defaultNarrationSupplier -> defaultNarrationSupplier.get()) {
                    @Override
                    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
                        this.extractDefaultSprite(graphics);
                        Identifier icon = loomassistant$shouldUseEditIconOnSaveButton()
                                ? LOOMASSISTANT_RECIPE_EDIT_ICON
                                : LOOMASSISTANT_RECIPE_ADD_ICON;
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

        this.loomassistant$craftButton = this.addRenderableWidget(
                new Button.Plain(
                        this.loomassistant$getLeftStripButtonX(),
                        this.topPos + LOOMASSISTANT_LEFT_STRIP_CRAFT_Y,
                        20,
                        18,
                        Component.empty(),
                        button -> {
                            if (loomassistant$panel != null) {
                                loomassistant$panel.craftSelectedBanner();
                            }
                        },
                        defaultNarrationSupplier -> defaultNarrationSupplier.get()) {
                    @Override
                    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
                        this.extractDefaultSprite(graphics);
                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                LOOMASSISTANT_RECIPE_WEAVE_ICON,
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

        this.loomassistant$importExportButton = this.addRenderableWidget(
                new Button.Plain(
                        this.loomassistant$getLeftStripButtonX(),
                        this.loomassistant$getImportExportButtonY(),
                        20,
                        18,
                        Component.empty(),
                        button -> this.minecraft.gui.setScreen(
                                new BannerRecipeImportExportScreen((LoomScreen) (Object) this, loomassistant$panel)),
                        defaultNarrationSupplier -> defaultNarrationSupplier.get()) {
                    @Override
                    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
                        this.extractDefaultSprite(graphics);
                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                LOOMASSISTANT_RECIPE_IMPORT_EXPORT_ICON,
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

        this.loomassistant$colorButton = this.addRenderableWidget(
                new Button.Plain(
                        this.loomassistant$getLeftStripButtonX(),
                        this.topPos + LOOMASSISTANT_LEFT_STRIP_COLOR_Y,
                        20,
                        18,
                        Component.empty(),
                        button -> {
                            if (loomassistant$panel == null || !loomassistant$panel.hasActiveBanner()) {
                                return;
                            }
                            if (loomassistant$panel.isPersistentDyeSwitchEnabled()) {
                                loomassistant$panel.disablePersistentDyeSwitchAndReload();
                                return;
                            }
                            this.minecraft.gui.setScreen(
                                    new BannerColorSwitchScreen((LoomScreen) (Object) this, loomassistant$panel));
                        },
                        defaultNarrationSupplier -> defaultNarrationSupplier.get()) {
                    @Override
                    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
                        this.extractDefaultSprite(graphics);
                        boolean persistent =
                                loomassistant$panel != null && loomassistant$panel.isPersistentDyeSwitchEnabled();
                        int iconOffset = persistent ? 1 : 0;
                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                LOOMASSISTANT_RECIPE_SWAP_COLORS_ICON,
                                this.getX() + 2 + iconOffset,
                                this.getY() + 1 + iconOffset,
                                0.0F,
                                0.0F,
                                16,
                                16,
                                16,
                                16);
                        if (loomassistant$panel != null && loomassistant$panel.isPersistentDyeSwitchEnabled()) {
                            // Small shadow line reinforces a pressed visual without obscuring the icon.
                            graphics.fill(
                                    this.getX() + 1,
                                    this.getY() + 1,
                                    this.getX() + this.getWidth() - 1,
                                    this.getY() + 2,
                                    0x55000000);
                        }
                    }
                });
        this.loomassistant$colorButton.setOverrideRenderHighlightedSprite(
                () -> (loomassistant$panel != null && loomassistant$panel.isPersistentDyeSwitchEnabled())
                        || this.loomassistant$colorButton.isHoveredOrFocused());

        this.loomassistant$craftButton.active = false;
        this.loomassistant$saveButton.active = false;
        this.loomassistant$saveButton.visible = true;
        this.loomassistant$colorButton.active = false;
        this.loomassistant$colorButton.visible = true;

        loomassistant$refreshPanel();
    }

    @Unique
    private void loomassistant$refreshControls() {
        if (this.loomassistant$recipeBookButton != null) {
            this.loomassistant$recipeBookButton.setPosition(
                    this.loomassistant$getLeftStripButtonX(), this.topPos + LOOMASSISTANT_LEFT_STRIP_RECIPE_Y);
        }
        if (this.loomassistant$saveButton != null) {
            this.loomassistant$saveButton.setPosition(
                    this.loomassistant$getLeftStripButtonX(), this.topPos + LOOMASSISTANT_LEFT_STRIP_SAVE_EDIT_Y);
        }
        if (this.loomassistant$craftButton != null) {
            this.loomassistant$craftButton.setPosition(
                    this.loomassistant$getLeftStripButtonX(), this.topPos + LOOMASSISTANT_LEFT_STRIP_CRAFT_Y);
        }
        if (this.loomassistant$colorButton != null) {
            this.loomassistant$colorButton.setPosition(
                    this.loomassistant$getLeftStripButtonX(), this.topPos + LOOMASSISTANT_LEFT_STRIP_COLOR_Y);
        }
        if (this.loomassistant$importExportButton != null) {
            this.loomassistant$importExportButton.setPosition(
                    this.loomassistant$getLeftStripButtonX(), this.loomassistant$getImportExportButtonY());
        }
    }

    @Unique
    private int loomassistant$getImportExportButtonY() {
        return this.topPos + this.imageHeight - 18 - LOOMASSISTANT_LEFT_STRIP_IMPORT_EXPORT_BOTTOM_MARGIN;
    }

    @Unique
    private int loomassistant$getClosedLeftPos() {
        int guiExtraLeft = LOOMASSISTANT_BG_LEFT_PADDING + LOOMASSISTANT_CONTENT_X_SHIFT;
        int visualGuiWidth = this.imageWidth + guiExtraLeft;
        int visualLeft = (this.width - visualGuiWidth) / 2;
        return visualLeft + guiExtraLeft;
    }

    @Unique
    private int loomassistant$getOpenLeftPos() {
        int leftExtensionWithoutTabs = LoomPanel.PANEL_WIDTH + 5 + LOOMASSISTANT_BG_LEFT_PADDING;

        // Center panel + loom as a combined area (tabs excluded from centering).
        int centeredAreaWidth = this.imageWidth + leftExtensionWithoutTabs;
        int centeredAreaLeft = (this.width - centeredAreaWidth) / 2;
        int leftPos = centeredAreaLeft + leftExtensionWithoutTabs;

        // If tabs would go off-screen on the left, shift everything right just enough.
        int panelLeft = leftPos - leftExtensionWithoutTabs;
        int tabLeft = panelLeft - LOOMASSISTANT_PANEL_TAB_LEFT_OVERHANG;
        if (tabLeft < 0) {
            leftPos += -tabLeft;
        }

        return leftPos;
    }

    @Unique
    private int loomassistant$getPanelX() {
        return this.leftPos - LoomPanel.PANEL_WIDTH - 5 - LOOMASSISTANT_BG_LEFT_PADDING;
    }

    @Unique
    private void loomassistant$refreshPanel() {
        this.loomassistant$panel = loomassistant$panelOpen
                ? new LoomPanel((LoomScreen) (Object) this, this.menu, loomassistant$getPanelX(), this.topPos)
                : null;
        if (this.loomassistant$panel != null) {
            this.loomassistant$panel.restorePersistentDyeSwitchState(
                    loomassistant$persistentDyeSwitchEnabled, Map.copyOf(loomassistant$persistentDyeMap));
        }
    }

    @Unique
    private int loomassistant$getLeftStripButtonX() {
        return this.leftPos - LOOMASSISTANT_BG_LEFT_PADDING + LOOMASSISTANT_LEFT_STRIP_BUTTON_X;
    }

    @Unique
    private boolean loomassistant$isInActiveSlot(int mouseX, int mouseY) {
        int slotX = this.loomassistant$getLeftStripButtonX();
        int slotY = this.topPos + LOOMASSISTANT_LEFT_STRIP_ACTIVE_SLOT_Y;
        return mouseX >= slotX
                && mouseX < slotX + LOOMASSISTANT_ACTIVE_SLOT_W
                && mouseY >= slotY
                && mouseY < slotY + LOOMASSISTANT_ACTIVE_SLOT_H;
    }

    @Redirect(
            method = "extractBackground",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private void loomassistant$drawCustomLoomBackground(
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
                LOOMASSISTANT_BG_LOCATION,
                x - LOOMASSISTANT_BG_LEFT_PADDING - LOOMASSISTANT_CONTENT_X_SHIFT,
                y,
                u,
                v,
                width + LOOMASSISTANT_BG_LEFT_PADDING + LOOMASSISTANT_CONTENT_X_SHIFT,
                height,
                LOOMASSISTANT_CUSTOM_BG_WIDTH,
                LOOMASSISTANT_CUSTOM_BG_HEIGHT);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void loomassistant$onExtractBackground(
            GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        boolean hasActiveBanner = false;
        boolean canCraftActiveBanner = false;
        String craftDisabledMessage = null;
        if (loomassistant$panel != null) {
            if (!loomassistant$pendingActiveBannerStack.isEmpty()) {
                loomassistant$panel.setActiveBannerFromItemStack(loomassistant$pendingActiveBannerStack);
                loomassistant$pendingActiveBannerStack = ItemStack.EMPTY;
            } else if (loomassistant$panel.getActiveBannerStack().isEmpty()
                    && !loomassistant$activeBannerStack.isEmpty()) {
                // Keep panel state in sync after opening/closing modal screens.
                loomassistant$panel.setActiveBannerFromItemStack(loomassistant$activeBannerStack);
            }
            loomassistant$panel.tick();
            loomassistant$persistentDyeSwitchEnabled = loomassistant$panel.isPersistentDyeSwitchEnabled();
            loomassistant$persistentDyeMap.clear();
            loomassistant$persistentDyeMap.putAll(loomassistant$panel.getPersistentDyeReplacementMapCopy());
            loomassistant$activeBannerStack = loomassistant$panel.getActiveBannerStack();
            loomassistant$syncPerWorldUiState();
            hasActiveBanner = !loomassistant$activeBannerStack.isEmpty();
            canCraftActiveBanner = loomassistant$panel.isActiveBannerCraftable();
            if (!canCraftActiveBanner) {
                craftDisabledMessage = loomassistant$panel.getActiveBannerMissingMaterialMessage();
            }
            if (this.loomassistant$saveButton != null) {
                this.loomassistant$saveButton.active = loomassistant$panel.hasActiveBanner();
                this.loomassistant$saveButton.visible = true;
            }
            if (this.loomassistant$colorButton != null) {
                this.loomassistant$colorButton.active = loomassistant$panel.hasActiveBanner();
                this.loomassistant$colorButton.visible = true;
            }
            loomassistant$panel.render(context, mouseX, mouseY, delta);
        } else if (this.loomassistant$saveButton != null) {
            this.loomassistant$saveButton.active = false;
            this.loomassistant$saveButton.visible = true;
            hasActiveBanner = !loomassistant$activeBannerStack.isEmpty();
            if (hasActiveBanner && loomassistant$craftabilityProbe != null) {
                if (this.minecraft != null
                        && this.minecraft.player != null
                        && this.minecraft.player.hasInfiniteMaterials()) {
                    canCraftActiveBanner = true;
                    craftDisabledMessage = null;
                } else {
                    BannerRecipe activeBanner =
                            BannerPreviewRenderer.extractBannerData(loomassistant$activeBannerStack);
                    if (activeBanner != null) {
                        canCraftActiveBanner = loomassistant$craftabilityProbe.canCraft(activeBanner);
                        if (!canCraftActiveBanner) {
                            craftDisabledMessage = loomassistant$buildMissingMaterialsMessage(
                                    activeBanner, loomassistant$craftabilityProbe);
                        }
                    }
                }
            }
            this.loomassistant$saveButton.active = hasActiveBanner;
            if (this.loomassistant$colorButton != null) {
                this.loomassistant$colorButton.active = false;
                this.loomassistant$colorButton.visible = true;
            }
        }

        if (this.loomassistant$craftButton != null) {
            this.loomassistant$craftButton.active = hasActiveBanner && canCraftActiveBanner;
        }

        if (this.loomassistant$saveButton != null
                && loomassistant$isMouseOverWidget(this.loomassistant$saveButton, mouseX, mouseY)) {
            Component tooltip = loomassistant$shouldUseEditIconOnSaveButton()
                    ? LOOMASSISTANT_EDIT_TOOLTIP
                    : LOOMASSISTANT_SAVE_TOOLTIP;
            loomassistant$setSingleLineTooltip(context, tooltip, mouseX, mouseY);
        }
        if (this.loomassistant$craftButton != null
                && loomassistant$isMouseOverWidget(this.loomassistant$craftButton, mouseX, mouseY)) {
            List<Component> tooltipLines = new ArrayList<>();
            tooltipLines.add(LOOMASSISTANT_WEAVE_TOOLTIP);

            if (!this.loomassistant$craftButton.active && hasActiveBanner) {
                String reason = craftDisabledMessage;
                if (reason != null && !reason.isBlank()) {
                    tooltipLines.add(Component.empty());
                    tooltipLines.addAll(reason.lines().map(Component::literal).toList());
                }
            }

            context.setTooltipForNextFrame(this.font, tooltipLines, Optional.empty(), mouseX, mouseY);
        }
        if (this.loomassistant$importExportButton != null
                && loomassistant$isMouseOverWidget(this.loomassistant$importExportButton, mouseX, mouseY)) {
            loomassistant$setSingleLineTooltip(context, LOOMASSISTANT_IMPORT_EXPORT_TOOLTIP, mouseX, mouseY);
        }
        if (this.loomassistant$colorButton != null
                && loomassistant$isMouseOverWidget(this.loomassistant$colorButton, mouseX, mouseY)) {
            loomassistant$setSingleLineTooltip(context, LOOMASSISTANT_CHANGE_COLORS_TOOLTIP, mouseX, mouseY);
        }

        if (!loomassistant$activeBannerStack.isEmpty()) {
            context.fakeItem(
                    loomassistant$activeBannerStack,
                    this.loomassistant$getLeftStripButtonX() + 2,
                    this.topPos + LOOMASSISTANT_LEFT_STRIP_ACTIVE_SLOT_Y + 1);

            // Draw n/m progress indicator in the gap above the active slot.
            if (loomassistant$panel != null) {
                int progress = loomassistant$panel.detectCraftingProgress();
                int total = loomassistant$panel.getActiveBannerLayerCount();
                if (progress >= 0 && total > 0) {
                    String badge = (progress + 1) + "/" + total;
                    int badgeW = this.font.width(badge);
                    int badgeX = this.loomassistant$getLeftStripButtonX() + (20 - badgeW) / 2;
                    int badgeY = this.topPos + LOOMASSISTANT_LEFT_STRIP_RECIPE_Y + 22;
                    context.text(this.font, badge, badgeX, badgeY, 0xFFFFFFFF, true);
                }
            }

            if (loomassistant$isInActiveSlot(mouseX, mouseY)) {
                if (loomassistant$panel != null) {
                    loomassistant$panel.setActiveBannerTooltip(context, mouseX, mouseY);
                } else {
                    BannerRecipe activeBanner =
                            BannerPreviewRenderer.extractBannerData(loomassistant$activeBannerStack);
                    LoomPanel.setBannerTooltip(context, activeBanner, mouseX, mouseY);
                }
            }
        }

        // Next-step guidance and output slot highlighting when progress is detected.
        if (loomassistant$panel != null) {
            int progress = loomassistant$panel.detectCraftingProgress();
            if (progress >= 0) {
                // Only render next-step hint when vanilla has no result computed yet.
                if (this.menu.getResultSlot().getItem().isEmpty()) {
                    loomassistant$renderNextStepHint(context, progress);
                }
                loomassistant$renderOutputSlotBorder(context, progress);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseClicked(
            MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (loomassistant$isInActiveSlot((int) mouseButtonEvent.x(), (int) mouseButtonEvent.y())) {
            ItemStack carried = this.menu.getCarried();
            if (!carried.isEmpty()) {
                if (loomassistant$panel != null) {
                    if (loomassistant$panel.setActiveBannerFromItemStack(carried)) {
                        loomassistant$activeBannerStack = loomassistant$panel.getActiveBannerStack();
                        loomassistant$playActiveSlotSetSound();
                    }
                } else {
                    loomassistant$pendingActiveBannerStack = carried.copyWithCount(1);
                    loomassistant$activeBannerStack = loomassistant$pendingActiveBannerStack.copy();
                    loomassistant$playActiveSlotSetSound();
                }
                cir.setReturnValue(true);
                return;
            }

            if (!loomassistant$activeBannerStack.isEmpty()) {
                if (loomassistant$panel != null) {
                    loomassistant$panel.clearSelectedBanner();
                }
                loomassistant$pendingActiveBannerStack = ItemStack.EMPTY;
                loomassistant$activeBannerStack = ItemStack.EMPTY;
                loomassistant$playActiveSlotClearSound();
                cir.setReturnValue(true);
                return;
            }
        }

        if (loomassistant$panel != null && loomassistant$panel.mouseClicked(mouseButtonEvent)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseReleased(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        if (mouseButtonEvent.button() != 0) {
            return;
        }

        if (loomassistant$isInActiveSlot((int) mouseButtonEvent.x(), (int) mouseButtonEvent.y())
                && !this.menu.getCarried().isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount,
            CallbackInfoReturnable<Boolean> cir) {
        if (loomassistant$panel != null
                && loomassistant$panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public LoomPanel loomassistant$getPanel() {
        return loomassistant$panel;
    }

    @Override
    public void loomassistant$setPendingActiveBannerStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            this.loomassistant$pendingActiveBannerStack = ItemStack.EMPTY;
            this.loomassistant$activeBannerStack = ItemStack.EMPTY;
            LoomUiStateStore.setPersistedActiveBannerStack(this.minecraft, ItemStack.EMPTY);
            this.loomassistant$lastPersistedActiveBannerJson = null;
            return;
        }
        this.loomassistant$pendingActiveBannerStack = stack.copyWithCount(1);
        this.loomassistant$activeBannerStack = this.loomassistant$pendingActiveBannerStack.copy();
        LoomUiStateStore.setPersistedActiveBannerStack(this.minecraft, this.loomassistant$activeBannerStack);
        BannerRecipe recipe = BannerRecipe.fromItem(this.loomassistant$activeBannerStack);
        this.loomassistant$lastPersistedActiveBannerJson = recipe == null ? null : recipe.toJson();
    }

    @Override
    public void loomassistant$setPersistentDyeSwitchState(boolean enabled, Map<DyeColor, DyeColor> replacements) {
        this.loomassistant$persistentDyeSwitchEnabled = enabled;
        this.loomassistant$persistentDyeMap.clear();
        if (replacements != null) {
            for (Map.Entry<DyeColor, DyeColor> entry : replacements.entrySet()) {
                DyeColor src = entry.getKey();
                DyeColor dst = entry.getValue();
                if (src != null && dst != null && src != dst) {
                    this.loomassistant$persistentDyeMap.put(src, dst);
                }
            }
        }

        LoomUiStateStore.setPersistentDyeState(
                this.minecraft, this.loomassistant$persistentDyeSwitchEnabled, this.loomassistant$persistentDyeMap);

        this.loomassistant$lastPersistedDyeEnabled = this.loomassistant$persistentDyeSwitchEnabled;
        this.loomassistant$lastPersistedDyeMap.clear();
        this.loomassistant$lastPersistedDyeMap.putAll(this.loomassistant$persistentDyeMap);

        if (this.loomassistant$panel != null) {
            this.loomassistant$panel.restorePersistentDyeSwitchState(
                    this.loomassistant$persistentDyeSwitchEnabled, Map.copyOf(this.loomassistant$persistentDyeMap));
        }
    }

    @Unique
    private static boolean loomassistant$isMouseOverWidget(AbstractWidget widget, int mouseX, int mouseY) {
        return mouseX >= widget.getX()
                && mouseX < widget.getX() + widget.getWidth()
                && mouseY >= widget.getY()
                && mouseY < widget.getY() + widget.getHeight();
    }

    @Unique
    private void loomassistant$setSingleLineTooltip(
            GuiGraphicsExtractor context, Component text, int mouseX, int mouseY) {
        context.setTooltipForNextFrame(this.font, List.of(text), Optional.empty(), mouseX, mouseY);
    }

    @Unique
    private boolean loomassistant$showEditOnSaveButton() {
        return loomassistant$panel != null
                && loomassistant$panel.hasActiveBanner()
                && !loomassistant$panel.isActiveBannerSavable();
    }

    @Unique
    private boolean loomassistant$shouldUseEditIconOnSaveButton() {
        if (loomassistant$panel == null || !loomassistant$panel.hasActiveBanner()) {
            return true;
        }
        return loomassistant$showEditOnSaveButton();
    }

    @Unique
    private void loomassistant$syncPerWorldUiState() {
        BannerRecipe activeRecipe = BannerRecipe.fromItem(this.loomassistant$activeBannerStack);
        String currentActiveJson = activeRecipe == null ? null : activeRecipe.toJson();
        boolean activeChanged = currentActiveJson == null
                ? this.loomassistant$lastPersistedActiveBannerJson != null
                : !currentActiveJson.equals(this.loomassistant$lastPersistedActiveBannerJson);
        if (activeChanged) {
            LoomUiStateStore.setPersistedActiveBannerStack(this.minecraft, this.loomassistant$activeBannerStack);
            this.loomassistant$lastPersistedActiveBannerJson = currentActiveJson;
        }

        boolean dyeChanged = this.loomassistant$lastPersistedDyeEnabled != this.loomassistant$persistentDyeSwitchEnabled
                || !this.loomassistant$lastPersistedDyeMap.equals(this.loomassistant$persistentDyeMap);
        if (dyeChanged) {
            LoomUiStateStore.setPersistentDyeState(
                    this.minecraft, this.loomassistant$persistentDyeSwitchEnabled, this.loomassistant$persistentDyeMap);
            this.loomassistant$lastPersistedDyeEnabled = this.loomassistant$persistentDyeSwitchEnabled;
            this.loomassistant$lastPersistedDyeMap.clear();
            this.loomassistant$lastPersistedDyeMap.putAll(this.loomassistant$persistentDyeMap);
        }
    }

    @Unique
    private static String loomassistant$buildMissingMaterialsMessage(
            BannerRecipe banner, AutoCraftStateMachine autoCraft) {
        List<String> missingMaterials = autoCraft.getMissingMaterialDescriptions(banner);
        if (missingMaterials.isEmpty()) {
            return null;
        }
        return Component.translatable("loom-assistant.active.missing_header").getString()
                + "\n"
                + String.join("\n", missingMaterials);
    }

    @Unique
    private void loomassistant$renderNextStepHint(GuiGraphicsExtractor context, int nextLayerIndex) {
        if (loomassistant$panel == null) return;
        BannerRecipe recipe = BannerPreviewRenderer.extractBannerData(loomassistant$activeBannerStack);
        if (recipe == null || nextLayerIndex >= recipe.getLayers().size()) return;

        var layer = recipe.getLayers().get(nextLayerIndex);
        int px = this.leftPos + 141;
        int py = this.topPos + 8;

        // Dye item (16×16) centred horizontally in the 20px preview area
        ItemStack dye = new ItemStack((net.minecraft.world.item.Item) BannerRecipe.getDyeItem(layer.getDyeColorEnum()));
        context.fakeItem(dye, px + 2, py + 2);

        // Pattern sprite (14×14) below the dye, using the same rendering as the tooltip
        try {
            Identifier patId = Identifier.tryParse(layer.patternId());
            if (patId != null && this.minecraft.level != null) {
                var reg = this.minecraft.level.registryAccess().lookup(Registries.BANNER_PATTERN);
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

    @Unique
    private void loomassistant$renderOutputSlotBorder(GuiGraphicsExtractor context, int progress) {
        // Result slot origin in screen coords (vanilla slot at container x=143, y=57)
        int rx = this.leftPos + 143;
        int ry = this.topPos + 57;
        ItemStack result = this.menu.getResultSlot().getItem();
        if (result.isEmpty()) return;

        boolean correct = loomassistant$resultMatchesExpected(result, progress);
        int color = correct ? 0xFF44FF44 : 0xFFFF4444;
        // Draw 1px border just outside the 16×16 slot
        context.fill(rx - 1, ry - 1, rx + 17, ry, color); // top
        context.fill(rx - 1, ry + 16, rx + 17, ry + 17, color); // bottom
        context.fill(rx - 1, ry, rx, ry + 16, color); // left
        context.fill(rx + 16, ry, rx + 17, ry + 16, color); // right
    }

    @Unique
    private boolean loomassistant$resultMatchesExpected(ItemStack result, int nextLayerIndex) {
        if (!(result.getItem() instanceof BannerItem bannerItem)) return false;
        BannerRecipe recipe = BannerPreviewRenderer.extractBannerData(loomassistant$activeBannerStack);
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

    @Unique
    private void loomassistant$playActiveSlotSetSound() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft
                    .player
                    .level()
                    .playLocalSound(
                            this.minecraft.player.getX(),
                            this.minecraft.player.getY(),
                            this.minecraft.player.getZ(),
                            LOOMASSISTANT_ACTIVE_SLOT_SET_SOUND,
                            SoundSource.PLAYERS,
                            0.42F,
                            1.0F,
                            false);
        }
    }

    @Unique
    private void loomassistant$playActiveSlotClearSound() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft
                    .player
                    .level()
                    .playLocalSound(
                            this.minecraft.player.getX(),
                            this.minecraft.player.getY(),
                            this.minecraft.player.getZ(),
                            LOOMASSISTANT_ACTIVE_SLOT_CLEAR_SOUND,
                            SoundSource.PLAYERS,
                            0.42F,
                            1.0F,
                            false);
        }
    }
}
