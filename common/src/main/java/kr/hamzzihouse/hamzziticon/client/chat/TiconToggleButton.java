package kr.hamzzihouse.hamzziticon.client.chat;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import kr.hamzzihouse.hamzziticon.catalog.Ticon;
import kr.hamzzihouse.hamzziticon.catalog.TiconCatalog;
import kr.hamzzihouse.hamzziticon.session.TiconSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class TiconToggleButton extends AbstractWidget {

    public static final int WIDTH = 18;
    public static final int HEIGHT = 12;

    private static final int ICON = 10;
    private static final String FALLBACK_ICON = "☺";

    private final Runnable onToggle;

    public TiconToggleButton(int x, int y, @NotNull Runnable onToggle) {
        super(x, y, WIDTH, HEIGHT, Component.translatable("screen.hamzziticon.button"));
        this.onToggle = onToggle;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHovered();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                hovered ? TiconSprites.BUTTON_HIGHLIGHTED : TiconSprites.BUTTON, getX(), getY(), WIDTH, HEIGHT);
        if (hovered) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        TiconCatalog catalog = TiconSession.getInstance().catalog();
        Optional<Ticon> icon = catalog.ticons().stream().findFirst();
        int iconX = getX() + (WIDTH - ICON) / 2;
        int iconY = getY() + (HEIGHT - ICON) / 2;

        Identifier texture = icon.map(TiconSprites::textureOf).orElse(null);
        if (texture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0F, 0F, ICON, ICON, 1, 1, 1, 1);
            return;
        }

        Component label = icon
                .map(ticon -> Component.literal(ticon.character()).withStyle(
                        Style.EMPTY.withFont(new FontDescription.Resource(Identifier.parse(catalog.font())))))
                .orElseGet(() -> Component.literal(FALLBACK_ICON));
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, label,
                getX() + (WIDTH - font.width(label)) / 2,
                getY() + (HEIGHT - font.lineHeight) / 2 + 1,
                0xFFFFFFFF, false);
    }

    @Override
    public void onClick(@NotNull MouseButtonEvent event, boolean doubleClick) {
        onToggle.run();
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}