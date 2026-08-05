/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.storage.ActivePacksConfig;
import se.icus.mag.loomassistant.storage.BannerPackRepository;
import se.icus.mag.loomassistant.types.bannerpack.BannerPack;

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
    private static final int ICON_SIZE = 32;
    private static final int ROW_HEIGHT = 36;
    private static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");
    private static final Identifier SELECT_SPRITE = Identifier.withDefaultNamespace("transferable_list/select");
    private static final Identifier SELECT_HIGHLIGHTED_SPRITE =
            Identifier.withDefaultNamespace("transferable_list/select_highlighted");
    private static final Identifier UNSELECT_SPRITE = Identifier.withDefaultNamespace("transferable_list/unselect");
    private static final Identifier UNSELECT_HIGHLIGHTED_SPRITE =
            Identifier.withDefaultNamespace("transferable_list/unselect_highlighted");

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final BannerPackRepository repository;
    private final ActivePacksConfig activeConfig;
    private final Map<String, Identifier> packIcons = new HashMap<>();

    private @Nullable BannerPackListWidget availablePackList;
    private @Nullable BannerPackListWidget activePackList;
    private @Nullable EditBox search;
    private @Nullable Button doneButton;

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
        }
        this.minecraft.gui.setScreen(null);
    }

    @Override
    protected void init() {
        this.layout.setHeaderHeight(4 + 9 + 4 + 9 + 4 + SEARCH_BOX_HEIGHT + 4);
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

        this.layout.visitWidgets(widget -> this.addRenderableWidget(widget));
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
        if (this.availablePackList == null || this.activePackList == null) {
            return;
        }

        String lowerCaseValue = value.toLowerCase(Locale.ROOT);
        Stream<BannerPackModel.Entry> available = this.repository.getPacks().values().stream()
                .filter(pack -> !this.isPackActive(pack.getMetadata().id()))
                .map(pack -> this.toEntry(pack, false))
                .filter(entry -> this.matchesFilter(entry, value, lowerCaseValue));
        Stream<BannerPackModel.Entry> active = this.repository.getPacks().values().stream()
                .filter(pack -> this.isPackActive(pack.getMetadata().id()))
                .map(pack -> this.toEntry(pack, true))
                .filter(entry -> this.matchesFilter(entry, value, lowerCaseValue));

        this.availablePackList.updateList(available);
        this.activePackList.updateList(active);
    }

    private boolean matchesFilter(BannerPackModel.Entry entry, String value, String lowerCaseValue) {
        return value.isBlank()
                || entry.getPackId().toLowerCase(Locale.ROOT).contains(lowerCaseValue)
                || entry.getTitle().getString().toLowerCase(Locale.ROOT).contains(lowerCaseValue)
                || entry.getDescription().getString().toLowerCase(Locale.ROOT).contains(lowerCaseValue);
    }

    private BannerPackModel.Entry toEntry(BannerPack pack, boolean active) {
        String packId = pack.getMetadata().id();
        return new BannerPackModel.Entry(packId, displayName(pack), description(pack), getPackIcon(pack), active);
    }

    private static Component description(BannerPack pack) {
        String description = pack.getMetadata().description();
        return description == null || description.isBlank() ? Component.empty() : Component.literal(description);
    }

    private static String displayName(BannerPack pack) {
        Path path = pack.getPath();
        Path fileName = path.getFileName();
        if (fileName == null) {
            return pack.getMetadata().id();
        }

        String name = fileName.toString();
        if (name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
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
        if (files.isEmpty()) {
            return;
        }

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

    void movePackToActive(String packId) {
        if (!this.isPackActive(packId)) {
            List<String> active = new ArrayList<>(this.activeConfig.getActivePacks());
            active.add(packId);
            this.activeConfig.setActivePacks(active);
            this.reload();
        }
    }

    void movePackToAvailable(String packId) {
        if (BannerPackRepository.LOCAL_PACK_ID.equals(packId)) {
            return;
        }
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
        if (!Files.exists(iconPath)) {
            return DEFAULT_ICON;
        }

        String id = pack.getMetadata().id();
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

    @Environment(EnvType.CLIENT)
    public static class BannerPackListWidget extends ObjectSelectionList<BannerPackListEntry> {
        private final Component title;
        private final BannerPackSelectionScreen screen;
        private final boolean active;

        public BannerPackListWidget(
                Minecraft minecraft,
                BannerPackSelectionScreen screen,
                int width,
                int height,
                Component title,
                boolean active) {
            super(minecraft, width, height, 33, ROW_HEIGHT);
            this.screen = screen;
            this.title = title;
            this.active = active;
            this.centerListVertically = false;
        }

        @Override
        public int getRowWidth() {
            return this.width - 4;
        }

        @Override
        protected int scrollBarX() {
            return this.getRight() - this.scrollbarWidth();
        }

        @Override
        public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
            BannerPackListEntry selected = this.getSelected();
            return selected != null ? selected.keyPressed(event) : super.keyPressed(event);
        }

        public void updateList(Stream<BannerPackModel.Entry> entries) {
            this.clearEntries();
            Component header =
                    Component.empty().append(this.title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
            this.addEntry(new HeaderEntry(this.minecraft.font, header), (int) (9.0F * 1.5F));
            this.setSelected(null);
            entries.forEach(
                    entry -> this.addEntry(new PackEntry(this.minecraft, this.screen, this, entry, this.active)));
            this.refreshScrollAmount();
        }
    }

    @Environment(EnvType.CLIENT)
    public abstract static class BannerPackListEntry extends ObjectSelectionList.Entry<BannerPackListEntry> {
        public abstract String getPackId();
    }

    @Environment(EnvType.CLIENT)
    public static class HeaderEntry extends BannerPackListEntry {
        private final net.minecraft.client.gui.Font font;
        private final Component text;

        public HeaderEntry(net.minecraft.client.gui.Font font, Component text) {
            this.font = font;
            this.text = text;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            graphics.centeredText(
                    this.font,
                    this.text,
                    this.getContentX() + this.getWidth() / 2,
                    this.getContentYMiddle() - 9 / 2,
                    -1);
        }

        @Override
        public Component getNarration() {
            return this.text;
        }

        @Override
        public String getPackId() {
            return "";
        }
    }

    @Environment(EnvType.CLIENT)
    public static class PackEntry extends BannerPackListEntry implements SelectableEntry {
        private final BannerPackSelectionScreen screen;
        private final BannerPackListWidget parent;
        private final Minecraft minecraft;
        private final BannerPackModel.Entry pack;
        private final StringWidget nameWidget;
        private final MultiLineTextWidget descriptionWidget;

        public PackEntry(
                Minecraft minecraft,
                BannerPackSelectionScreen screen,
                BannerPackListWidget parent,
                BannerPackModel.Entry pack,
                boolean active) {
            this.minecraft = minecraft;
            this.screen = screen;
            this.parent = parent;
            this.pack = pack.withActive(active);
            this.nameWidget = new StringWidget(this.pack.getTitle(), minecraft.font);
            this.descriptionWidget = new MultiLineTextWidget(
                    ComponentUtils.mergeStyles(this.pack.getDescription(), Style.EMPTY.withColor(-8355712)),
                    minecraft.font);
            this.descriptionWidget.setMaxRows(2);
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.pack.getTitle());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            Identifier iconTexture = this.pack.getIconTexture();
            int iconY = this.getContentY();
            graphics.blit(RenderPipelines.GUI_TEXTURED, iconTexture, this.getContentX(), iconY, 0f, 0f, 32, 32, 32, 32);

            if (this.showHoverOverlay() && hovered) {
                Identifier actionSprite =
                        this.pack.isActive() ? UNSELECT_HIGHLIGHTED_SPRITE : SELECT_HIGHLIGHTED_SPRITE;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, actionSprite, this.getContentX(), iconY, 32, 32);
            }

            if (!this.nameWidget.getMessage().equals(this.pack.getTitle())) {
                this.nameWidget.setMessage(this.pack.getTitle());
            }
            if (!this.descriptionWidget
                    .getMessage()
                    .getContents()
                    .equals(this.pack.getDescription().getContents())) {
                this.descriptionWidget.setMessage(
                        ComponentUtils.mergeStyles(this.pack.getDescription(), Style.EMPTY.withColor(-8355712)));
            }

            int textX = this.getContentX() + ICON_SIZE + 2;
            this.nameWidget.setMaxWidth(157);
            this.nameWidget.setPosition(textX, this.getContentY() + 1);
            this.nameWidget.extractRenderState(graphics, mouseX, mouseY, a);

            this.descriptionWidget.setMaxWidth(157);
            this.descriptionWidget.setPosition(textX, this.getContentY() + 12);
            this.descriptionWidget.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            if (this.showHoverOverlay() && this.mouseOverIcon((int) event.x(), (int) event.y())) {
                this.togglePack();
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
            if (event.isConfirmation() && this.showHoverOverlay()) {
                this.togglePack();
                return true;
            }
            return super.keyPressed(event);
        }

        private boolean showHoverOverlay() {
            return !BannerPackRepository.LOCAL_PACK_ID.equals(this.pack.getPackId());
        }

        private boolean mouseOverIcon(int mouseX, int mouseY) {
            int relX = mouseX - this.getContentX();
            int relY = mouseY - this.getContentY();
            return relX >= 0 && relX < ICON_SIZE && relY >= 0 && relY < ICON_SIZE;
        }

        private void togglePack() {
            if (this.pack.isActive()) {
                this.screen.movePackToAvailable(this.pack.getPackId());
            } else {
                this.screen.movePackToActive(this.pack.getPackId());
            }
        }

        @Override
        public String getPackId() {
            return this.pack.getPackId();
        }

        @Override
        public boolean shouldTakeFocusAfterInteraction() {
            return this.parent.children().stream()
                    .anyMatch(entry -> entry.getPackId().equals(this.getPackId()));
        }
    }

    private static final class BannerPackModel {
        private static final class Entry {
            private final String packId;
            private final String title;
            private final Component description;
            private final Identifier iconTexture;
            private final boolean active;

            private Entry(String packId, String title, Component description, Identifier iconTexture, boolean active) {
                this.packId = packId;
                this.title = title;
                this.description = description;
                this.iconTexture = iconTexture;
                this.active = active;
            }

            private Entry withActive(boolean active) {
                return new Entry(this.packId, this.title, this.description, this.iconTexture, active);
            }

            private String getPackId() {
                return this.packId;
            }

            private Component getTitle() {
                return Component.literal(this.title);
            }

            private Component getDescription() {
                return this.description;
            }

            private Identifier getIconTexture() {
                return this.iconTexture;
            }

            private boolean isActive() {
                return this.active;
            }
        }
    }
}
