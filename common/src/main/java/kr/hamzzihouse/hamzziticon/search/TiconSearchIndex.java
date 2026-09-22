package kr.hamzzihouse.hamzziticon.search;

import kr.hamzzihouse.hamzziticon.catalog.Ticon;
import kr.hamzzihouse.hamzziticon.catalog.TiconCatalog;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

public final class TiconSearchIndex {

    public static final TiconSearchIndex EMPTY = new TiconSearchIndex(TiconCatalog.EMPTY);

    private static final int SCORE_ID_EXACT = 1000;
    private static final int SCORE_ALIAS_EXACT = 960;
    private static final int SCORE_NAME_EXACT = 940;
    private static final int SCORE_NAME_PREFIX = 800;
    private static final int SCORE_ALIAS_PREFIX = 760;
    private static final int SCORE_ID_PREFIX = 720;
    private static final int SCORE_NAME_CONTAINS = 600;
    private static final int SCORE_ALIAS_CONTAINS = 560;
    private static final int SCORE_ID_CONTAINS = 520;
    private static final int SCORE_INITIALS_PREFIX = 480;
    private static final int SCORE_INITIALS_CONTAINS = 380;

    private static final int PENALTY_LOCKED = 2000;

    private static final int MAX_RECENCY_BONUS = 60;

    private final List<Entry> entries;

    public TiconSearchIndex(@NotNull TiconCatalog catalog) {
        List<Ticon> ticons = catalog.ticons();
        List<Entry> built = new ArrayList<>(ticons.size());

        for (int order = 0; order < ticons.size(); order++) {
            Ticon ticon = ticons.get(order);

            List<String> aliases = new ArrayList<>(ticon.aliases().size());
            List<String> aliasInitials = new ArrayList<>(ticon.aliases().size());
            for (String alias : ticon.aliases()) {
                String normalized = normalize(alias);
                if (normalized.isEmpty()) {
                    continue;
                }
                aliases.add(normalized);
                aliasInitials.add(Hangul.initials(alias));
            }

            built.add(new Entry(
                    ticon,
                    normalize(ticon.displayName()),
                    normalize(ticon.id()),
                    Hangul.initials(ticon.displayName()),
                    List.copyOf(aliases),
                    List.copyOf(aliasInitials),
                    order
            ));
        }

        this.entries = List.copyOf(built);
    }

    @NotNull
    public List<SearchResult> search(@NotNull String query, int limit, @NotNull ToIntFunction<String> recencyBonus) {
        String normalized = normalize(query);
        boolean initialsQuery = Hangul.isInitialsQuery(query.strip());
        String initials = initialsQuery ? query.replaceAll("\\s+", "") : "";

        List<SearchResult> results = new ArrayList<>(Math.min(limit * 2, entries.size()));

        for (Entry entry : entries) {
            int base = initialsQuery
                    ? entry.scoreByInitials(initials)
                    : entry.score(normalized);
            if (base <= 0) {
                continue;
            }

            int bonus = Math.clamp(recencyBonus.applyAsInt(entry.ticon.id()), 0, MAX_RECENCY_BONUS);
            int penalty = entry.ticon.locked() ? PENALTY_LOCKED : 0;

            int tieBreaker = Math.max(0, 40 - entry.order / 8);

            results.add(new SearchResult(entry.ticon, base + bonus + tieBreaker - penalty));
        }

        results.sort(null);
        return results.size() <= limit ? List.copyOf(results) : List.copyOf(results.subList(0, limit));
    }

    @NotNull
    private static String normalize(@NotNull String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private record Entry(
            Ticon ticon,
            String name,
            String id,
            String initials,
            List<String> aliases,
            List<String> aliasInitials,
            int order
    ) {

        int score(String query) {
            if (query.isEmpty()) {
                return 1;
            }

            if (id.equals(query)) {
                return SCORE_ID_EXACT;
            }
            if (name.equals(query)) {
                return SCORE_NAME_EXACT;
            }
            for (String alias : aliases) {
                if (alias.equals(query)) {
                    return SCORE_ALIAS_EXACT;
                }
            }

            if (name.startsWith(query)) {
                return SCORE_NAME_PREFIX;
            }
            for (String alias : aliases) {
                if (alias.startsWith(query)) {
                    return SCORE_ALIAS_PREFIX;
                }
            }
            if (id.startsWith(query)) {
                return SCORE_ID_PREFIX;
            }

            if (name.contains(query)) {
                return SCORE_NAME_CONTAINS;
            }
            for (String alias : aliases) {
                if (alias.contains(query)) {
                    return SCORE_ALIAS_CONTAINS;
                }
            }
            if (id.contains(query)) {
                return SCORE_ID_CONTAINS;
            }

            return scoreByInitials(query);
        }

        int scoreByInitials(String query) {
            if (query.isEmpty()) {
                return 0;
            }
            if (initials.startsWith(query)) {
                return SCORE_INITIALS_PREFIX;
            }
            for (String aliasInitial : aliasInitials) {
                if (aliasInitial.startsWith(query)) {
                    return SCORE_INITIALS_PREFIX - 20;
                }
            }
            if (initials.contains(query)) {
                return SCORE_INITIALS_CONTAINS;
            }
            for (String aliasInitial : aliasInitials) {
                if (aliasInitial.contains(query)) {
                    return SCORE_INITIALS_CONTAINS - 20;
                }
            }
            return 0;
        }
    }
}