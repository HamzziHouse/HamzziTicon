package kr.hamzzihouse.hamzziticon.session;

import kr.hamzzihouse.hamzziticon.HamzziTicon;
import kr.hamzzihouse.hamzziticon.catalog.TiconCatalog;
import kr.hamzzihouse.hamzziticon.history.RecentTicons;
import kr.hamzzihouse.hamzziticon.search.SearchResult;
import kr.hamzzihouse.hamzziticon.search.TiconSearchIndex;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TiconSession {

    @Getter
    private static final TiconSession instance = new TiconSession();

    private final AtomicReference<CatalogState> state = new AtomicReference<>(new CatalogState.Unsupported());
    private final List<Consumer<CatalogState>> listeners = new CopyOnWriteArrayList<>();

    @NotNull
    public CatalogState state() {
        return state.get();
    }

    @NotNull
    public TiconCatalog catalog() {
        return state.get().catalogOrEmpty();
    }

    @NotNull
    public TiconSearchIndex index() {
        return state.get().indexOrEmpty();
    }

    @NotNull
    @Unmodifiable
    public List<SearchResult> search(@NotNull String query, int limit) {
        return index().search(query, limit, RecentTicons.getInstance()::bonusOf);
    }

    public void set(@NotNull CatalogState next) {
        if (state.getAndSet(next).equals(next)) {
            return;
        }
        for (Consumer<CatalogState> listener : listeners) {
            try {
                listener.accept(next);
            } catch (Throwable error) {
                HamzziTicon.logger().error("카탈로그 리스너가 실패했습니다.", error);
            }
        }
    }

    public void reset() {
        set(new CatalogState.Unsupported());
    }

    public void addListener(@NotNull Consumer<CatalogState> listener) {
        listeners.add(listener);
        listener.accept(state.get());
    }

    public void removeListener(@NotNull Consumer<CatalogState> listener) {
        listeners.remove(listener);
    }
}