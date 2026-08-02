/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public abstract class BannerPack {
    public static final String MCMETA_FILE = "bannerpack.mcmeta";
    public static final String BANNERS_DIR = "banners";
    private static final Gson GSON = new GsonBuilder().create();
    private static final Gson PRETTY_GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private final BannerPackMetadata metadata;
    private final Path path;
    private final Map<String, BannerRecipe> recipes = new LinkedHashMap<>();

    protected BannerPack(BannerPackMetadata metadata, Path path) {
        this.metadata = metadata;
        this.path = path;
    }

    public BannerPackMetadata getMetadata() {
        return metadata;
    }

    public Path getPath() {
        return path;
    }

    public BannerRecipe getDesign(String recipeId) {
        return recipes.get(recipeId);
    }

    public Collection<BannerRecipe> getDesigns() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    public BannerRecipe copyRecipeTo(BannerPack target, String recipeId) throws IOException {
        if (target.isReadOnly()) {
            throw new IllegalArgumentException("cannot copy to read-only pack");
        }
        BannerRecipe recipe = getDesign(recipeId);
        if (recipe == null) {
            throw new IllegalArgumentException("recipe not found: " + recipeId);
        }
        return target.addBannerRecipe(recipe.withId(null));
    }

    public abstract boolean isReadOnly();

    public abstract BannerRecipe addBannerRecipe(BannerRecipe recipe) throws IOException;

    public abstract BannerRecipe updateBannerRecipe(BannerRecipe recipe) throws IOException;

    public abstract void removeBannerRecipe(String recipeId) throws IOException;

    protected final void includeRecipe(BannerRecipe recipe) {
        recipes.put(recipe.id(), recipe);
    }

    protected final void excludeRecipe(String recipeId) {
        recipes.remove(recipeId);
    }

    public static DirectoryBannerPack createDirectoryPack(Path packDir, BannerPackMetadata metadata)
            throws IOException {
        Files.createDirectories(packDir.resolve(BANNERS_DIR));
        writeMcmeta(packDir, metadata);
        return new DirectoryBannerPack(metadata, packDir);
    }

    public static DirectoryBannerPack loadDirectoryPack(Path packDir, String packId) throws IOException {
        BannerPackMetadata metadata = readMcmeta(packDir.resolve(MCMETA_FILE), packId);
        if (metadata == null) {
            return null;
        }

        return new DirectoryBannerPack(metadata, packDir, packDir.resolve(BANNERS_DIR));
    }

    public static ZipBannerPack loadZipPack(Path zipPath, String packId) throws IOException {
        ZipBannerPack pack;
        try (FileSystem fs = FileSystems.newFileSystem(zipPath, (ClassLoader) null)) {
            Path root = fs.getPath("/");
            BannerPackMetadata metadata = readMcmeta(root.resolve(MCMETA_FILE), packId);
            if (metadata == null) {
                return null;
            }

            pack = new ZipBannerPack(metadata, zipPath, root.resolve(BANNERS_DIR));
        }
        return pack;
    }

    private static BannerPackMetadata readMcmeta(Path path, String packId) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("pack")) {
                return null;
            }

            JsonObject packObj = root.getAsJsonObject("pack");
            if (!packObj.has("pack_format")) {
                throw new IllegalStateException("Missing required pack.pack_format in " + path);
            }
            int packFormat = packObj.get("pack_format").getAsInt();
            if (packFormat != 1) {
                throw new IllegalStateException("Unsupported bannerpack format version " + packFormat + " in " + path);
            }
            if (!packObj.has("description")) {
                throw new IllegalStateException("Missing required pack.description in " + path);
            }
            String description = packObj.get("description").getAsString();

            String author = null;
            String url = null;
            if (root.has("bannerpack")) {
                JsonObject bannerpackObj = root.getAsJsonObject("bannerpack");
                if (bannerpackObj.has("author")) {
                    author = bannerpackObj.get("author").getAsString();
                }
                if (bannerpackObj.has("url")) {
                    url = bannerpackObj.get("url").getAsString();
                }
            }

            return new BannerPackMetadata(packId, description, author, url);
        }
    }

    public static void writeMcmeta(Path packDir, BannerPackMetadata metadata) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject packObj = new JsonObject();
        packObj.addProperty("pack_format", 1);
        packObj.addProperty("description", metadata.description() != null ? metadata.description() : "");
        root.add("pack", packObj);

        boolean hasAuthor = metadata.author() != null && !metadata.author().isBlank();
        boolean hasUrl = metadata.url() != null && !metadata.url().isBlank();
        if (hasAuthor || hasUrl) {
            JsonObject bannerpackObj = new JsonObject();
            if (hasAuthor) {
                bannerpackObj.addProperty("author", metadata.author());
            }
            if (hasUrl) {
                bannerpackObj.addProperty("url", metadata.url());
            }
            root.add("bannerpack", bannerpackObj);
        }

        try (Writer writer = Files.newBufferedWriter(packDir.resolve(MCMETA_FILE))) {
            PRETTY_GSON.toJson(root, writer);
        }
    }

    protected final void loadRecipesFromPath(Path bannersPath) throws IOException {
        if (!Files.exists(bannersPath) || !Files.isDirectory(bannersPath)) {
            return;
        }

        try (Stream<Path> namespaceDirs = Files.list(bannersPath)) {
            namespaceDirs.filter(Files::isDirectory).sorted().forEach(nsDir -> {
                String namespace = nsDir.getFileName().toString();
                try (Stream<Path> designFiles = Files.list(nsDir)) {
                    designFiles
                            .filter(p -> p.getFileName().toString().endsWith(".json"))
                            .sorted()
                            .forEach(path -> {
                                try (Reader reader = Files.newBufferedReader(path)) {
                                    BannerRecipe recipe = BannerRecipe.fromJson(readAll(reader));
                                    if (recipe != null) {
                                        String fileName = path.getFileName().toString();
                                        String baseName = fileName.substring(0, fileName.length() - ".json".length());
                                        includeRecipe(recipe.withId(namespace + ":" + baseName));
                                    }
                                } catch (IOException e) {
                                    throw new IllegalStateException("Failed to read recipe " + path, e);
                                }
                            });
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to list namespace dir " + nsDir, e);
                }
            });
        }
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, read);
        }
        return sb.toString();
    }
}
