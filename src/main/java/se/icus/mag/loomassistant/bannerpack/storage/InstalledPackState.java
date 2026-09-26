/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.storage;

public record InstalledPackState(String packId, String fileName, String sourceUrl, String sha256, String installedAt) {
}
