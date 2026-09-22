package kr.hamzzihouse.hamzziticon.client.chat;

import kr.hamzzihouse.hamzziticon.HamzziTicon;
import kr.hamzzihouse.hamzziticon.catalog.Ticon;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class TiconSprites {

    public final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
    public final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
    public final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
    public final Identifier TAB = Identifier.withDefaultNamespace("widget/tab");
    public final Identifier TAB_SELECTED = Identifier.withDefaultNamespace("widget/tab_selected");
    public final Identifier TAB_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/tab_highlighted");
    public final Identifier CREATIVE_SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
    public final Identifier FAVORITE_STAR = Identifier.fromNamespaceAndPath(HamzziTicon.MOD_ID, "textures/gui/star.png");
    public final Identifier CREATIVE_SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");

    public final int PANEL_FILL = 0xFFC6C6C6;
    public final int PANEL_LIGHT = 0xFFFFFFFF;
    public final int PANEL_DARK = 0xFF555555;
    public final int PANEL_OUTLINE = 0xFF000000;
    public final int TRACK_FILL = 0xFF8B8B8B;
    public final int TRACK_DARK = 0xFF373737;
    public final int SLOT_HOVER = 0x80FFFFFF;
    public final int SLOT_LOCKED = 0x99000000;
    public final int TEXT_DARK = 0xFF404040;
    public final int TEXT_MUTED = 0xFF7A7A7A;
    public final int TEXT_LOCKED = 0xFFB02E26;

    private final Map<String, Identifier> RESOLVED = new ConcurrentHashMap<>();

    public void invalidate() {
        RESOLVED.clear();
    }

    @Nullable
    public Identifier textureOf(@NotNull Ticon ticon) {
        Identifier resolved = RESOLVED.computeIfAbsent(ticon.id(), id -> resolve(ticon));
        return resolved == MISSING ? null : resolved;
    }

    private final Identifier MISSING = Identifier.fromNamespaceAndPath(HamzziTicon.MOD_ID, "missing");

    @NotNull
    private Identifier resolve(@NotNull Ticon ticon) {
        Identifier bundled = Identifier.fromNamespaceAndPath(HamzziTicon.MOD_ID, "textures/ticon/" + ticon.id() + ".png");
        if (exists(bundled)) {
            return bundled;
        }
        if (!ticon.hasTexture()) {
            return MISSING;
        }
        Identifier external = Identifier.tryBuild(Identifier.DEFAULT_NAMESPACE, "textures/" + ticon.texture() + ".png");
        return external != null && exists(external) ? external : MISSING;
    }

    private boolean exists(@NotNull Identifier texture) {
        return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
    }
}