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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecentTicons {

    private static final int CAPACITY = 32;
    private static final int MAX_BONUS = 60;
    private static final String FILE_NAME = "recent.txt";

    @Getter
    private static final RecentTicons instance = new RecentTicons();

    private final Deque<String> recent = new ArrayDeque<>(CAPACITY);
    private final AtomicBoolean dirty = new AtomicBoolean();
    private final AtomicBoolean loaded = new AtomicBoolean();

    public void loadAsync() {
        if (!loaded.compareAndSet(false, true)) {
            return;
        }
        TiconWorker.execute(() -> {
            List<String> stored = read();
            synchronized (recent) {
                recent.clear();
                for (String id : stored) {
                    if (recent.size() >= CAPACITY) {
                        break;
                    }
                    recent.addLast(id);
                }
            }
        });
    }

    public void use(@NotNull String ticonId) {
        synchronized (recent) {
            recent.remove(ticonId);
            recent.addFirst(ticonId);
            while (recent.size() > CAPACITY) {
                recent.removeLast();
            }
        }
        dirty.set(true);
    }

    @NotNull
    @Unmodifiable
    public List<String> ids() {
        synchronized (recent) {
            return List.copyOf(recent);
        }
    }

    public int bonusOf(@NotNull String ticonId) {
        int position = 0;
        synchronized (recent) {
            for (String id : recent) {
                if (id.equals(ticonId)) {
                    return Math.max(0, MAX_BONUS - position * 2);
                }
                position++;
            }
        }
        return 0;
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
            HamzziTicon.logger().warn("최근 사용 목록을 읽지 못했습니다: {}", file, error);
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
            HamzziTicon.logger().warn("최근 사용 목록을 저장하지 못했습니다: {}", file, error);
        }
    }

    @NotNull
    private Path file() {
        return Platforms.get().configDirectory().resolve(FILE_NAME);
    }
}