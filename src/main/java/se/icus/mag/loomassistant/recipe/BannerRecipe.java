/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public record BannerRecipe(
        String id,
        String description,
        String author,
        String url,
        String category,
        String bannerColor,
        List<BannerRecipeLayer> layers) {
    public static final String DEFAULT_DESCRIPTION = "Unnamed banner";
    public static final String DEFAULT_CATEGORY = "misc";
    public static final Codec<BannerRecipe> CODEC;
    public static final Gson GSON = new GsonBuilder().create();

    public BannerRecipe {
        description = blankToNull(description);
        if (description == null) throw new IllegalArgumentException("description is required");

        author = blankToNull(author);
        url = blankToNull(url);
        category = (category == null || category.isBlank()) ? DEFAULT_CATEGORY : category;
        bannerColor = (bannerColor == null || bannerColor.isBlank()) ? DyeColor.WHITE.getName() : bannerColor;
        if (DyeColor.byName(bannerColor, null) == null)
            throw new IllegalArgumentException("Invalid banner color: " + bannerColor);

        layers = List.copyOf(layers == null ? List.of() : layers);
    }

    public BannerRecipe(
            String id,
            String description,
            String author,
            String url,
            String bannerColor,
            List<BannerRecipeLayer> layers) {
        this(id, description, author, url, DEFAULT_CATEGORY, bannerColor, layers);
    }

    public BannerRecipe(String description, DyeColor bannerColor, List<BannerRecipeLayer> layers) {
        this(null, description, null, null, DEFAULT_CATEGORY, bannerColor.getName(), layers);
    }

    public DyeColor getBannerColorEnum() {
        return DyeColor.byName(bannerColor, DyeColor.WHITE);
    }

    public BannerRecipe withId(String newId) {
        return new BannerRecipe(newId, description, author, url, category, bannerColor, layers);
    }

    public BannerRecipe withDescription(String newDescription) {
        return new BannerRecipe(id, newDescription, author, url, category, bannerColor, layers);
    }

    public BannerRecipe withBannerColor(String newBannerColor) {
        return new BannerRecipe(id, description, author, url, category, newBannerColor, layers);
    }

    public BannerRecipe withCategory(String newCategory) {
        return new BannerRecipe(id, description, author, url, newCategory, bannerColor, layers);
    }

    public BannerRecipe withLayers(List<BannerRecipeLayer> newLayers) {
        return new BannerRecipe(id, description, author, url, category, bannerColor, newLayers);
    }

    /**
     * Returns true if this recipe can be crafted in the loom (max 6 pattern layers).
     */
    public boolean isWeavable() {
        return layers.size() <= 6;
    }

    // Bridge methods matching the old BannerRecipe API
    public String getName() {
        return description;
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getBaseColor() {
        return bannerColor;
    }

    public DyeColor getBaseColorEnum() {
        return getBannerColorEnum();
    }

    public List<BannerRecipeLayer> getLayers() {
        return layers;
    }

    public String getDisplayName() {
        if (description == null || description.isEmpty() || description.equals(DEFAULT_DESCRIPTION))
            return getUnnamedBanner();

        return description;
    }

    public static String getUnnamedBanner() {
        return Component.translatable("loom-assistant.banner.unnamed").getString();
    }

    public Item getBaseBannerItem() {
        return switch (getBannerColorEnum()) {
            case WHITE -> Items.BANNER.white();
            case ORANGE -> Items.BANNER.orange();
            case MAGENTA -> Items.BANNER.magenta();
            case LIGHT_BLUE -> Items.BANNER.lightBlue();
            case YELLOW -> Items.BANNER.yellow();
            case LIME -> Items.BANNER.lime();
            case PINK -> Items.BANNER.pink();
            case GRAY -> Items.BANNER.gray();
            case LIGHT_GRAY -> Items.BANNER.lightGray();
            case CYAN -> Items.BANNER.cyan();
            case PURPLE -> Items.BANNER.purple();
            case BLUE -> Items.BANNER.blue();
            case BROWN -> Items.BANNER.brown();
            case GREEN -> Items.BANNER.green();
            case RED -> Items.BANNER.red();
            case BLACK -> Items.BANNER.black();
        };
    }

    public static Item getDyeItem(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.DYE.white();
            case ORANGE -> Items.DYE.orange();
            case MAGENTA -> Items.DYE.magenta();
            case LIGHT_BLUE -> Items.DYE.lightBlue();
            case YELLOW -> Items.DYE.yellow();
            case LIME -> Items.DYE.lime();
            case PINK -> Items.DYE.pink();
            case GRAY -> Items.DYE.gray();
            case LIGHT_GRAY -> Items.DYE.lightGray();
            case CYAN -> Items.DYE.cyan();
            case PURPLE -> Items.DYE.purple();
            case BLUE -> Items.DYE.blue();
            case BROWN -> Items.DYE.brown();
            case GREEN -> Items.DYE.green();
            case RED -> Items.DYE.red();
            case BLACK -> Items.DYE.black();
        };
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    static {
        CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("description").forGetter(BannerRecipe::description),
                        Codec.STRING.optionalFieldOf("author").forGetter(d -> Optional.ofNullable(d.author())),
                        Codec.STRING.optionalFieldOf("url").forGetter(d -> Optional.ofNullable(d.url())),
                        Codec.STRING
                                .optionalFieldOf("category", DEFAULT_CATEGORY)
                                .forGetter(BannerRecipe::category),
                        Codec.STRING.fieldOf("banner_color").forGetter(BannerRecipe::bannerColor),
                        BannerRecipeLayer.CODEC.listOf().fieldOf("layers").forGetter(BannerRecipe::layers))
                .apply(
                        instance,
                        (description, author, url, category, bannerColor, layers) -> new BannerRecipe(
                                null,
                                description,
                                author.orElse(null),
                                url.orElse(null),
                                category,
                                bannerColor,
                                layers)));
    }
}
