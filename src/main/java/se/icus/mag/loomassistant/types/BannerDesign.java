/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public record BannerDesign(
        String id, String description, String author, String url, String bannerColor, List<BannerDesignLayer> layers) {
    public static final String DEFAULT_DESCRIPTION = "Unnamed banner";
    public static final Codec<BannerDesign> CODEC;
    private static final Gson GSON = new GsonBuilder().create();

    public BannerDesign {
        description = blankToNull(description);
        if (description == null) {
            throw new IllegalArgumentException("description is required");
        }
        author = blankToNull(author);
        url = blankToNull(url);
        bannerColor = (bannerColor == null || bannerColor.isBlank()) ? DyeColor.WHITE.getName() : bannerColor;
        if (DyeColor.byName(bannerColor, null) == null) {
            throw new IllegalArgumentException("Invalid banner color: " + bannerColor);
        }
        layers = List.copyOf(layers == null ? List.of() : layers);
    }

    public BannerDesign(String description, DyeColor bannerColor, List<BannerDesignLayer> layers) {
        this(null, description, null, null, bannerColor.getName(), layers);
    }

    public DyeColor getBannerColorEnum() {
        return DyeColor.byName(bannerColor, DyeColor.WHITE);
    }

    public BannerDesign withId(String newId) {
        return new BannerDesign(newId, description, author, url, bannerColor, layers);
    }

    public BannerDesign withDescription(String newDescription) {
        return new BannerDesign(id, newDescription, author, url, bannerColor, layers);
    }

    public BannerDesign withBannerColor(String newBannerColor) {
        return new BannerDesign(id, description, author, url, newBannerColor, layers);
    }

    static BannerDesign fromBannerPatterns(String description, DyeColor baseColor, BannerPatternLayers patterns) {
        List<BannerDesignLayer> parsedLayers = new ArrayList<>();
        if (patterns != null) {
            for (BannerPatternLayers.Layer layer : patterns.layers()) {
                parsedLayers.add(BannerDesignLayer.of(
                        layer.pattern().getRegisteredName(), layer.color().getName()));
            }
        }
        return new BannerDesign(null, description, null, null, baseColor.getName(), parsedLayers);
    }

    public static BannerDesign fromJson(String json) {
        JsonElement element = JsonParser.parseString(json);
        return CODEC.parse(JsonOps.INSTANCE, element)
                .getOrThrow(msg -> new IllegalStateException("Failed to parse BannerDesign: " + msg));
    }

    public String toJson() {
        JsonElement element = CODEC.encodeStart(JsonOps.INSTANCE, this)
                .getOrThrow(msg -> new IllegalStateException("Failed to encode BannerDesign: " + msg));
        return GSON.toJson(element);
    }

    public static BannerDesign fromItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BannerItem bannerItem)) {
            return null;
        }
        var customName = stack.getCustomName();
        String description = customName != null ? customName.getString() : DEFAULT_DESCRIPTION;
        return fromBannerPatterns(description, bannerItem.getColor(), stack.get(DataComponents.BANNER_PATTERNS));
    }

    public static BannerDesign fromCommand(String input) {
        return CommandFormat.parseCommand(input);
    }

    public String toCommand() {
        return CommandFormat.buildCommand(this);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    static {
        CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("description").forGetter(BannerDesign::description),
                        Codec.STRING.optionalFieldOf("author").forGetter(d -> Optional.ofNullable(d.author())),
                        Codec.STRING.optionalFieldOf("url").forGetter(d -> Optional.ofNullable(d.url())),
                        Codec.STRING.fieldOf("banner_color").forGetter(BannerDesign::bannerColor),
                        BannerDesignLayer.CODEC.listOf().fieldOf("layers").forGetter(BannerDesign::layers))
                .apply(
                        instance,
                        (description, author, url, bannerColor, layers) -> new BannerDesign(
                                null, description, author.orElse(null), url.orElse(null), bannerColor, layers)));
    }

    private static final class CommandFormat {
        private static final Pattern BANNER_ITEM_PATTERN =
                Pattern.compile("(?:^|\\s)(?:minecraft:)?([a-z_]+)_banner(?=\\b|\\[)");
        private static final Pattern BANNER_PATTERNS_PATTERN = Pattern.compile("banner_patterns=\\[(.*?)]");

        public static String buildCommand(BannerDesign design) {
            String itemName = "minecraft:" + design.getBannerColorEnum().getName() + "_banner";
            if (design.layers().isEmpty()) {
                return "/give @p " + itemName;
            }

            JsonArray patterns = new JsonArray();
            for (BannerDesignLayer layer : design.layers()) {
                JsonObject obj = new JsonObject();
                obj.addProperty(
                        "pattern", stripMinecraftNamespace(layer.pattern().toString()));
                obj.addProperty("color", layer.color().getName());
                patterns.add(obj);
            }
            return "/give @p " + itemName + "[banner_patterns=" + GSON.toJson(patterns) + "]";
        }

        public static BannerDesign parseCommand(String input) {
            if (input == null || input.isBlank()) {
                return null;
            }
            String trimmed = input.trim();
            String normalized = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;

            if (normalized.startsWith("give ")) {
                return parseInner(normalized.substring("give ".length()).trim());
            }
            if (BANNER_ITEM_PATTERN.matcher(normalized).find()) {
                return parseInner(normalized);
            }
            return null;
        }

        private static BannerDesign parseInner(String body) {
            Matcher itemMatcher = BANNER_ITEM_PATTERN.matcher(body);
            if (!itemMatcher.find()) {
                return null;
            }

            DyeColor baseColor = DyeColor.byName(itemMatcher.group(1), null);
            if (baseColor == null) {
                return null;
            }

            List<BannerDesignLayer> parsedLayers = new ArrayList<>();
            Matcher patternsMatcher = BANNER_PATTERNS_PATTERN.matcher(body);
            if (patternsMatcher.find()) {
                String patternsJson = "[" + patternsMatcher.group(1) + "]";
                try {
                    JsonArray patternsArray = GSON.fromJson(patternsJson, JsonArray.class);
                    for (JsonElement element : patternsArray) {
                        if (!element.isJsonObject()) {
                            continue;
                        }
                        JsonObject patternObj = element.getAsJsonObject();
                        if (!patternObj.has("pattern") || !patternObj.has("color")) {
                            continue;
                        }
                        String patternId = patternObj.get("pattern").getAsString();
                        String namespacedPattern = patternId.contains(":") ? patternId : "minecraft:" + patternId;
                        parsedLayers.add(BannerDesignLayer.of(
                                namespacedPattern, patternObj.get("color").getAsString()));
                    }
                } catch (IllegalArgumentException | JsonSyntaxException ignored) {
                    return null;
                }
            }

            return new BannerDesign(null, DEFAULT_DESCRIPTION, null, null, baseColor.getName(), parsedLayers);
        }

        private static String stripMinecraftNamespace(String value) {
            if (value == null) {
                return "";
            }
            return value.startsWith("minecraft:") ? value.substring("minecraft:".length()) : value;
        }
    }
}
