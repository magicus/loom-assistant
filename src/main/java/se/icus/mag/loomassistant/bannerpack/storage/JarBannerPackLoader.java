/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.bannerpack.BannerPack;

/**
 * Loader for banner packs bundled with the mod JAR.
 *
 * <p>Bundled packs should be stored in the classpath at:
 * `/data/loom-assistant/bannerpacks/*.zip`
 *
 * <p>These are loaded as read-only packs into a cache directory.
 */
public class JarBannerPackLoader {
    private static final String JAR_PACK_DIR = "data/" + LoomAssistantMod.MOD_ID + "/bannerpacks";
    private static final String CACHE_DIR = "jarBannerPacks";

    private final Path cacheRoot;

    public JarBannerPackLoader() {
        this.cacheRoot = FabricLoader.getInstance()
                .getGameDir()
                .resolve(".cache")
                .resolve(LoomAssistantMod.MOD_ID)
                .resolve(CACHE_DIR);
    }

    /**
     * Loads all banner packs bundled with the mod JAR.
     *
     * @return A list of loaded banner packs
     */
    public List<BannerPack> loadBundledPacks() {
        List<BannerPack> packs = new ArrayList<>();

        try {
            Files.createDirectories(cacheRoot);

            // Try to load from classpath resources
            // This depends on how the JAR is structured
            loadFromModResources(packs);
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.error("Failed to load bundled banner packs", e);
        }

        return packs;
    }

    /**
     * Attempts to load banner packs from mod resources.
     * This is a simplified implementation that may need adjustment
     * based on actual JAR structure and Fabric API capabilities.
     */
    private void loadFromModResources(List<BannerPack> packs) {
        FabricLoader loader = FabricLoader.getInstance();
        for (ModContainer modContainer : loader.getAllMods()) {
            if (modContainer.getMetadata().getId().equals(LoomAssistantMod.MOD_ID)) {
                try {
                    loadFromModContainer(modContainer, packs);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    LoomAssistantMod.LOGGER.debug("Could not load bundled packs from mod container", e);
                }
            }
        }
    }

    /**
     * Loads banner packs from a specific mod container.
     * This is framework-specific and may need adjustment.
     */
    private void loadFromModContainer(ModContainer modContainer, List<BannerPack> packs) throws IOException {
        Optional<Path> resourceDir = modContainer.findPath(JAR_PACK_DIR);
        if (resourceDir.isEmpty() || !Files.isDirectory(resourceDir.get())) {
            LoomAssistantMod.LOGGER.debug("No bundled banner pack directory found at {}", JAR_PACK_DIR);
            return;
        }

        try (Stream<Path> stream = Files.list(resourceDir.get())) {
            for (Path zipResource : stream.filter(
                            path -> path.getFileName().toString().endsWith(".zip"))
                    .toList()) {
                String zipName = zipResource.getFileName().toString();
                String packId = zipName.substring(0, zipName.length() - ".zip".length());
                Path cachedZip = extractResourceToCache(zipResource, zipName);
                BannerPack pack = BannerPack.loadZipPack(cachedZip, packId);
                if (pack != null) {
                    packs.add(pack);
                    LoomAssistantMod.LOGGER.info("Loaded bundled banner pack {}", packId);
                }
            }
        }
    }

    /**
     * Extracts a resource from the JAR to the cache directory.
     */
    private Path extractResourceToCache(String resourcePath, String cacheFileName) throws IOException {
        Path cachePath = cacheRoot.resolve(cacheFileName);

        try (InputStream in = getResourceStream(resourcePath)) {
            if (in == null) throw new IOException("Resource not found: " + resourcePath);

            Files.createDirectories(cachePath.getParent());
            Files.copy(in, cachePath, StandardCopyOption.REPLACE_EXISTING);
        }

        return cachePath;
    }

    private Path extractResourceToCache(Path resourcePath, String cacheFileName) throws IOException {
        Path cachePath = cacheRoot.resolve(cacheFileName);
        Files.createDirectories(cachePath.getParent());
        Files.copy(resourcePath, cachePath, StandardCopyOption.REPLACE_EXISTING);
        return cachePath;
    }

    /**
     * Gets an input stream for a classpath resource.
     */
    private InputStream getResourceStream(String resourcePath) {
        return JarBannerPackLoader.class.getResourceAsStream("/" + resourcePath);
    }
}
