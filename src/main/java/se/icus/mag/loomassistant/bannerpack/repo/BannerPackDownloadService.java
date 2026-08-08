/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.repo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.util.Util;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.bannerpack.storage.ActivePacksConfig;
import se.icus.mag.loomassistant.bannerpack.storage.BannerPackRepository;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;
import se.icus.mag.loomassistant.bannerpack.storage.InstalledPackRegistry;
import se.icus.mag.loomassistant.bannerpack.storage.InstalledPackState;

/**
 * Orchestrates downloading, installing, updating, and deleting repository-managed banner packs.
 */
public class BannerPackDownloadService {
    private final BannerPackRepoClient client;
    private final InstalledPackRegistry registry;
    private final Path packsRoot;

    public BannerPackDownloadService(BannerPackRepoClient client, InstalledPackRegistry registry, Path packsRoot) {
        this.client = client;
        this.registry = registry;
        this.packsRoot = packsRoot;
    }

    /**
     * Fetches the remote index asynchronously on the IO pool.
     */
    public CompletableFuture<RemoteRepoIndex> fetchIndex() {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return client.fetchIndex();
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                },
                Util.ioPool());
    }

    /**
     * Computes the install status of each pack in the index relative to local state.
     */
    public List<PackUpdateStatus> getPackStatuses(RemoteRepoIndex index) {
        BannerPackRepository repo = BannerStorage.getInstance().getRepository();
        List<PackUpdateStatus> statuses = new ArrayList<>();
        for (RemotePackEntry entry : index.packs()) {
            InstalledPackState installed = registry.getState(entry.id());
            PackUpdateStatus.InstallStatus status;
            if (installed == null) {
                boolean conflictsWithUnmanaged = repo != null && repo.getPack(entry.id()) != null;
                status = conflictsWithUnmanaged
                        ? PackUpdateStatus.InstallStatus.CONFLICT_UNMANAGED
                        : PackUpdateStatus.InstallStatus.NOT_INSTALLED;
            } else if (entry.sha256().equalsIgnoreCase(installed.sha256())) {
                status = PackUpdateStatus.InstallStatus.INSTALLED_UP_TO_DATE;
            } else {
                status = PackUpdateStatus.InstallStatus.UPDATE_AVAILABLE;
            }
            statuses.add(new PackUpdateStatus(entry, status));
        }
        return statuses;
    }

    /**
     * Downloads and installs a pack asynchronously. isUpdate=true allows overwriting a managed pack.
     */
    public CompletableFuture<InstallResult> install(RemotePackEntry entry, boolean activate, boolean isUpdate) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return doInstall(entry, activate, isUpdate);
                    } catch (IOException e) {
                        LoomAssistantMod.LOGGER.error("Failed to install pack {}", entry.id(), e);
                        return InstallResult.DOWNLOAD_FAILED;
                    }
                },
                Util.ioPool());
    }

    private InstallResult doInstall(RemotePackEntry entry, boolean activate, boolean isUpdate) throws IOException {
        boolean existsOnDisk = Files.exists(packsRoot.resolve(entry.id() + ".zip"))
                || Files.isDirectory(packsRoot.resolve(entry.id()));
        boolean isManaged = registry.isManaged(entry.id());

        if (existsOnDisk && !isManaged) return InstallResult.CONFLICT_UNMANAGED;
        if (existsOnDisk && isManaged && !isUpdate) return InstallResult.CONFLICT_UNMANAGED;

        Path targetZip = packsRoot.resolve(entry.id() + ".zip");
        Path tmpZip = packsRoot.resolve(entry.id() + ".zip.tmp");

        try {
            client.downloadZip(entry.downloadUrl(), tmpZip);
            String actualSha = sha256Hex(tmpZip);
            if (!actualSha.equalsIgnoreCase(entry.sha256())) {
                LoomAssistantMod.LOGGER.error(
                        "SHA256 mismatch for {}: expected {} got {}", entry.id(), entry.sha256(), actualSha);
                Files.deleteIfExists(tmpZip);
                return InstallResult.HASH_MISMATCH;
            }
            try {
                Files.move(tmpZip, targetZip, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmpZip, targetZip, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Files.deleteIfExists(tmpZip);
            throw e;
        }

        String now = Instant.now().toString();
        String activatedAt = activate ? now : null;
        InstalledPackState state = new InstalledPackState(
                entry.id(), entry.id() + ".zip", entry.downloadUrl(), entry.sha256(), now, activatedAt);
        registry.put(state);

        if (activate) {
            BannerStorage.getInstance().getActivePacksConfig().enablePack(entry.id());
        }

        boolean wasUpdate = isManaged;
        return wasUpdate ? InstallResult.SUCCESS_UPDATED : InstallResult.SUCCESS_NEW;
    }

    /**
     * Deletes a managed pack from disk and the registry. Returns false if not managed.
     */
    public boolean delete(String packId) {
        if (!registry.isManaged(packId)) return false;

        InstalledPackState state = registry.getState(packId);
        String fileName = state != null ? state.fileName() : packId + ".zip";

        ActivePacksConfig activeConfig = BannerStorage.getInstance().getActivePacksConfig();
        if (activeConfig != null) {
            activeConfig.disablePack(packId);
        }

        try {
            Path zipFile = packsRoot.resolve(fileName);
            Files.deleteIfExists(zipFile);
            Path dirFile = packsRoot.resolve(packId);
            if (Files.isDirectory(dirFile)) {
                deleteRecursively(dirFile);
            }
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.error("Failed to delete pack {} from disk", packId, e);
        }

        registry.remove(packId);
        return true;
    }

    /**
     * Auto-installs any packs from defaultPackIds that are not yet present on disk.
     * Intended for startup use. Does NOT call BannerStorage.load() – caller must do that.
     */
    public void autoInstallDefaults(List<String> defaultPackIds, boolean activate) {
        if (defaultPackIds.isEmpty()) return;

        RemoteRepoIndex index;
        try {
            index = client.fetchIndex();
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.warn("Auto-install skipped: could not fetch repo index – {}", e.getMessage());
            return;
        }

        for (String packId : defaultPackIds) {
            if (registry.isManaged(packId)) continue;
            if (Files.exists(packsRoot.resolve(packId + ".zip")) || Files.isDirectory(packsRoot.resolve(packId))) {
                LoomAssistantMod.LOGGER.info("Skipping auto-install of '{}': already on disk", packId);
                continue;
            }

            RemotePackEntry entry = index.packs().stream()
                    .filter(p -> p.id().equals(packId))
                    .findFirst()
                    .orElse(null);
            if (entry == null) {
                LoomAssistantMod.LOGGER.warn("Auto-install: pack '{}' not found in repo index", packId);
                continue;
            }

            try {
                InstallResult result = doInstall(entry, activate, false);
                LoomAssistantMod.LOGGER.info("Auto-installed '{}': {}", packId, result);
            } catch (IOException e) {
                LoomAssistantMod.LOGGER.warn("Auto-install of '{}' failed: {}", packId, e.getMessage());
            }
        }
    }

    private static String sha256Hex(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) {
                    digest.update(buf, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    LoomAssistantMod.LOGGER.warn("Failed to delete {}", p, e);
                }
            });
        }
    }
}
