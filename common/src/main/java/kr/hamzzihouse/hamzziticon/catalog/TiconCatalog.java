package kr.hamzzihouse.hamzziticon.catalog;

import kr.hamzzihouse.hamzziticon.net.TiconProtocol;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Getter
@Accessors(fluent = true)
public final class TiconCatalog {

    public static final String DEFAULT_FONT = "minecraft:default";

    public static final TiconCatalog EMPTY = new TiconCatalog(0, DEFAULT_FONT, List.of(), List.of());

    private final int revision;
    private final String font;

    private final @Unmodifiable List<TiconCategory> categories;
    private final @Unmodifiable List<Ticon> ticons;

    @Getter(AccessLevel.NONE)
    private final Map<String, Ticon> byId;

    @Getter(AccessLevel.NONE)
    private final Map<String, List<Ticon>> byCategory;

    public TiconCatalog(int revision,
                        @NotNull String font,
                        @NotNull Collection<TiconCategory> categories,
                        @NotNull Collection<Ticon> ticons) {
        this.revision = revision;
        this.font = TiconProtocol.FONT_PATTERN.matcher(font).matches() ? font : DEFAULT_FONT;

        List<TiconCategory> sortedCategories = new ArrayList<>(Objects.requireNonNull(categories, "categories"));
        sortedCategories.sort(null);
        this.categories = List.copyOf(sortedCategories);
        this.ticons = List.copyOf(Objects.requireNonNull(ticons, "ticons"));

        Map<String, Ticon> idIndex = new LinkedHashMap<>(this.ticons.size() * 2);
        Map<String, List<Ticon>> categoryIndex = new LinkedHashMap<>(this.categories.size() * 2);
        for (TiconCategory category : this.categories) {
            categoryIndex.put(category.id(), new ArrayList<>());
        }

        for (Ticon ticon : this.ticons) {
            if (idIndex.putIfAbsent(ticon.id(), ticon) != null) {
                continue;
            }
            categoryIndex.computeIfAbsent(ticon.categoryId(), key -> new ArrayList<>()).add(ticon);
        }

        Map<String, List<Ticon>> frozen = new LinkedHashMap<>(categoryIndex.size() * 2);
        categoryIndex.forEach((key, value) -> frozen.put(key, List.copyOf(value)));

        this.byId = Map.copyOf(idIndex);
        this.byCategory = Map.copyOf(frozen);
    }

    public boolean isEmpty() {
        return ticons.isEmpty();
    }

    public int size() {
        return ticons.size();
    }

    @NotNull
    public Optional<Ticon> find(@Nullable String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    @NotNull
    @Unmodifiable
    public List<Ticon> ticonsIn(@NotNull String categoryId) {
        return byCategory.getOrDefault(categoryId, List.of());
    }

    @NotNull
    public Optional<Ticon> iconOf(@NotNull TiconCategory category) {
        return find(category.iconTiconId())
                .or(() -> ticonsIn(category.id()).stream().findFirst());
    }
}