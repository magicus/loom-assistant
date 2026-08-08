/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.storage;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public record BannerPackModelEntry(
        String packId,
        String title,
        Component description,
        Component secondLine,
        Identifier iconTexture,
        boolean active) {
    public BannerPackModelEntry withActive(boolean active) {
        return new BannerPackModelEntry(
                this.packId, this.title, this.description, this.secondLine, this.iconTexture, active);
    }

    public Component getTitle() {
        return Component.literal(this.title);
    }
}
