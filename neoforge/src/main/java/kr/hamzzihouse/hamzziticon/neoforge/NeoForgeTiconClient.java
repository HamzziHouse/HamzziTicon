package kr.hamzzihouse.hamzziticon.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import kr.hamzzihouse.hamzziticon.HamzziTicon;
import kr.hamzzihouse.hamzziticon.client.TiconChatIntegration;
import kr.hamzzihouse.hamzziticon.client.TiconClient;
import kr.hamzzihouse.hamzziticon.client.TiconKeys;
import kr.hamzzihouse.hamzziticon.client.chat.TiconChatOverlay;
import kr.hamzzihouse.hamzziticon.net.TiconPayload;
import kr.hamzzihouse.hamzziticon.net.TiconProtocol;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(value = HamzziTicon.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeTiconClient {

    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(TiconKeys.CATEGORY_ID);

    private static final KeyMapping TOGGLE_KEY = new KeyMapping(
            TiconKeys.TOGGLE_KEY_NAME, InputConstants.Type.KEYSYM, TiconKeys.DEFAULT_TOGGLE_KEY, CATEGORY);

    public NeoForgeTiconClient(IEventBus modEventBus) {
        TiconClient.start(new NeoForgeTiconPlatform());
        TiconKeys.bind(TOGGLE_KEY);

        modEventBus.addListener(NeoForgeTiconClient::onRegisterPayloads);
        modEventBus.addListener(NeoForgeTiconClient::onRegisterKeyMappings);

        NeoForge.EVENT_BUS.register(NeoForgeTiconClient.class);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE_KEY);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(String.valueOf(TiconProtocol.VERSION)).optional();
        registrar.playToClient(TiconPayload.TYPE, TiconPayload.CODEC,
                (payload, context) -> TiconClient.onPayload(payload.data()));
        registrar.playToServer(TiconPayload.TYPE, TiconPayload.CODEC, (payload, context) -> {
        });
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        TiconChatOverlay.current().ifPresent(TiconChatOverlay::tick);
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        TiconClient.onJoin();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        TiconClient.onDisconnect();
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (TiconChatIntegration.isChatScreen(event.getScreen())) {
            TiconChatOverlay.attach(event.getScreen(), event::addListener);
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        TiconChatOverlay.detach(event.getScreen());
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        TiconChatOverlay.current()
                .filter(overlay -> overlay.handleMouseClick(event.getMouseButtonEvent()))
                .ifPresent(overlay -> event.setCanceled(true));
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        TiconChatOverlay.current()
                .filter(overlay -> overlay.handleMouseScroll(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY()))
                .ifPresent(overlay -> event.setCanceled(true));
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        TiconChatOverlay.current()
                .filter(overlay -> overlay.handleMouseDrag(event.getMouseX(), event.getMouseY()))
                .ifPresent(overlay -> event.setCanceled(true));
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        TiconChatOverlay.current().ifPresent(TiconChatOverlay::handleMouseRelease);
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        TiconChatOverlay.current()
                .filter(overlay -> overlay.handleKeyPress(event.getKeyEvent()))
                .ifPresent(overlay -> event.setCanceled(true));
    }
}