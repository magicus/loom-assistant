/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.extensions;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.LoomScreenStateManager;
import se.icus.mag.loomassistant.gui.ScreenExtension;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;
import se.icus.mag.loomassistant.recipe.converters.BannerRecipeItemConverter;

public class WeavingGuide implements ScreenExtension {
    private static final Identifier RECIPE_WEAVE_ICON =
            Identifier.fromNamespaceAndPath("loom-assistant", "textures/gui/recipe-weave.png");

    private final LoomScreen screen;
    private final LoomScreenStateManager manager;

    public WeavingGuide(LoomScreen screen, LoomScreenStateManager manager) {
        this.screen = screen;
        this.manager = manager;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!manager.hasActiveBanner()) return;

        Minecraft mc = screen.minecraft;
        boolean survivalNotWeavable = !manager.isEffectiveActiveBannerWeavable() && !mc.player.hasInfiniteMaterials();
        if (!survivalNotWeavable) {
            int progress = manager.detectCraftingProgress();
            if (progress >= 0) {
                if (screen.menu.getResultSlot().getItem().isEmpty()) {
                    renderNextStepHint(context, mc, progress);
                }
                renderOutputSlotBorder(context, progress);
            }
        } else if (screen.menu.getResultSlot().getItem().isEmpty()) {
            renderUncraftablePreview(context);
        }
    }

    public static void renderBannerPreview(
            GuiGraphicsExtractor context, Minecraft mc, BannerRecipe banner, int x, int y) {
        ItemStack bannerStack = BannerRecipeItemConverter.toItem(mc, banner);
        context.item(bannerStack, x, y);
    }

    public static boolean resultMatchesExpected(ItemStack activeBannerStack, ItemStack result, int nextLayerIndex) {
        if (!(result.getItem() instanceof BannerItem bannerItem)) return false;
        BannerRecipe recipe = BannerRecipeItemConverter.fromItem(activeBannerStack);
        if (recipe == null || nextLayerIndex >= recipe.getLayers().size()) return false;
        if (bannerItem.getColor() != recipe.getBannerColorEnum()) return false;

        BannerPatternLayers layers = result.get(DataComponents.BANNER_PATTERNS);
        if (layers == null) return false;
        int expected = nextLayerIndex + 1;
        if (layers.layers().size() != expected) return false;

        for (int i = 0; i < expected; i++) {
            BannerPatternLayers.Layer current = layers.layers().get(i);
            BannerRecipeLayer expectedLayer = recipe.getLayers().get(i);
            if (current.color() != expectedLayer.getDyeColorEnum()) return false;
            String currentId = current.pattern()
                    .unwrapKey()
                    .map(key -> key.identifier().toString())
                    .orElse(null);
            if (currentId == null) return false;
            String expectedId = expectedLayer.patternId().contains(":")
                    ? expectedLayer.patternId()
                    : "minecraft:" + expectedLayer.patternId();
            if (!currentId.equals(expectedId)) return false;
        }
        return true;
    }

    private void renderUncraftablePreview(GuiGraphicsExtractor context) {
        int iconSize = 16;
        int previewX = screen.leftPos + 141 + (20 - iconSize) / 2;
        int previewY = screen.topPos + 8 + (40 - iconSize) / 2 + 6;
        drawWeaveWithSlash(context, previewX, previewY, iconSize);
    }

    private void renderNextStepHint(GuiGraphicsExtractor context, Minecraft mc, int nextLayerIndex) {
        BannerRecipe recipe = manager.getEffectiveActiveBanner();
        if (recipe == null || nextLayerIndex >= recipe.getLayers().size()) return;

        BannerRecipeLayer layer = recipe.getLayers().get(nextLayerIndex);
        int previewX = screen.leftPos + 141;
        int previewY = screen.topPos + 8;

        ItemStack dye = new ItemStack(BannerRecipe.getDyeItem(layer.getDyeColorEnum()));
        context.fakeItem(dye, previewX + 2, previewY + 2);

        try {
            Identifier patternId = Identifier.tryParse(layer.patternId());
            if (patternId == null) return;

            Registry<BannerPattern> registry = LoomAssistantMod.getBannerPatternRegistry(mc);
            Optional<Holder.Reference<BannerPattern>> entry = registry.get(patternId);
            if (entry.isEmpty()) return;

            Holder<BannerPattern> holder = entry.get();
            TextureAtlasSprite sprite = context.getSprite(Sheets.getBannerSprite(holder));
            float u0 = sprite.getU0();
            float u1 = u0 + (sprite.getU1() - u0) * 21.0F / 64.0F;
            float vSpan = sprite.getV1() - sprite.getV0();
            float v0 = sprite.getV0() + vSpan / 64.0F;
            float v1 = v0 + vSpan * 40.0F / 64.0F;
            context.pose().pushMatrix();
            context.pose().translate(previewX + 3, previewY + 22);
            context.fill(0, 0, 7, 14, DyeColor.GRAY.getTextureDiffuseColor());
            context.blit(sprite.atlasLocation(), 0, 0, 7, 14, u0, u1, v0, v1);
            context.pose().popMatrix();
        } catch (RuntimeException ignored) {
        }
    }

    private void renderOutputSlotBorder(GuiGraphicsExtractor context, int progress) {
        int resultX = screen.leftPos + 143;
        int resultY = screen.topPos + 57;
        ItemStack result = screen.menu.getResultSlot().getItem();
        if (result.isEmpty()) return;

        boolean correct = resultMatchesExpected(manager.getEffectiveActiveBannerStack(), result, progress);
        int color = correct ? 0xFF44FF44 : 0xFFFF4444;
        context.fill(resultX - 1, resultY - 1, resultX + 17, resultY, color);
        context.fill(resultX - 1, resultY + 16, resultX + 17, resultY + 17, color);
        context.fill(resultX - 1, resultY, resultX, resultY + 16, color);
        context.fill(resultX + 16, resultY, resultX + 17, resultY + 16, color);
    }

    private static void drawWeaveWithSlash(GuiGraphicsExtractor context, int x, int y, int size) {
        context.blit(RenderPipelines.GUI_TEXTURED, RECIPE_WEAVE_ICON, x, y, 0f, 0f, size, size, size, size);
        for (int row = 0; row < size; row++) {
            int xOffset = size - 1 - row;
            context.fill(x + xOffset - 1, y + row, x + xOffset + 1, y + row + 1, 0xFFFF2222);
        }
    }
}
