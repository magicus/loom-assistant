/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BannerPackRepositoryTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @TempDir
    Path tempDir;

    @Test
    void createsRootPackWhenMissing() {
        Path packsRoot = tempDir.resolve("bannerpacks");
        BannerPackRepository repository = new BannerPackRepository(packsRoot);

        repository.load();

        assertTrue(Files.exists(packsRoot.resolve("root")));
        assertTrue(Files.exists(packsRoot.resolve("root").resolve("bannerpack.mcmeta")));
        assertTrue(Files.exists(packsRoot.resolve("root").resolve("banners")));
        assertNotNull(repository.getPack("root"));
    }

    @Test
    void canCreateAndSerializeDesign() {
        BannerDesign design = new BannerDesign(
                "Letter A",
                DyeColor.LIGHT_BLUE,
                List.of(BannerDesignLayer.of("minecraft:small_stripes", DyeColor.BLUE.getName())));

        String json = design.toJson();
        BannerDesign parsed = BannerDesign.fromJson(json);

        assertTrue(json.contains("\"color\":\"blue\""));
        assertTrue(json.contains("\"banner_color\":\"light_blue\""));
        assertFalse(json.contains("\"author\""));
        assertFalse(json.contains("\"id\""));
        assertFalse(json.contains("\"dye\""));
        assertFalse(json.contains("\"baseColor\""));
        assertEquals("Letter A", parsed.description());
        assertEquals("light_blue", parsed.bannerColor());
        assertEquals(1, parsed.layers().size());
        assertEquals("minecraft:small_stripes", parsed.layers().get(0).pattern().toString());

        BannerDesign whiteDesign = new BannerDesign("Test", DyeColor.WHITE, List.of());
        String whiteJson = whiteDesign.toJson();
        assertTrue(whiteJson.contains("\"banner_color\":\"white\""));
        BannerDesign whiteParsed = BannerDesign.fromJson(whiteJson);
        assertEquals("white", whiteParsed.bannerColor());
    }

    @Test
    void missingRequiredFieldsFailParsing() {
        assertThrows(IllegalStateException.class, () -> BannerDesign.fromJson("{\"layers\":[]}"));
        assertThrows(IllegalStateException.class, () -> BannerDesign.fromJson("{\"description\":\"x\",\"layers\":[]}"));
        assertThrows(
                IllegalStateException.class,
                () -> BannerDesign.fromJson("{\"description\":\"x\",\"banner_color\":\"white\"}"));
    }

    @Test
    void canCreateMoveAndDeleteDesignsAndPacks() throws Exception {
        BannerPackRepository repository = new BannerPackRepository(tempDir.resolve("bannerpacks"));
        repository.load();

        repository.createPack("letters", "Letters");
        assertNotNull(repository.getPack("letters"));

        BannerDesign design = new BannerDesign(
                "Letter B",
                DyeColor.LIGHT_BLUE,
                List.of(BannerDesignLayer.of("minecraft:cross", DyeColor.WHITE.getName())));
        BannerDesign created =
                repository.getPack(BannerPackRepository.ROOT_PACK_ID).addBannerDesign(design);
        assertNotNull(created.id());
        assertNotNull(repository.getBannerDesignById(created.id()));
        assertEquals(BannerPackRepository.ROOT_PACK_ID, repository.getBannerDesignPackId(created.id()));

        BannerDesign moved = repository.moveBannerDesign(BannerPackRepository.ROOT_PACK_ID, "letters", created.id());
        assertEquals("letters", repository.getBannerDesignPackId(moved.id()));

        repository.getPack("letters").removeBannerDesign(moved.id());
        assertNull(repository.getBannerDesignById(moved.id()));

        repository.deletePack("letters");
        assertNull(repository.getPack("letters"));
        assertFalse(Files.exists(tempDir.resolve("bannerpacks").resolve("letters")));
    }

    @Test
    void supportsGiveImportAndExportFormats() {
        String complexBanner =
                "minecraft:light_blue_banner[banner_patterns=[{\"pattern\":\"cross\",\"color\":\"white\"},"
                        + "{\"pattern\":\"minecraft:curly_border\",\"color\":\"black\"},"
                        + "{\"pattern\":\"triangle_top\",\"color\":\"yellow\"}]]";
        BannerDesign fromGive = BannerDesign.fromCommand("/give @p " + complexBanner);
        assertNotNull(fromGive);
        assertEquals("light_blue", fromGive.bannerColor());
        assertEquals(3, fromGive.layers().size());
        assertEquals("minecraft:cross", fromGive.layers().get(0).pattern().toString());
        assertEquals(DyeColor.WHITE, fromGive.layers().get(0).color());
        assertEquals(
                "minecraft:curly_border", fromGive.layers().get(1).pattern().toString());
        assertEquals(DyeColor.BLACK, fromGive.layers().get(1).color());
        assertEquals(
                "minecraft:triangle_top", fromGive.layers().get(2).pattern().toString());
        assertEquals(DyeColor.YELLOW, fromGive.layers().get(2).color());

        BannerDesign fromBareItem = BannerDesign.fromCommand("light_blue_banner");
        assertNotNull(fromBareItem);
        assertEquals("light_blue", fromBareItem.bannerColor());

        BannerDesign fromNamespacedItem = BannerDesign.fromCommand(complexBanner);
        assertNotNull(fromNamespacedItem);
        assertEquals("light_blue", fromNamespacedItem.bannerColor());
        assertEquals(3, fromNamespacedItem.layers().size());

        BannerDesign fromInvalidItem = BannerDesign.fromCommand("minecraft:dirt");
        assertNull(fromInvalidItem);

        String give = fromGive.toCommand();
        assertTrue(give.startsWith("/give @p minecraft:light_blue_banner"));
        assertTrue(give.contains("\"pattern\":\"cross\""));
        assertTrue(give.contains("\"pattern\":\"curly_border\""));
        assertTrue(give.contains("\"pattern\":\"triangle_top\""));

        BannerDesign roundTripped = BannerDesign.fromCommand(give);
        assertNotNull(roundTripped);
        assertEquals(fromGive.bannerColor(), roundTripped.bannerColor());
        assertEquals(fromGive.layers(), roundTripped.layers());
    }

    @Test
    void canCreateDesignFromBannerItem() {
        if (!Items.BANNER.lightBlue().builtInRegistryHolder().areComponentsBound()) {
            Items.BANNER.lightBlue().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }

        BannerDesign design = BannerDesign.fromItem(new ItemStack(Items.BANNER.lightBlue()));

        assertNotNull(design);
        assertEquals(BannerDesign.DEFAULT_DESCRIPTION, design.description());
        assertEquals("light_blue", design.bannerColor());
        assertTrue(design.layers().isEmpty());
    }
}
