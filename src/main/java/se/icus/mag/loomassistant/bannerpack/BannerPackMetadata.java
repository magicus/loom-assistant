/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack;

public record BannerPackMetadata(String id, String description, String author, String url) {
    public BannerPackMetadata(String id, String description) {
        this(id, description, null, null);
    }
}
