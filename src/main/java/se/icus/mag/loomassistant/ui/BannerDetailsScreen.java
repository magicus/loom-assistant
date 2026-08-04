/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import se.icus.mag.loomassistant.types.BannerRecipe;
import se.icus.mag.loomassistant.types.BannerRecipeLayer;

public class BannerDetailsScreen extends Screen {
    private static final int PANEL_W = 170;
    private static final int PADDING = 6;
    private static final int BTN_H = 16;
    private static final int ROW_H = 18;

    private final Screen previousScreen;
    private final BannerRecipe banner;

    public BannerDetailsScreen(Screen previousScreen, BannerRecipe banner) {
        super(Component.translatable("loom-assistant.screen.banner_details.title"));
        this.previousScreen = previousScreen;
        this.banner = banner;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        int panelH = panelHeight();
        int cx = (this.width - PANEL_W) / 2;
        int cy = (this.height - panelH) / 2;

        ctx.fill(0, 0, this.width, this.height, 0xAA000000);
        ctx.fill(cx, cy, cx + PANEL_W, cy + panelH, 0xFF1A1A2E);
        ctx.outline(cx, cy, PANEL_W, panelH, 0xFF4169E1);

        int ty = cy + PADDING;

        BannerPreviewRenderer.render(ctx, banner, null, cx + (PANEL_W - 16) / 2, ty, 16);
        ty += 20;

        String name = trunc(banner.getDisplayName(), PANEL_W - PADDING * 2);
        ctx.text(this.font, Component.literal(name), cx + (PANEL_W - this.font.width(name)) / 2, ty, 0xFFFFFFFF, true);
        ty += 11;

        ctx.fill(cx + PADDING, ty, cx + PANEL_W - PADDING, ty + 1, 0xFF4169E1);
        ty += 1 + PADDING;

        int lx = cx + PADDING;
        int iconW = 16;
        int textX = lx + (iconW + 2) * 2;
        int textMaxW = PANEL_W - PADDING * 2 - (iconW + 2) * 2;

        // Base banner row
        ctx.item(new ItemStack(banner.getBaseBannerItem()), lx, ty + 1);
        String baseName = Language.getInstance()
                .getOrDefault("block.minecraft." + banner.getBaseColorEnum().getSerializedName() + "_banner");
        ctx.text(this.font, Component.literal(trunc(baseName, textMaxW)), textX, ty + 5, 0xFFAAAAAA, true);
        ty += ROW_H;

        for (BannerRecipeLayer layer : banner.getLayers()) {
            BannerPreviewRenderer.render(
                    ctx, new BannerRecipe(null, DyeColor.WHITE, List.of(layer)), null, lx, ty + 1, 16);
            ctx.item(new ItemStack(BannerRecipe.getDyeItem(layer.getDyeColorEnum())), lx + iconW + 2, ty + 1);
            ctx.text(
                    this.font,
                    Component.literal(trunc(getPatternName(layer), textMaxW)),
                    textX,
                    ty + 5,
                    0xFFAAAAAA,
                    true);
            ty += ROW_H;
        }

        int btnY = cy + panelH - BTN_H - PADDING;
        int btnX = cx + PADDING;
        int btnW = PANEL_W - PADDING * 2;
        boolean hov = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + BTN_H;
        ctx.fill(btnX, btnY, btnX + btnW, btnY + BTN_H, hov ? 0xFF5C7CFA : 0xFF1E40AF);
        String cl = Component.translatable("loom-assistant.common.close").getString();
        ctx.text(
                this.font,
                Component.literal(cl),
                btnX + (btnW - this.font.width(cl)) / 2,
                btnY + (BTN_H - 7) / 2,
                0xFFFFFFFF,
                true);

        super.extractRenderState(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return false;
        int panelH = panelHeight();
        int cx = (this.width - PANEL_W) / 2;
        int cy = (this.height - panelH) / 2;
        int btnY = cy + panelH - BTN_H - PADDING;
        int btnX = cx + PADDING;
        int btnW = PANEL_W - PADDING * 2;
        if (event.x() >= btnX && event.x() < btnX + btnW && event.y() >= btnY && event.y() < btnY + BTN_H) {
            this.minecraft.gui.setScreen(previousScreen);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.gui.setScreen(previousScreen);
            return true;
        }
        return super.keyPressed(event);
    }

    private int panelHeight() {
        return PADDING
                + 20
                + 11
                + PADDING
                + 1
                + PADDING
                + (1 + banner.getLayers().size()) * ROW_H
                + PADDING
                + BTN_H
                + PADDING;
    }

    @SuppressWarnings("unchecked")
    private String getPatternName(BannerRecipeLayer layer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            try {
                var regOpt = mc.level.registryAccess().lookup(Registries.BANNER_PATTERN);
                if (regOpt.isPresent()) {
                    Identifier id = Identifier.tryParse(layer.patternId());
                    if (id != null) {
                        var entry = regOpt.get().get(id);
                        if (entry.isPresent()) {
                            net.minecraft.world.level.block.entity.BannerPattern pat =
                                    (net.minecraft.world.level.block.entity.BannerPattern)
                                            entry.get().value();
                            return Component.translatable(pat.translationKey() + "."
                                            + layer.getDyeColorEnum().getName())
                                    .getString();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return toTitle(layer.getDyeColorEnum().getSerializedName()) + " " + toTitle(layer.patternId());
    }

    private static String toTitle(String id) {
        String raw = id.contains(":") ? id.split(":", 2)[1] : id;
        StringBuilder sb = new StringBuilder();
        for (String w : raw.split("_")) {
            if (!sb.isEmpty()) sb.append(' ');
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private String trunc(String text, int maxW) {
        if (this.font.width(text) <= maxW) return text;
        while (!text.isEmpty() && this.font.width(text + "..") > maxW) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "..";
    }
}
