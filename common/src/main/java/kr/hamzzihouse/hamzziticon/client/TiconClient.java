package kr.hamzzihouse.hamzziticon.client;

import kr.hamzzihouse.hamzziticon.HamzziTicon;
import kr.hamzzihouse.hamzziticon.platform.Platforms;
import kr.hamzzihouse.hamzziticon.platform.TiconPlatform;
import kr.hamzzihouse.hamzziticon.session.TiconSync;
import kr.hamzzihouse.hamzziticon.util.TiconWorker;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

@UtilityClass
public class TiconClient {

    private final AtomicBoolean started = new AtomicBoolean();

    public void start(@NotNull TiconPlatform platform) {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        Platforms.override(platform);
        HamzziTicon.logger().info("{} {} 시작 ({})", HamzziTicon.MOD_NAME, platform.modVersion(), platform.loaderName());
    }

    public void onJoin() {
        TiconSync.onJoin();
    }

    public void onDisconnect() {
        TiconSync.onDisconnect();
    }

    public void onPayload(byte @NotNull [] payload) {
        TiconSync.onPayload(payload);
    }

    public void stop() {
        TiconSync.onDisconnect();
        TiconWorker.shutdown();
    }
}