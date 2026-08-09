/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.icus.mag.loomassistant.bannerpack.repo.BannerPackDownloadService;
import se.icus.mag.loomassistant.bannerpack.repo.BannerPackRepoClient;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;
import se.icus.mag.loomassistant.bannerpack.storage.InstalledPackRegistry;
import se.icus.mag.loomassistant.config.LoomAssistantConfig;
import se.icus.mag.loomassistant.config.NestedPathConfigSerializer;

public class LoomAssistantMod implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "loom-assistant";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final LoomScreenStateManager LOOM_MANAGER = new LoomScreenStateManager();

    public static LoomScreenStateManager getLoomManager() {
        return LOOM_MANAGER;
    }

    public static LoomAssistantConfig getConfig() {
        return AutoConfig.getConfigHolder(LoomAssistantConfig.class).getConfig();
    }

    public static Registry<BannerPattern> getBannerPatternRegistry(Minecraft mc) {
        Registry<BannerPattern> registry =
                mc.level.registryAccess().lookup(Registries.BANNER_PATTERN).orElse(null);
        if (registry == null) {
            LOGGER.error("BannerPattern registry is unavailable — this should never happen");
            throw new IllegalStateException("BannerPattern registry is unavailable");
        }
        return registry;
    }

    @Override
    public void onInitialize() {
        LoomAssistantMod.LOGGER.info("Initializing LoomAssistantMod");
        AutoConfig.register(LoomAssistantConfig.class, NestedPathConfigSerializer::new);
        BannerStorage.getInstance().load();
    }

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
            mc.execute(() -> scheduleAutoInstall(mc));
        });
    }

    private void scheduleAutoInstall(Minecraft mc) {
        LoomAssistantConfig.BannerPackRepoSettings repoSettings =
                LoomAssistantMod.getConfig().getBannerPackRepo();
        if (repoSettings.getAutoInstallPackIdList().isEmpty()) return;

        BannerStorage storage = BannerStorage.getInstance();
        InstalledPackRegistry registry = new InstalledPackRegistry();
        BannerPackRepoClient repoClient = new BannerPackRepoClient(repoSettings.repoIndexUrl);
        BannerPackDownloadService service = new BannerPackDownloadService(
                repoClient, registry, storage.getRepository().getPacksRoot());

        java.util.concurrent.CompletableFuture.runAsync(
                        () -> service.autoInstallDefaults(repoSettings.getAutoInstallPackIdList(), true),
                        net.minecraft.util.Util.ioPool())
                .thenRunAsync(
                        () -> {
                            storage.load();
                            LoomAssistantMod.LOGGER.info("Auto-install of default packs completed");
                        },
                        mc);
    }
}
