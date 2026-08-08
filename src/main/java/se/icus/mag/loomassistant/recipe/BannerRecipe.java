/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import se.icus.mag.loomassistant.LoomAssistantMod;

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
    private static final Codec<BannerRecipe> CODEC;
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

    /** Returns true if this recipe can be crafted in the loom (max 6 pattern layers). */
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
        if (description != null && !description.isEmpty() && !description.equals(DEFAULT_DESCRIPTION)) {
            return description;
        }
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

    private static BannerRecipe fromBannerPatterns(
            String description, DyeColor baseColor, BannerPatternLayers patterns) {
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

    public static ItemStack toItem(Minecraft client, BannerRecipe recipe) {
        return toItem(
                LoomAssistantMod.getBannerPatternRegistry(client), recipe.getBaseBannerItem(), recipe.getLayers());
    }

    public static ItemStack toItem(
            Registry<BannerPattern> registry, Item baseBannerItem, List<BannerRecipeLayer> layers) {
        ItemStack stack = new ItemStack(baseBannerItem);

        if (registry == null || layers.isEmpty()) {
            return stack;
        }

        try {
            BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
            for (BannerRecipeLayer layer : layers) {
                try {
                    Identifier patternId = Identifier.tryParse(layer.patternId());
                    if (patternId == null) {
                        continue;
                    }

                    Optional<Holder.Reference<BannerPattern>> entry = registry.get(patternId);
                    if (entry.isEmpty()) {
                        LoomAssistantMod.LOGGER.debug("Pattern not found in registry: {}", patternId);
                        continue;
                    }

                    builder.add(entry.get(), layer.getDyeColorEnum());
                } catch (RuntimeException e) {
                    LoomAssistantMod.LOGGER.debug("Error processing banner pattern: {}", layer.patternId(), e);
                }
            }
            stack.set(DataComponents.BANNER_PATTERNS, builder.build());
        } catch (RuntimeException e) {
            LoomAssistantMod.LOGGER.debug("Error creating banner patterns component", e);
        }

        return stack;
    }

    public record CommandParseResult(BannerRecipe recipe, String errorMessage, Integer errorPosition) {}

    public static CommandParseResult parseCommandDetailed(String input) {
        if (input == null || input.isBlank()) {
            return new CommandParseResult(null, "Invalid syntax", null);
        }

        int start = 0;
        int end = input.length();
        while (start < end && Character.isWhitespace(input.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(input.charAt(end - 1))) {
            end--;
        }

        String working = input.substring(start, end);
        int prefixOffset = start;

        if (working.startsWith("/")) {
            working = working.substring(1);
            prefixOffset++;
        }

        if (working.startsWith("give") && (working.length() == 4 || Character.isWhitespace(working.charAt(4)))) {
            int i = 4;
            while (i < working.length() && Character.isWhitespace(working.charAt(i))) {
                i++;
            }
            working = working.substring(i);
            prefixOffset += i;
        }

        if (working.isBlank()) {
            return new CommandParseResult(null, "Invalid syntax", null);
        }

        int braceIndex = working.indexOf('{');
        if (braceIndex >= 0) {
            return parseModernCommand(working.substring(braceIndex), prefixOffset + braceIndex);
        }

        BannerRecipe legacy = parseLegacyCommand(working);
        if (legacy != null) {
            return new CommandParseResult(legacy, null, null);
        }

        return new CommandParseResult(null, "Invalid syntax", null);
    }

    public static BannerRecipe fromCommand(String input) {
        return parseCommandDetailed(input).recipe();
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

    private static CommandParseResult parseModernCommand(String objectText, int offset) {
        CommandSyntaxException parseError = null;
        try {
            CompoundTag itemTag = TagParser.parseCompoundFully(objectText);
            BannerRecipe recipe = parseItemTag(itemTag);
            if (recipe != null) {
                return new CommandParseResult(recipe, null, null);
            }
        } catch (CommandSyntaxException e) {
            parseError = e;
        }

        BannerRecipe manual = parseModernCommandManually(objectText);
        if (manual != null) {
            return new CommandParseResult(manual, null, null);
        }

        Integer errorPosition =
                parseError != null && parseError.getCursor() >= 0 ? offset + parseError.getCursor() : null;
        return new CommandParseResult(null, "Invalid syntax", errorPosition);
    }

    private static BannerRecipe parseModernCommandManually(String objectText) {
        String trimmed = objectText == null ? null : objectText.trim();
        if (trimmed == null
                || trimmed.length() < 2
                || trimmed.charAt(0) != '{'
                || trimmed.charAt(trimmed.length() - 1) != '}') {
            return null;
        }

        String body = trimmed.substring(1, trimmed.length() - 1);
        String idValue = findTopLevelFieldValue(body, "id");
        if (idValue == null) {
            return null;
        }

        DyeColor baseColor = parseBannerColor(unquoteIfNeeded(idValue));
        if (baseColor == null) {
            return null;
        }

        String description = DEFAULT_DESCRIPTION;
        String componentsValue = findTopLevelFieldValue(body, "components");
        List<BannerRecipeLayer> parsedLayers = new ArrayList<>();
        if (componentsValue != null) {
            String componentsBody = stripOuterBraces(componentsValue);
            if (componentsBody == null) {
                return null;
            }

            String customName = findTopLevelFieldValue(componentsBody, "custom_name");
            if (customName != null && !customName.isBlank()) {
                description = unquoteIfNeeded(customName);
            }

            String patternsValue = findTopLevelFieldValue(componentsBody, "banner_patterns");
            if (patternsValue != null) {
                String listBody = stripOuterBrackets(patternsValue);
                if (listBody == null) {
                    return null;
                }
                for (String entry : splitTopLevelEntries(listBody)) {
                    String patternBody = stripOuterBraces(entry);
                    if (patternBody == null) {
                        return null;
                    }
                    String patternValue = findTopLevelFieldValue(patternBody, "pattern");
                    String colorValue = findTopLevelFieldValue(patternBody, "color");
                    if (patternValue == null || colorValue == null) {
                        return null;
                    }
                    String patternId = unquoteIfNeeded(patternValue);
                    String namespacedPattern = patternId.contains(":") ? patternId : "minecraft:" + patternId;
                    try {
                        parsedLayers.add(BannerRecipeLayer.of(namespacedPattern, unquoteIfNeeded(colorValue)));
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                }
            }
        }

        return new BannerRecipe(null, description, null, null, DEFAULT_CATEGORY, baseColor.getName(), parsedLayers);
    }

    private static String findTopLevelFieldValue(String body, String key) {
        if (body == null || body.isBlank()) {
            return null;
        }

        int braceDepth = 0;
        int bracketDepth = 0;
        boolean inQuotes = false;
        char quoteChar = 0;
        int segmentStart = 0;

        for (int i = 0; i <= body.length(); i++) {
            char c = i < body.length() ? body.charAt(i) : ',';
            if (inQuotes) {
                if (c == '\\') {
                    i++;
                    continue;
                }
                if (c == quoteChar) {
                    inQuotes = false;
                }
                continue;
            }

            if (i < body.length()) {
                if (c == '"' || c == '\'') {
                    inQuotes = true;
                    quoteChar = c;
                    continue;
                }
                if (c == '{') {
                    braceDepth++;
                    continue;
                }
                if (c == '}') {
                    braceDepth--;
                    continue;
                }
                if (c == '[') {
                    bracketDepth++;
                    continue;
                }
                if (c == ']') {
                    bracketDepth--;
                    continue;
                }
            }

            if ((c == ',' && braceDepth == 0 && bracketDepth == 0) || i == body.length()) {
                String segment = body.substring(segmentStart, i).trim();
                if (!segment.isEmpty()) {
                    int colon = segment.indexOf(':');
                    if (colon > 0) {
                        String segmentKey = segment.substring(0, colon).trim();
                        if (segmentKey.equals(key)) {
                            return segment.substring(colon + 1).trim();
                        }
                    }
                }
                segmentStart = i + 1;
            }
        }

        return null;
    }

    private static List<String> splitTopLevelEntries(String body) {
        List<String> entries = new ArrayList<>();
        if (body == null || body.isBlank()) {
            return entries;
        }

        int braceDepth = 0;
        int bracketDepth = 0;
        boolean inQuotes = false;
        char quoteChar = 0;
        int segmentStart = 0;

        for (int i = 0; i <= body.length(); i++) {
            char c = i < body.length() ? body.charAt(i) : ',';
            if (inQuotes) {
                if (c == '\\') {
                    i++;
                    continue;
                }
                if (c == quoteChar) {
                    inQuotes = false;
                }
                continue;
            }

            if (i < body.length()) {
                if (c == '"' || c == '\'') {
                    inQuotes = true;
                    quoteChar = c;
                    continue;
                }
                if (c == '{') {
                    braceDepth++;
                    continue;
                }
                if (c == '}') {
                    braceDepth--;
                    continue;
                }
                if (c == '[') {
                    bracketDepth++;
                    continue;
                }
                if (c == ']') {
                    bracketDepth--;
                    continue;
                }
            }

            if ((c == ',' && braceDepth == 0 && bracketDepth == 0) || i == body.length()) {
                String segment = body.substring(segmentStart, i).trim();
                if (!segment.isEmpty()) {
                    entries.add(segment);
                }
                segmentStart = i + 1;
            }
        }

        return entries;
    }

    private static String stripOuterBraces(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() >= 2 && trimmed.charAt(0) == '{' && trimmed.charAt(trimmed.length() - 1) == '}'
                ? trimmed.substring(1, trimmed.length() - 1)
                : null;
    }

    private static String stripOuterBrackets(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() >= 2 && trimmed.charAt(0) == '[' && trimmed.charAt(trimmed.length() - 1) == ']'
                ? trimmed.substring(1, trimmed.length() - 1)
                : null;
    }

    private static String unquoteIfNeeded(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"') {
            return trimmed.substring(1, trimmed.length() - 1).replace("\\\"", "\"");
        }
        return trimmed;
    }

    private static BannerRecipe parseItemTag(CompoundTag tag) {
        String id = tag.getString("id").orElse(null);
        DyeColor baseColor = parseBannerColor(id);
        if (baseColor == null) {
            return null;
        }

        String description = DEFAULT_DESCRIPTION;
        CompoundTag components = tag.getCompoundOrEmpty("components");
        String customName = components.getString("custom_name").orElse(null);
        if (customName != null && !customName.isBlank()) {
            description = customName;
        }

        List<BannerRecipeLayer> parsedLayers = new ArrayList<>();
        ListTag bannerPatterns = components.getListOrEmpty("banner_patterns");
        for (int i = 0; i < bannerPatterns.size(); i++) {
            CompoundTag layerTag = bannerPatterns.getCompound(i).orElse(null);
            if (layerTag == null) {
                return null;
            }
            String patternId = layerTag.getString("pattern").orElse(null);
            String colorName = layerTag.getString("color").orElse(null);
            if (patternId == null || colorName == null) {
                return null;
            }
            String namespacedPattern = patternId.contains(":") ? patternId : "minecraft:" + patternId;
            try {
                parsedLayers.add(BannerRecipeLayer.of(namespacedPattern, colorName));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        return new BannerRecipe(null, description, null, null, DEFAULT_CATEGORY, baseColor.getName(), parsedLayers);
    }

    private static BannerRecipe parseLegacyCommand(String working) {
        Matcher itemMatcher = Pattern.compile("(?:^|\\s)(?:minecraft:)?([a-z_]+)_banner(?=\\b|\\[)")
                .matcher(working);
        if (!itemMatcher.find()) {
            return null;
        }

        DyeColor baseColor = DyeColor.byName(itemMatcher.group(1), null);
        if (baseColor == null) {
            return null;
        }

        List<BannerRecipeLayer> parsedLayers = new ArrayList<>();
        Matcher patternsMatcher = Pattern.compile("banner_patterns=\\[(.*?)]").matcher(working);
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

    private static DyeColor parseBannerColor(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        String stripped = stripMinecraftNamespace(itemId);
        if (!stripped.endsWith("_banner")) {
            return null;
        }
        return DyeColor.byName(stripped.substring(0, stripped.length() - "_banner".length()), null);
    }

    private static String stripMinecraftNamespace(String value) {
        if (value == null) {
            return "";
        }
        return value.startsWith("minecraft:") ? value.substring("minecraft:".length()) : value;
    }

    private static final class CommandFormat {
        private static String buildCommand(BannerRecipe recipe) {
            String itemName = recipe.getBannerColorEnum().getName() + "_banner";
            StringBuilder builder = new StringBuilder("/give @p {id:").append(itemName);

            boolean hasLayers = !recipe.layers().isEmpty();
            boolean hasCustomName = recipe.description() != null && !DEFAULT_DESCRIPTION.equals(recipe.description());
            if (hasLayers || hasCustomName) {
                builder.append(",components:{");
                boolean needComma = false;

                if (hasLayers) {
                    builder.append("banner_patterns:[");
                    for (int i = 0; i < recipe.layers().size(); i++) {
                        BannerRecipeLayer layer = recipe.layers().get(i);
                        if (i > 0) {
                            builder.append(',');
                        }
                        builder.append("{pattern:")
                                .append(stripMinecraftNamespace(layer.pattern().toString()))
                                .append(",color:")
                                .append(layer.color().getName())
                                .append('}');
                    }
                    builder.append(']');
                    needComma = true;
                }

                if (hasCustomName) {
                    if (needComma) {
                        builder.append(',');
                    }
                    builder.append("custom_name:").append(GSON.toJson(recipe.description()));
                }

                builder.append('}');
            }

            builder.append('}');
            return builder.toString();
        }
    }
}
