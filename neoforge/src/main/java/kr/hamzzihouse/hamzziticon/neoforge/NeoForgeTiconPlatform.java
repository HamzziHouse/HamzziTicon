package kr.hamzzihouse.hamzziticon.neoforge;

import kr.hamzzihouse.hamzziticon.HamzziTicon;
import kr.hamzzihouse.hamzziticon.net.TiconPayload;
import kr.hamzzihouse.hamzziticon.platform.TiconPlatform;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class NeoForgeTiconPlatform implements TiconPlatform {

    @Override
    @NotNull
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    @NotNull
    public String modVersion() {
        return ModList.get()
                .getModContainerById(HamzziTicon.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    @Override
    @NotNull
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve(HamzziTicon.MOD_ID);
    }

    @Override
    public boolean canSendToServer() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() != null
                && minecraft.getConnection().hasChannel(TiconPayload.TYPE);
    }

    @Override
    public void sendToServer(byte @NotNull [] payload) {
        ClientPacketDistributor.sendToServer(new TiconPayload(payload));
    }

    @Override
    public void runOnClientThread(@NotNull Runnable task) {
        Minecraft.getInstance().execute(task);
    }
}