package kr.hamzzihouse.hamzziticon.session;

import kr.hamzzihouse.hamzziticon.HamzziTicon;
import kr.hamzzihouse.hamzziticon.catalog.TiconCatalog;
import kr.hamzzihouse.hamzziticon.net.ServerPacket;
import kr.hamzzihouse.hamzziticon.net.TiconCodec;
import kr.hamzzihouse.hamzziticon.platform.Platforms;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@UtilityClass
public class CatalogCache {

    private final String FILE_PREFIX = "catalog-";
    private final String FILE_SUFFIX = ".bin";
    private final long MAX_SIZE_BYTES = 4L * 1024 * 1024;

    @NotNull
    public Optional<TiconCatalog> read(@NotNull String serverKey) {
        Path file = file(serverKey);
        try {
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_SIZE_BYTES) {
                return Optional.empty();
            }
            ServerPacket packet = TiconCodec.decode(Files.readAllBytes(file));
            if (packet instanceof ServerPacket.Catalog(TiconCatalog catalog1)) {
                return Optional.of(catalog1);
            }
            return Optional.empty();
        } catch (IOException | RuntimeException error) {
            HamzziTicon.logger().warn("티콘 캐시를 읽지 못했습니다. 서버에서 다시 받습니다.", error);
            return Optional.empty();
        }
    }

    public void write(@NotNull String serverKey, @NotNull TiconCatalog catalog) {
        Path file = file(serverKey);
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.write(temporary, TiconCodec.encodeCatalog(catalog));
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException error) {
            HamzziTicon.logger().warn("티콘 캐시를 저장하지 못했습니다.", error);
        }
    }

    @NotNull
    private Path file(@NotNull String serverKey) {
        return Platforms.get().configDirectory().resolve(FILE_PREFIX + serverKey + FILE_SUFFIX);
    }
}