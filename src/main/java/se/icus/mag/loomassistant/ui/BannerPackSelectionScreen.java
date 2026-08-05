/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;
import se.icus.mag.loomassistant.storage.ActivePacksConfig;
import se.icus.mag.loomassistant.storage.BannerPackRepository;
import se.icus.mag.loomassistant.types.bannerpack.BannerPack;

@Environment(EnvType.CLIENT)
public class BannerPackSelectionScreen extends Screen {
    private static final Component AVAILABLE_TITLE = Component.literal("Available Packs");
    private static final Component ACTIVE_TITLE = Component.literal("Active Packs");
    private static final int LIST_WIDTH = 200;
    private static final int HEADER_ELEMENT_SPACING = 4;
    private static final int SEARCH_BOX_HEIGHT = 15;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final BannerPackRepository repository;
    private final ActivePacksConfig activeConfig;

    private @Nullable BannerPackListWidget availablePackList;
    private @Nullable BannerPackListWidget activePackList;
    private @Nullable EditBox search;
    private @Nullable Button doneButton;

    public BannerPackSelectionScreen(BannerPackRepository repository, ActivePacksConfig activeConfig) {
        super(Component.literal("Manage Banner Packs"));
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
            this.activeConfig.setActivePacks(activePacks);
            this.activeConfig.save();
        }
        this.minecraft.gui.setScreen(null);
    }

    @Override
    protected void init() {
        this.layout.setHeaderHeight(4 + 9 + 4 + 9 + 4 + 15 + 4);
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.getTitle(), this.font));

        this.search = header.addChild(new EditBox(this.font, 0, 0, 200, 15, Component.empty()));
        this.search.setHint(Component.literal("Search..."));
        this.search.setResponder(this::updateFilteredEntries);

        this.availablePackList = this.layout.addToContents(
                new BannerPackListWidget(this.minecraft, this, 200, this.height - 66, AVAILABLE_TITLE));
        this.activePackList = this.layout.addToContents(
                new BannerPackListWidget(this.minecraft, this, 200, this.height - 66, ACTIVE_TITLE));

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .build());

        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
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
        if (this.availablePackList != null && this.activePackList != null) {
            String lowerCaseValue = value.toLowerCase();

            Stream<BannerPackModel.Entry> available = this.repository.getPacks().values().stream()
                    .filter(pack -> !this.isPackActive(pack.getMetadata().id()))
                    .map(pack -> new BannerPackModel.Entry(pack))
                    .filter(entry -> value.isBlank()
                            || entry.getId().toLowerCase().contains(lowerCaseValue)
                            || entry.getTitle().getString().toLowerCase().contains(lowerCaseValue));

            Stream<BannerPackModel.Entry> active = this.repository.getPacks().values().stream()
                    .filter(pack -> this.isPackActive(pack.getMetadata().id()))
                    .map(pack -> new BannerPackModel.Entry(pack))
                    .filter(entry -> value.isBlank()
                            || entry.getId().toLowerCase().contains(lowerCaseValue)
                            || entry.getTitle().getString().toLowerCase().contains(lowerCaseValue));

            this.availablePackList.updateList(available);
            this.activePackList.updateList(active);
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
                    200, this.layout.getContentHeight(), this.width / 2 - 15 - 200, this.layout.getHeaderHeight());
        }

        if (this.activePackList != null) {
            this.activePackList.updateSizeAndPosition(
                    200, this.layout.getContentHeight(), this.width / 2 + 15, this.layout.getHeaderHeight());
        }
    }

    private void reload() {
        this.updateFilteredEntries(this.search != null ? this.search.getValue() : "");
        if (this.doneButton != null && this.activePackList != null) {
            this.doneButton.active = !this.activePackList.children().isEmpty();
        }
    }

    void movePackToActive(String packId) {
        BannerPack pack = this.repository.getPack(packId);
        if (pack != null && !this.isPackActive(packId)) {
            List<String> active = new ArrayList<>(this.activeConfig.getActivePacks());
            active.add(packId);
            this.activeConfig.setActivePacks(active);
            this.reload();
        }
    }

    void movePackToAvailable(String packId) {
        List<String> active = new ArrayList<>(this.activeConfig.getActivePacks());
        active.remove(packId);
        this.activeConfig.setActivePacks(active);
        this.reload();
    }

    @Environment(EnvType.CLIENT)
    public static class BannerPackListWidget extends ObjectSelectionList<BannerPackListEntry> {
        private final Component title;
        private final BannerPackSelectionScreen screen;

        public BannerPackListWidget(
                Minecraft minecraft, BannerPackSelectionScreen screen, int width, int height, Component title) {
            super(minecraft, width, height, 33, 36);
            this.screen = screen;
            this.title = title;
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
        public boolean keyPressed(KeyEvent event) {
            BannerPackListEntry selected = this.getSelected();
            return selected != null ? selected.keyPressed(event) : super.keyPressed(event);
        }

        public void updateList(Stream<BannerPackModel.Entry> entries) {
            this.clearEntries();
            Component header =
                    Component.empty().append(this.title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
            this.addEntry(new HeaderEntry(this.minecraft.font, header));
            this.setSelected(null);

            entries.forEach(entry -> {
                PackEntry packEntry = new PackEntry(this.minecraft, this, entry);
                this.addEntry(packEntry);
            });
            this.refreshScrollAmount();
        }
    }

    @Environment(EnvType.CLIENT)
    public abstract static class BannerPackListEntry extends ObjectSelectionList.Entry<BannerPackListEntry> {
        @Override
        public int getWidth() {
            return super.getWidth();
        }

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
        private static final int MAX_DESCRIPTION_WIDTH_PIXELS = 157;

        private final BannerPackSelectionScreen.BannerPackListWidget parent;
        private final Minecraft minecraft;
        private final BannerPackModel.Entry packEntry;
        private final StringWidget nameWidget;
        private final MultiLineTextWidget descriptionWidget;

        public PackEntry(
                Minecraft minecraft,
                BannerPackSelectionScreen.BannerPackListWidget parent,
                BannerPackModel.Entry packEntry) {
            this.minecraft = minecraft;
            this.packEntry = packEntry;
            this.parent = parent;
            this.nameWidget = new StringWidget(packEntry.getTitle(), minecraft.font);
            this.descriptionWidget = new MultiLineTextWidget(
                    ComponentUtils.mergeStyles(packEntry.getDescription(), Style.EMPTY.withColor(-8355712)),
                    minecraft.font);
            this.descriptionWidget.setMaxRows(2);
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.packEntry.getTitle());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            if (!this.nameWidget.getMessage().equals(this.packEntry.getTitle())) {
                this.nameWidget.setMessage(this.packEntry.getTitle());
            }

            if (!this.descriptionWidget
                    .getMessage()
                    .getContents()
                    .equals(this.packEntry.getDescription().getContents())) {
                this.descriptionWidget.setMessage(
                        ComponentUtils.mergeStyles(this.packEntry.getDescription(), Style.EMPTY.withColor(-8355712)));
            }

            if (hovered || this.parent.getSelected() == this && this.parent.isFocused()) {
                graphics.fill(
                        this.getContentX(),
                        this.getContentY(),
                        this.getContentX() + 32,
                        this.getContentY() + 32,
                        -1601138544);
            }

            this.nameWidget.setMaxWidth(157);
            this.nameWidget.setPosition(this.getContentX() + 2, this.getContentY() + 1);
            this.nameWidget.extractRenderState(graphics, mouseX, mouseY, a);

            this.descriptionWidget.setMaxWidth(157);
            this.descriptionWidget.setPosition(this.getContentX() + 2, this.getContentY() + 12);
            this.descriptionWidget.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() == 0) {
                this.handlePackTransfer();
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isConfirmation()) {
                this.handlePackTransfer();
                return true;
            }
            return super.keyPressed(event);
        }

        private void handlePackTransfer() {
            BannerPackSelectionScreen screen = this.parent.screen;
            String packId = this.packEntry.getId();
            boolean isActive = screen.isPackActive(packId);

            if (isActive) {
                screen.movePackToAvailable(packId);
            } else {
                screen.movePackToActive(packId);
            }
        }

        @Override
        public String getPackId() {
            return this.packEntry.getId();
        }

        @Override
        public boolean shouldTakeFocusAfterInteraction() {
            return this.parent.children().stream()
                    .anyMatch(entry -> entry.getPackId().equals(this.getPackId()));
        }
    }

    @Environment(EnvType.CLIENT)
    private static class BannerPackModel {
        public static class Entry {
            private final BannerPack pack;

            public Entry(BannerPack pack) {
                this.pack = pack;
            }

            public String getId() {
                return this.pack.getMetadata().id();
            }

            public Component getTitle() {
                return Component.literal(this.pack.getMetadata().description());
            }

            public Component getDescription() {
                String author = this.pack.getMetadata().author();
                if (author != null && !author.isBlank()) {
                    return Component.literal("by " + author);
                }
                return Component.literal("");
            }

            public BannerPack getPack() {
                return this.pack;
            }
        }
    }
}
