package kr.hamzzihouse.hamzziticon.session;

import kr.hamzzihouse.hamzziticon.catalog.TiconCatalog;
import kr.hamzzihouse.hamzziticon.search.TiconSearchIndex;
import org.jetbrains.annotations.NotNull;

public sealed interface CatalogState {

    long SYNC_TIMEOUT_MILLIS = 10_000L;

    record Unsupported() implements CatalogState {
    }

    record Syncing(long startedAt) implements CatalogState {

        public static Syncing now() {
            return new Syncing(System.currentTimeMillis());
        }

        public boolean timedOut() {
            return System.currentTimeMillis() - startedAt > SYNC_TIMEOUT_MILLIS;
        }
    }

    record Ready(@NotNull TiconCatalog catalog, @NotNull TiconSearchIndex index) implements CatalogState {
    }

    record Failed(@NotNull String reason) implements CatalogState {
    }

    @NotNull
    default TiconCatalog catalogOrEmpty() {
        return this instanceof Ready ready ? ready.catalog() : TiconCatalog.EMPTY;
    }

    @NotNull
    default TiconSearchIndex indexOrEmpty() {
        return this instanceof Ready ready ? ready.index() : TiconSearchIndex.EMPTY;
    }

    default boolean isReady() {
        return this instanceof Ready;
    }
}