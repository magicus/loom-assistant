///usr/bin/env java --source 21 "$0" "$@"; exit $?
// Usage: run from the directory containing *.zip banner packs
//   ./UpdateBannerPackDatabase.java [--base-url <url>]
//
// Creates or updates bannerpack-index-v1.json.
// Icon lookup order: {packId}-icon.png alongside the zip, then pack.png inside the zip.
// Timestamps: publishedAt from zip mtime, generatedAt from today.

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

public class UpdateBannerPackDatabase {
    static final String INDEX_FILE = "bannerpack-index-v1.json";

    public static void main(String[] args) throws Exception {
        Path dir = Path.of(".");
        String baseUrl = null;
        for (int i = 0; i + 1 < args.length; i++) {
            if ("--base-url".equals(args[i])) baseUrl = args[i + 1];
        }

        Map<String, String> existing = loadExisting(dir.resolve(INDEX_FILE));
        if (baseUrl == null) {
            baseUrl = existing.getOrDefault("baseUrl", "https://example.com/bannerpacks/");
        }
        String repoVersion = existing.getOrDefault("repoVersion", "1");

        List<String> packEntries = new ArrayList<>();
        List<String> foundIds = new ArrayList<>();

        try (var stream = Files.list(dir)) {
            var zips = stream
                    .filter(p -> p.getFileName().toString().endsWith(".zip"))
                    .sorted()
                    .toList();
            for (Path zip : zips) {
                String name = zip.getFileName().toString();
                String packId = name.substring(0, name.length() - 4);
                System.out.println("Processing: " + packId);
                packEntries.add(buildPackEntry(zip, packId, baseUrl, dir));
                foundIds.add(packId);
            }
        }

        if (packEntries.isEmpty()) {
            System.err.println("No *.zip files found in " + dir.toAbsolutePath());
            System.exit(1);
        }

        String today = LocalDate.now().toString();
        String json = buildIndex(repoVersion, today, baseUrl, packEntries);

        Path indexPath = dir.resolve(INDEX_FILE);
        Files.writeString(indexPath, json, StandardCharsets.UTF_8);
        System.out.println("Written: " + indexPath.toAbsolutePath());
    }

    static String buildPackEntry(Path zipPath, String packId, String baseUrl, Path dir) throws Exception {
        String sha256 = computeSha256(zipPath);
        long size = Files.size(zipPath);
        String publishedAt = Files.getLastModifiedTime(zipPath)
                .toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .toString();

        // Read bannerpack.mcmeta from inside zip
        String title = packId, description = "", author = "";
        try (ZipFile zf = new ZipFile(zipPath.toFile())) {
            ZipEntry mcmeta = zf.getEntry("bannerpack.mcmeta");
            if (mcmeta != null) {
                String content = new String(zf.getInputStream(mcmeta).readAllBytes(), StandardCharsets.UTF_8);
                String desc = extractJsonString(content, "description");
                String auth = extractJsonString(content, "author");
                if (!desc.isEmpty()) {
                    description = desc;
                    title = desc;
                }
                if (!auth.isEmpty()) author = auth;
            }
        }

        // Icon: prefer {packId}-icon.png alongside zip, then pack.png inside zip
        String iconData = null;
        Path externalIcon = dir.resolve(packId + "-icon.png");
        if (Files.exists(externalIcon)) {
            byte[] bytes = Files.readAllBytes(externalIcon);
            iconData = Base64.getEncoder().encodeToString(bytes);
            System.out.println("  icon: " + externalIcon.getFileName() + " (" + bytes.length + " bytes, base64 " + iconData.length() + " chars)");
        } else {
            try (ZipFile zf = new ZipFile(zipPath.toFile())) {
                ZipEntry packPng = zf.getEntry("pack.png");
                if (packPng != null) {
                    byte[] bytes = zf.getInputStream(packPng).readAllBytes();
                    iconData = Base64.getEncoder().encodeToString(bytes);
                    System.out.println("  icon: pack.png from zip (" + bytes.length + " bytes, base64 " + iconData.length() + " chars)");
                } else {
                    System.out.println("  icon: none (no " + packId + "-icon.png or pack.png in zip)");
                }
            }
        }

        String downloadUrl = baseUrl.endsWith("/") ? baseUrl + packId + ".zip" : baseUrl + "/" + packId + ".zip";

        StringBuilder sb = new StringBuilder();
        sb.append("    {\n");
        sb.append("      \"id\": ").append(jsonStr(packId)).append(",\n");
        sb.append("      \"title\": ").append(jsonStr(title)).append(",\n");
        sb.append("      \"description\": ").append(jsonStr(description)).append(",\n");
        sb.append("      \"author\": ").append(jsonStr(author)).append(",\n");
        sb.append("      \"downloadUrl\": ").append(jsonStr(downloadUrl)).append(",\n");
        sb.append("      \"sha256\": ").append(jsonStr(sha256)).append(",\n");
        sb.append("      \"sizeBytes\": ").append(size).append(",\n");
        sb.append("      \"publishedAt\": ").append(jsonStr(publishedAt));
        if (iconData != null) {
            sb.append(",\n      \"iconData\": ").append(jsonStr(iconData));
        }
        sb.append("\n    }");
        return sb.toString();
    }

    static String buildIndex(
            String repoVersion, String generatedAt, String baseUrl, List<String> packs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"repoVersion\": ").append(repoVersion).append(",\n");
        sb.append("  \"generatedAt\": ").append(jsonStr(generatedAt)).append(",\n");
        sb.append("  \"baseUrl\": ").append(jsonStr(baseUrl)).append(",\n");
        sb.append("  \"packs\": [\n");
        for (int i = 0; i < packs.size(); i++) {
            sb.append(packs.get(i));
            if (i < packs.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    static Map<String, String> loadExisting(Path indexPath) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!Files.exists(indexPath)) return result;
        try {
            String content = Files.readString(indexPath, StandardCharsets.UTF_8);
            for (String key : List.of("baseUrl", "generatedAt")) {
                String val = extractJsonString(content, key);
                if (!val.isEmpty()) result.put(key, val);
            }
            // repoVersion is a number
            Matcher mv = Pattern.compile("\"repoVersion\"\\s*:\\s*(\\d+)").matcher(content);
            if (mv.find()) result.put("repoVersion", mv.group(1));
        } catch (IOException e) {
            System.err.println("Warning: could not read existing index: " + e.getMessage());
        }
        return result;
    }

    static String extractJsonString(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? m.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : "";
    }

    static String computeSha256(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
        }
        return HexFormat.of().formatHex(md.digest());
    }

    static String jsonStr(String s) {
        if (s == null) return "null";
        return "\""
                + s.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                + "\"";
    }
}
