/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.DyeColor;
import se.icus.mag.loomassistant.LoomAssistantMod;

/**
 * Manages persistence of saved banner patterns to a JSON file.
 */
public class BannerStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static BannerStorage instance;

    private final Path configPath;
    private List<SavedBanner> banners = new ArrayList<>();

    public BannerStorage() {
        this.configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("loom-assistant")
                .resolve("banners");
    }

    public static BannerStorage getInstance() {
        if (instance == null) {
            instance = new BannerStorage();
        }
        return instance;
    }

    public void load() {
        if (!Files.exists(configPath)) {
            LoomAssistantMod.LOGGER.info("No saved banners directory found, starting fresh");
            return;
        }

        try {
            Files.list(configPath).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try (Reader reader = Files.newBufferedReader(p)) {
                    SavedBanner banner = GSON.fromJson(reader, SavedBanner.class);
                    if (banner != null) {
                        banners.add(banner);
                    }
                } catch (IOException e) {
                    LoomAssistantMod.LOGGER.error("Failed to load banner from {}", p, e);
                }
            });
            LoomAssistantMod.LOGGER.info("Loaded {} saved banners", banners.size());
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.error("Failed to load saved banners directory", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath);
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.error("Failed to create banners directory", e);
        }
    }

    public void addBanner(SavedBanner banner) {
        banners.add(banner);
        saveBannerToFile(banner);
    }

    public void removeBanner(String id) {
        banners.removeIf(b -> b.getId().equals(id));
        try {
            Path bannerFile = configPath.resolve(id + ".json");
            Files.deleteIfExists(bannerFile);
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.error("Failed to delete banner file for id: {}", id, e);
        }
    }

    private void saveBannerToFile(SavedBanner banner) {
        try {
            Files.createDirectories(configPath);
            Path bannerFile = configPath.resolve(banner.getId() + ".json");
            try (Writer writer = Files.newBufferedWriter(bannerFile)) {
                GSON.toJson(banner, writer);
            }
            LoomAssistantMod.LOGGER.debug("Saved banner {} to disk", banner.getId());
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.error("Failed to save banner to disk", e);
        }
    }

    public List<SavedBanner> getBanners() {
        return Collections.unmodifiableList(banners);
    }

    public SavedBanner getBannerById(String id) {
        return banners.stream().filter(b -> b.getId().equals(id)).findFirst().orElse(null);
    }

    public void renameBanner(String id, String newName) {
        SavedBanner banner = getBannerById(id);
        if (banner != null) {
            banner.setName(newName);
            saveBannerToFile(banner);
            LoomAssistantMod.LOGGER.info("Renamed banner {} to '{}'", id, newName);
        }
    }

    /**
     * Exports a banner to JSON string format for sharing/backup
     */
    public String exportBannerToJson(String bannerId) {
        SavedBanner banner = getBannerById(bannerId);
        if (banner == null) {
            return null;
        }
        return GSON.toJson(banner);
    }

    /**
     * Imports a banner from JSON string format or /give command format
     * Auto-detects the format and parses accordingly
     * Can handle single banner JSON or JSON array of banners
     */
    public SavedBanner importBannerFromJson(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        input = input.trim();

        // Try to detect if it's a /give command
        if (input.startsWith("/give")) {
            return importBannerFromGiveCommand(input);
        }

        // Try to parse as JSON array first
        if (input.startsWith("[")) {
            try {
                JsonArray jsonArray = GSON.fromJson(input, JsonArray.class);
                SavedBanner lastBanner = null;
                for (JsonElement element : jsonArray) {
                    if (element.isJsonObject()) {
                        try {
                            SavedBanner banner = GSON.fromJson(element, SavedBanner.class);
                            if (banner != null) {
                                // Check if banner with this ID already exists
                                if (getBannerById(banner.getId()) != null) {
                                    // Generate new ID to avoid conflicts
                                    banner.setId(java.util.UUID.randomUUID().toString());
                                }
                                addBanner(banner);
                                lastBanner = banner;
                            }
                        } catch (Exception e) {
                            LoomAssistantMod.LOGGER.warn("Failed to import individual banner from array", e);
                        }
                    }
                }
                if (lastBanner != null) {
                    LoomAssistantMod.LOGGER.info("Successfully imported {} banners from JSON array", jsonArray.size());
                    return lastBanner;
                }
            } catch (Exception e) {
                LoomAssistantMod.LOGGER.debug("Input is not a valid JSON array, trying single banner format", e);
            }
        }

        // Otherwise, try to parse as single banner JSON
        try {
            SavedBanner banner = GSON.fromJson(input, SavedBanner.class);
            if (banner != null) {
                // Check if banner with this ID already exists
                if (getBannerById(banner.getId()) != null) {
                    // Generate new ID to avoid conflicts
                    banner.setId(java.util.UUID.randomUUID().toString());
                }
                addBanner(banner);
                return banner;
            }
        } catch (Exception e) {
            LoomAssistantMod.LOGGER.error("Failed to import banner from JSON", e);
        }
        return null;
    }

    /**
     * Parses a /give command and extracts banner data
     * Example: /give @p minecraft:white_banner[banner_patterns=[{"pattern":"small_stripes","color":"lime"}]] 1
     */
    private SavedBanner importBannerFromGiveCommand(String command) {
        try {
            // Extract the banner item type
            Pattern itemPattern = Pattern.compile("minecraft:(\\w+)_banner");
            Matcher itemMatcher = itemPattern.matcher(command);
            if (!itemMatcher.find()) {
                LoomAssistantMod.LOGGER.error("Could not find banner item in /give command");
                return null;
            }

            String colorName = itemMatcher.group(1);
            DyeColor baseColor = parseDyeColor(colorName);
            if (baseColor == null) {
                LoomAssistantMod.LOGGER.error("Unknown banner color: {}", colorName);
                return null;
            }

            // Extract the banner_patterns component
            Pattern patternsPattern = Pattern.compile("banner_patterns=\\[(.*?)\\](?:\\]|,)");
            Matcher patternsMatcher = patternsPattern.matcher(command);

            List<BannerPatternLayer> layers = new ArrayList<>();

            if (patternsMatcher.find()) {
                String patternsJson = "[" + patternsMatcher.group(1) + "]";
                try {
                    JsonArray patternsArray = GSON.fromJson(patternsJson, JsonArray.class);
                    for (JsonElement element : patternsArray) {
                        if (element.isJsonObject()) {
                            JsonObject patternObj = element.getAsJsonObject();
                            String patternId = patternObj.get("pattern").getAsString();
                            String colorStr = patternObj.get("color").getAsString();

                            DyeColor dyeColor = parseDyeColor(colorStr);
                            if (dyeColor != null) {
                                // Convert pattern name to minecraft namespace
                                String fullPatternId = "minecraft:" + patternId;
                                layers.add(BannerPatternLayer.of(fullPatternId, dyeColor));
                            }
                        }
                    }
                } catch (Exception e) {
                    LoomAssistantMod.LOGGER.error("Failed to parse banner patterns from /give command", e);
                    return null;
                }
            }

            SavedBanner banner = new SavedBanner(null, baseColor, layers);
            addBanner(banner);
            return banner;

        } catch (Exception e) {
            LoomAssistantMod.LOGGER.error("Failed to import banner from /give command", e);
        }
        return null;
    }

    /**
     * Parses a dye color name to DyeColor enum
     */
    private DyeColor parseDyeColor(String colorName) {
        if (colorName == null) return null;

        return switch (colorName.toLowerCase()) {
            case "white" -> DyeColor.WHITE;
            case "orange" -> DyeColor.ORANGE;
            case "magenta" -> DyeColor.MAGENTA;
            case "light_blue" -> DyeColor.LIGHT_BLUE;
            case "yellow" -> DyeColor.YELLOW;
            case "lime" -> DyeColor.LIME;
            case "pink" -> DyeColor.PINK;
            case "gray" -> DyeColor.GRAY;
            case "light_gray" -> DyeColor.LIGHT_GRAY;
            case "cyan" -> DyeColor.CYAN;
            case "purple" -> DyeColor.PURPLE;
            case "blue" -> DyeColor.BLUE;
            case "brown" -> DyeColor.BROWN;
            case "green" -> DyeColor.GREEN;
            case "red" -> DyeColor.RED;
            case "black" -> DyeColor.BLACK;
            default -> null;
        };
    }
}
