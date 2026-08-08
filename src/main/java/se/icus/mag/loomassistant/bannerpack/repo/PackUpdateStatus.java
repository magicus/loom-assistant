/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.repo;

public class PackUpdateStatus {
    public enum InstallStatus {
        NOT_INSTALLED,
        INSTALLED_UP_TO_DATE,
        UPDATE_AVAILABLE,
        CONFLICT_UNMANAGED
    }

    private final RemotePackEntry remoteEntry;
    private final InstallStatus status;

    public PackUpdateStatus(RemotePackEntry remoteEntry, InstallStatus status) {
        this.remoteEntry = remoteEntry;
        this.status = status;
    }

    public RemotePackEntry remoteEntry() {
        return remoteEntry;
    }

    public InstallStatus status() {
        return status;
    }
}
