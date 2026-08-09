/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.screens.packdownload;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.bannerpack.repo.BannerPackDownloadService;
import se.icus.mag.loomassistant.bannerpack.repo.BannerPackRepoClient;
import se.icus.mag.loomassistant.bannerpack.repo.InstallResult;
import se.icus.mag.loomassistant.bannerpack.repo.PackUpdateStatus;
import se.icus.mag.loomassistant.bannerpack.repo.RemoteRepoIndex;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;
import se.icus.mag.loomassistant.bannerpack.storage.InstalledPackRegistry;
import se.icus.mag.loomassistant.config.LoomAssistantConfig;

@Environment(EnvType.CLIENT)
public class BannerPackDownloadManagementScreen extends Screen {
    private static final int LIST_WIDTH = 420;
    private static final int ROW_HEIGHT = 40;
    private static final int FOOTER_H = 32;
    private static final int BTN_ROW_H = 28;
    private static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");

    private final Screen previousScreen;
    private final BannerPackDownloadService service;
    private final LoomAssistantConfig.BannerPackRepoSettings repoSettings;
    private final Map<String, Identifier> iconCache = new HashMap<>();

    private PackListWidget packList;
    private StringWidget titleWidget;
    private Button checkUpdatesButton;
    private Button downloadButton;
    private Button deleteButton;
    private Checkbox activateCheckbox;
    private Button doneButton;

    private RemoteRepoIndex cachedIndex;
    private List<PackUpdateStatus> packStatuses = List.of();
    private boolean fetching;
    private String fetchError;
    private boolean operationInProgress;
    private String operationError;
    private String selectedPackIdBeforeRefresh;

    private boolean activateAfterDownload;

    public BannerPackDownloadManagementScreen(Screen previousScreen) {
        super(Component.literal("Download Banner Packs"));
        this.previousScreen = previousScreen;
        this.repoSettings = LoomAssistantMod.getConfig().getBannerPackRepo();
        this.activateAfterDownload = repoSettings.activateAfterDownload;
        LoomAssistantMod.LOGGER.info("[PackDownload] screen created, repo URL: {}", repoSettings.repoIndexUrl);
        InstalledPackRegistry registry = new InstalledPackRegistry(
                BannerStorage.getInstance().getRepository().getPacksRoot());
        BannerPackRepoClient client = new BannerPackRepoClient(repoSettings.repoIndexUrl);
        this.service = new BannerPackDownloadService(
                client, registry, BannerStorage.getInstance().getRepository().getPacksRoot());
    }

    @Override
    protected void init() {
        // Explicit bounds: title at top, two button rows at bottom, list fills middle
        int listX = (this.width - LIST_WIDTH) / 2;
        int listY = FOOTER_H;
        int bottomBtnY = this.height - BTN_ROW_H + 4;
        int topBtnY = bottomBtnY - BTN_ROW_H;
        int listBottom = topBtnY - 4;
        int listH = listBottom - listY;
        int btnH = 20;

        this.titleWidget = addRenderableOnly(new StringWidget(this.getTitle(), this.font));
        this.titleWidget.setPosition((this.width - this.titleWidget.getWidth()) / 2, 8);

        // Top row: first two buttons
        this.activateCheckbox = Checkbox.builder(
                        Component.translatable("loom-assistant.screen.pack_download.activate_after_download"),
                        this.minecraft.font)
                .pos(0, topBtnY)
                .selected(activateAfterDownload)
                .maxWidth(280)
                .onValueChange((checkbox, value) -> {
                    activateAfterDownload = value;
                    repoSettings.activateAfterDownload = value;
                    me.shedaniel.autoconfig.AutoConfig.getConfigHolder(LoomAssistantConfig.class)
                            .save();
                })
                .build();
        int topRowTotalW = 150 + 8 + this.activateCheckbox.getWidth();
        int bx = (this.width - topRowTotalW) / 2;
        this.checkUpdatesButton = addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.pack_download.check_updates"),
                        b -> onCheckUpdates())
                .bounds(bx, topBtnY, 150, btnH)
                .build());
        bx += 150 + 8;
        this.activateCheckbox.setPosition(bx, topBtnY);
        addRenderableWidget(this.activateCheckbox);

        // Bottom row: remaining three buttons
        int bottomRowTotalW = 80 + 8 + 80 + 8 + 60;
        bx = (this.width - bottomRowTotalW) / 2;
        this.downloadButton = addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.pack_download.action.download"),
                        b -> onDownload())
                .bounds(bx, bottomBtnY, 80, btnH)
                .build());
        this.downloadButton.active = false;
        bx += 80 + 8;
        this.deleteButton = addRenderableWidget(Button.builder(
                        Component.translatable("loom-assistant.screen.pack_download.action.delete"), b -> onDelete())
                .bounds(bx, bottomBtnY, 80, btnH)
                .build());
        this.deleteButton.active = false;
        bx += 80 + 8;
        this.doneButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(bx, bottomBtnY, 60, btnH)
                .build());

        this.packList = new PackListWidget(this.minecraft, LIST_WIDTH, listH, listX, listY);
        addRenderableWidget(this.packList);

        // Restore cached data on resize; auto-fetch on first open
        if (packStatuses.isEmpty()) {
            onCheckUpdates();
        } else {
            reload();
        }
    }

    @Override
    protected void repositionElements() {
        // rebuildWidgets re-runs init(); keep the title centered if the screen is resized directly.
        if (this.titleWidget != null) {
            this.titleWidget.setPosition((this.width - this.titleWidget.getWidth()) / 2, 8);
        }
    }

    private void onCheckUpdates() {
        if (fetching || operationInProgress) return;
        selectedPackIdBeforeRefresh = getSelectedStatus() == null
                ? null
                : getSelectedStatus().remoteEntry().id();
        fetching = true;
        fetchError = null;
        operationError = null;
        checkUpdatesButton.active = false;
        checkUpdatesButton.setMessage(Component.translatable("loom-assistant.screen.pack_download.checking"));
        reload();
        LoomAssistantMod.LOGGER.info("[PackDownload] fetching index from {}", repoSettings.repoIndexUrl);

        service.fetchIndex()
                .whenCompleteAsync(
                        (index, error) -> {
                            fetching = false;
                            checkUpdatesButton.setMessage(
                                    Component.translatable("loom-assistant.screen.pack_download.check_updates"));
                            checkUpdatesButton.active = true;
                            if (error != null) {
                                LoomAssistantMod.LOGGER.error("[PackDownload] fetch failed", error);
                                fetchError = error.getMessage();
                            } else {
                                LoomAssistantMod.LOGGER.info(
                                        "[PackDownload] fetched {} pack(s): {}",
                                        index.packs().size(),
                                        index.packs().stream().map(p -> p.id()).toList());
                                cachedIndex = index;
                                packStatuses = service.getPackStatuses(index);
                                loadIconsFromData(packStatuses);
                            }
                            reload();
                        },
                        this.minecraft);
    }

    private void onDownload() {
        PackUpdateStatus selected = getSelectedStatus();
        if (selected == null || operationInProgress) return;

        PackUpdateStatus.InstallStatus status = selected.status();
        if (status == PackUpdateStatus.InstallStatus.UPDATE_AVAILABLE) {
            this.minecraft.gui.setScreen(new ConfirmScreen(
                    confirmed -> {
                        this.minecraft.gui.setScreen(this);
                        if (confirmed) startInstall(selected, true);
                    },
                    Component.translatable(
                            "loom-assistant.screen.pack_download.confirm.update",
                            selected.remoteEntry().displayTitle()),
                    Component.translatable("loom-assistant.screen.pack_download.confirm.update_body")));
        } else if (status == PackUpdateStatus.InstallStatus.NOT_INSTALLED) {
            startInstall(selected, false);
        }
    }

    private void startInstall(PackUpdateStatus target, boolean isUpdate) {
        operationInProgress = true;
        operationError = null;
        updateActionButtons(null);
        setButtonsEnabled(false);
        LoomAssistantMod.LOGGER.info(
                "[PackDownload] installing '{}' from {} (update={})",
                target.remoteEntry().id(),
                target.remoteEntry().downloadUrl(),
                isUpdate);

        service.install(target.remoteEntry(), activateAfterDownload, isUpdate)
                .whenCompleteAsync(
                        (result, error) -> {
                            operationInProgress = false;
                            setButtonsEnabled(true);
                            LoomAssistantMod.LOGGER.info(
                                    "[PackDownload] install '{}' result: {}",
                                    target.remoteEntry().id(),
                                    error != null ? error.getMessage() : result);
                            if (error != null
                                    || result == InstallResult.DOWNLOAD_FAILED
                                    || result == InstallResult.NETWORK_ERROR) {
                                operationError = Component.translatable(
                                                "loom-assistant.screen.pack_download.error.download_failed")
                                        .getString();
                            } else if (result == InstallResult.HASH_MISMATCH) {
                                operationError = Component.translatable(
                                                "loom-assistant.screen.pack_download.error.hash_mismatch")
                                        .getString();
                            } else {
                                BannerStorage.getInstance().load();
                                if (cachedIndex != null) {
                                    packStatuses = service.getPackStatuses(cachedIndex);
                                }
                            }
                            reload();
                        },
                        this.minecraft);
    }

    private void onDelete() {
        PackUpdateStatus selected = getSelectedStatus();
        if (selected == null || operationInProgress) return;
        this.minecraft.gui.setScreen(new ConfirmScreen(
                confirmed -> {
                    this.minecraft.gui.setScreen(this);
                    if (confirmed) {
                        service.delete(selected.remoteEntry().id());
                        BannerStorage.getInstance().load();
                        if (cachedIndex != null) {
                            packStatuses = service.getPackStatuses(cachedIndex);
                        }
                        reload();
                    }
                },
                Component.translatable(
                        "loom-assistant.screen.pack_download.confirm.delete",
                        selected.remoteEntry().displayTitle()),
                Component.translatable("loom-assistant.screen.pack_download.confirm.delete_body")));
    }

    private PackUpdateStatus getSelectedStatus() {
        if (packList == null) return null;
        PackListEntry entry = packList.getSelected();
        if (!(entry instanceof PackRowEntry row)) return null;
        return row.status;
    }

    private void reload() {
        if (packList == null) return;
        packList.refresh(packStatuses, fetchError, fetching, operationError);
        updateActionButtons(getSelectedStatus());
    }

    void onEntrySelected(PackUpdateStatus status) {
        updateActionButtons(status);
    }

    private void updateActionButtons(PackUpdateStatus status) {
        if (downloadButton == null || deleteButton == null) return;
        if (status == null || operationInProgress) {
            downloadButton.active = false;
            deleteButton.active = false;
            return;
        }
        PackUpdateStatus.InstallStatus s = status.status();
        downloadButton.active = s == PackUpdateStatus.InstallStatus.NOT_INSTALLED
                || s == PackUpdateStatus.InstallStatus.UPDATE_AVAILABLE;
        deleteButton.active = s == PackUpdateStatus.InstallStatus.INSTALLED_UP_TO_DATE
                || s == PackUpdateStatus.InstallStatus.UPDATE_AVAILABLE;

        if (s == PackUpdateStatus.InstallStatus.UPDATE_AVAILABLE) {
            downloadButton.setMessage(Component.translatable("loom-assistant.screen.pack_download.action.update"));
        } else {
            downloadButton.setMessage(Component.translatable("loom-assistant.screen.pack_download.action.download"));
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        if (checkUpdatesButton != null) checkUpdatesButton.active = enabled;
        if (downloadButton != null) downloadButton.active = enabled;
        if (deleteButton != null) deleteButton.active = enabled;
        if (doneButton != null) doneButton.active = enabled;
        if (activateCheckbox != null) activateCheckbox.active = enabled;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(previousScreen);
    }

    private void loadIconsFromData(List<PackUpdateStatus> statuses) {
        for (PackUpdateStatus s : statuses) {
            String packId = s.remoteEntry().id();
            if (iconCache.containsKey(packId)) continue;
            String iconData = s.remoteEntry().iconData();
            if (iconData == null || iconData.isBlank()) continue;
            try {
                byte[] bytes = Base64.getDecoder().decode(iconData);
                com.mojang.blaze3d.platform.NativeImage image =
                        com.mojang.blaze3d.platform.NativeImage.read(new ByteArrayInputStream(bytes));
                Identifier loc = Identifier.withDefaultNamespace(
                        "pack-download/icon/" + Util.sanitizeName(packId, Identifier::validPathChar));
                minecraft.getTextureManager().register(loc, new DynamicTexture(loc::toString, image));
                iconCache.put(packId, loc);
            } catch (Exception e) {
                LoomAssistantMod.LOGGER.debug("[PackDownload] could not load icon for pack {}", packId, e);
            }
        }
    }

    // --- Inner list widget ---

    @Environment(EnvType.CLIENT)
    class PackListWidget extends ObjectSelectionList<PackListEntry> {
        PackListWidget(Minecraft mc, int width, int height, int x, int y) {
            super(mc, width, height, y, ROW_HEIGHT);
            this.centerListVertically = false;
            this.setX(x);
        }

        @Override
        public int getRowWidth() {
            return this.width - 6;
        }

        @Override
        protected int scrollBarX() {
            return this.getRight() - this.scrollbarWidth();
        }

        void refresh(List<PackUpdateStatus> statuses, String fetchError, boolean fetching, String operationError) {
            LoomAssistantMod.LOGGER.info(
                    "[PackDownload] refresh: listX={} listY={} listW={} listH={} listEntries={}",
                    this.getX(),
                    this.getY(),
                    this.getWidth(),
                    this.getHeight(),
                    this.children().size());
            for (PackUpdateStatus s : statuses) {
                LoomAssistantMod.LOGGER.info(
                        "[PackDownload]   pack id='{}' status={}",
                        s.remoteEntry().id(),
                        s.status());
            }
            String prevSelectedId = BannerPackDownloadManagementScreen.this.selectedPackIdBeforeRefresh;
            if (prevSelectedId == null) {
                PackListEntry prevSelected = this.getSelected();
                if (prevSelected instanceof PackRowEntry r) {
                    prevSelectedId = r.status.remoteEntry().id();
                }
            }
            this.clearEntries();

            if (operationError != null) {
                this.addEntry(new StatusTextEntry(
                        this.minecraft.font, Component.literal(operationError).withStyle(ChatFormatting.RED)));
            }

            if (fetching) {
                this.addEntry(new StatusTextEntry(
                        this.minecraft.font,
                        Component.translatable("loom-assistant.screen.pack_download.checking")
                                .withStyle(ChatFormatting.GRAY)));
            } else if (fetchError != null) {
                this.addEntry(new StatusTextEntry(
                        this.minecraft.font, Component.literal(fetchError).withStyle(ChatFormatting.RED)));
            } else if (statuses.isEmpty()) {
                this.addEntry(new StatusTextEntry(
                        this.minecraft.font,
                        Component.translatable("loom-assistant.screen.pack_download.no_packs")
                                .withStyle(ChatFormatting.GRAY)));
            } else {
                PackRowEntry toReselect = null;
                for (PackUpdateStatus s : statuses) {
                    PackRowEntry row = new PackRowEntry(BannerPackDownloadManagementScreen.this, s);
                    this.addEntry(row);
                    if (s.remoteEntry().id().equals(prevSelectedId)) {
                        toReselect = row;
                    }
                }
                if (toReselect != null) {
                    this.setSelected(toReselect);
                }
            }
            if (!fetching) {
                BannerPackDownloadManagementScreen.this.selectedPackIdBeforeRefresh = null;
            }
        }
    }

    // --- Abstract base for list entries ---

    abstract static class PackListEntry extends ObjectSelectionList.Entry<PackListEntry> {}

    // --- Status text entry (loading / error / no packs) ---

    @Environment(EnvType.CLIENT)
    static class StatusTextEntry extends PackListEntry {
        private final Font font;
        private final Component text;

        StatusTextEntry(Font font, Component text) {
            this.font = font;
            this.text = text;
        }

        @Override
        public Component getNarration() {
            return text;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            graphics.centeredText(
                    font, text, this.getContentX() + this.getWidth() / 2, this.getContentYMiddle() - 4, 0xAAAAAA);
        }
    }

    // --- Pack row entry ---

    @Environment(EnvType.CLIENT)
    static class PackRowEntry extends PackListEntry {
        private static final int PADDING = 4;
        private final BannerPackDownloadManagementScreen screen;
        final PackUpdateStatus status;

        PackRowEntry(BannerPackDownloadManagementScreen screen, PackUpdateStatus status) {
            this.screen = screen;
            this.status = status;
        }

        @Override
        public Component getNarration() {
            return Component.literal(status.remoteEntry().displayTitle());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            boolean selected = screen.packList != null && screen.packList.getSelected() == this;
            if (hovered || selected) {
                graphics.fill(
                        this.getContentX(),
                        this.getContentY(),
                        this.getContentX() + this.getWidth(),
                        this.getContentY() + this.getContentHeight(),
                        selected ? 0x66FFFFFF : 0x33FFFFFF);
            }

            Identifier icon = screen.iconCache.getOrDefault(status.remoteEntry().id(), DEFAULT_ICON);
            // Renders the full texture scaled to 32x32 (textureWidth/Height=32 maps UV 0-32 to full image)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    icon,
                    this.getContentX() + 2,
                    this.getContentY() + 4,
                    0f,
                    0f,
                    32,
                    32,
                    32,
                    32);

            var font = screen.font;
            int textX = this.getContentX() + 38;
            String title = status.remoteEntry().displayTitle();
            graphics.text(font, Component.literal(title), textX, this.getContentY() + PADDING, 0xFFFFFFFF, false);

            String author = status.remoteEntry().author();
            Component statusLabel = getStatusLabel(status.status());
            String secondLine = (author != null && !author.isBlank() ? author + " — " : "") + statusLabel.getString();
            graphics.text(
                    font, Component.literal(secondLine), textX, this.getContentY() + PADDING + 14, 0xFFAAAAAA, false);
        }

        private Component getStatusLabel(PackUpdateStatus.InstallStatus s) {
            return switch (s) {
                case NOT_INSTALLED ->
                    Component.translatable("loom-assistant.screen.pack_download.status.not_installed")
                            .withStyle(ChatFormatting.GRAY);
                case INSTALLED_UP_TO_DATE ->
                    Component.translatable("loom-assistant.screen.pack_download.status.installed")
                            .withStyle(ChatFormatting.GREEN);
                case UPDATE_AVAILABLE ->
                    Component.translatable("loom-assistant.screen.pack_download.status.update_available")
                            .withStyle(ChatFormatting.YELLOW);
                case CONFLICT_UNMANAGED ->
                    Component.translatable("loom-assistant.screen.pack_download.status.conflict")
                            .withStyle(ChatFormatting.RED);
            };
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (screen.packList != null) {
                screen.packList.setSelected(this);
            }
            screen.onEntrySelected(status);
            return true;
        }
    }
}
