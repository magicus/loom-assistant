/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.storage;

public final class InstalledPackState {
    private String packId;
    private String fileName;
    private String sourceUrl;
    private String sha256;
    private String installedAt;
    private String activatedAt;

    public InstalledPackState() {}

    public InstalledPackState(
            String packId, String fileName, String sourceUrl, String sha256, String installedAt, String activatedAt) {
        this.packId = packId;
        this.fileName = fileName;
        this.sourceUrl = sourceUrl;
        this.sha256 = sha256;
        this.installedAt = installedAt;
        this.activatedAt = activatedAt;
    }

    public String packId() {
        return packId;
    }

    public String fileName() {
        return fileName;
    }

    public String sourceUrl() {
        return sourceUrl;
    }

    public String sha256() {
        return sha256;
    }

    public String installedAt() {
        return installedAt;
    }

    public String activatedAt() {
        return activatedAt;
    }

    public InstalledPackState withActivatedAt(String activatedAt) {
        return new InstalledPackState(packId, fileName, sourceUrl, sha256, installedAt, activatedAt);
    }
}
