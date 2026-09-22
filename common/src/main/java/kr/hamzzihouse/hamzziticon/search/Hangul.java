package kr.hamzzihouse.hamzziticon.search;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Hangul {

    private final char SYLLABLE_BASE = 0xAC00;
    private final char SYLLABLE_LAST = 0xD7A3;

    private final int MEDIAL_COUNT = 21;
    private final int FINAL_COUNT = 28;

    private final char[] INITIALS = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    private final char COMPAT_JAMO_FIRST = 0x3131;
    private final char COMPAT_JAMO_LAST = 0x318E;

    public boolean isSyllable(char character) {
        return character >= SYLLABLE_BASE && character <= SYLLABLE_LAST;
    }

    public boolean isCompatibilityJamo(char character) {
        return character >= COMPAT_JAMO_FIRST && character <= COMPAT_JAMO_LAST;
    }

    @NotNull
    public String initials(@NotNull String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (isSyllable(character)) {
                builder.append(INITIALS[(character - SYLLABLE_BASE) / (MEDIAL_COUNT * FINAL_COUNT)]);
            } else if (isCompatibilityJamo(character)) {
                builder.append(character);
            } else if (!Character.isWhitespace(character)) {
                builder.append(Character.toLowerCase(character));
            }
        }
        return builder.toString();
    }

    public boolean isInitialsQuery(@NotNull String query) {
        if (query.isEmpty()) {
            return false;
        }
        boolean sawJamo = false;
        for (int index = 0; index < query.length(); index++) {
            char character = query.charAt(index);
            if (isCompatibilityJamo(character)) {
                sawJamo = true;
            } else if (!Character.isWhitespace(character)) {
                return false;
            }
        }
        return sawJamo;
    }
}