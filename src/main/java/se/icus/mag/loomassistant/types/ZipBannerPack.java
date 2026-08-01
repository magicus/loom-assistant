/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types;

import java.io.IOException;
import java.nio.file.Path;

public class ZipBannerPack extends BannerPack {
    public ZipBannerPack(BannerPackMetadata metadata, Path path, Path bannersPath) throws IOException {
        super(metadata, path);
        loadDesignsFromPath(bannersPath);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public BannerDesign addBannerDesign(BannerDesign design) {
        throw new IllegalStateException(
                "Cannot add design to read-only pack " + getMetadata().id());
    }

    @Override
    public BannerDesign updateBannerDesign(BannerDesign design) {
        throw new IllegalStateException(
                "Cannot update design in read-only pack " + getMetadata().id());
    }

    @Override
    public void removeBannerDesign(String designId) {
        throw new IllegalStateException(
                "Cannot remove design from read-only pack " + getMetadata().id());
    }
}
