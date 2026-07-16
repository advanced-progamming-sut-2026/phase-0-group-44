package model.leaderboard;

public enum LeaderboardColumn {
    PROGRESS("progress"),
    MINIGAMES("minigames"),
    DAILY_QUESTS("daily-quests"),
    NON_DAILY_QUESTS("non-daily-quests"),
    HIGHEST_SCORE("highest-score");

    private final String token;

    LeaderboardColumn(String token) { this.token = token; }
    public String getToken() { return token; }

    public static LeaderboardColumn fromToken(String input) {
        if (input == null || input.isBlank()) return PROGRESS;
        String value = input.trim().toLowerCase().replace('_', '-').replace(' ', '-');
        for (LeaderboardColumn column : values()) {
            if (column.token.equals(value)) return column;
        }
        if ("score".equals(value)) return HIGHEST_SCORE;
        if ("daily".equals(value)) return DAILY_QUESTS;
        if ("non-daily".equals(value)) return NON_DAILY_QUESTS;
        return null;
    }
}
