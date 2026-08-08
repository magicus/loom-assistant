/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.DyeColor;

public class BannerRecipeCommandConverter extends BannerRecipeConverter<String> {
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
                || trimmed.charAt(trimmed.length() - 1) != '}') return null;

        String body = trimmed.substring(1, trimmed.length() - 1);
        String idValue = findTopLevelFieldValue(body, "id");
        if (idValue == null) return null;

        DyeColor baseColor = parseBannerColor(unquoteIfNeeded(idValue));
        if (baseColor == null) return null;

        String description = BannerRecipe.DEFAULT_DESCRIPTION;
        String componentsValue = findTopLevelFieldValue(body, "components");
        List<BannerRecipeLayer> parsedLayers = new ArrayList<>();
        if (componentsValue != null) {
            String componentsBody = stripOuterBraces(componentsValue);
            if (componentsBody == null) return null;

            String customName = findTopLevelFieldValue(componentsBody, "custom_name");
            if (customName != null && !customName.isBlank()) {
                description = unquoteIfNeeded(customName);
            }

            String patternsValue = findTopLevelFieldValue(componentsBody, "banner_patterns");
            if (patternsValue != null) {
                String listBody = stripOuterBrackets(patternsValue);
                if (listBody == null) return null;

                for (String entry : splitTopLevelEntries(listBody)) {
                    String patternBody = stripOuterBraces(entry);
                    if (patternBody == null) return null;

                    String patternValue = findTopLevelFieldValue(patternBody, "pattern");
                    String colorValue = findTopLevelFieldValue(patternBody, "color");
                    if (patternValue == null || colorValue == null) return null;

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

        return new BannerRecipe(
                null, description, null, null, BannerRecipe.DEFAULT_CATEGORY, baseColor.getName(), parsedLayers);
    }

    private static List<String> splitTopLevelEntries(String body) {
        List<String> entries = new ArrayList<>();
        if (body == null || body.isBlank()) return entries;

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
        if (value == null) return null;

        String trimmed = value.trim();
        return trimmed.length() >= 2 && trimmed.charAt(0) == '{' && trimmed.charAt(trimmed.length() - 1) == '}'
                ? trimmed.substring(1, trimmed.length() - 1)
                : null;
    }

    private static String stripOuterBrackets(String value) {
        if (value == null) return null;

        String trimmed = value.trim();
        return trimmed.length() >= 2 && trimmed.charAt(0) == '[' && trimmed.charAt(trimmed.length() - 1) == ']'
                ? trimmed.substring(1, trimmed.length() - 1)
                : null;
    }

    private static String unquoteIfNeeded(String value) {
        if (value == null) return null;

        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"') {
            return trimmed.substring(1, trimmed.length() - 1).replace("\\\"", "\"");
        } else {
            return trimmed;
        }
    }

    private static String findTopLevelFieldValue(String body, String key) {
        if (body == null || body.isBlank()) return null;

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

    private static BannerRecipe parseLegacyCommand(String working) {
        Matcher itemMatcher = Pattern.compile("(?:^|\\s)(?:minecraft:)?([a-z_]+)_banner(?=\\b|\\[)")
                .matcher(working);
        if (!itemMatcher.find()) return null;

        DyeColor baseColor = DyeColor.byName(itemMatcher.group(1), null);
        if (baseColor == null) return null;

        List<BannerRecipeLayer> parsedLayers = new ArrayList<>();
        Matcher patternsMatcher = Pattern.compile("banner_patterns=\\[(.*?)]").matcher(working);
        if (patternsMatcher.find()) {
            String patternsJson = "[" + patternsMatcher.group(1) + "]";
            try {
                JsonArray patternsArray = BannerRecipe.GSON.fromJson(patternsJson, JsonArray.class);
                for (JsonElement element : patternsArray) {
                    if (!element.isJsonObject()) continue;

                    JsonObject patternObj = element.getAsJsonObject();
                    if (!patternObj.has("pattern") || !patternObj.has("color")) continue;

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
                null,
                BannerRecipe.DEFAULT_DESCRIPTION,
                null,
                null,
                BannerRecipe.DEFAULT_CATEGORY,
                baseColor.getName(),
                parsedLayers);
    }

    static DyeColor parseBannerColor(String itemId) {
        if (itemId == null || itemId.isBlank()) return null;

        String stripped = stripMinecraftNamespace(itemId);
        if (!stripped.endsWith("_banner")) return null;

        return DyeColor.byName(stripped.substring(0, stripped.length() - "_banner".length()), null);
    }

    private static BannerRecipe parseItemTag(CompoundTag tag) {
        String id = tag.getString("id").orElse(null);
        DyeColor baseColor = parseBannerColor(id);
        if (baseColor == null) return null;

        String description = BannerRecipe.DEFAULT_DESCRIPTION;
        CompoundTag components = tag.getCompoundOrEmpty("components");
        String customName = components.getString("custom_name").orElse(null);
        if (customName != null && !customName.isBlank()) {
            description = customName;
        }

        List<BannerRecipeLayer> parsedLayers = new ArrayList<>();
        ListTag bannerPatterns = components.getListOrEmpty("banner_patterns");
        for (int i = 0; i < bannerPatterns.size(); i++) {
            CompoundTag layerTag = bannerPatterns.getCompound(i).orElse(null);
            if (layerTag == null) return null;

            String patternId = layerTag.getString("pattern").orElse(null);
            String colorName = layerTag.getString("color").orElse(null);
            if (patternId == null || colorName == null) return null;

            String namespacedPattern = patternId.contains(":") ? patternId : "minecraft:" + patternId;
            try {
                parsedLayers.add(BannerRecipeLayer.of(namespacedPattern, colorName));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        return new BannerRecipe(
                null, description, null, null, BannerRecipe.DEFAULT_CATEGORY, baseColor.getName(), parsedLayers);
    }

    @Override
    public String fromRecipe(BannerRecipe recipe) {
        return CommandFormat.buildCommand(recipe);
    }

    @Override
    public BannerRecipe toRecipe(String source) {
        return fromCommand(source);
    }

    public static final class CommandFormat {
        private static String buildCommand(BannerRecipe recipe) {
            String itemName = recipe.getBannerColorEnum().getName() + "_banner";
            StringBuilder builder = new StringBuilder("/give @p {id:").append(itemName);

            boolean hasLayers = !recipe.layers().isEmpty();
            boolean hasCustomName =
                    recipe.description() != null && !BannerRecipe.DEFAULT_DESCRIPTION.equals(recipe.description());
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
                    builder.append("custom_name:").append(BannerRecipe.GSON.toJson(recipe.description()));
                }

                builder.append('}');
            }

            builder.append('}');
            return builder.toString();
        }
    }

    private static String stripMinecraftNamespace(String value) {
        if (value == null) return "";

        return value.startsWith("minecraft:") ? value.substring("minecraft:".length()) : value;
    }

    public record CommandParseResult(BannerRecipe recipe, String errorMessage, Integer errorPosition) {}
}
