package com.myagree.app.common;

import java.util.Locale;

/** LIKE patterns for text search in JPQL queries, which must declare {@code escape '!'}. */
public final class SearchPatterns {

    private SearchPatterns() {
    }

    /**
     * Matches values containing {@code text}, ignoring case: compare {@code lower(column) like :pattern escape '!'}.
     * LIKE wildcards typed by the user ({@code %}, {@code _}) are matched literally.
     */
    public static String containsIgnoringCase(String text) {
        String escaped = text.strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
