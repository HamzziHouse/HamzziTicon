package kr.hamzzihouse.hamzziticon.client;

import kr.hamzzihouse.hamzziticon.HamzziTicon;
import lombok.experimental.UtilityClass;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@UtilityClass
public class TiconKeys {

    public final Identifier CATEGORY_ID = Identifier.fromNamespaceAndPath(HamzziTicon.MOD_ID, "main");

    public final String TOGGLE_KEY_NAME = "key." + HamzziTicon.MOD_ID + ".toggle";

    public final int DEFAULT_TOGGLE_KEY = GLFW.GLFW_KEY_G;

    private volatile @Nullable KeyMapping toggle;

    public void bind(@NotNull KeyMapping mapping) {
        toggle = mapping;
    }

    public boolean isToggleChord(@NotNull KeyEvent event) {
        if (!event.hasControlDownWithQuirk()) {
            return false;
        }
        KeyMapping mapping = toggle;
        return mapping != null ? mapping.matches(event) : event.key() == DEFAULT_TOGGLE_KEY;
    }
}