/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import se.icus.mag.loomassistant.types.BannerDesign;

/**
 * Represents a saved banner design with all its pattern layers.
 */
public class SavedBanner {
    private String id;
    private String name;
    private String baseColor;
    private List<BannerPatternLayer> layers;
    private long createdAt;

    public SavedBanner() {
        this.id = UUID.randomUUID().toString();
        this.layers = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public SavedBanner(String name, DyeColor baseColor, List<BannerPatternLayer> layers) {
        this();
        this.name = name;
        this.baseColor = baseColor.getName();
        this.layers = new ArrayList<>(layers);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseColor() {
        return baseColor;
    }

    public DyeColor getBaseColorEnum() {
        return DyeColor.byName(baseColor, DyeColor.WHITE);
    }

    public void setBaseColor(String baseColor) {
        this.baseColor = baseColor;
    }

    public List<BannerPatternLayer> getLayers() {
        return layers;
    }

    public void setLayers(List<BannerPatternLayer> layers) {
        this.layers = layers;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getDisplayName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return "Banner " + id.substring(0, 4);
    }

    public Item getBaseBannerItem() {
        DyeColor color = getBaseColorEnum();
        return switch (color) {
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

    public BannerDesign toType() {
        List<se.icus.mag.loomassistant.types.BannerDesignLayer> convertedLayers = layers == null
                ? new ArrayList<>()
                : layers.stream().map(BannerPatternLayer::toType).collect(Collectors.toCollection(ArrayList::new));
        return new BannerDesign(id, name, null, null, baseColor, convertedLayers);
    }

    public static SavedBanner fromType(BannerDesign design) {
        List<BannerPatternLayer> convertedLayers = design.layers() == null
                ? new ArrayList<>()
                : design.layers().stream()
                        .map(BannerPatternLayer::fromType)
                        .collect(Collectors.toCollection(ArrayList::new));

        SavedBanner banner = new SavedBanner(design.description(), design.getBannerColorEnum(), convertedLayers);
        banner.setId(design.id());
        return banner;
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
}
