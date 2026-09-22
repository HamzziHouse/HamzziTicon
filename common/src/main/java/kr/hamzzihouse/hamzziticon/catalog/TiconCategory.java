package kr.hamzzihouse.hamzziticon.catalog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record TiconCategory(
        @NotNull String id,
        @NotNull String displayName,
        @Nullable String iconTiconId,
        int order
) implements Comparable<TiconCategory> {

    public TiconCategory {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        if (iconTiconId != null && iconTiconId.isBlank()) {
            iconTiconId = null;
        }
    }

    @Override
    public int compareTo(@NotNull TiconCategory other) {
        int byOrder = Integer.compare(order, other.order);
        return byOrder != 0 ? byOrder : id.compareTo(other.id);
    }
}