package service;

import model.user.User;
import model.utility.Leaderboard;
import model.utility.LeaderboardColumn;
import model.utility.LeaderboardEntry;
import model.utility.SortDirection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Builds deterministic global leaderboard snapshots from persisted profiles. */
public final class LeaderboardService {
    private final UserService users;

    public LeaderboardService(UserService users) {
        if (users == null) {
            throw new IllegalArgumentException("User service is required.");
        }
        this.users = users;
    }

    /** Default view emphasizes overall adventure progress rather than bonus score. */
    public Leaderboard getLeaderboard() {
        return getLeaderboard(LeaderboardColumn.PROGRESS, SortDirection.DESCENDING);
    }

    public Leaderboard getLeaderboard(
            LeaderboardColumn column,
            SortDirection direction
    ) {
        LeaderboardColumn resolvedColumn = column == null
                ? LeaderboardColumn.PROGRESS : column;
        SortDirection resolvedDirection = direction == null
                ? SortDirection.DESCENDING : direction;

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (User user : users.getRegisteredUsers()) {
            if (user != null && user.getUsername() != null) {
                entries.add(LeaderboardEntry.fromUser(user));
            }
        }
        entries.sort(comparator(resolvedColumn, resolvedDirection));
        return new Leaderboard(entries, resolvedColumn, resolvedDirection);
    }

    private Comparator<LeaderboardEntry> comparator(
            LeaderboardColumn column,
            SortDirection direction
    ) {
        Comparator<LeaderboardEntry> primary = switch (column) {
            case USERNAME -> usernameComparator();
            case PROGRESS -> Comparator
                    .comparingInt(LeaderboardEntry::getLatestCompletedChapter)
                    .thenComparingInt(LeaderboardEntry::getLatestCompletedLevel);
            case MINIGAMES -> Comparator.comparingInt(LeaderboardEntry::getCompletedMinigames);
            case DAILY_QUESTS -> Comparator.comparingInt(
                    LeaderboardEntry::getCompletedDailyQuests);
            case NON_DAILY_QUESTS -> Comparator.comparingInt(
                    LeaderboardEntry::getCompletedNonDailyQuests);
            case HIGHEST_SCORE -> Comparator.comparingInt(LeaderboardEntry::getHighestScore);
        };

        if (direction == SortDirection.DESCENDING) {
            primary = primary.reversed();
        }

        // Primary direction never changes the deterministic tie-break: username ascending.
        return primary.thenComparing(usernameComparator());
    }

    private Comparator<LeaderboardEntry> usernameComparator() {
        return Comparator
                .comparing((LeaderboardEntry entry) ->
                        entry.getUsername().toLowerCase(Locale.ROOT))
                .thenComparing(LeaderboardEntry::getUsername);
    }
}
