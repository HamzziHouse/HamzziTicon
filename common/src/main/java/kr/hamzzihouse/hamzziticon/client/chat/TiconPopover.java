package kr.hamzzihouse.hamzziticon.client.chat;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import kr.hamzzihouse.hamzziticon.catalog.Ticon;
import kr.hamzzihouse.hamzziticon.catalog.TiconCatalog;
import kr.hamzzihouse.hamzziticon.catalog.TiconCategory;
import kr.hamzzihouse.hamzziticon.history.FavoriteTicons;
import kr.hamzzihouse.hamzziticon.history.RecentTicons;
import kr.hamzzihouse.hamzziticon.search.SearchResult;
import kr.hamzzihouse.hamzziticon.session.CatalogState;
import kr.hamzzihouse.hamzziticon.session.TiconSession;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public final class TiconPopover extends AbstractWidget {

    public static final int COLUMNS = 10;
    public static final int ROWS = 5;
    public static final int SLOT = 18;
    public static final int PADDING = 6;
    public static final int TAB_WIDTH = 16;
    public static final int TAB_HEIGHT = 18;
    public static final int TAB_ICON = 12;
    public static final int FAVORITE_BADGE = 7;
    public static final int UNSELECTED_TAB_DROP = 3;
    public static final int FOOTER_HEIGHT = 12;
    public static final int SCROLLBAR_WIDTH = 14;
    public static final int SCROLLER_WIDTH = 12;
    public static final int SCROLLER_HEIGHT = 15;
    public static final int WIDTH = PADDING * 2 + COLUMNS * SLOT + 2 + SCROLLBAR_WIDTH;
    public static final int HEIGHT = PADDING + TAB_HEIGHT + 2 + ROWS * SLOT + 2 + FOOTER_HEIGHT + PADDING;
    public static final int TARGET_GUI_SCALE = 4;

    private static final double SCROLL_RATE = SLOT / 2.0;
    private static final float GLYPH_SCALE = 1.6F;
    private static final float ITEM_ICON_SCALE = TAB_ICON / 16F;
    private static final int SEARCH_LIMIT = COLUMNS * ROWS * 4;
    private static final String ALL_TAB_ID = "$all";
    private static final String FAVORITE_TAB_ID = "$favorite";
    private static final String RECENT_TAB_ID = "$recent";
    private static final ItemStack ALL_TAB_ICON = new ItemStack(Items.CHEST);
    private static final ItemStack FAVORITE_TAB_ICON = new ItemStack(Items.NETHER_STAR);
    private static final ItemStack RECENT_TAB_ICON = new ItemStack(Items.CLOCK);

    private final BiConsumer<Ticon, Boolean> onChoose;

    @Getter
    private final int zoom;

    private String activeTabId = ALL_TAB_ID;
    private String query = "";
    private double scrollAmount;
    private List<Ticon> visible = List.of();
    private Style glyphStyle = Style.EMPTY;
    private boolean pointer;
    private boolean draggingScroller;
    private boolean scrollerHovered;
    private int tooltipX;
    private int tooltipY;

    @Getter
    private @Nullable Ticon hovered;

    @Setter
    private boolean open;

    public TiconPopover(int x, int y, int zoom, @NotNull BiConsumer<Ticon, Boolean> onChoose) {
        super(x, y, WIDTH * zoom, HEIGHT * zoom, Component.translatable("screen.hamzziticon.picker"));
        this.zoom = zoom;
        this.onChoose = onChoose;
        rebuild();
    }

    public static int zoomFor(int availableWidth, int availableHeight) {
        int zoom = Math.max(1, TARGET_GUI_SCALE / Minecraft.getInstance().getWindow().getGuiScale());
        while (zoom > 1 && (WIDTH * zoom > availableWidth || HEIGHT * zoom > availableHeight)) {
            zoom--;
        }
        return zoom;
    }

    private int localX(int mouseX) {
        return getX() + Math.floorDiv(mouseX - getX(), zoom);
    }

    private int localY(int mouseY) {
        return getY() + Math.floorDiv(mouseY - getY(), zoom);
    }

    private double localX(double mouseX) {
        return getX() + (mouseX - getX()) / zoom;
    }

    private double localY(double mouseY) {
        return getY() + (mouseY - getY()) / zoom;
    }

    public boolean isOpen() {
        return open;
    }

    public void setQuery(@NotNull String next) {
        if (next.equals(query)) {
            return;
        }
        query = next;
        scrollAmount = 0;
        rebuild();
    }

    @NotNull
    public Optional<Ticon> firstVisible() {
        return visible.stream().findFirst();
    }

    public boolean hasQuery() {
        return !query.isBlank();
    }

    public void rebuild() {
        TiconCatalog catalog = TiconSession.getInstance().catalog();
        glyphStyle = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.parse(catalog.font())));
        visible = resolveVisible(catalog);
        setScrollAmount(scrollAmount);
    }

    @NotNull
    private List<Ticon> resolveVisible(@NotNull TiconCatalog catalog) {
        if (hasQuery()) {
            List<SearchResult> results = TiconSession.getInstance().search(query, SEARCH_LIMIT);
            List<Ticon> ticons = new ArrayList<>(results.size());
            for (SearchResult result : results) {
                ticons.add(result.ticon());
            }
            return List.copyOf(ticons);
        }
        return switch (activeTabId) {
            case ALL_TAB_ID -> catalog.ticons();
            case FAVORITE_TAB_ID -> resolveIds(catalog, FavoriteTicons.getInstance().ids());
            case RECENT_TAB_ID -> resolveIds(catalog, RecentTicons.getInstance().ids());
            default -> catalog.ticonsIn(activeTabId);
        };
    }

    @NotNull
    private List<Ticon> resolveIds(@NotNull TiconCatalog catalog, @NotNull List<String> ids) {
        List<Ticon> ticons = new ArrayList<>(ids.size());
        for (String id : ids) {
            catalog.find(id).ifPresent(ticons::add);
        }
        return List.copyOf(ticons);
    }

    private int contentHeight() {
        return Math.max(1, (visible.size() + COLUMNS - 1) / COLUMNS) * SLOT;
    }

    private int viewportHeight() {
        return ROWS * SLOT;
    }

    private int maxScrollAmount() {
        return Math.max(0, contentHeight() - viewportHeight());
    }

    private void setScrollAmount(double amount) {
        scrollAmount = Mth.clamp(amount, 0.0, maxScrollAmount());
    }

    private int gridLeft() {
        return getX() + PADDING;
    }

    private int gridTop() {
        return getY() + PADDING + TAB_HEIGHT + 2;
    }

    private int trackLeft() {
        return gridLeft() + COLUMNS * SLOT + 2;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!open) {
            hovered = null;
            return;
        }
        pointer = false;
        tooltipX = mouseX;
        tooltipY = mouseY;
        int localX = localX(mouseX);
        int localY = localY(mouseY);
        Font font = Minecraft.getInstance().font;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(getX(), getY());
        pose.scale(zoom, zoom);
        pose.translate(-getX(), -getY());
        renderPanel(graphics);
        renderTabs(graphics, font, localX, localY);

        switch (TiconSession.getInstance().state()) {
            case CatalogState.Ready ignored -> renderGrid(graphics, localX, localY);
            case CatalogState.Syncing syncing -> renderNotice(graphics, font, syncing.timedOut()
                    ? Component.translatable("screen.hamzziticon.notice.timeout")
                    : Component.translatable("screen.hamzziticon.notice.syncing"), TiconSprites.TEXT_MUTED);
            case CatalogState.Unsupported ignored -> renderNotice(graphics, font,
                    Component.translatable("screen.hamzziticon.notice.unsupported"), TiconSprites.TEXT_MUTED);
            case CatalogState.Failed failed -> renderNotice(graphics, font,
                    Component.literal(failed.reason()), TiconSprites.TEXT_LOCKED);
        }
        renderScrollbar(graphics, localX, localY);
        renderFooter(graphics, font);
        pose.popMatrix();

        if (draggingScroller) {
            graphics.requestCursor(CursorTypes.RESIZE_NS);
        } else if (pointer || scrollerHovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private void renderPanel(@NotNull GuiGraphics graphics) {
        int left = getX();
        int top = getY();
        int right = left + WIDTH;
        int bottom = top + HEIGHT;
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, TiconSprites.PANEL_OUTLINE);
        graphics.fill(left, top, right, bottom, TiconSprites.PANEL_FILL);
        graphics.fill(left, top, right - 1, top + 1, TiconSprites.PANEL_LIGHT);
        graphics.fill(left, top, left + 1, bottom - 1, TiconSprites.PANEL_LIGHT);
        graphics.fill(left + 1, bottom - 1, right, bottom, TiconSprites.PANEL_DARK);
        graphics.fill(right - 1, top + 1, right, bottom, TiconSprites.PANEL_DARK);
    }

    private void renderTabs(@NotNull GuiGraphics graphics, @NotNull Font font, int mouseX, int mouseY) {
        int x = gridLeft();
        int y = getY() + PADDING;
        if (hasQuery()) {
            Component label = Component.translatable("screen.hamzziticon.searching", query);
            graphics.drawString(font, label, x, y + (TAB_HEIGHT - font.lineHeight) / 2, TiconSprites.TEXT_DARK, false);
            return;
        }
        TiconCatalog catalog = TiconSession.getInstance().catalog();
        drawTab(graphics, x, y, mouseX, mouseY, ALL_TAB_ID, null, ALL_TAB_ICON);
        x += TAB_WIDTH;
        drawTab(graphics, x, y, mouseX, mouseY, FAVORITE_TAB_ID, null, FAVORITE_TAB_ICON);
        x += TAB_WIDTH;
        drawTab(graphics, x, y, mouseX, mouseY, RECENT_TAB_ID, null, RECENT_TAB_ICON);
        x += TAB_WIDTH;
        for (TiconCategory category : catalog.categories()) {
            if (x + TAB_WIDTH > gridLeft() + COLUMNS * SLOT) {
                break;
            }
            drawTab(graphics, x, y, mouseX, mouseY, category.id(), catalog.iconOf(category).orElse(null), null);
            x += TAB_WIDTH;
        }
    }

    private void drawTab(@NotNull GuiGraphics graphics, int x, int y, int mouseX, int mouseY,
                         @NotNull String tabId, @Nullable Ticon icon, @Nullable ItemStack item) {
        boolean active = tabId.equals(activeTabId);
        boolean hover = mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + TAB_HEIGHT;
        if (hover) {
            graphics.setTooltipForNextFrame(tabLabel(tabId), tooltipX, tooltipY);
            if (!active) {
                pointer = true;
            }
        }
        Identifier sprite = active ? TiconSprites.TAB_SELECTED : hover ? TiconSprites.TAB_HIGHLIGHTED : TiconSprites.TAB;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, TAB_WIDTH, TAB_HEIGHT);

        int drop = active ? 0 : UNSELECTED_TAB_DROP;
        int iconX = x + (TAB_WIDTH - TAB_ICON) / 2;
        int iconY = y + (TAB_HEIGHT - TAB_ICON) / 2 + drop;

        if (item != null) {
            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(iconX, iconY);
            pose.scale(ITEM_ICON_SCALE, ITEM_ICON_SCALE);
            graphics.renderItem(item, 0, 0);
            pose.popMatrix();
            return;
        }
        if (icon == null) {
            return;
        }
        Identifier texture = TiconSprites.textureOf(icon);
        if (texture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0F, 0F, TAB_ICON, TAB_ICON, 1, 1, 1, 1);
            return;
        }
        Font font = Minecraft.getInstance().font;
        Component label = Component.literal(icon.character()).withStyle(glyphStyle);
        graphics.drawString(font, label, x + (TAB_WIDTH - font.width(label)) / 2,
                y + (TAB_HEIGHT - font.lineHeight) / 2 + drop, 0xFFFFFFFF, false);
    }

    @NotNull
    private Component tabLabel(@NotNull String tabId) {
        return switch (tabId) {
            case ALL_TAB_ID -> Component.translatable("screen.hamzziticon.tab.all");
            case FAVORITE_TAB_ID -> Component.translatable("screen.hamzziticon.tab.favorite");
            case RECENT_TAB_ID -> Component.translatable("screen.hamzziticon.tab.recent");
            default -> TiconSession.getInstance().catalog().categories().stream()
                    .filter(category -> category.id().equals(tabId))
                    .findFirst()
                    .map(category -> Component.literal(category.displayName()))
                    .orElseGet(() -> Component.literal(tabId));
        };
    }

    private void renderGrid(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        hovered = null;
        int left = gridLeft();
        int top = gridTop();
        int right = left + COLUMNS * SLOT;
        int bottom = top + viewportHeight();
        boolean insideViewport = mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;

        int offset = (int) Math.round(scrollAmount);
        int firstRow = offset / SLOT;
        int rowShift = offset % SLOT;
        int totalRows = (visible.size() + COLUMNS - 1) / COLUMNS;

        graphics.enableScissor(left, top, right, bottom);
        for (int row = firstRow; row <= firstRow + ROWS && row < Math.max(totalRows, ROWS); row++) {
            int cellY = top + (row - firstRow) * SLOT - rowShift;
            for (int column = 0; column < COLUMNS; column++) {
                int cellX = left + column * SLOT;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TiconSprites.SLOT, cellX, cellY, SLOT, SLOT);

                int index = row * COLUMNS + column;
                if (index >= visible.size()) {
                    continue;
                }
                Ticon ticon = visible.get(index);
                boolean hover = insideViewport
                        && mouseX >= cellX && mouseX < cellX + SLOT && mouseY >= cellY && mouseY < cellY + SLOT;
                if (hover) {
                    hovered = ticon;
                    if (!ticon.locked()) {
                        pointer = true;
                    }
                    graphics.fill(cellX + 1, cellY + 1, cellX + SLOT - 1, cellY + SLOT - 1, TiconSprites.SLOT_HOVER);
                }
                drawGlyph(graphics, ticon, cellX + 1, cellY + 1);
                if (FavoriteTicons.getInstance().contains(ticon.id())) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, TiconSprites.FAVORITE_STAR,
                            cellX + SLOT - FAVORITE_BADGE - 1, cellY + 1, 0F, 0F,
                            FAVORITE_BADGE, FAVORITE_BADGE, 1, 1, 1, 1);
                }
                if (ticon.locked()) {
                    graphics.fill(cellX + 1, cellY + 1, cellX + SLOT - 1, cellY + SLOT - 1, TiconSprites.SLOT_LOCKED);
                }
            }
        }
        graphics.disableScissor();

        if (hovered != null) {
            graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, hoverTooltip(hovered), tooltipX, tooltipY);
        }
    }

    @NotNull
    private List<Component> hoverTooltip(@NotNull Ticon ticon) {
        boolean favorite = FavoriteTicons.getInstance().contains(ticon.id());
        return List.of(
                Component.literal(ticon.displayName())
                        .append(Component.literal("  " + ticon.shortcode())
                                .withStyle(ticon.locked() ? ChatFormatting.RED : ChatFormatting.GRAY)),
                Component.translatable(favorite
                                ? "screen.hamzziticon.hint.unfavorite"
                                : "screen.hamzziticon.hint.favorite")
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    private void drawGlyph(@NotNull GuiGraphics graphics, @NotNull Ticon ticon, int x, int y) {
        int inner = SLOT - 2;
        Identifier texture = TiconSprites.textureOf(ticon);
        if (texture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0F, 0F, inner, inner, 1, 1, 1, 1);
            return;
        }
        Font font = Minecraft.getInstance().font;
        Component glyph = Component.literal(ticon.character()).withStyle(glyphStyle);
        float glyphWidth = font.width(glyph) * GLYPH_SCALE;
        float glyphHeight = font.lineHeight * GLYPH_SCALE;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x + (inner - glyphWidth) / 2F, y + (inner - glyphHeight) / 2F + 1F);
        pose.scale(GLYPH_SCALE, GLYPH_SCALE);
        graphics.drawString(font, glyph, 0, 0, 0xFFFFFFFF, false);
        pose.popMatrix();
    }

    private int scrollerTop() {
        int travel = viewportHeight() - 2 - SCROLLER_HEIGHT;
        int max = maxScrollAmount();
        return gridTop() + 1 + (max <= 0 ? 0 : (int) Math.round(travel * scrollAmount / max));
    }

    private void renderScrollbar(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        int left = trackLeft();
        int top = gridTop();
        int right = left + SCROLLBAR_WIDTH;
        int bottom = top + viewportHeight();
        graphics.fill(left, top, right, bottom, TiconSprites.TRACK_FILL);
        graphics.fill(left, top, right, top + 1, TiconSprites.TRACK_DARK);
        graphics.fill(left, top, left + 1, bottom, TiconSprites.TRACK_DARK);
        graphics.fill(left, bottom - 1, right, bottom, TiconSprites.PANEL_LIGHT);
        graphics.fill(right - 1, top, right, bottom, TiconSprites.PANEL_LIGHT);

        boolean scrollable = maxScrollAmount() > 0;
        int thumbTop = scrollerTop();
        scrollerHovered = scrollable
                && mouseX >= left + 1 && mouseX < left + 1 + SCROLLER_WIDTH
                && mouseY >= thumbTop && mouseY < thumbTop + SCROLLER_HEIGHT;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                scrollable ? TiconSprites.CREATIVE_SCROLLER : TiconSprites.CREATIVE_SCROLLER_DISABLED,
                left + 1, thumbTop, SCROLLER_WIDTH, SCROLLER_HEIGHT);
    }

    private boolean inTrack(double mouseX, double mouseY) {
        return mouseX >= trackLeft() && mouseX < trackLeft() + SCROLLBAR_WIDTH
                && mouseY >= gridTop() && mouseY < gridTop() + viewportHeight();
    }

    private void scrollTo(double mouseY) {
        int max = maxScrollAmount();
        if (max <= 0) {
            return;
        }
        double travel = viewportHeight() - 2 - SCROLLER_HEIGHT;
        double offset = mouseY - gridTop() - 1 - SCROLLER_HEIGHT / 2.0;
        setScrollAmount(Mth.clamp(offset / travel, 0.0, 1.0) * max);
    }

    private void renderNotice(@NotNull GuiGraphics graphics, @NotNull Font font, @NotNull Component message, int color) {
        int centerX = gridLeft() + COLUMNS * SLOT / 2;
        int centerY = gridTop() + viewportHeight() / 2 - font.lineHeight / 2;
        graphics.drawCenteredString(font, message, centerX, centerY, color);
    }

    private void renderFooter(@NotNull GuiGraphics graphics, @NotNull Font font) {
        int y = gridTop() + viewportHeight() + 2 + (FOOTER_HEIGHT - font.lineHeight) / 2;
        Component count = Component.translatable("screen.hamzziticon.footer.count", visible.size());
        graphics.drawString(font, count, gridLeft(), y, TiconSprites.TEXT_MUTED, false);
    }

    public boolean contains(double mouseX, double mouseY) {
        return open && mouseX >= getX() && mouseX < getX() + getWidth() && mouseY >= getY() && mouseY < getY() + getHeight();
    }

    public boolean handleClick(@NotNull MouseButtonEvent event) {
        if (!contains(event.x(), event.y())) {
            return false;
        }
        if (event.button() == 1) {
            if (hovered != null) {
                boolean added = FavoriteTicons.getInstance().toggle(hovered.id());
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                        added ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.ITEM_PICKUP, 1.0F));
                if (activeTabId.equals(FAVORITE_TAB_ID)) {
                    rebuild();
                }
            }
            return true;
        }
        if (event.button() != 0) {
            return true;
        }
        double x = localX(event.x());
        double y = localY(event.y());
        if (inTrack(x, y)) {
            draggingScroller = maxScrollAmount() > 0;
            scrollTo(y);
            return true;
        }
        if (!hasQuery() && handleTabClick(x, y)) {
            return true;
        }
        if (hovered != null && !hovered.locked()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.0F));
            onChoose.accept(hovered, event.hasShiftDown());
        }
        return true;
    }

    private boolean handleTabClick(double mouseX, double mouseY) {
        int y = getY() + PADDING;
        if (mouseY < y || mouseY >= y + TAB_HEIGHT) {
            return false;
        }
        int relative = (int) (mouseX - gridLeft());
        if (relative < 0) {
            return false;
        }
        int slot = relative / TAB_WIDTH;
        switch (slot) {
            case 0 -> selectTab(ALL_TAB_ID);
            case 1 -> selectTab(FAVORITE_TAB_ID);
            case 2 -> selectTab(RECENT_TAB_ID);
            default -> {
                List<TiconCategory> categories = TiconSession.getInstance().catalog().categories();
                if (slot - 3 >= categories.size()) {
                    return false;
                }
                selectTab(categories.get(slot - 3).id());
            }
        }
        return true;
    }

    private void selectTab(@NotNull String tabId) {
        if (tabId.equals(activeTabId)) {
            return;
        }
        activeTabId = tabId;
        scrollAmount = 0;
        rebuild();
        playDownSound(Minecraft.getInstance().getSoundManager());
    }

    public boolean handleScroll(double mouseX, double mouseY, double scrollY) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        setScrollAmount(scrollAmount - scrollY * SCROLL_RATE);
        return true;
    }

    public boolean handleDrag(double mouseX, double mouseY) {
        if (!draggingScroller) {
            return false;
        }
        scrollTo(localY(mouseY));
        return true;
    }

    public void handleRelease() {
        draggingScroller = false;
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
    }
}