/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import se.icus.mag.loomassistant.autocraft.AutoCraftStateMachine;
import se.icus.mag.loomassistant.data.BannerPatternLayer;
import se.icus.mag.loomassistant.data.BannerStorage;
import se.icus.mag.loomassistant.data.SavedBanner;
import se.icus.mag.loomassistant.ui.tooltip.BannerRecipeTooltipComponent;
import org.lwjgl.glfw.GLFW;

public class LoomPanel {
    public static final int PANEL_WIDTH = 147;
    public static final int PANEL_HEIGHT = 166;
    private static final int GRID_COLUMNS = 5;
    private static final int GRID_ROWS = 4;
    private static final int GRID_CELL = 25;
    private static final int GRID_START_X = 11;
    private static final int GRID_START_Y = 31;
    private static final int TAB_X_OFFSET = 30;
    private static final int TAB_Y_START = 3;
    private static final int SEARCH_X = 25;
    private static final int SEARCH_Y = 13;
    private static final int SEARCH_W = 81;
    private static final int SEARCH_H = 14;
    private static final int FILTER_X = 110;
    private static final int FILTER_Y = 12;
    private static final int FILTER_W = 26;
    private static final int FILTER_H = 16;
    private static final int GUIDE_BUTTON_X = 8;
    private static final int GUIDE_BUTTON_Y = PANEL_HEIGHT - 24;
    private static final int GUIDE_BUTTON_W = PANEL_WIDTH - 16;
    private static final int GUIDE_BUTTON_H = 20;

    private static final Identifier RECIPE_BOOK_TEXTURE =
        Identifier.withDefaultNamespace("textures/gui/recipe_book.png");
    private static final Identifier FILTER_ENABLED =
        Identifier.withDefaultNamespace("recipe_book/filter_enabled");
    private static final Identifier FILTER_DISABLED =
        Identifier.withDefaultNamespace("recipe_book/filter_disabled");
    private static final Identifier FILTER_ENABLED_HOVER =
        Identifier.withDefaultNamespace("recipe_book/filter_enabled_highlighted");
    private static final Identifier FILTER_DISABLED_HOVER =
        Identifier.withDefaultNamespace("recipe_book/filter_disabled_highlighted");
    private static final Identifier SLOT_CRAFTABLE_SPRITE =
        Identifier.withDefaultNamespace("recipe_book/slot_craftable");
    private static final Identifier SLOT_UNCRAFTABLE_SPRITE =
        Identifier.withDefaultNamespace("recipe_book/slot_uncraftable");

    private enum Tab {
    BANNERS,
    PACKS,
    EDIT,
    GUIDE
    }

    private final LoomScreen screen;
    private final LoomMenu handler;
    private int x;
    private int y;
    private final AutoCraftStateMachine autoCraft;
    private final EditBox searchBox;
    private final Button autoCraftButton;
    private Tab activeTab = Tab.BANNERS;
    private boolean craftableOnly = false;
    private int page = 0;
    private String selectedBannerId = null;

    public LoomPanel(LoomScreen screen, LoomMenu handler, int x, int y) {
        this.screen = screen;
        this.handler = handler;
        this.x = x;
        this.y = y;
        this.autoCraft = new AutoCraftStateMachine(handler);
        this.searchBox = new EditBox(
                Minecraft.getInstance().font,
                x + SEARCH_X,
                y + SEARCH_Y,
                SEARCH_W,
                SEARCH_H,
                Component.translatable("itemGroup.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(-1);
        this.searchBox.setHint(Component.translatable("gui.recipebook.search_hint").withStyle(EditBox.SEARCH_HINT_STYLE));
        this.autoCraftButton = Button.builder(Component.literal("Auto craft"), button -> startSelectedBannerAutoCraft())
            .bounds(x + GUIDE_BUTTON_X, y + GUIDE_BUTTON_Y, GUIDE_BUTTON_W, GUIDE_BUTTON_H)
            .build();
        this.autoCraftButton.active = false;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;

        ctx.blit(RenderPipelines.GUI_TEXTURED, RECIPE_BOOK_TEXTURE, x, y, 1.0F, 1.0F, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);
        renderTabs(ctx, mouseX, mouseY);
        if (activeTab == Tab.BANNERS) {
            searchBox.extractRenderState(ctx, mouseX, mouseY, delta);
            renderFilterButton(ctx, mouseX, mouseY);
        }

        if (activeTab == Tab.BANNERS) {
            renderBannerGrid(ctx, font, mouseX, mouseY);
        } else if (activeTab == Tab.PACKS) {
            renderPacksTab(ctx, font);
        } else if (activeTab == Tab.EDIT) {
            renderEditTab(ctx, font);
        } else if (activeTab == Tab.GUIDE) {
            renderGuideTab(ctx, font, mouseX, mouseY);
        }

        if (autoCraft.isActive()) {
            ctx.text(font, Component.literal("Crafting..."), x + 8, y + PANEL_HEIGHT + 2, 0xFFFFFF00, true);
        }
    }

    private void renderTabs(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        Tab[] tabs = new Tab[] {Tab.BANNERS, Tab.PACKS, Tab.EDIT, Tab.GUIDE};

        for (int i = 0; i < tabs.length; i++) {
            int tx = x - TAB_X_OFFSET;
            int ty = y + TAB_Y_START + i * RecipeBookTabButton.HEIGHT;
            RecipeBookTabButton button = createTabButton(tabs[i], tx, ty);
            if (activeTab == tabs[i]) {
                button.select();
            }
            button.extractContents(ctx, mouseX, mouseY, 0.0F);
            if (mouseX >= tx - 2 && mouseX < tx - 2 + RecipeBookTabButton.WIDTH && mouseY >= ty && mouseY < ty + RecipeBookTabButton.HEIGHT) {
                ctx.requestCursor(com.mojang.blaze3d.platform.cursor.CursorTypes.POINTING_HAND);
            }
        }
    }

    private RecipeBookTabButton createTabButton(Tab tab, int tx, int ty) {
        RecipeBookComponent.TabInfo tabInfo = switch (tab) {
            case BANNERS -> new RecipeBookComponent.TabInfo(Items.COMPASS, new RecipeBookCategory());
            case PACKS -> new RecipeBookComponent.TabInfo(Items.BRICKS, new RecipeBookCategory());
            case EDIT -> new RecipeBookComponent.TabInfo(Items.REDSTONE, new RecipeBookCategory());
            case GUIDE -> new RecipeBookComponent.TabInfo(Items.BOOK, new RecipeBookCategory());
        };

        return new RecipeBookTabButton(tx, ty, tabInfo, button -> {
            activeTab = tab;
            page = 0;
            searchBox.setFocused(activeTab == Tab.BANNERS);
        });
    }

    private void renderFilterButton(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        int bx = x + FILTER_X;
        int by = y + FILTER_Y;
        boolean hov = mouseX >= bx && mouseX < bx + FILTER_W && mouseY >= by && mouseY < by + FILTER_H;
        Identifier sprite;
        if (craftableOnly) {
            sprite = hov ? FILTER_ENABLED_HOVER : FILTER_ENABLED;
        } else {
            sprite = hov ? FILTER_DISABLED_HOVER : FILTER_DISABLED;
        }
        ctx.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, bx, by, FILTER_W, FILTER_H);
    }

    private void renderBannerGrid(GuiGraphicsExtractor ctx, Font font, int mouseX, int mouseY) {
        List<SavedBanner> items = getFilteredBanners();
        int start = page * GRID_COLUMNS * GRID_ROWS;
        int end = Math.min(items.size(), start + GRID_COLUMNS * GRID_ROWS);
        for (int i = start; i < end; i++) {
            SavedBanner banner = items.get(i);
            int local = i - start;
            int col = local % GRID_COLUMNS;
            int row = local / GRID_COLUMNS;
            int bx = x + GRID_START_X + col * GRID_CELL;
            int by = y + GRID_START_Y + row * GRID_CELL;

            Identifier sprite = isCraftableNow(banner) ? SLOT_CRAFTABLE_SPRITE : SLOT_UNCRAFTABLE_SPRITE;
            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, bx - 1, by - 1, 25, 25);
            BannerPreviewRenderer.render(ctx, banner, handler, bx + 4, by + 4, 16);
            if (banner.getId().equals(selectedBannerId)) {
                ctx.outline(bx - 1, by - 1, 25, 25, 0xFF5C7CFA);
            }

            if (mouseX >= bx && mouseX < bx + 16 && mouseY >= by && mouseY < by + 16) {
                ctx.requestCursor(com.mojang.blaze3d.platform.cursor.CursorTypes.POINTING_HAND);
                Optional<TooltipComponent> image = buildTooltipImage(banner);
                ctx.setTooltipForNextFrame(font, List.of(Component.literal(banner.getDisplayName())), image, mouseX, mouseY);
            }
        }

        if (items.isEmpty()) {
            ctx.text(font, Component.literal("No banners"), x + 38, y + 76, 0xFF777777, false);
        }
    }

    private void renderGuideTab(GuiGraphicsExtractor ctx, Font font, int mouseX, int mouseY) {
        SavedBanner banner = getSelectedBanner();
        if (banner == null) {
            ctx.text(font, Component.literal("Select a banner"), x + 18, y + 48, 0xFFDDDDDD, false);
            ctx.text(font, Component.literal("to see the craft guide."), x + 18, y + 60, 0xFF777777, false);
            autoCraftButton.active = false;
            autoCraftButton.setPosition(x + GUIDE_BUTTON_X, y + GUIDE_BUTTON_Y);
            autoCraftButton.extractRenderState(ctx, mouseX, mouseY, 0.0F);
            return;
        }

        ctx.text(font, Component.literal("Craft guide"), x + 12, y + 20, 0xFFDDDDDD, false);
        ctx.text(font, Component.literal(banner.getDisplayName()), x + 12, y + 32, 0xFFFFFFFF, false);

        List<GuideCard> cards = buildGuideCards(banner);
        int cardY = y + 48;
        for (int i = 0; i < cards.size(); i++) {
            GuideCard card = cards.get(i);
            int cardHeight = card.rows().size() * 18 + 6;
            int cardWidth = PANEL_WIDTH - 24;
            int cardX = x + 8;

            int bgColor = card.highlighted() ? 0x4A3D2A66 : 0x241A1A1A;
            int borderColor = card.highlighted() ? 0xFF8FB6FF : 0x60FFFFFF;
            ctx.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, bgColor);
            ctx.outline(cardX, cardY, cardWidth, cardHeight, borderColor);

            int rowY = cardY + 4;
            for (BannerRecipeTooltipComponent.Row row : card.rows()) {
                ctx.text(font, row.text(), cardX + 6, rowY + 1, 0xFFFFFFFF, false);
                ctx.item(row.primary(), cardX + 6, rowY + 11);
                if (row.hasSecondary()) {
                    ctx.item(row.secondary(), cardX + 24, rowY + 11);
                }
                rowY += 18;
            }

            cardY += cardHeight + 4;
        }

        autoCraftButton.active = !autoCraft.isActive();
        autoCraftButton.setPosition(x + GUIDE_BUTTON_X, y + GUIDE_BUTTON_Y);
        autoCraftButton.extractRenderState(ctx, mouseX, mouseY, 0.0F);
    }

    private Optional<TooltipComponent> buildTooltipImage(SavedBanner banner) {
        List<BannerRecipeTooltipComponent.Row> rows = buildRecipeRows(banner);
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new BannerRecipeTooltipComponent(rows));
    }

    private List<GuideCard> buildGuideCards(SavedBanner banner) {
        List<BannerRecipeTooltipComponent.Row> rows = buildRecipeRows(banner);
        List<GuideCard> cards = new ArrayList<>();
        if (rows.isEmpty()) {
            return cards;
        }

        if (rows.size() == 1) {
            cards.add(new GuideCard(rows, true));
            return cards;
        }

        cards.add(new GuideCard(rows.subList(0, Math.min(2, rows.size())), true));
        for (int i = 2; i < rows.size(); i++) {
            cards.add(new GuideCard(List.of(rows.get(i)), false));
        }
        return cards;
    }

    private List<BannerRecipeTooltipComponent.Row> buildRecipeRows(SavedBanner banner) {
        List<BannerRecipeTooltipComponent.Row> rows = new ArrayList<>();
        String baseKey = "block.minecraft." + banner.getBaseColorEnum().getSerializedName() + "_banner";
        String baseName = Language.getInstance().getOrDefault(baseKey);
        rows.add(BannerRecipeTooltipComponent.Row.single(
                new ItemStack(banner.getBaseBannerItem()),
                Component.literal("1. " + baseName)));

        int idx = 2;
        for (BannerPatternLayer layer : banner.getLayers()) {
            ItemStack patternIcon = getPatternItem(layer.patternId());
            if (patternIcon.isEmpty()) {
                patternIcon = createLayerPreviewStack(layer);
            }
            ItemStack dyeStack = new ItemStack(SavedBanner.getDyeItem(layer.getDyeColorEnum()));
            rows.add(BannerRecipeTooltipComponent.Row.pair(
                    patternIcon,
                    dyeStack,
                    Component.literal(idx + ". " + getPatternDisplayName(layer))));
            idx++;
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private ItemStack createLayerPreviewStack(BannerPatternLayer layer) {
        ItemStack stack = new ItemStack(Items.BANNER.white());
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return stack;
        }

        try {
            var patternRegistry = mc.level.registryAccess().lookup(Registries.BANNER_PATTERN);
            if (patternRegistry.isEmpty()) {
                return stack;
            }
            Identifier id = Identifier.tryParse(layer.patternId());
            if (id == null) {
                return stack;
            }

            var entry = patternRegistry.get().get(id);
            if (entry.isEmpty()) {
                return stack;
            }

            BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
            builder.add((Holder) entry.get(), layer.getDyeColorEnum());
            stack.set(DataComponents.BANNER_PATTERNS, builder.build());
        } catch (Exception ignored) {
        }

        return stack;
    }

    @SuppressWarnings("unchecked")
    private ItemStack getPatternItem(String patternId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return ItemStack.EMPTY;
        }
        try {
            var itemRegistry = mc.level.registryAccess().lookup(Registries.ITEM);
            if (itemRegistry.isEmpty()) {
                return ItemStack.EMPTY;
            }

            String[] parts = patternId.split(":");
            String namespace = parts.length > 1 ? parts[0] : "minecraft";
            String patternName = parts.length > 1 ? parts[1] : patternId;
            Identifier itemId = Identifier.tryParse(namespace + ":" + patternName + "_banner_pattern");
            if (itemId == null) {
                return ItemStack.EMPTY;
            }

            var entry = itemRegistry.get().get(itemId);
            if (entry.isPresent()) {
                return new ItemStack(entry.get().value());
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    private void renderPacksTab(GuiGraphicsExtractor ctx, Font font) {
        int ty = y + GRID_START_Y;
        List<String> packs = getInstalledPacks();
        if (packs.isEmpty()) {
            ctx.text(font, Component.literal("No packs installed"), x + 12, ty, 0xFF777777, false);
            return;
        }
        for (String pack : packs) {
            ctx.text(font, Component.literal("- " + pack), x + 12, ty, 0xFFDDDDDD, false);
            ty += 11;
            if (ty > y + 130) break;
        }
    }

    private void renderEditTab(GuiGraphicsExtractor ctx, Font font) {
        ctx.text(font, Component.literal("Edit"), x + 12, y + GRID_START_Y, 0xFFDDDDDD, false);
        ctx.text(font, Component.literal("Coming soon"), x + 12, y + GRID_START_Y + 12, 0xFF777777, false);
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    public boolean mouseClicked(MouseButtonEvent event) {
        if (event.button() != 0) return false;
        int mx = (int) event.x();
        int my = (int) event.y();

        if (clickTabs(mx, my)) {
            return true;
        }

        if (activeTab == Tab.BANNERS && searchBox.mouseClicked(event, false)) {
            return true;
        }

        if (activeTab == Tab.BANNERS) {
            int filterX = x + FILTER_X;
            int filterY = y + FILTER_Y;
            if (isIn(mx, my, filterX, filterY, FILTER_W, FILTER_H)) {
                craftableOnly = !craftableOnly;
                page = 0;
                return true;
            }
        }

        if (activeTab == Tab.BANNERS && clickBannerGrid(mx, my)) {
            return true;
        }

        if (activeTab == Tab.GUIDE && autoCraftButton.mouseClicked(event, false)) {
            return true;
        }

        return false;
    }

    private boolean clickTabs(int mx, int my) {
        Tab[] tabs = new Tab[] {Tab.BANNERS, Tab.PACKS, Tab.EDIT};
        for (int i = 0; i < tabs.length; i++) {
            int tx = x - TAB_X_OFFSET;
            int ty = y + TAB_Y_START + i * RecipeBookTabButton.HEIGHT;
            if (isIn(mx, my, tx - (activeTab == tabs[i] ? 2 : 0), ty, RecipeBookTabButton.WIDTH, RecipeBookTabButton.HEIGHT)) {
                activeTab = tabs[i];
                page = 0;
                searchBox.setFocused(activeTab == Tab.BANNERS);
                return true;
            }
        }
        return false;
    }

    private boolean clickBannerGrid(int mx, int my) {
        List<SavedBanner> items = getFilteredBanners();
        int start = page * GRID_COLUMNS * GRID_ROWS;
        int end = Math.min(items.size(), start + GRID_COLUMNS * GRID_ROWS);
        boolean isShiftPressed = InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);

        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % GRID_COLUMNS;
            int row = local / GRID_COLUMNS;
            int bx = x + GRID_START_X + col * GRID_CELL;
            int by = y + GRID_START_Y + row * GRID_CELL;
            if (isIn(mx, my, bx, by, 16, 16)) {
                SavedBanner banner = items.get(i);
                if (isShiftPressed) {
                    autoCraft.start(banner);
                } else {
                    selectedBannerId = banner.getId();
                    activeTab = Tab.GUIDE;
                    searchBox.setFocused(false);
                }
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        if (activeTab != Tab.BANNERS) {
            return false;
        }
        if (searchBox.keyPressed(event)) {
            page = 0;
            return true;
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        if (activeTab != Tab.BANNERS) {
            return false;
        }
        if (searchBox.charTyped(event)) {
            page = 0;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (activeTab != Tab.BANNERS) {
            return false;
        }
        return searchBox.mouseDragged(event, dx, dy);
    }

    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (activeTab != Tab.BANNERS) {
            return false;
        }
        List<SavedBanner> items = getFilteredBanners();
        int maxPage = Math.max(0, (items.size() - 1) / (GRID_COLUMNS * GRID_ROWS));
        if (vAmt > 0 && page > 0) {
            page--;
            return true;
        }
        if (vAmt < 0 && page < maxPage) {
            page++;
            return true;
        }
        return false;
    }

    public void tick() {
        autoCraft.tick();
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        this.searchBox.setPosition(x + SEARCH_X, y + SEARCH_Y);
        this.autoCraftButton.setPosition(x + GUIDE_BUTTON_X, y + GUIDE_BUTTON_Y);
    }

    private void startSelectedBannerAutoCraft() {
        SavedBanner selectedBanner = getSelectedBanner();
        if (selectedBanner != null) {
            autoCraft.start(selectedBanner);
        }
    }

    // -------------------------------------------------------------------------
    // Data helpers
    // -------------------------------------------------------------------------

    private List<SavedBanner> getFilteredBanners() {
        String query = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<SavedBanner> out = new ArrayList<>();
        for (SavedBanner banner : BannerStorage.getInstance().getBanners()) {
            if (!query.isEmpty() && !banner.getDisplayName().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            if (craftableOnly && !isCraftableNow(banner)) {
                continue;
            }
            out.add(banner);
        }
        return out;
    }

    private boolean isCraftableNow(SavedBanner banner) {
        return true;
    }

    private SavedBanner getSelectedBanner() {
        if (selectedBannerId == null) {
            return null;
        }
        for (SavedBanner banner : BannerStorage.getInstance().getBanners()) {
            if (selectedBannerId.equals(banner.getId())) {
                return banner;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String getPatternDisplayName(BannerPatternLayer layer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            try {
                var regOpt = mc.level.registryAccess().lookup(Registries.BANNER_PATTERN);
                if (regOpt.isPresent()) {
                    Identifier id = Identifier.tryParse(layer.patternId());
                    if (id != null) {
                        var entry = regOpt.get().get(id);
                        if (entry.isPresent()) {
                            net.minecraft.world.level.block.entity.BannerPattern pattern =
                                    (net.minecraft.world.level.block.entity.BannerPattern) entry.get().value();
                            String key = pattern.translationKey() + "." + layer.getDyeColorEnum().getName();
                            return Component.translatable(key).getString();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return toTitle(layer.getDyeColorEnum().getSerializedName()) + " " + toTitle(layer.patternId());
    }

    private List<String> getInstalledPacks() {
        List<String> packs = new ArrayList<>();
        packs.add("root");
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("loom-assistant").resolve("bannerpacks");
            if (Files.exists(dir)) {
                try (var stream = Files.list(dir)) {
                    stream.map(path -> path.getFileName().toString())
                            .filter(name -> !name.equalsIgnoreCase("root"))
                            .sorted()
                            .forEach(packs::add);
                }
            }
        } catch (Exception ignored) {
        }
        return packs;
    }

    private static String toTitle(String id) {
        String raw = id.contains(":") ? id.split(":", 2)[1] : id;
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split("_")) {
            if (!sb.isEmpty()) sb.append(' ');
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private static boolean isIn(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private record GuideCard(List<BannerRecipeTooltipComponent.Row> rows, boolean highlighted) {
    }

    public static boolean saveBannerFromOutput(LoomMenu handler) {
        var dyeStack = handler.getSlot(1).getItem();
        var patternStack = handler.getSlot(2).getItem();
        if (dyeStack.isEmpty() && patternStack.isEmpty()) {
            var bannerStack = handler.getSlot(0).getItem();
            if (bannerStack.isEmpty()) return false;
            SavedBanner banner = BannerPreviewRenderer.extractBannerData(bannerStack);
            if (banner != null) {
                BannerStorage.getInstance().addBanner(banner);
                return true;
            }
        } else {
            var outputStack = handler.getSlot(3).getItem();
            if (outputStack.isEmpty()) return false;
            SavedBanner banner = BannerPreviewRenderer.extractBannerData(outputStack);
            if (banner != null) {
                BannerStorage.getInstance().addBanner(banner);
                return true;
            }
        }
        return false;
    }
}
