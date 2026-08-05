/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.icus.mag.loomassistant.ui.LoomRecipePanel;
import se.icus.mag.loomassistant.ui.extensions.LoomScreenAdapter;
import se.icus.mag.loomassistant.ui.extensions.LoomScreenExtension;
import se.icus.mag.loomassistant.ui.support.LoomActiveBannerHost;
import se.icus.mag.loomassistant.ui.support.LoomPanelHost;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin extends AbstractContainerScreen<LoomMenu>
        implements LoomPanelHost, LoomActiveBannerHost, LoomScreenAdapter {
    @Unique
    private LoomScreenExtension loomassistant$extension;

    public LoomScreenMixin(LoomMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    // ── LoomScreenAdapter implementation ──────────────────────────────────────

    @Override
    public int loomassistant$getLeftPos() {
        return this.leftPos;
    }

    @Override
    public void loomassistant$setLeftPos(int x) {
        this.leftPos = x;
    }

    @Override
    public int loomassistant$getTopPos() {
        return this.topPos;
    }

    @Override
    public int loomassistant$getImageWidth() {
        return this.imageWidth;
    }

    @Override
    public int loomassistant$getImageHeight() {
        return this.imageHeight;
    }

    @Override
    public int loomassistant$getScreenWidth() {
        return this.width;
    }

    @Override
    public Font loomassistant$getFont() {
        return this.font;
    }

    @Override
    public Minecraft loomassistant$getMinecraft() {
        return this.minecraft;
    }

    @Override
    public LoomMenu loomassistant$getMenu() {
        return this.menu;
    }

    @Override
    public LoomScreen loomassistant$asLoomScreen() {
        return (LoomScreen) (Object) this;
    }

    @Override
    public <T extends AbstractWidget> T loomassistant$addWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    // ── mixin hooks ───────────────────────────────────────────────────────────

    @Inject(method = "init", at = @At("TAIL"))
    private void loomassistant$onInit(CallbackInfo ci) {
        this.loomassistant$extension = new LoomScreenExtension(this);
        this.loomassistant$extension.onInit();
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
        this.loomassistant$extension.drawCustomBackground(
                graphics, renderPipeline, originalTexture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void loomassistant$onExtractBackground(
            GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.loomassistant$extension.onExtractBackground(context, mouseX, mouseY, delta);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseClicked(
            MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        this.loomassistant$extension.onMouseClicked(mouseButtonEvent, cir);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseReleased(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        this.loomassistant$extension.onMouseReleased(mouseButtonEvent, cir);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void loomassistant$onMouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount,
            CallbackInfoReturnable<Boolean> cir) {
        this.loomassistant$extension.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount, cir);
    }

    // ── LoomPanelHost / LoomActiveBannerHost ──────────────────────────────────

    @Override
    public LoomRecipePanel loomassistant$getPanel() {
        return this.loomassistant$extension.getPanel();
    }

    @Override
    public void loomassistant$setPendingActiveBannerStack(ItemStack stack) {
        this.loomassistant$extension.setPendingActiveBannerStack(stack);
    }

    @Override
    public void loomassistant$setPersistentDyeSwitchState(boolean enabled, Map<DyeColor, DyeColor> replacements) {
        this.loomassistant$extension.setPersistentDyeSwitchState(enabled, replacements);
    }
}
