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
    private static final Gson GSON = new GsonBuilder().create();

    public BannerRecipe {
        description = blankToNull(description);
        if (description == null) {
            throw new IllegalArgumentException("description is required");
        }
        author = blankToNull(author);
        url = blankToNull(url);
        category = (category == null || category.isBlank()) ? DEFAULT_CATEGORY : category;
        bannerColor = (bannerColor == null || bannerColor.isBlank()) ? DyeColor.WHITE.getName() : bannerColor;
        if (DyeColor.byName(bannerColor, null) == null) {
            throw new IllegalArgumentException("Invalid banner color: " + bannerColor);
        }
        layers = List.copyOf(layers == null ? List.of() : layers);
    }

    public BannerRecipe(String id, String description, String author, String url, String bannerColor, List<BannerRecipeLayer> layers) {
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

    static BannerRecipe fromBannerPatterns(String description, DyeColor baseColor, BannerPatternLayers patterns) {
        List<BannerRecipeLayer> parsedLayers = new ArrayList<>();
        if (patterns != null) {
            for (BannerPatternLayers.Layer layer : patterns.layers()) {
                parsedLayers.add(BannerRecipeLayer.of(
                        layer.pattern().getRegisteredName(), layer.color().getName()));
            }
        }
        return new BannerRecipe(null, description, null, null, DEFAULT_CATEGORY, baseColor.getName(), parsedLayers);
    }

    public static BannerRecipe fromJson(String json) {
        JsonElement element = JsonParser.parseString(json);
        return CODEC.parse(JsonOps.INSTANCE, element)
                .getOrThrow(msg -> new IllegalStateException("Failed to parse BannerRecipe: " + msg));
    }

    public String toJson() {
        JsonElement element = CODEC.encodeStart(JsonOps.INSTANCE, this)
                .getOrThrow(msg -> new IllegalStateException("Failed to encode BannerRecipe: " + msg));
        return GSON.toJson(element);
    }

    public static BannerRecipe fromItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BannerItem bannerItem)) {
            return null;
        }
        var customName = stack.getCustomName();
        String description = customName != null ? customName.getString() : DEFAULT_DESCRIPTION;
        return fromBannerPatterns(description, bannerItem.getColor(), stack.get(DataComponents.BANNER_PATTERNS));
    }

    public static BannerRecipe fromCommand(String input) {
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
                        Codec.STRING.fieldOf("description").forGetter(BannerRecipe::description),
                        Codec.STRING.optionalFieldOf("author").forGetter(d -> Optional.ofNullable(d.author())),
                        Codec.STRING.optionalFieldOf("url").forGetter(d -> Optional.ofNullable(d.url())),
                        Codec.STRING.optionalFieldOf("category", DEFAULT_CATEGORY).forGetter(BannerRecipe::category),
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

    private static final class CommandFormat {
        private static final Pattern BANNER_ITEM_PATTERN =
                Pattern.compile("(?:^|\\s)(?:minecraft:)?([a-z_]+)_banner(?=\\b|\\[)");
        private static final Pattern BANNER_PATTERNS_PATTERN = Pattern.compile("banner_patterns=\\[(.*?)]");

        public static String buildCommand(BannerRecipe recipe) {
            String itemName = "minecraft:" + recipe.getBannerColorEnum().getName() + "_banner";
            if (recipe.layers().isEmpty()) {
                return "/give @p " + itemName;
            }

            JsonArray patterns = new JsonArray();
            for (BannerRecipeLayer layer : recipe.layers()) {
                JsonObject obj = new JsonObject();
                obj.addProperty(
                        "pattern", stripMinecraftNamespace(layer.pattern().toString()));
                obj.addProperty("color", layer.color().getName());
                patterns.add(obj);
            }
            return "/give @p " + itemName + "[banner_patterns=" + GSON.toJson(patterns) + "]";
        }

        public static BannerRecipe parseCommand(String input) {
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

        private static BannerRecipe parseInner(String body) {
            Matcher itemMatcher = BANNER_ITEM_PATTERN.matcher(body);
            if (!itemMatcher.find()) {
                return null;
            }

            DyeColor baseColor = DyeColor.byName(itemMatcher.group(1), null);
            if (baseColor == null) {
                return null;
            }

            List<BannerRecipeLayer> parsedLayers = new ArrayList<>();
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
                        parsedLayers.add(BannerRecipeLayer.of(
                                namespacedPattern, patternObj.get("color").getAsString()));
                    }
                } catch (IllegalArgumentException | JsonSyntaxException ignored) {
                    return null;
                }
            }

                return new BannerRecipe(
                    null, DEFAULT_DESCRIPTION, null, null, DEFAULT_CATEGORY, baseColor.getName(), parsedLayers);
        }

        private static String stripMinecraftNamespace(String value) {
            if (value == null) {
                return "";
            }
            return value.startsWith("minecraft:") ? value.substring("minecraft:".length()) : value;
        }
    }
}
