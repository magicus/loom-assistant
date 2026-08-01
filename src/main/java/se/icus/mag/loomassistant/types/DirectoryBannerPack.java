/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class DirectoryBannerPack extends BannerPack {
    public DirectoryBannerPack(BannerPackMetadata metadata, Path path) {
        super(metadata, path);
    }

    public DirectoryBannerPack(BannerPackMetadata metadata, Path path, Path bannersPath) throws IOException {
        this(metadata, path);
        loadDesignsFromPath(bannersPath);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public BannerDesign addBannerDesign(BannerDesign design) throws IOException {
        BannerDesign normalized = normalizeBannerDesignForPack(design);
        writeDesign(normalized);
        return normalized;
    }

    @Override
    public BannerDesign updateBannerDesign(BannerDesign design) throws IOException {
        if (design.id() == null || design.id().isBlank()) {
            throw new IllegalArgumentException("design id is required for update");
        }
        return addBannerDesign(design);
    }

    @Override
    public void removeBannerDesign(String designId) throws IOException {
        deleteDesignFile(designId);
    }

    public BannerDesign moveDesignTo(BannerPack target, String designId) throws IOException {
        if (target.isReadOnly()) {
            throw new IllegalArgumentException("cannot move to read-only pack");
        }
        BannerDesign design = getDesign(designId);
        if (design == null) {
            throw new IllegalArgumentException("design not found: " + designId);
        }
        BannerDesign moved = target.addBannerDesign(design.withId(null));
        removeBannerDesign(designId);
        return moved;
    }

    private BannerDesign normalizeBannerDesignForPack(BannerDesign design) {
        String id = design.id();
        if (id == null || id.isBlank()) {
            return design.withId(
                    getMetadata().id() + ":" + UUID.randomUUID().toString().replace("-", ""));
        } else if (!id.contains(":")) {
            return design.withId(getMetadata().id() + ":" + id);
        }
        return design;
    }

    private void writeDesign(BannerDesign design) throws IOException {
        Path designFile = getDesignFile(design.id());
        Files.createDirectories(designFile.getParent());
        try (Writer writer = Files.newBufferedWriter(designFile)) {
            writer.write(design.toJson());
        }
        includeDesign(design);
    }

    private void deleteDesignFile(String designId) throws IOException {
        excludeDesign(designId);
        Files.deleteIfExists(getDesignFile(designId));
    }

    private Path getDesignFile(String designId) {
        int colon = designId.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("design id must be in namespace:name format: " + designId);
        }
        String namespace = designId.substring(0, colon);
        String name = designId.substring(colon + 1);
        return getPath().resolve(BANNERS_DIR).resolve(namespace).resolve(name + ".json");
    }
}
