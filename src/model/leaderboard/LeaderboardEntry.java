package model.leaderboard;

import model.user.User;

public final class LeaderboardEntry {
    private final String username;
    private final int latestCompletedChapter;
    private final int latestCompletedLevel;
    private final int completedMiniGames;
    private final int completedDailyQuests;
    private final int completedNonDailyQuests;
    private final int highestScore;

    public LeaderboardEntry(User user) {
        username = user.getUsername();
        latestCompletedChapter = user.getLatestCompletedChapter();
        latestCompletedLevel = user.getLatestCompletedLevel();
        completedMiniGames = user.getCompletedMiniGames();
        completedDailyQuests = user.getCompletedDailyQuests();
        completedNonDailyQuests = user.getCompletedNonDailyQuests();
        highestScore = user.getHighestMewPoint();
    }

    public String getUsername() { return username; }
    public int getLatestCompletedChapter() { return latestCompletedChapter; }
    public int getLatestCompletedLevel() { return latestCompletedLevel; }
    public int getCompletedMiniGames() { return completedMiniGames; }
    public int getCompletedDailyQuests() { return completedDailyQuests; }
    public int getCompletedNonDailyQuests() { return completedNonDailyQuests; }
    public int getHighestScore() { return highestScore; }

    public String progressLabel() { return latestCompletedChapter + "-" + latestCompletedLevel; }
}
