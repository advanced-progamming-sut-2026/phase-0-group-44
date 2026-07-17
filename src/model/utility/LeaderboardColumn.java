package model.utility;

import java.util.Locale;

/** Columns available to the global local-user leaderboard. */
public enum LeaderboardColumn {
    USERNAME("username"),
    PROGRESS("progress"),
    MINIGAMES("minigames"),
    DAILY_QUESTS("daily-quests"),
    NON_DAILY_QUESTS("non-daily-quests"),
    HIGHEST_SCORE("highest-score");

    private final String token;

    LeaderboardColumn(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    /** Resolves command-friendly aliases without changing the canonical token. */
    public static LeaderboardColumn fromToken(String input) {
        if (input == null) {
            return null;
        }
        String normalized = normalize(input);
        for (LeaderboardColumn column : values()) {
            if (normalize(column.token).equals(normalized)) {
                return column;
            }
        }
        return switch (normalized) {
            case "chapter", "chapterlevel", "level", "latest", "latestcompleted" -> PROGRESS;
            case "minigame", "completedminigames" -> MINIGAMES;
            case "daily", "dailyquest", "completedailyquests" -> DAILY_QUESTS;
            case "nondaily", "nondailyquest", "completednondailyquests" -> NON_DAILY_QUESTS;
            case "score", "mewpoint", "miopoint", "highestmewpoint" -> HIGHEST_SCORE;
            default -> null;
        };
    }

    private static String normalize(String input) {
        return input.trim().toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");
    }
}
