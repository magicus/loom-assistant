/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.screens.packselection;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.bannerpack.BannerPack;
import se.icus.mag.loomassistant.bannerpack.storage.ActivePacksConfig;
import se.icus.mag.loomassistant.bannerpack.storage.BannerPackModelEntry;
import se.icus.mag.loomassistant.bannerpack.storage.BannerPackRepository;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;

@Environment(EnvType.CLIENT)
public class BannerPackSelectionScreen extends Screen {
    private static final Component AVAILABLE_TITLE = Component.translatable("pack.available.title");
    private static final Component ACTIVE_TITLE = Component.translatable("pack.selected.title");
    private static final Component OPEN_PACK_DIR_TITLE = Component.translatable("pack.openFolder");
    private static final Component SEARCH =
            Component.translatable("gui.packSelection.search").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final Component DRAG_AND_DROP =
            Component.translatable("pack.dropInfo").withStyle(ChatFormatting.GRAY);
    private static final Component PACK_FOLDER_TOOLTIP = Component.translatable("pack.folderInfo");
    private static final int LIST_WIDTH = 200;
    private static final int HEADER_ELEMENT_SPACING = 4;
    private static final int SEARCH_BOX_HEIGHT = 15;
    private static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");
    private static final Identifier SELECT_SPRITE = Identifier.withDefaultNamespace("transferable_list/select");
    private static final Identifier UNSELECT_SPRITE = Identifier.withDefaultNamespace("transferable_list/unselect");

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final BannerPackRepository repository;
    private final ActivePacksConfig activeConfig;
    private final Map<String, Identifier> packIcons = new HashMap<>();

    private BannerPackListWidget availablePackList;
    private BannerPackListWidget activePackList;
    private EditBox search;
    private Button doneButton;

    public BannerPackSelectionScreen(BannerPackRepository repository, ActivePacksConfig activeConfig) {
        super(Component.literal("Select Banner Packs"));
        this.repository = repository;
        this.activeConfig = activeConfig;
    }

    @Override
    public void onClose() {
        this.saveAndClose();
    }

    private void saveAndClose() {
        if (this.activePackList != null) {
            List<String> activePacks = new ArrayList<>();
            for (BannerPackListEntry entry : this.activePackList.children()) {
                if (entry instanceof PackEntry packEntry) {
                    activePacks.add(packEntry.getPackId());
                }
            }
            this.activeConfig.setActivePacks(activePacks);
            BannerStorage.getInstance().load();
        }
        this.minecraft.gui.setScreen(null);
    }

    @Override
    protected void init() {
        this.layout.setHeaderHeight(SEARCH_BOX_HEIGHT + 34);
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(HEADER_ELEMENT_SPACING));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.getTitle(), this.font));
        header.addChild(new StringWidget(DRAG_AND_DROP, this.font));
        this.search = header.addChild(new EditBox(this.font, 0, 0, LIST_WIDTH, SEARCH_BOX_HEIGHT, Component.empty()));
        this.search.setHint(SEARCH);
        this.search.setResponder(this::updateFilteredEntries);

        this.availablePackList = this.layout.addToContents(
                new BannerPackListWidget(this.minecraft, this, LIST_WIDTH, this.height - 66, AVAILABLE_TITLE, false));
        this.activePackList = this.layout.addToContents(
                new BannerPackListWidget(this.minecraft, this, LIST_WIDTH, this.height - 66, ACTIVE_TITLE, true));

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(
                        OPEN_PACK_DIR_TITLE, button -> Util.getPlatform().openPath(this.repository.getPacksRoot()))
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(PACK_FOLDER_TOOLTIP))
                .build());
        this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
        this.reload();
    }

    @Override
    protected void setInitialFocus() {
        if (this.search != null) {
            this.setInitialFocus(this.search);
        } else {
            super.setInitialFocus();
        }
    }

    private void updateFilteredEntries(String value) {
        if (this.availablePackList == null || this.activePackList == null) return;

        String lowerCaseValue = value.toLowerCase(Locale.ROOT);
        Stream<BannerPackModelEntry> available = this.repository.getPacks().values().stream()
                .filter(pack -> !this.isPackActive(pack.getMetadata().id()))
                .map(pack -> this.toEntry(pack, false))
                .filter(entry -> this.matchesFilter(entry, value, lowerCaseValue));
        Stream<BannerPackModelEntry> active = this.repository.getPacks().values().stream()
                .filter(pack -> this.isPackActive(pack.getMetadata().id()))
                .map(pack -> this.toEntry(pack, true))
                .filter(entry -> this.matchesFilter(entry, value, lowerCaseValue));

        this.availablePackList.updateList(available);
        this.activePackList.updateList(active);
    }

    private boolean matchesFilter(BannerPackModelEntry entry, String value, String lowerCaseValue) {
        return value.isBlank()
                || entry.packId().toLowerCase(Locale.ROOT).contains(lowerCaseValue)
                || entry.getTitle().getString().toLowerCase(Locale.ROOT).contains(lowerCaseValue)
                || entry.description().getString().toLowerCase(Locale.ROOT).contains(lowerCaseValue);
    }

    private BannerPackModelEntry toEntry(BannerPack pack, boolean active) {
        String packId = pack.getMetadata().id();
        return new BannerPackModelEntry(
                packId, displayName(pack), description(pack), secondLine(pack), getPackIcon(pack), active);
    }

    private static Component description(BannerPack pack) {
        String description = pack.getMetadata().description();
        return description == null || description.isBlank() ? Component.empty() : Component.literal(description);
    }

    private static Component secondLine(BannerPack pack) {
        String author = pack.getMetadata().author();
        if (author != null && !author.isBlank()) {
            return Component.literal(author);
        } else {
            return description(pack);
        }
    }

    private static String displayName(BannerPack pack) {
        Path path = pack.getPath();
        Path fileName = path.getFileName();
        if (fileName != null) {
            String name = fileName.toString();
            if (name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                name = name.substring(0, name.length() - 4);
            }
            return name;
        } else {
            return pack.getMetadata().id();
        }
    }

    private boolean isPackActive(String packId) {
        return this.activeConfig.getActivePacks().contains(packId);
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.availablePackList != null) {
            this.availablePackList.updateSizeAndPosition(
                    LIST_WIDTH,
                    this.layout.getContentHeight(),
                    this.width / 2 - 15 - LIST_WIDTH,
                    this.layout.getHeaderHeight());
        }
        if (this.activePackList != null) {
            this.activePackList.updateSizeAndPosition(
                    LIST_WIDTH, this.layout.getContentHeight(), this.width / 2 + 15, this.layout.getHeaderHeight());
        }
    }

    private void reload() {
        this.packIcons.clear();
        this.updateFilteredEntries(this.search != null ? this.search.getValue() : "");
        if (this.doneButton != null && this.activePackList != null) {
            this.doneButton.active = !this.activePackList.children().isEmpty();
        }
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        if (files.isEmpty()) return;

        for (Path file : files) {
            try {
                this.copyPackToRoot(file);
            } catch (IOException e) {
                LoomAssistantMod.LOGGER.error("Failed to add dropped banner pack {}", file, e);
            }
        }

        this.repository.load();
        this.reload();
    }

    public void movePackToActive(String packId) {
        if (!this.isPackActive(packId)) {
            List<String> active = new ArrayList<>(this.activeConfig.getActivePacks());
            active.add(packId);
            this.activeConfig.setActivePacks(active);
            this.reload();
        }
    }

    public void movePackToAvailable(String packId) {
        if (BannerPackRepository.LOCAL_PACK_ID.equals(packId)) return;

        List<String> active = new ArrayList<>(this.activeConfig.getActivePacks());
        active.remove(packId);
        this.activeConfig.setActivePacks(active);
        this.reload();
    }

    private Identifier getPackIcon(BannerPack pack) {
        String packId = pack.getMetadata().id();
        return this.packIcons.computeIfAbsent(packId, id -> this.loadPackIcon(pack));
    }

    private Identifier loadPackIcon(BannerPack pack) {
        Path path = pack.getPath();
        try {
            if (Files.isDirectory(path)) {
                Path iconPath = path.resolve("pack.png");
                return loadIconTexture(pack, iconPath);
            }

            try (FileSystem fs = FileSystems.newFileSystem(path, (ClassLoader) null)) {
                Path iconPath = fs.getPath("/pack.png");
                return loadIconTexture(pack, iconPath);
            }
        } catch (IOException e) {
            LoomAssistantMod.LOGGER.warn(
                    "Failed to load icon for banner pack {}", pack.getMetadata().id(), e);
            return DEFAULT_ICON;
        }
    }

    private void copyPackToRoot(Path source) throws IOException {
        Path targetRoot = this.repository.getPacksRoot();
        if (Files.isDirectory(source)) {
            Path targetDir = targetRoot.resolve(source.getFileName().toString());
            copyDirectory(source, targetDir);
        } else if (source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            Files.copy(
                    source,
                    targetRoot.resolve(source.getFileName().toString()),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            paths.forEach(path -> {
                try {
                    Path relative = source.relativize(path);
                    Path dest = target.resolve(relative.toString());
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.createDirectories(dest.getParent());
                        Files.copy(path, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private Identifier loadIconTexture(BannerPack pack, Path iconPath) throws IOException {
        if (!Files.exists(iconPath)) return DEFAULT_ICON;

        String id = pack.getMetadata().id();
        @SuppressWarnings("deprecation")
        Identifier location = Identifier.withDefaultNamespace("pack/"
                + Util.sanitizeName(id, Identifier::validPathChar)
                + "/"
                + Hashing.sha1().hashUnencodedChars(id)
                + "/icon");
        try (InputStream stream = Files.newInputStream(iconPath)) {
            NativeImage iconImage = NativeImage.read(stream);
            TextureManager textureManager = this.minecraft.getTextureManager();
            textureManager.register(location, new DynamicTexture(location::toString, iconImage));
            return location;
        }
    }
}
