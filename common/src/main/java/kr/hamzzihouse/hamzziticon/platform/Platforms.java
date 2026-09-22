package kr.hamzzihouse.hamzziticon.platform;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.ServiceLoader;

@UtilityClass
public class Platforms {

    private volatile TiconPlatform current;

    @NotNull
    public TiconPlatform get() {
        TiconPlatform resolved = current;
        if (resolved == null) {
            synchronized (Platforms.class) {
                resolved = current;
                if (resolved == null) {
                    resolved = load();
                    current = resolved;
                }
            }
        }
        return resolved;
    }

    public void override(@NotNull TiconPlatform platform) {
        current = platform;
    }

    @NotNull
    private TiconPlatform load() {
        Iterator<TiconPlatform> candidates =
                ServiceLoader.load(TiconPlatform.class, Platforms.class.getClassLoader()).iterator();
        if (!candidates.hasNext()) {
            throw new IllegalStateException("""
                    TiconPlatform 구현을 찾지 못했습니다.
                    로더별 모듈의 META-INF/services/kr.hamzzihouse.hamzziticon.platform.TiconPlatform 파일이
                    JAR 에 포함되었는지 확인해 주세요.""");
        }
        return candidates.next();
    }
}