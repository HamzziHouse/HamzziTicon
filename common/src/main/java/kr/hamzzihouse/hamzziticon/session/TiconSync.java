package kr.hamzzihouse.hamzziticon.session;

import kr.hamzzihouse.hamzziticon.HamzziTicon;
import kr.hamzzihouse.hamzziticon.catalog.TiconCatalog;
import kr.hamzzihouse.hamzziticon.client.chat.TiconSprites;
import kr.hamzzihouse.hamzziticon.history.FavoriteTicons;
import kr.hamzzihouse.hamzziticon.history.RecentTicons;
import kr.hamzzihouse.hamzziticon.net.PacketReader;
import kr.hamzzihouse.hamzziticon.net.ServerPacket;
import kr.hamzzihouse.hamzziticon.net.TiconCodec;
import kr.hamzzihouse.hamzziticon.net.TiconProtocol;
import kr.hamzzihouse.hamzziticon.platform.Platforms;
import kr.hamzzihouse.hamzziticon.platform.TiconPlatform;
import kr.hamzzihouse.hamzziticon.search.TiconSearchIndex;
import kr.hamzzihouse.hamzziticon.util.TiconWorker;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@UtilityClass
public class TiconSync {

    private final long[] HELLO_RETRY_MILLIS = {500L, 1_500L, 3_000L, 6_000L, 12_000L};

    private final AtomicInteger cachedRevision = new AtomicInteger(TiconProtocol.NO_REVISION);
    private final AtomicInteger generation = new AtomicInteger();
    private final AtomicBoolean answered = new AtomicBoolean();

    private volatile @Nullable String serverKey;

    public void onJoin() {
        int session = generation.incrementAndGet();
        answered.set(false);
        serverKey = currentServerKey();
        TiconSession.getInstance().set(CatalogState.Syncing.now());
        RecentTicons.getInstance().loadAsync();
        FavoriteTicons.getInstance().loadAsync();

        String key = serverKey;
        TiconWorker.execute(() -> {
            Optional<TiconCatalog> cached = key == null ? Optional.empty() : CatalogCache.read(key);
            cached.ifPresent(catalog -> {
                if (session != generation.get()) {
                    return;
                }
                cachedRevision.set(catalog.revision());
                publish(catalog);
            });
            for (int attempt = 0; attempt < HELLO_RETRY_MILLIS.length; attempt++) {
                int index = attempt;
                TiconWorker.schedule(() -> Platforms.get().runOnClientThread(() -> sendHello(session, index)),
                        HELLO_RETRY_MILLIS[attempt]);
            }
        });
    }

    public void onDisconnect() {
        generation.incrementAndGet();
        serverKey = null;
        cachedRevision.set(TiconProtocol.NO_REVISION);
        RecentTicons.getInstance().flush();
        FavoriteTicons.getInstance().flush();
        TiconSession.getInstance().reset();
    }

    public void onPayload(byte @NotNull [] payload) {
        int session = generation.get();
        byte[] copy = payload.clone();
        TiconWorker.execute(() -> handle(session, copy));
    }

    @Nullable
    private String currentServerKey() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        if (server == null || server.ip.isBlank()) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(server.ip.strip().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private void sendHello(int session, int attempt) {
        if (session != generation.get() || answered.get()) {
            return;
        }
        TiconPlatform platform = Platforms.get();
        if (!platform.canSendToServer()) {
            HamzziTicon.logger().debug("서버가 아직 {} 채널을 열지 않았습니다. (시도 {})", TiconProtocol.CHANNEL, attempt + 1);
            return;
        }
        platform.sendToServer(TiconCodec.encodeHello(platform.modVersion(), cachedRevision.get()));
        HamzziTicon.logger().info("서버에 티콘 목록을 요청했습니다. (시도 {}, 캐시 리비전 {})", attempt + 1, cachedRevision.get());
    }

    private void handle(int session, byte @NotNull [] payload) {
        if (session != generation.get()) {
            return;
        }
        answered.set(true);
        ServerPacket packet;
        try {
            packet = TiconCodec.decode(payload);
        } catch (PacketReader.MalformedPacketException error) {
            HamzziTicon.logger().warn("서버가 보낸 티콘 패킷을 해석하지 못했습니다: {}", error.getMessage());
            TiconSession.getInstance().set(new CatalogState.Failed(error.getMessage()));
            return;
        } catch (RuntimeException error) {
            HamzziTicon.logger().warn("서버가 보낸 티콘 패킷이 올바르지 않습니다.", error);
            TiconSession.getInstance().set(new CatalogState.Failed("서버가 보낸 티콘 목록이 올바르지 않습니다."));
            return;
        }

        switch (packet) {
            case ServerPacket.Catalog message -> {
                TiconCatalog catalog = message.catalog();
                cachedRevision.set(catalog.revision());
                publish(catalog);
                String key = serverKey;
                if (key != null) {
                    CatalogCache.write(key, catalog);
                }
                HamzziTicon.logger().info("티콘 {}개를 받았습니다. (리비전 {})", catalog.size(), catalog.revision());
            }
            case ServerPacket.UpToDate message -> {
                if (!TiconSession.getInstance().state().isReady()) {
                    TiconSession.getInstance().set(new CatalogState.Failed(
                            "서버가 캐시를 쓰라고 했지만 로컬 캐시가 없습니다. 재접속해 주세요."));
                    return;
                }
                HamzziTicon.logger().debug("캐시가 최신입니다. (리비전 {})", message.revision());
            }
            case ServerPacket.Disabled message -> {
                HamzziTicon.logger().info("서버가 티콘을 제공하지 않습니다: {}", message.reason());
                TiconSession.getInstance().set(new CatalogState.Failed(message.reason()));
            }
        }
    }

    private void publish(@NotNull TiconCatalog catalog) {
        TiconSprites.invalidate();
        TiconSession.getInstance().set(new CatalogState.Ready(catalog, new TiconSearchIndex(catalog)));
    }
}