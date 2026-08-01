package model.utility;

import model.user.User;

/** Immutable leaderboard projection of one persisted registered user. */
public final class LeaderboardEntry {
    private final String username;
    private final int latestCompletedChapter;
    private final int latestCompletedLevel;
    private final int completedMinigames;
    private final int completedDailyQuests;
    private final int completedNonDailyQuests;
    private final int highestScore;

    /** Retains source compatibility with the original empty model stub. */
    public LeaderboardEntry() {
        this("", 0, 0, 0, 0, 0, 0);
    }

    public LeaderboardEntry(User user) {
        this(
                required(user).getUsername(),
                user.getLatestCompletedChapter(),
                user.getLatestCompletedLevel(),
                user.getCompletedMiniGames(),
                user.getCompletedDailyQuests(),
                user.getCompletedNonDailyQuests(),
                user.getHighestMewPoint()
        );
    }

    public LeaderboardEntry(
            String username,
            int latestCompletedChapter,
            int latestCompletedLevel,
            int completedMinigames,
            int completedDailyQuests,
            int completedNonDailyQuests,
            int highestScore
    ) {
        this.username = username == null ? "" : username;
        this.latestCompletedChapter = nonNegative(latestCompletedChapter);
        this.latestCompletedLevel = nonNegative(latestCompletedLevel);
        this.completedMinigames = nonNegative(completedMinigames);
        this.completedDailyQuests = nonNegative(completedDailyQuests);
        this.completedNonDailyQuests = nonNegative(completedNonDailyQuests);
        this.highestScore = nonNegative(highestScore);
    }

    public static LeaderboardEntry fromUser(User user) {
        return new LeaderboardEntry(user);
    }

    public String getUsername() {
        return username;
    }

    public int getLatestCompletedChapter() {
        return latestCompletedChapter;
    }

    public int getLatestCompletedLevel() {
        return latestCompletedLevel;
    }

    public int getCompletedMinigames() {
        return completedMinigames;
    }

    /** Alias matching the profile field's original camel-casing. */
    public int getCompletedMiniGames() {
        return completedMinigames;
    }

    public int getCompletedDailyQuests() {
        return completedDailyQuests;
    }

    public int getCompletedNonDailyQuests() {
        return completedNonDailyQuests;
    }

    public int getHighestScore() {
        return highestScore;
    }

    /** Human-readable progress cell; zero means no adventure level completed yet. */
    public String getProgressLabel() {
        if (latestCompletedChapter == 0 && latestCompletedLevel == 0) {
            return "none";
        }
        return latestCompletedChapter + "-" + latestCompletedLevel;
    }

    private static User required(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User is required.");
        }
        return user;
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }
}
