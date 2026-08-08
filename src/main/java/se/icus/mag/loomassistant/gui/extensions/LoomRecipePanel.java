/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.gui.extensions;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.lwjgl.glfw.GLFW;
import se.icus.mag.loomassistant.LoomAssistantMod;
import se.icus.mag.loomassistant.LoomScreenStateManager;
import se.icus.mag.loomassistant.bannerpack.storage.BannerStorage;
import se.icus.mag.loomassistant.gui.tooltip.BannerRecipeTooltipComponent;
import se.icus.mag.loomassistant.recipe.BannerRecipe;
import se.icus.mag.loomassistant.recipe.BannerRecipeCategories;
import se.icus.mag.loomassistant.recipe.BannerRecipeCategory;
import se.icus.mag.loomassistant.recipe.BannerRecipeItemConverter;
import se.icus.mag.loomassistant.recipe.BannerRecipeLayer;
import se.icus.mag.loomassistant.weaving.Weaver;

public class LoomRecipePanel extends ScreenExtensionWidget {
    public static final int PANEL_WIDTH = 147;
    private static final int PANEL_HEIGHT = 166;
    private static final int GRID_COLUMNS = 5;
    private static final int GRID_ROWS = 4;
    private static final int GRID_CELL = 25;
    private static final int GRID_START_X = 11;
    private static final int GRID_START_Y = 31;
    private static final int TAB_X_OFFSET = 30;
    private static final int TAB_Y_START = 3;
    private static final int VISIBLE_TAB_SLOTS = 6;
    private static final int TAB_SCROLL_ARROW_W = 16;
    private static final int TAB_SCROLL_ARROW_H = 16;
    private static final int SEARCH_X = 25;
    private static final int SEARCH_Y = 13;
    private static final int SEARCH_W = 81;
    private static final int SEARCH_H = 14;
    private static final int FILTER_X = 110;
    private static final int FILTER_Y = 12;
    private static final int FILTER_W = 26;
    private static final int FILTER_H = 16;
    private static final Identifier RECIPE_BOOK_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/recipe_book.png");
    private static final Identifier FILTER_ENABLED =
            Identifier.fromNamespaceAndPath("loom-assistant", "loom_recipe_book/filter_enabled");
    private static final Identifier FILTER_DISABLED =
            Identifier.fromNamespaceAndPath("loom-assistant", "loom_recipe_book/filter_disabled");
    private static final Identifier FILTER_ENABLED_HOVER =
            Identifier.fromNamespaceAndPath("loom-assistant", "loom_recipe_book/filter_enabled_highlighted");
    private static final Identifier FILTER_DISABLED_HOVER =
            Identifier.fromNamespaceAndPath("loom-assistant", "loom_recipe_book/filter_disabled_highlighted");
    private static final Component ALL_RECIPES_TOOLTIP = Component.translatable("loom-assistant.panel.showing_all");
    private static final Component ALL_CATEGORIES_TOOLTIP = Component.translatable("loom-assistant.panel.all_recipes");
    private static final Component ONLY_CRAFTABLES_TOOLTIP =
            Component.translatable("loom-assistant.panel.show_weavable");
    private static final Component WEAVING_LABEL = Component.translatable("loom-assistant.panel.weaving");
    private static final Component NO_BANNERS_LABEL = Component.translatable("loom-assistant.panel.no_banners");
    private static final Component CATEGORY_SCROLL_UP_TOOLTIP = Component.literal("Scroll tabs up");
    private static final Component CATEGORY_SCROLL_DOWN_TOOLTIP = Component.literal("Scroll tabs down");
    private static final Identifier TAB_SCROLL_UP_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_up");
    private static final Identifier TAB_SCROLL_UP_HIGHLIGHTED_SPRITE =
            Identifier.withDefaultNamespace("transferable_list/move_up_highlighted");
    private static final Identifier TAB_SCROLL_DOWN_SPRITE =
            Identifier.withDefaultNamespace("transferable_list/move_down");
    private static final Identifier TAB_SCROLL_DOWN_HIGHLIGHTED_SPRITE =
            Identifier.withDefaultNamespace("transferable_list/move_down_highlighted");
    private static final Identifier SLOT_CRAFTABLE_SPRITE =
            Identifier.withDefaultNamespace("recipe_book/slot_craftable");
    private static final Identifier SLOT_UNCRAFTABLE_SPRITE =
            Identifier.withDefaultNamespace("recipe_book/slot_uncraftable");
    private static final WidgetSprites PAGE_FORWARD_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/page_forward"),
            Identifier.withDefaultNamespace("recipe_book/page_forward_highlighted"));
    private static final WidgetSprites PAGE_BACKWARD_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/page_backward"),
            Identifier.withDefaultNamespace("recipe_book/page_backward_highlighted"));
    // Page button size matches vanilla recipe book
    private static final int PAGE_BTN_W = 12;
    private static final int PAGE_BTN_H = 17;
    private static final int PAGE_BTN_Y_OFFSET = 137; // relative to panel top, same as vanilla

    private final LoomScreenStateManager manager;
    private final LoomMenu handler;
    private int x;
    private int y;
    private final EditBox searchBox;
    private final List<BannerRecipeCategory> categoryTabs;
    private List<TabDescriptor> tabs;
    private String selectedCategoryId;
    private int tabScrollOffset = 0;
    private boolean craftableOnly = false;
    private int page = 0;
    private final ImageButton pageForwardButton;
    private final ImageButton pageBackButton;

    public LoomRecipePanel(LoomScreenStateManager manager, LoomScreen screen, LoomMenu handler, int x, int y) {
        this.manager = manager;
        this.handler = handler;
        this.x = x;
        this.y = y;
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
        this.searchBox.setHint(
                Component.translatable("gui.recipebook.search_hint").withStyle(EditBox.SEARCH_HINT_STYLE));
        this.categoryTabs = BannerRecipeCategories.getCategories();
        this.tabs = List.of();
        this.selectedCategoryId = manager.getSelectedCategoryId();
        refreshVisibleTabs();

        pageForwardButton = new ImageButton(
                x + 93,
                y + PAGE_BTN_Y_OFFSET,
                PAGE_BTN_W,
                PAGE_BTN_H,
                PAGE_FORWARD_SPRITES,
                button -> page++,
                Component.translatable("gui.recipebook.next_page"));
        pageBackButton = new ImageButton(
                x + 38,
                y + PAGE_BTN_Y_OFFSET,
                PAGE_BTN_W,
                PAGE_BTN_H,
                PAGE_BACKWARD_SPRITES,
                button -> page--,
                Component.translatable("gui.recipebook.previous_page"));
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        refreshVisibleTabs();
        Font font = Minecraft.getInstance().font;

        context.blit(
                RenderPipelines.GUI_TEXTURED,
                RECIPE_BOOK_TEXTURE,
                x,
                y,
                1.0F,
                1.0F,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                256,
                256);
        renderTabs(context, mouseX, mouseY);
        searchBox.extractRenderState(context, mouseX, mouseY, delta);
        renderFilterButton(context, mouseX, mouseY);
        renderBannerGrid(context, font, mouseX, mouseY);

        if (manager.isWeavingActive()) {
            context.text(font, WEAVING_LABEL, x + 8, y + PANEL_HEIGHT + 2, 0xFFFFFF00, true);
        }
    }

    private void renderTabs(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        int tx = x - TAB_X_OFFSET;
        int visibleTabCount = getVisibleTabCount();
        for (int slot = 0; slot < visibleTabCount; slot++) {
            TabDescriptor tab = tabs.get(tabScrollOffset + slot);
            int ty = y + TAB_Y_START + slot * RecipeBookTabButton.HEIGHT;
            RecipeBookTabButton button = createTabButton(tab, tx, ty);

            if (isTabSelected(tab)) {
                button.select();
            }
            button.extractContents(ctx, mouseX, mouseY, 0.0F);
            if (isIn(mouseX, mouseY, tx - 2, ty, RecipeBookTabButton.WIDTH, RecipeBookTabButton.HEIGHT)) {
                ctx.requestCursor(CursorTypes.POINTING_HAND);
                ctx.setTooltipForNextFrame(
                        Minecraft.getInstance().font, List.of(tab.tooltip()), Optional.empty(), mouseX, mouseY);
            }
        }

        renderTabScrollArrows(ctx, mouseX, mouseY, tx, visibleTabCount);
    }

    private void renderTabScrollArrows(GuiGraphicsExtractor ctx, int mouseX, int mouseY, int tx, int visibleTabCount) {
        if (!hasScrollableTabs()) return;

        int arrowX = tx + (RecipeBookTabButton.WIDTH - TAB_SCROLL_ARROW_W) / 2;

        int upArrowY = y + TAB_Y_START - TAB_SCROLL_ARROW_H - 2;
        boolean upEnabled = canScrollTabsUp();
        if (upEnabled) {
            boolean upHover = isIn(mouseX, mouseY, arrowX, upArrowY, TAB_SCROLL_ARROW_W, TAB_SCROLL_ARROW_H);
            Identifier upSprite = upHover ? TAB_SCROLL_UP_HIGHLIGHTED_SPRITE : TAB_SCROLL_UP_SPRITE;
            ctx.blitSprite(
                    RenderPipelines.GUI_TEXTURED, upSprite, arrowX, upArrowY, TAB_SCROLL_ARROW_W, TAB_SCROLL_ARROW_H);
            if (upHover) {
                ctx.requestCursor(CursorTypes.POINTING_HAND);
                ctx.setTooltipForNextFrame(
                        Minecraft.getInstance().font,
                        List.of(CATEGORY_SCROLL_UP_TOOLTIP),
                        Optional.empty(),
                        mouseX,
                        mouseY);
            }
        }

        int bottomTabY = y + TAB_Y_START + (visibleTabCount - 1) * RecipeBookTabButton.HEIGHT;
        int downArrowY = bottomTabY + RecipeBookTabButton.HEIGHT;
        boolean downEnabled = canScrollTabsDown();
        if (downEnabled) {
            boolean downHover = isIn(mouseX, mouseY, arrowX, downArrowY, TAB_SCROLL_ARROW_W, TAB_SCROLL_ARROW_H);
            Identifier downSprite = downHover ? TAB_SCROLL_DOWN_HIGHLIGHTED_SPRITE : TAB_SCROLL_DOWN_SPRITE;
            ctx.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    downSprite,
                    arrowX,
                    downArrowY,
                    TAB_SCROLL_ARROW_W,
                    TAB_SCROLL_ARROW_H);
            if (downHover) {
                ctx.requestCursor(CursorTypes.POINTING_HAND);
                ctx.setTooltipForNextFrame(
                        Minecraft.getInstance().font,
                        List.of(CATEGORY_SCROLL_DOWN_TOOLTIP),
                        Optional.empty(),
                        mouseX,
                        mouseY);
            }
        }
    }

    private RecipeBookTabButton createTabButton(TabDescriptor tab, int tx, int ty) {
        ItemStack icon = tab.category() == null
                ? new ItemStack(Items.COMPASS)
                : BannerRecipeCategories.resolveIcon(tab.category());
        RecipeBookComponent.TabInfo tabInfo = new RecipeBookComponent.TabInfo(icon.getItem(), new RecipeBookCategory());

        return new RecipeBookTabButton(tx, ty, tabInfo, button -> {
            setSelectedCategoryId(tab.categoryId());
            page = 0;
            searchBox.setFocused(true);
        });
    }

    private boolean isTabSelected(TabDescriptor tab) {
        return tab.categoryId() == null
                ? selectedCategoryId == null
                : tab.categoryId().equals(selectedCategoryId);
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
        if (hov) {
            ctx.requestCursor(CursorTypes.POINTING_HAND);
            Component tooltip = craftableOnly ? ONLY_CRAFTABLES_TOOLTIP : ALL_RECIPES_TOOLTIP;
            ctx.setTooltipForNextFrame(
                    Minecraft.getInstance().font, List.of(tooltip), Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderBannerGrid(GuiGraphicsExtractor ctx, Font font, int mouseX, int mouseY) {
        List<BannerRecipe> items = getFilteredBanners();
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) (GRID_COLUMNS * GRID_ROWS)));
        page = Math.min(page, totalPages - 1);
        int start = page * GRID_COLUMNS * GRID_ROWS;
        int end = Math.min(items.size(), start + GRID_COLUMNS * GRID_ROWS);
        for (int i = start; i < end; i++) {
            BannerRecipe banner = items.get(i);
            int local = i - start;
            int col = local % GRID_COLUMNS;
            int row = local / GRID_COLUMNS;
            int bx = x + GRID_START_X + col * GRID_CELL;
            int by = y + GRID_START_Y + row * GRID_CELL;

            Identifier sprite = isCraftableNow(banner) ? SLOT_CRAFTABLE_SPRITE : SLOT_UNCRAFTABLE_SPRITE;
            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, bx - 1, by - 1, 25, 25);
            WeavingGuide.renderBannerPreview(ctx, banner, bx + 4, by + 4);

            if (mouseX >= bx && mouseX < bx + 16 && mouseY >= by && mouseY < by + 16) {
                ctx.requestCursor(CursorTypes.POINTING_HAND);
                setBannerTooltip(ctx, banner, mouseX, mouseY);
            }
        }

        if (items.isEmpty()) {
            ctx.text(font, NO_BANNERS_LABEL, x + 38, y + 76, 0xFF777777, false);
        }

        // Page navigation (only when there is more than one page)
        if (totalPages > 1) {
            pageForwardButton.visible = page < totalPages - 1;
            pageBackButton.visible = page > 0;
            if (pageForwardButton.visible) {
                pageForwardButton.extractRenderState(ctx, mouseX, mouseY, 0f);
            }
            if (pageBackButton.visible) {
                pageBackButton.extractRenderState(ctx, mouseX, mouseY, 0f);
            }

            Component pageText = Component.translatable("gui.recipebook.page", page + 1, totalPages);
            int textW = font.width(pageText);
            ctx.text(font, pageText, x - textW / 2 + 73, y + 141, -1, false);
        }
    }

    private static Optional<TooltipComponent> buildTooltipImage(BannerRecipe banner, int currentRowIndex) {
        List<BannerRecipeTooltipComponent.Row> rows = buildRecipeRows(banner);
        if (rows.isEmpty()) return Optional.empty();

        ItemStack previewStack = BannerRecipeItemConverter.toItem(Minecraft.getInstance(), banner);
        BannerPatternLayers patterns = previewStack.get(DataComponents.BANNER_PATTERNS);
        DyeColor baseColor = banner.getBannerColorEnum();
        boolean notWeavable = !banner.isWeavable();
        return Optional.of(new BannerRecipeTooltipComponent(rows, baseColor, patterns, currentRowIndex, notWeavable));
    }

    private static List<BannerRecipeTooltipComponent.Row> buildRecipeRows(BannerRecipe banner) {
        List<BannerRecipeTooltipComponent.Row> rows = new ArrayList<>();
        String baseKey = "block.minecraft." + banner.getBaseColorEnum().getSerializedName() + "_banner";
        String baseName = Language.getInstance().getOrDefault(baseKey);
        rows.add(BannerRecipeTooltipComponent.Row.singleIndented(
                new ItemStack(banner.getBaseBannerItem()), Component.literal(baseName)));

        int idx = 1;
        for (BannerRecipeLayer layer : banner.getLayers()) {
            ItemStack dyeStack = new ItemStack(BannerRecipe.getDyeItem(layer.getDyeColorEnum()));
            Component stepText = Component.literal(idx + ". " + getPatternDisplayName(layer));
            Identifier patternId = Identifier.tryParse(layer.patternId());
            if (patternId != null) {
                rows.add(BannerRecipeTooltipComponent.Row.withPattern(dyeStack, patternId, stepText));
            } else {
                ItemStack patternIcon = getPatternItem(layer.patternId());
                if (patternIcon.isEmpty()) {
                    patternIcon = BannerRecipeItemConverter.toItem(
                            LoomAssistantMod.getBannerPatternRegistry(Minecraft.getInstance()),
                            Items.BANNER.white(),
                            List.of(layer));
                }
                rows.add(BannerRecipeTooltipComponent.Row.pair(dyeStack, patternIcon, stepText));
            }
            idx++;
        }
        return rows;
    }

    private static ItemStack getPatternItem(String patternId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return ItemStack.EMPTY;

        Optional<Registry<Item>> itemRegistry = mc.level.registryAccess().lookup(Registries.ITEM);
        if (itemRegistry.isEmpty()) return ItemStack.EMPTY;

        String[] parts = patternId.split(":");
        String namespace = parts.length > 1 ? parts[0] : "minecraft";
        String patternName = parts.length > 1 ? parts[1] : patternId;
        Identifier itemId = Identifier.tryParse(namespace + ":" + patternName + "_banner_pattern");
        if (itemId == null) return ItemStack.EMPTY;

        Optional<Holder.Reference<Item>> entry = itemRegistry.get().get(itemId);
        if (entry.isEmpty()) return ItemStack.EMPTY;

        return new ItemStack(entry.get().value());
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    public boolean mouseClicked(MouseButtonEvent event) {
        if (event.button() != 0) return false;
        refreshVisibleTabs();
        int mx = (int) event.x();
        int my = (int) event.y();

        int searchX = x + SEARCH_X;
        int searchY = y + SEARCH_Y;
        if (isIn(mx, my, searchX, searchY, SEARCH_W, SEARCH_H)) {
            searchBox.setFocused(true);
            searchBox.mouseClicked(event, false);
            return true;
        }

        if (clickTabs(mx, my)) {
            return true;
        }

        if (searchBox.mouseClicked(event, false)) {
            return true;
        }

        int filterX = x + FILTER_X;
        int filterY = y + FILTER_Y;
        if (isIn(mx, my, filterX, filterY, FILTER_W, FILTER_H)) {
            craftableOnly = !craftableOnly;
            page = 0;
            playUiClickSound();
            return true;
        }

        if (clickBannerGrid(mx, my)) {
            return true;
        }

        if (pageForwardButton.visible && pageForwardButton.mouseClicked(event, false)) {
            playUiClickSound();
            return true;
        }
        if (pageBackButton.visible && pageBackButton.mouseClicked(event, false)) {
            playUiClickSound();
            return true;
        }

        return false;
    }

    private boolean clickTabs(int mx, int my) {
        int tx = x - TAB_X_OFFSET;
        int visibleTabCount = getVisibleTabCount();
        for (int slot = 0; slot < visibleTabCount; slot++) {
            TabDescriptor tab = tabs.get(tabScrollOffset + slot);
            int ty = y + TAB_Y_START + slot * RecipeBookTabButton.HEIGHT;
            if (isIn(
                    mx,
                    my,
                    tx - (isTabSelected(tab) ? 2 : 0),
                    ty,
                    RecipeBookTabButton.WIDTH,
                    RecipeBookTabButton.HEIGHT)) {
                setSelectedCategoryId(tab.categoryId());
                page = 0;
                searchBox.setFocused(true);
                playUiClickSound();
                return true;
            }
        }

        if (hasScrollableTabs()) {
            int arrowX = tx + (RecipeBookTabButton.WIDTH - TAB_SCROLL_ARROW_W) / 2;
            int upArrowY = y + TAB_Y_START - TAB_SCROLL_ARROW_H - 2;
            if (isIn(mx, my, arrowX, upArrowY, TAB_SCROLL_ARROW_W, TAB_SCROLL_ARROW_H) && canScrollTabsUp()) {
                scrollTabs(-1);
                playUiClickSound();
                return true;
            }

            int bottomTabY = y + TAB_Y_START + (visibleTabCount - 1) * RecipeBookTabButton.HEIGHT;
            int downArrowY = bottomTabY + RecipeBookTabButton.HEIGHT;
            if (isIn(mx, my, arrowX, downArrowY, TAB_SCROLL_ARROW_W, TAB_SCROLL_ARROW_H) && canScrollTabsDown()) {
                scrollTabs(1);
                playUiClickSound();
                return true;
            }
        }
        return false;
    }

    private int getVisibleTabCount() {
        return Math.min(VISIBLE_TAB_SLOTS, tabs.size());
    }

    private boolean hasScrollableTabs() {
        return tabs.size() > VISIBLE_TAB_SLOTS;
    }

    private boolean canScrollTabsUp() {
        return tabScrollOffset > 0;
    }

    private boolean canScrollTabsDown() {
        return tabScrollOffset + getVisibleTabCount() < tabs.size();
    }

    private void scrollTabs(int direction) {
        int newOffset = tabScrollOffset + direction;
        int maxOffset = Math.max(0, tabs.size() - getVisibleTabCount());
        newOffset = Math.clamp(newOffset, 0, maxOffset);
        if (newOffset == tabScrollOffset) return;

        tabScrollOffset = newOffset;
        clampSelectedTabToVisibleWindow();
        page = 0;
    }

    private void clampSelectedTabToVisibleWindow() {
        int selectedIndex = indexOfSelectedTab();
        if (selectedIndex < 0) {
            setSelectedCategoryId(null);
            selectedIndex = 0;
        }

        int visibleCount = getVisibleTabCount();
        int minVisible = tabScrollOffset;
        int maxVisible = tabScrollOffset + visibleCount - 1;

        if (selectedIndex < minVisible) {
            setSelectedCategoryId(tabs.get(minVisible).categoryId());
            return;
        }
        if (selectedIndex > maxVisible) {
            setSelectedCategoryId(tabs.get(maxVisible).categoryId());
        }
    }

    private int indexOfSelectedTab() {
        if (selectedCategoryId == null) return 0;

        for (int i = 0; i < tabs.size(); i++) {
            if (selectedCategoryId.equals(tabs.get(i).categoryId())) {
                return i;
            }
        }
        return -1;
    }

    private List<TabDescriptor> buildTabs(List<BannerRecipeCategory> categories) {
        Set<String> nonEmptyCategoryIds = new LinkedHashSet<>();
        for (BannerRecipe banner : BannerStorage.getInstance().getBanners()) {
            String categoryId = banner.getCategory();
            if (categoryId != null && !categoryId.isBlank()) {
                nonEmptyCategoryIds.add(categoryId.toLowerCase(Locale.ROOT));
            }
        }

        List<TabDescriptor> out = new ArrayList<>();
        out.add(new TabDescriptor(null, ALL_CATEGORIES_TOOLTIP, null));
        for (BannerRecipeCategory category : categories) {
            if (nonEmptyCategoryIds.contains(category.id().toLowerCase(Locale.ROOT))) {
                out.add(new TabDescriptor(
                        category.id(),
                        Component.literal(BannerRecipeCategories.getLocalizedDescription(category.id())),
                        category));
            }
        }
        return List.copyOf(out);
    }

    private void refreshVisibleTabs() {
        tabs = buildTabs(this.categoryTabs);

        if (selectedCategoryId != null) {
            boolean selectedStillVisible = false;
            for (TabDescriptor tab : tabs) {
                if (selectedCategoryId.equals(tab.categoryId())) {
                    selectedStillVisible = true;
                    break;
                }
            }
            if (!selectedStillVisible) {
                setSelectedCategoryId(null);
                page = 0;
            }
        }

        int maxOffset = Math.max(0, tabs.size() - getVisibleTabCount());
        if (tabScrollOffset > maxOffset) {
            tabScrollOffset = maxOffset;
        }
    }

    private record TabDescriptor(String categoryId, Component tooltip, BannerRecipeCategory category) {}

    private void setSelectedCategoryId(String categoryId) {
        this.selectedCategoryId = categoryId;
        manager.setSelectedCategoryId(categoryId);
    }

    private boolean clickBannerGrid(int mx, int my) {
        List<BannerRecipe> items = getFilteredBanners();
        int start = page * GRID_COLUMNS * GRID_ROWS;
        int end = Math.min(items.size(), start + GRID_COLUMNS * GRID_ROWS);
        boolean isShiftPressed =
                InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                        || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);

        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % GRID_COLUMNS;
            int row = local / GRID_COLUMNS;
            int bx = x + GRID_START_X + col * GRID_CELL;
            int by = y + GRID_START_Y + row * GRID_CELL;
            if (isIn(mx, my, bx, by, 16, 16)) {
                BannerRecipe banner = items.get(i);
                manager.setActiveBannerFromRecipe(banner, banner.getId());
                playUiClickSound();
                if (isShiftPressed) {
                    craftSelectedBanner();
                } else {
                    searchBox.setFocused(true);
                }
                return true;
            }
        }
        return false;
    }

    private void playUiClickSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() == null) return;

        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public boolean keyPressed(KeyEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (searchBox.canConsumeInput() && mc != null && mc.options != null && mc.options.keyInventory.matches(event)) {
            return true;
        }

        if (searchBox.keyPressed(event)) {
            page = 0;
            return true;
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        if (searchBox.charTyped(event)) {
            page = 0;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return searchBox.mouseDragged(event, dx, dy);
    }

    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (hasScrollableTabs() && isMouseOverTabs((int) mx, (int) my)) {
            if (vAmt > 0 && canScrollTabsUp()) {
                scrollTabs(-1);
                playUiClickSound();
                return true;
            }
            if (vAmt < 0 && canScrollTabsDown()) {
                scrollTabs(1);
                playUiClickSound();
                return true;
            }
        }
        return false;
    }

    public void tick() {
        manager.tick();
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        this.searchBox.setPosition(x + SEARCH_X, y + SEARCH_Y);
        this.pageForwardButton.setPosition(x + 93, y + PAGE_BTN_Y_OFFSET);
        this.pageBackButton.setPosition(x + 38, y + PAGE_BTN_Y_OFFSET);
    }

    // -------------------------------------------------------------------------
    // Data helpers
    // -------------------------------------------------------------------------

    private List<BannerRecipe> getFilteredBanners() {
        String query = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<BannerRecipe> out = new ArrayList<>();
        for (BannerRecipe banner : BannerStorage.getInstance().getBanners()) {
            if (selectedCategoryId != null && !selectedCategoryId.equalsIgnoreCase(banner.getCategory())) continue;

            if (!query.isEmpty()
                    && !banner.getDisplayName().toLowerCase(Locale.ROOT).contains(query)) continue;

            if (craftableOnly && !isCraftableNow(banner)) continue;

            out.add(banner);
        }
        return out;
    }

    private boolean isCraftableNow(BannerRecipe banner) {
        return Weaver.getWeaver(handler).canWeave(banner);
    }

    /**
     * Checks the loom's banner slot against the active recipe.
     * Returns the index of the NEXT layer to craft (0 = blank banner is in, start first layer),
     * or -1 if the slot is empty, wrong color, or layers don't match the recipe so far.
     */
    public int getActiveBannerLayerCount() {
        return manager.getActiveBannerLayerCount();
    }

    public int detectCraftingProgress() {
        return manager.detectCraftingProgress();
    }

    private BannerRecipe getSelectedBanner() {
        return manager.getActiveBannerRecipe();
    }

    public ItemStack getActiveBannerStack() {
        return manager.getActiveBannerStack();
    }

    public BannerRecipe getActiveBannerRecipe() {
        return manager.getActiveBannerRecipe();
    }

    public void setActiveBannerTooltip(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        BannerRecipe selectedBanner = getSelectedBanner();
        if (selectedBanner == null) return;

        // currentRowIndex: row 0 = base banner (always "done" when banner is in slot)
        // row n+1 = layer n; nextLayerIndex = progress -> currentRowIndex = progress + 1
        int progress = detectCraftingProgress();
        int currentRowIndex = progress >= 0 ? progress + 1 : -1;
        setBannerTooltip(ctx, manager.getActiveBannerDisplayName(), selectedBanner, currentRowIndex, mouseX, mouseY);
    }

    public static void setBannerTooltip(GuiGraphicsExtractor ctx, BannerRecipe banner, int mouseX, int mouseY) {
        setBannerTooltip(ctx, banner.getDisplayName(), banner, -1, mouseX, mouseY);
    }

    public static void setBannerTooltip(
            GuiGraphicsExtractor ctx, String title, BannerRecipe banner, int currentRowIndex, int mouseX, int mouseY) {
        Optional<TooltipComponent> image = buildTooltipImage(banner, currentRowIndex);
        ctx.setTooltipForNextFrame(
                Minecraft.getInstance().font, List.of(Component.literal(title)), image, mouseX, mouseY);
    }

    public void craftSelectedBanner() {
        manager.craftActiveBanner();
    }

    public boolean isActiveBannerCraftable() {
        return manager.isActiveBannerCraftable();
    }

    public String getActiveBannerMissingMaterialMessage() {
        return manager.getActiveBannerMissingMaterialMessage();
    }

    public boolean isActiveBannerWeavable() {
        return manager.isActiveBannerWeavable();
    }

    public void clearSelectedBanner() {
        manager.clearActiveBanner();
    }

    public boolean setActiveBannerFromItemStack(ItemStack stack) {
        return manager.setActiveBannerFromItemStack(stack);
    }

    public boolean hasActiveBanner() {
        return manager.hasActiveBanner();
    }

    public boolean isActiveBannerSavable() {
        return manager.isActiveBannerSavable();
    }

    public boolean isActiveBannerAlreadySaved() {
        return manager.isActiveBannerAlreadySaved();
    }

    public boolean isActiveBannerFromReadOnlySource() {
        return manager.isActiveBannerFromReadOnlySource();
    }

    public String getActiveBannerDialogName(boolean editMode) {
        return manager.getActiveBannerDialogName(editMode);
    }

    public String getActiveBannerDialogCategory(boolean editMode) {
        return manager.getActiveBannerDialogCategory(editMode);
    }

    public void applyActiveBannerMetadata(String nameInput, String categoryInput) {
        manager.applyActiveBannerMetadata(nameInput, categoryInput);
    }

    public boolean saveActiveBanner() {
        if (!manager.hasActiveBanner() || !manager.isActiveBannerSavable()) return false;
        manager.applyActiveBannerMetadata(manager.getActiveBannerDisplayName(), getActiveBannerDialogCategory(false));
        return manager.isActiveBannerAlreadySaved();
    }

    public void loadImportedBanner(BannerRecipe imported) {
        manager.loadImportedBanner(imported);
    }

    public boolean isPersistentDyeSwitchEnabled() {
        return manager.isPersistentDyeSwitchEnabled();
    }

    public Map<DyeColor, DyeColor> getPersistentDyeReplacementMapCopy() {
        return manager.getPersistentDyeReplacementMapCopy();
    }

    public void restorePersistentDyeSwitchState(boolean enabled, Map<DyeColor, DyeColor> replacements) {
        if (!enabled) {
            manager.disablePersistentDyeSwitchAndReload();
            return;
        }
        manager.applyDyeSwitch(replacements, true);
    }

    public List<DyeColor> getActiveBannerUsedColors() {
        return manager.getActiveBannerUsedColors();
    }

    public Map<DyeColor, DyeColor> getInitialDyeReplacementTargets(List<DyeColor> sourceColors) {
        return manager.getInitialDyeReplacementTargets(sourceColors);
    }

    public boolean applyDyeSwitch(Map<DyeColor, DyeColor> replacements, boolean persistent) {
        return manager.applyDyeSwitch(replacements, persistent);
    }

    public void disablePersistentDyeSwitchAndReload() {
        manager.disablePersistentDyeSwitchAndReload();
    }

    private static String getPatternDisplayName(BannerRecipeLayer layer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            try {
                Optional<Registry<BannerPattern>> regOpt =
                        mc.level.registryAccess().lookup(Registries.BANNER_PATTERN);
                if (regOpt.isPresent()) {
                    Identifier id = Identifier.tryParse(layer.patternId());
                    if (id != null) {
                        Optional<Holder.Reference<BannerPattern>> entry =
                                regOpt.get().get(id);
                        if (entry.isPresent()) {
                            BannerPattern pattern = entry.get().value();
                            String key = pattern.translationKey() + "."
                                    + layer.getDyeColorEnum().getName();
                            return Component.translatable(key).getString();
                        }
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }
        return toTitle(layer.getDyeColorEnum().getSerializedName()) + " " + toTitle(layer.patternId());
    }

    private static String toTitle(String id) {
        String raw = id.contains(":") ? id.split(":", 2)[1] : id;
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split("_")) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return sb.toString();
    }

    private static boolean isIn(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private boolean isMouseOverTabs(int mx, int my) {
        int tx = x - TAB_X_OFFSET;
        int ty = y + TAB_Y_START;
        int h = getVisibleTabCount() * RecipeBookTabButton.HEIGHT;
        return isIn(mx, my, tx - 2, ty, RecipeBookTabButton.WIDTH, h);
    }

    public static boolean saveBannerFromOutput(LoomMenu handler) {
        ItemStack dyeStack = handler.getSlot(1).getItem();
        ItemStack patternStack = handler.getSlot(2).getItem();
        if (dyeStack.isEmpty() && patternStack.isEmpty()) {
            ItemStack bannerStack = handler.getSlot(0).getItem();
            if (bannerStack.isEmpty()) return false;
            BannerRecipe banner = BannerRecipeItemConverter.fromItem(bannerStack);
            if (banner != null) {
                BannerStorage.getInstance().addBanner(banner);
                return true;
            }
        } else {
            ItemStack outputStack = handler.getSlot(3).getItem();
            if (outputStack.isEmpty()) return false;
            BannerRecipe banner = BannerRecipeItemConverter.fromItem(outputStack);
            if (banner != null) {
                BannerStorage.getInstance().addBanner(banner);
                return true;
            }
        }
        return false;
    }
}
