package kr.hamzzihouse.hamzziticon.client;

import kr.hamzzihouse.hamzziticon.catalog.Ticon;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.regex.Pattern;

@UtilityClass
public class TiconChatIntegration {

    public final Pattern SHORTCODE_PATTERN = Pattern.compile(":([a-z0-9_]{2,32}):");

    public boolean isChatScreen(@Nullable Screen screen) {
        return screen instanceof ChatScreen;
    }

    @NotNull
    public Optional<EditBox> chatInput(@Nullable Screen screen) {
        if (!(screen instanceof ChatScreen)) {
            return Optional.empty();
        }
        for (GuiEventListener child : screen.children()) {
            if (child instanceof EditBox editBox) {
                return Optional.of(editBox);
            }
        }
        return Optional.empty();
    }

    @NotNull
    public Optional<String> pendingShortcode(@NotNull String chatText, int caret) {
        int end = Math.min(caret, chatText.length());
        int start = chatText.lastIndexOf(':', end - 1);
        if (start < 0) {
            return Optional.empty();
        }

        String candidate = chatText.substring(start + 1, end);
        if (candidate.isEmpty() || candidate.indexOf(' ') >= 0 || candidate.indexOf(':') >= 0) {
            return Optional.empty();
        }
        return Optional.of(candidate);
    }

    @NotNull
    public String applySuggestion(@NotNull String chatText, int caret, @NotNull Ticon ticon) {
        int end = Math.min(caret, chatText.length());
        int start = chatText.lastIndexOf(':', end - 1);
        if (start < 0) {
            return chatText;
        }
        return chatText.substring(0, start) + ticon.shortcode() + chatText.substring(end);
    }
}