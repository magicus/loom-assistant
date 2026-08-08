/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.repo;

public record RemotePackEntry(
        String id,
        String title,
        String description,
        String author,
        String downloadUrl,
        String sha256,
        long sizeBytes,
        String publishedAt,
        String iconData) {
    public String displayTitle() {
        return title != null && !title.isBlank() ? title : id;
    }
}
