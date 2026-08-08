/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.screens.packselection;

import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import se.icus.mag.loomassistant.bannerpack.storage.BannerPackModelEntry;

@Environment(EnvType.CLIENT)
public class BannerPackListWidget extends ObjectSelectionList<BannerPackListEntry> {
    private static final int ROW_HEIGHT = 36;
    private final Component title;
    private final BannerPackSelectionScreen screen;
    private final boolean active;

    public BannerPackListWidget(
            Minecraft minecraft,
            BannerPackSelectionScreen screen,
            int width,
            int height,
            Component title,
            boolean active) {
        super(minecraft, width, height, 33, ROW_HEIGHT);
        this.screen = screen;
        this.title = title;
        this.active = active;
        this.centerListVertically = false;
    }

    @Override
    public int getRowWidth() {
        return this.width - 4;
    }

    @Override
    protected int scrollBarX() {
        return this.getRight() - this.scrollbarWidth();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        BannerPackListEntry selected = this.getSelected();
        return selected != null ? selected.keyPressed(event) : super.keyPressed(event);
    }

    public void updateList(Stream<BannerPackModelEntry> entries) {
        this.clearEntries();
        Component header =
                Component.empty().append(this.title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
        this.addEntry(new HeaderEntry(this.minecraft.font, header), (int) (9.0F * 1.5F));
        this.setSelected(null);
        entries.forEach(entry -> this.addEntry(new PackEntry(this.minecraft, this.screen, this, entry, this.active)));
        this.refreshScrollAmount();
    }
}
