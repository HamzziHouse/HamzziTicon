package kr.hamzzihouse.hamzziticon.client.chat;

import kr.hamzzihouse.hamzziticon.catalog.Ticon;
import kr.hamzzihouse.hamzziticon.catalog.TiconCatalog;
import kr.hamzzihouse.hamzziticon.client.TiconChatIntegration;
import kr.hamzzihouse.hamzziticon.client.TiconKeys;
import kr.hamzzihouse.hamzziticon.history.FavoriteTicons;
import kr.hamzzihouse.hamzziticon.history.RecentTicons;
import kr.hamzzihouse.hamzziticon.session.CatalogState;
import kr.hamzzihouse.hamzziticon.session.TiconSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.function.Consumer;

public final class TiconChatOverlay {

    private static final int BUTTON_GAP = 4;
    private static @Nullable TiconChatOverlay current;

    private final Screen screen;
    private final EditBox input;
    private final TiconToggleButton button;
    private final TiconPopover popover;
    private final Consumer<CatalogState> stateListener;
    private Style glyphStyle = Style.EMPTY;

    private TiconChatOverlay(@NotNull Screen screen, @NotNull EditBox input) {
        this.screen = screen;
        this.input = input;

        int buttonX = screen.width - BUTTON_GAP - TiconToggleButton.WIDTH;
        int buttonY = screen.height - 14 + (12 - TiconToggleButton.HEIGHT) / 2;
        input.setWidth(input.getWidth() - TiconToggleButton.WIDTH - BUTTON_GAP * 2);

        this.button = new TiconToggleButton(buttonX, buttonY, this::toggle);
        int zoom = TiconPopover.zoomFor(screen.width - 4, screen.height - 14 - 4);
        this.popover = new TiconPopover(
                screen.width - 2 - TiconPopover.WIDTH * zoom,
                screen.height - 14 - 2 - TiconPopover.HEIGHT * zoom,
                zoom,
                this::choose);
        this.stateListener = state -> Minecraft.getInstance().execute(this::refresh);
        input.addFormatter(this::formatInput);
        refresh();
    }

    private void refresh() {
        glyphStyle = Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.parse(TiconSession.getInstance().catalog().font())));
        popover.rebuild();
    }

    @Nullable
    private FormattedCharSequence formatInput(@NotNull String text, int offset) {
        TiconCatalog catalog = TiconSession.getInstance().catalog();
        if (catalog.isEmpty() || text.indexOf(':') < 0) {
            return null;
        }
        Matcher matcher = TiconChatIntegration.SHORTCODE_PATTERN.matcher(text);
        List<FormattedCharSequence> parts = new ArrayList<>();
        int last = 0;
        while (matcher.find()) {
            Optional<Ticon> ticon = catalog.find(matcher.group(1));
            if (ticon.isEmpty() || ticon.get().locked()) {
                continue;
            }
            if (matcher.start() > last) {
                parts.add(FormattedCharSequence.forward(text.substring(last, matcher.start()), Style.EMPTY));
            }
            parts.add(FormattedCharSequence.forward(ticon.get().character(), glyphStyle));
            last = matcher.end();
        }
        if (last == 0) {
            return null;
        }
        if (last < text.length()) {
            parts.add(FormattedCharSequence.forward(text.substring(last), Style.EMPTY));
        }
        return FormattedCharSequence.composite(parts);
    }

    @NotNull
    public static Optional<TiconChatOverlay> attach(@NotNull Screen screen, @NotNull Consumer<AbstractWidget> addWidget) {
        Optional<EditBox> input = TiconChatIntegration.chatInput(screen);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        detach(screen);
        TiconChatOverlay overlay = new TiconChatOverlay(screen, input.get());
        addWidget.accept(overlay.popover);
        addWidget.accept(overlay.button);
        TiconSession.getInstance().addListener(overlay.stateListener);
        current = overlay;
        return Optional.of(overlay);
    }

    public static void detach(@NotNull Screen screen) {
        TiconChatOverlay overlay = current;
        if (overlay == null || overlay.screen != screen) {
            return;
        }
        TiconSession.getInstance().removeListener(overlay.stateListener);
        RecentTicons.getInstance().flush();
        FavoriteTicons.getInstance().flush();
        current = null;
    }

    @NotNull
    public static Optional<TiconChatOverlay> current() {
        return Optional.ofNullable(current);
    }

    public void toggle() {
        setOpen(!popover.isOpen());
    }

    public void setOpen(boolean open) {
        popover.setOpen(open);
        if (open) {
            popover.rebuild();
        } else {
            RecentTicons.getInstance().flush();
            FavoriteTicons.getInstance().flush();
        }
    }

    public void tick() {
        if (!popover.isOpen()) {
            return;
        }
        popover.setQuery(TiconChatIntegration.pendingShortcode(input.getValue(), input.getCursorPosition()).orElse(""));
    }

    public boolean handleMouseClick(@NotNull MouseButtonEvent event) {
        if (!popover.isOpen()) {
            return false;
        }
        tick();
        if (popover.handleClick(event)) {
            return true;
        }
        if (!button.isMouseOver(event.x(), event.y())) {
            setOpen(false);
        }
        return false;
    }

    public boolean handleMouseScroll(double mouseX, double mouseY, double scrollY) {
        return popover.isOpen() && popover.handleScroll(mouseX, mouseY, scrollY);
    }

    public boolean handleMouseDrag(double mouseX, double mouseY) {
        return popover.isOpen() && popover.handleDrag(mouseX, mouseY);
    }

    public void handleMouseRelease() {
        popover.handleRelease();
    }

    public boolean handleKeyPress(@NotNull KeyEvent event) {
        if (!popover.isOpen()) {
            if (TiconKeys.isToggleChord(event)) {
                toggle();
                return true;
            }
            return false;
        }
        tick();
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            setOpen(false);
            return true;
        }
        if (TiconKeys.isToggleChord(event)) {
            toggle();
            return true;
        }
        boolean confirm = event.key() == GLFW.GLFW_KEY_ENTER
                || event.key() == GLFW.GLFW_KEY_KP_ENTER
                || event.key() == GLFW.GLFW_KEY_TAB;
        if (confirm && popover.hasQuery()) {
            popover.firstVisible()
                    .filter(ticon -> !ticon.locked())
                    .ifPresent(ticon -> choose(ticon, event.hasShiftDown()));
            return true;
        }
        return false;
    }

    private void choose(@NotNull Ticon ticon, boolean keepOpen) {
        RecentTicons.getInstance().use(ticon.id());

        String text = input.getValue();
        int caret = input.getCursorPosition();
        if (TiconChatIntegration.pendingShortcode(text, caret).isPresent()) {
            int start = text.lastIndexOf(':', Math.min(caret, text.length()) - 1);
            String next = TiconChatIntegration.applySuggestion(text, caret, ticon);
            input.setValue(next);
            int position = start + ticon.shortcode().length();
            input.setCursorPosition(position);
            input.setHighlightPos(position);
        } else {
            input.insertText(ticon.shortcode());
        }

        if (keepOpen) {
            popover.rebuild();
            return;
        }
        setOpen(false);
    }
}