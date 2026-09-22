package kr.hamzzihouse.hamzziticon.net;

import kr.hamzzihouse.hamzziticon.catalog.TiconCatalog;
import org.jetbrains.annotations.NotNull;

public sealed interface ServerPacket {

    record Catalog(@NotNull TiconCatalog catalog) implements ServerPacket {
    }

    record UpToDate(int revision) implements ServerPacket {
    }

    record Disabled(@NotNull String reason) implements ServerPacket {
    }
}