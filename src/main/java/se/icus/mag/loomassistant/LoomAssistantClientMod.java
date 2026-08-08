/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import se.icus.mag.loomassistant.bannerpack.repo.BannerPackDownloadService;
import se.icus.mag.loomassistant.bannerpack.repo.BannerPackRepoClient;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;
import se.icus.mag.loomassistant.bannerpack.storage.InstalledPackRegistry;
import se.icus.mag.loomassistant.config.LoomAssistantConfig;

@Environment(EnvType.CLIENT)
public class LoomAssistantClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            client.execute(this::scheduleAutoInstall);
        });
    }

    private void scheduleAutoInstall() {
        LoomAssistantConfig.BannerPackRepoSettings repoSettings =
                LoomAssistantMod.getConfig().getBannerPackRepo();
        if (repoSettings.getAutoInstallPackIdList().isEmpty()) return;

        BannerStorage storage = BannerStorage.getInstance();
        InstalledPackRegistry registry =
                new InstalledPackRegistry(storage.getRepository().getPacksRoot());
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
                        net.minecraft.client.Minecraft.getInstance());
    }
}
