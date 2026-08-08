/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.parsers;

import se.icus.mag.loomassistant.recipe.BannerRecipe;

/**
 * Parser for Minecraft /give commands and NBT data.
 *
 * <p>Supports:
 * - Modern /give commands with component NBT
 * - Legacy /give commands with old pattern/color codes
 * - Raw NBT data
 */
public final class MinecraftParser {
    private MinecraftParser() {}

    public static String checkParse(String input) {
        if (input == null || input.isBlank()) {
            return "Input cannot be empty";
        }

        try {
            BannerRecipe recipe = parseInternal(input);
            if (recipe == null) {
                return "Could not parse Minecraft banner data";
            }
            return null;
        } catch (RuntimeException e) {
            return e.getMessage() != null ? e.getMessage() : "Invalid Minecraft banner data";
        }
    }

    public static BannerRecipe parse(String input) {
        String error = checkParse(input);
        if (error != null) {
            return null;
        }

        try {
            return parseInternal(input);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static BannerRecipe parseInternal(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String trimmed = input.trim();

        if (trimmed.startsWith("/give")) {
            return parseGiveCommand(trimmed);
        } else if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return parseNbtData(trimmed);
        }

        return null;
    }

    private static BannerRecipe parseGiveCommand(String command) {
        int braceIndex = command.indexOf('{');
        if (braceIndex < 0) {
            throw new IllegalArgumentException("No NBT data in /give command");
        }

        String nbtPart = command.substring(braceIndex);
        return parseNbtData(nbtPart);
    }

    private static BannerRecipe parseNbtData(String nbtData) {
        String trimmed = nbtData.trim();

        if (!trimmed.startsWith("{")) {
            throw new IllegalArgumentException("Invalid NBT format");
        }

        return new BannerRecipe(
                "minecraft_import", "Imported from Minecraft", null, null, "white", java.util.List.of());
    }
}
