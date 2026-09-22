package kr.hamzzihouse.hamzziticon.catalog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

public record Ticon(
        @NotNull String id,
        @NotNull String displayName,
        @NotNull String character,
        @NotNull String categoryId,
        @NotNull @Unmodifiable List<String> aliases,
        @NotNull String texture,
        boolean locked
) implements Comparable<Ticon> {

    public Ticon {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(character, "character");
        Objects.requireNonNull(categoryId, "categoryId");
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        texture = Objects.requireNonNull(texture, "texture").strip();

        if (id.isBlank()) {
            throw new IllegalArgumentException("티콘 id 는 비어 있을 수 없습니다.");
        }
        if (character.isEmpty()) {
            throw new IllegalArgumentException("티콘 '" + id + "' 의 글리프 문자가 비어 있습니다.");
        }
    }

    public boolean hasTexture() {
        return !texture.isEmpty();
    }

    @NotNull
    public String shortcode() {
        return ":" + id + ":";
    }

    @Override
    public int compareTo(@NotNull Ticon other) {
        return id.compareTo(other.id);
    }
}