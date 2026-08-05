/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.types.bannerpack.BannerPack;

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
    private void loadFromModResources(List<BannerPack> packs) throws IOException {
        // Note: Loading from JAR resources in Minecraft mods is complex
        // and depends on the mod loader and resource location setup.
        // This is a placeholder that can be enhanced.

        // Try to access mod container and iterate resources
        FabricLoader loader = FabricLoader.getInstance();
        for (var modContainer : loader.getAllMods()) {
            if (modContainer.getMetadata().getId().equals(LoomAssistantMod.MOD_ID)) {
                // Try to find banner pack resources in this mod
                try {
                    loadFromModContainer(modContainer, packs);
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
    private void loadFromModContainer(net.fabricmc.loader.api.ModContainer modContainer, List<BannerPack> packs)
            throws IOException {
        // This is a simplified placeholder.
        // In a real implementation, you would:
        // 1. Access the mod's JAR file
        // 2. Look for zip files in data/loom-assistant/bannerpacks/
        // 3. Extract them to cache and load as ZipBannerPack

        // For now, this serves as documentation of the intended approach
        LoomAssistantMod.LOGGER.debug(
                "Attempting to load bundled packs from mod: {}",
                modContainer.getMetadata().getId());
    }

    /**
     * Extracts a resource from the JAR to the cache directory.
     */
    private Path extractResourceToCache(String resourcePath, String cacheFileName) throws IOException {
        Path cachePath = cacheRoot.resolve(cacheFileName);

        // Skip if already cached
        if (Files.exists(cachePath)) {
            return cachePath;
        }

        try (InputStream in = getResourceStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Files.createDirectories(cachePath.getParent());
            Files.copy(in, cachePath);
        }

        return cachePath;
    }

    /**
     * Gets an input stream for a classpath resource.
     */
    private InputStream getResourceStream(String resourcePath) {
        return JarBannerPackLoader.class.getResourceAsStream("/" + resourcePath);
    }
}
