package kr.hamzzihouse.hamzziticon.history;

import kr.hamzzihouse.hamzziticon.HamzziTicon;
import kr.hamzzihouse.hamzziticon.platform.Platforms;
import kr.hamzzihouse.hamzziticon.util.TiconWorker;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FavoriteTicons {

    private static final int CAPACITY = 128;
    private static final String FILE_NAME = "favorites.txt";

    @Getter
    private static final FavoriteTicons instance = new FavoriteTicons();

    private final Set<String> favorites = new LinkedHashSet<>();
    private final AtomicBoolean dirty = new AtomicBoolean();
    private final AtomicBoolean loaded = new AtomicBoolean();

    public void loadAsync() {
        if (!loaded.compareAndSet(false, true)) {
            return;
        }
        TiconWorker.execute(() -> {
            List<String> stored = read();
            synchronized (favorites) {
                favorites.clear();
                for (String id : stored) {
                    if (favorites.size() >= CAPACITY) {
                        break;
                    }
                    favorites.add(id);
                }
            }
        });
    }

    public boolean contains(@NotNull String ticonId) {
        synchronized (favorites) {
            return favorites.contains(ticonId);
        }
    }

    public boolean toggle(@NotNull String ticonId) {
        boolean added;
        synchronized (favorites) {
            if (favorites.remove(ticonId)) {
                added = false;
            } else {
                if (favorites.size() >= CAPACITY) {
                    return false;
                }
                favorites.add(ticonId);
                added = true;
            }
        }
        dirty.set(true);
        return added;
    }

    @NotNull
    @Unmodifiable
    public List<String> ids() {
        synchronized (favorites) {
            return List.copyOf(favorites);
        }
    }

    public void flush() {
        if (!dirty.compareAndSet(true, false)) {
            return;
        }
        List<String> snapshot = ids();
        TiconWorker.execute(() -> write(snapshot));
    }

    @NotNull
    private List<String> read() {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            List<String> ids = new ArrayList<>(lines.size());
            for (String line : lines) {
                String id = line.strip();
                if (!id.isEmpty() && !ids.contains(id)) {
                    ids.add(id);
                }
            }
            return ids;
        } catch (IOException error) {
            HamzziTicon.logger().warn("즐겨찾기 목록을 읽지 못했습니다: {}", file, error);
            return List.of();
        }
    }

    private void write(@NotNull List<String> ids) {
        Path file = file();
        Path temporary = file.resolveSibling(FILE_NAME + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.write(temporary, ids, StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException error) {
            HamzziTicon.logger().warn("즐겨찾기 목록을 저장하지 못했습니다: {}", file, error);
        }
    }

    @NotNull
    private Path file() {
        return Platforms.get().configDirectory().resolve(FILE_NAME);
    }
}