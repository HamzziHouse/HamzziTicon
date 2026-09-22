package kr.hamzzihouse.hamzziticon.search;

import kr.hamzzihouse.hamzziticon.catalog.Ticon;
import org.jetbrains.annotations.NotNull;

public record SearchResult(@NotNull Ticon ticon, int score) implements Comparable<SearchResult> {

    @Override
    public int compareTo(@NotNull SearchResult other) {
        int byScore = Integer.compare(other.score, score);
        return byScore != 0 ? byScore : ticon.id().compareTo(other.ticon.id());
    }
}