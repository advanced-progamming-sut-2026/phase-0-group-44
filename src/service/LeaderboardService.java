package service;

import model.Store;
import model.leaderboard.LeaderboardColumn;
import model.leaderboard.LeaderboardEntry;
import model.leaderboard.SortDirection;
import model.user.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds the global leaderboard from every persisted local profile. */
public class LeaderboardService {
    public List<LeaderboardEntry> getLeaderboard(LeaderboardColumn column, SortDirection direction) {
        LeaderboardColumn actualColumn = column == null ? LeaderboardColumn.PROGRESS : column;
        SortDirection actualDirection = direction == null ? SortDirection.DESC : direction;
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (User user : Store.getUsers()) {
            if (user != null && user.getUsername() != null) entries.add(new LeaderboardEntry(user));
        }
        entries.sort(comparator(actualColumn, actualDirection));
        return entries;
    }

    private Comparator<LeaderboardEntry> comparator(LeaderboardColumn column, SortDirection direction) {
        Comparator<LeaderboardEntry> primary;
        switch (column) {
            case MINIGAMES:
                primary = Comparator.comparingInt(LeaderboardEntry::getCompletedMiniGames);
                break;
            case DAILY_QUESTS:
                primary = Comparator.comparingInt(LeaderboardEntry::getCompletedDailyQuests);
                break;
            case NON_DAILY_QUESTS:
                primary = Comparator.comparingInt(LeaderboardEntry::getCompletedNonDailyQuests);
                break;
            case HIGHEST_SCORE:
                primary = Comparator.comparingInt(LeaderboardEntry::getHighestScore);
                break;
            case PROGRESS:
            default:
                primary = Comparator.comparingInt(LeaderboardEntry::getLatestCompletedChapter)
                        .thenComparingInt(LeaderboardEntry::getLatestCompletedLevel);
                break;
        }
        if (direction == SortDirection.DESC) primary = primary.reversed();
        return primary.thenComparing(LeaderboardEntry::getUsername, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(LeaderboardEntry::getUsername);
    }
}
