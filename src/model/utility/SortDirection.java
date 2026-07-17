package model.utility;

import java.util.Locale;

/** Direction of a leaderboard's primary sort. */
public enum SortDirection {
    ASCENDING("asc"),
    DESCENDING("desc");

    private final String token;

    SortDirection(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public static SortDirection fromToken(String input) {
        if (input == null) {
            return null;
        }
        return switch (input.trim().toLowerCase(Locale.ROOT)) {
            case "asc", "ascending" -> ASCENDING;
            case "desc", "descending" -> DESCENDING;
            default -> null;
        };
    }
}
