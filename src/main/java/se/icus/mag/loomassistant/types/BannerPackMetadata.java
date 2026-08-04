/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types;

import java.util.List;

public record BannerPackMetadata(
        String id, String description, String author, String url, List<BannerRecipeCategory> categories) {
    public BannerPackMetadata(String id, String description) {
        this(id, description, null, null, List.of());
    }

    public BannerPackMetadata withAuthor(String newAuthor) {
        return new BannerPackMetadata(id, description, newAuthor, url, categories);
    }

    public BannerPackMetadata withUrl(String newUrl) {
        return new BannerPackMetadata(id, description, author, newUrl, categories);
    }
}
