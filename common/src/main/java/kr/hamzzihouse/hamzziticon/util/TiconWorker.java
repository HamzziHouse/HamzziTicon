package kr.hamzzihouse.hamzziticon.util;

import kr.hamzzihouse.hamzziticon.HamzziTicon;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@UtilityClass
public class TiconWorker {

    private final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, HamzziTicon.MOD_NAME + "-Worker-" + counter.incrementAndGet());

            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            thread.setUncaughtExceptionHandler((t, error) ->
                    HamzziTicon.logger().error("[{}] 처리되지 않은 예외", t.getName(), error));
            return thread;
        }
    };

    private final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(THREAD_FACTORY);

    public void execute(@NotNull Runnable task) {
        EXECUTOR.execute(guard(task));
    }

    public void schedule(@NotNull Runnable task, long delayMillis) {
        CompletableFuture.runAsync(guard(task),
                CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS, EXECUTOR));
    }

    public void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @NotNull
    private Runnable guard(@NotNull Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable error) {
                HamzziTicon.logger().error("백그라운드 작업이 실패했습니다.", error);
            }
        };
    }
}