/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ActivePacksConfigTest {
    @TempDir
    private Path tempDir;

    @Test
    void canReloadChangesWrittenByAnotherInstance() {
        Path configFile = tempDir.resolve("config").resolve("loom-assistant").resolve("bannerpacks.json");
        ActivePacksConfig first = new ActivePacksConfig(configFile);
        ActivePacksConfig second = new ActivePacksConfig(configFile);

        second.setActivePacks(List.of(BannerPackRepository.LOCAL_PACK_ID, "categories"));
        first.reloadFromDisk();

        assertEquals(List.of(BannerPackRepository.LOCAL_PACK_ID, "categories"), first.getActivePacks());
    }
}
