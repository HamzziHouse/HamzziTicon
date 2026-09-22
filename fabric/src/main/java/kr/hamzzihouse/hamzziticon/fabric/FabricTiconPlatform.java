package kr.hamzzihouse.hamzziticon.fabric;

import kr.hamzzihouse.hamzziticon.HamzziTicon;
import kr.hamzzihouse.hamzziticon.net.TiconPayload;
import kr.hamzzihouse.hamzziticon.platform.TiconPlatform;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class FabricTiconPlatform implements TiconPlatform {

    @Override
    @NotNull
    public String loaderName() {
        return "Fabric";
    }

    @Override
    @NotNull
    public String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(HamzziTicon.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override
    @NotNull
    public Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve(HamzziTicon.MOD_ID);
    }

    @Override
    public boolean canSendToServer() {
        return ClientPlayNetworking.canSend(TiconPayload.TYPE);
    }

    @Override
    public void sendToServer(byte @NotNull [] payload) {
        ClientPlayNetworking.send(new TiconPayload(payload));
    }

    @Override
    public void runOnClientThread(@NotNull Runnable task) {
        Minecraft.getInstance().execute(task);
    }
}