package kr.hamzzihouse.hamzziticon.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import kr.hamzzihouse.hamzziticon.client.TiconChatIntegration;
import kr.hamzzihouse.hamzziticon.client.TiconClient;
import kr.hamzzihouse.hamzziticon.client.TiconKeys;
import kr.hamzzihouse.hamzziticon.client.chat.TiconChatOverlay;
import kr.hamzzihouse.hamzziticon.net.TiconPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;

public final class FabricTiconClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TiconClient.start(new FabricTiconPlatform());

        PayloadTypeRegistry.playS2C().register(TiconPayload.TYPE, TiconPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TiconPayload.TYPE, TiconPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(TiconPayload.TYPE,
                (payload, context) -> TiconClient.onPayload(payload.data()));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> TiconClient.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TiconClient.onDisconnect());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TiconClient.stop());

        KeyMapping.Category category = KeyMapping.Category.register(TiconKeys.CATEGORY_ID);
        TiconKeys.bind(KeyBindingHelper.registerKeyBinding(new KeyMapping(
                TiconKeys.TOGGLE_KEY_NAME, InputConstants.Type.KEYSYM, TiconKeys.DEFAULT_TOGGLE_KEY, category)));


        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!TiconChatIntegration.isChatScreen(screen)) {
                return;
            }
            TiconChatOverlay.attach(screen, widget -> Screens.getButtons(screen).add(widget)).ifPresent(overlay -> {
                ScreenMouseEvents.allowMouseClick(screen).register((target, event) -> !overlay.handleMouseClick(event));
                ScreenMouseEvents.allowMouseScroll(screen).register(
                        (target, mouseX, mouseY, horizontal, vertical) -> !overlay.handleMouseScroll(mouseX, mouseY, vertical));
                ScreenMouseEvents.allowMouseDrag(screen).register(
                        (target, event, deltaX, deltaY) -> !overlay.handleMouseDrag(event.x(), event.y()));
                ScreenMouseEvents.allowMouseRelease(screen).register((target, event) -> {
                    overlay.handleMouseRelease();
                    return true;
                });
                ScreenKeyboardEvents.allowKeyPress(screen).register((target, event) -> !overlay.handleKeyPress(event));
                ScreenEvents.afterTick(screen).register(target -> overlay.tick());
                ScreenEvents.remove(screen).register(TiconChatOverlay::detach);
            });
        });
    }
}