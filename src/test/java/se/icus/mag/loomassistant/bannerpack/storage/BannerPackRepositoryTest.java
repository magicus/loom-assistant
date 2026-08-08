/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeCommandConverter;
import se.icus.mag.loomassistant.recipe.BannerRecipeItemConverter;
import se.icus.mag.loomassistant.recipe.BannerRecipeJsonConverter;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;

class BannerPackRepositoryTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @TempDir
    private Path tempDir;

    @Test
    void createsLocalPackWhenMissing() {
        Path packsRoot = tempDir.resolve("bannerpacks");
        BannerPackRepository repository = new BannerPackRepository(packsRoot);

        repository.load();

        assertTrue(Files.exists(packsRoot.resolve("local")));
        assertTrue(Files.exists(packsRoot.resolve("local").resolve("bannerpack.mcmeta")));
        assertTrue(Files.exists(packsRoot.resolve("local").resolve("banners")));
        assertNotNull(repository.getPack("local"));
    }

    @Test
    void canCreateAndSerializeDesign() {
        BannerRecipe recipe = new BannerRecipe(
                "Letter A",
                DyeColor.LIGHT_BLUE,
                List.of(BannerRecipeLayer.of("minecraft:small_stripes", DyeColor.BLUE.getName())));

        BannerRecipeJsonConverter converter = new BannerRecipeJsonConverter();
        String json = converter.fromRecipe(recipe);
        BannerRecipe parsed = converter.toRecipe(json);

        assertTrue(json.contains("\"color\":\"blue\""));
        assertTrue(json.contains("\"banner_color\":\"light_blue\""));
        assertFalse(json.contains("\"author\""));
        assertFalse(json.contains("\"id\""));
        assertFalse(json.contains("\"dye\""));
        assertFalse(json.contains("\"baseColor\""));
        assertEquals("Letter A", parsed.description());
        assertEquals("light_blue", parsed.bannerColor());
        assertEquals(1, parsed.layers().size());
        assertEquals(
                "minecraft:small_stripes", parsed.layers().getFirst().pattern().toString());

        BannerRecipe whiteDesign = new BannerRecipe("Test", DyeColor.WHITE, List.of());
        String whiteJson = converter.fromRecipe(whiteDesign);
        assertTrue(whiteJson.contains("\"banner_color\":\"white\""));
        BannerRecipe whiteParsed = converter.toRecipe(whiteJson);
        assertEquals("white", whiteParsed.bannerColor());
    }

    @Test
    void missingRequiredFieldsFailParsing() {
        BannerRecipeJsonConverter converter = new BannerRecipeJsonConverter();
        assertThrows(IllegalStateException.class, () -> converter.toRecipe("{\"layers\":[]}"));
        assertThrows(IllegalStateException.class, () -> converter.toRecipe("{\"description\":\"x\",\"layers\":[]}"));
        assertThrows(
                IllegalStateException.class,
                () -> converter.toRecipe("{\"description\":\"x\",\"banner_color\":\"white\"}"));
    }

    @Test
    void canCreateMoveAndDeleteDesignsAndPacks() throws IOException {
        BannerPackRepository repository = new BannerPackRepository(tempDir.resolve("bannerpacks"));
        repository.load();

        repository.createPack("letters", "Letters");
        assertNotNull(repository.getPack("letters"));

        BannerRecipe recipe = new BannerRecipe(
                "Letter B",
                DyeColor.LIGHT_BLUE,
                List.of(BannerRecipeLayer.of("minecraft:cross", DyeColor.WHITE.getName())));
        BannerRecipe created =
                repository.getPack(BannerPackRepository.LOCAL_PACK_ID).addBannerRecipe(recipe);
        assertNotNull(created.id());
        assertNotNull(repository.getBannerRecipeById(created.id()));
        assertEquals(BannerPackRepository.LOCAL_PACK_ID, repository.getBannerRecipePackId(created.id()));

        BannerRecipe moved = repository.moveBannerRecipe(BannerPackRepository.LOCAL_PACK_ID, "letters", created.id());
        assertEquals("letters", repository.getBannerRecipePackId(moved.id()));

        repository.getPack("letters").removeBannerRecipe(moved.id());
        assertNull(repository.getBannerRecipeById(moved.id()));

        repository.deletePack("letters");
        assertNull(repository.getPack("letters"));
        assertFalse(Files.exists(tempDir.resolve("bannerpacks").resolve("letters")));
    }

    @Test
    void supportsGiveImportAndExportFormats() {
        String complexBanner = "{id:light_blue_banner,components:{banner_patterns:[{pattern:cross,color:white},"
                + "{pattern:minecraft:curly_border,color:black},"
                + "{pattern:triangle_top,color:yellow}],custom_name:\"Letter C\"},count:16}";
        BannerRecipeCommandConverter converter = new BannerRecipeCommandConverter();
        BannerRecipe fromGive = converter.toRecipe("/give @p " + complexBanner);
        ;
        assertNotNull(fromGive);
        assertEquals("Letter C", fromGive.description());
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

        BannerRecipe fromBareItem = converter.toRecipe("light_blue_banner");
        assertNotNull(fromBareItem);
        assertEquals("light_blue", fromBareItem.bannerColor());

        BannerRecipe fromNamespacedItem = converter.toRecipe(
                "/give @p {id:minecraft:light_blue_banner,components:{banner_patterns:[{pattern:cross,color:white},{pattern:minecraft:curly_border,color:black},{pattern:triangle_top,color:yellow}],custom_name:\"Letter C\"},count:16}");
        assertNotNull(fromNamespacedItem);
        assertEquals("Letter C", fromNamespacedItem.description());
        assertEquals("light_blue", fromNamespacedItem.bannerColor());
        assertEquals(3, fromNamespacedItem.layers().size());

        BannerRecipe fromInvalidItem = converter.toRecipe("minecraft:dirt");
        assertNull(fromInvalidItem);

        String give = converter.fromRecipe(fromGive);
        assertTrue(give.startsWith("/give @p {id:light_blue_banner"));
        assertTrue(give.contains("custom_name:\"Letter C\""));
        assertTrue(give.contains("banner_patterns:["));
        assertTrue(give.contains("pattern:cross"));
        assertTrue(give.contains("pattern:curly_border"));
        assertTrue(give.contains("pattern:triangle_top"));

        BannerRecipe roundTripped = converter.toRecipe(give);
        assertNotNull(roundTripped);
        assertEquals(fromGive.bannerColor(), roundTripped.bannerColor());
        assertEquals(fromGive.layers(), roundTripped.layers());
    }

    @Test
    void canCreateDesignFromBannerItem() {
        if (!Items.BANNER.lightBlue().builtInRegistryHolder().areComponentsBound()) {
            Items.BANNER.lightBlue().builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }

        BannerRecipe recipe = BannerRecipeItemConverter.fromItem(new ItemStack(Items.BANNER.lightBlue()));

        assertNotNull(recipe);
        assertEquals(BannerRecipe.DEFAULT_DESCRIPTION, recipe.description());
        assertEquals("light_blue", recipe.bannerColor());
        assertTrue(recipe.layers().isEmpty());
    }
}
