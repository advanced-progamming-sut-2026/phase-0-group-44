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

/**
 * Builds deterministic global leaderboard snapshots
 * from persisted profiles.
 */
public final class LeaderboardService {

    private final UserService users;

    public LeaderboardService(UserService users) {

        if (users == null) {
            throw new IllegalArgumentException(
                    "User service is required."
            );
        }

        this.users = users;
    }

    /**
     * Default leaderboard:
     * progress, highest to lowest.
     */
    public Leaderboard getLeaderboard() {

        return getLeaderboard(
                LeaderboardColumn.PROGRESS,
                SortDirection.DESCENDING
        );
    }

    /**
     * Builds the leaderboard using the selected
     * column and direction.
     */
    public Leaderboard getLeaderboard(
            LeaderboardColumn column,
            SortDirection direction
    ) {

        /*
         * If null is passed, fall back to defaults.
         */
        LeaderboardColumn resolvedColumn =
                column == null
                        ? LeaderboardColumn.PROGRESS
                        : column;

        SortDirection resolvedDirection =
                direction == null
                        ? SortDirection.DESCENDING
                        : direction;

        /*
         * Build leaderboard entries from
         * every registered user.
         */
        List<LeaderboardEntry> entries =
                new ArrayList<>();

        for (User user : users.getRegisteredUsers()) {

            if (user == null) {
                continue;
            }

            if (user.getUsername() == null) {
                continue;
            }

            entries.add(
                    LeaderboardEntry.fromUser(user)
            );
        }

        /*
         * Sort using the selected settings.
         */
        entries.sort(
                comparator(
                        resolvedColumn,
                        resolvedDirection
                )
        );

        /*
         * Return an immutable leaderboard snapshot.
         */
        return new Leaderboard(
                entries,
                resolvedColumn,
                resolvedDirection
        );
    }

    /**
     * Creates the comparator for the selected
     * leaderboard column.
     *
     * Numeric fields:
     *
     * ASCENDING:
     * lowest -> highest
     *
     * DESCENDING:
     * highest -> lowest
     *
     * When two players have the same value,
     * usernames are always sorted A -> Z.
     *
     * Username itself is handled separately,
     * because ascending means A -> Z and
     * descending means Z -> A.
     */
    private Comparator<LeaderboardEntry> comparator(
            LeaderboardColumn column,
            SortDirection direction
    ) {

        Comparator<LeaderboardEntry> username =
                usernameComparator();

        /*
         * USERNAME SORT
         *
         * Username is special because username is
         * normally the tie-breaker for every other
         * column.
         */
        if (column == LeaderboardColumn.USERNAME) {

            if (direction == SortDirection.DESCENDING) {

                return username.reversed();
            }

            return username;
        }

        /*
         * PRIMARY COMPARATOR
         */
        Comparator<LeaderboardEntry> primary =
                switch (column) {

                    /*
                     * Adventure progress.
                     *
                     * Compare chapter first.
                     * If chapters are equal,
                     * compare level.
                     */
                    case PROGRESS ->
                            Comparator
                                    .comparingInt(
                                            LeaderboardEntry::
                                                    getLatestCompletedChapter
                                    )
                                    .thenComparingInt(
                                            LeaderboardEntry::
                                                    getLatestCompletedLevel
                                    );

                    /*
                     * Completed minigames.
                     */
                    case MINIGAMES ->
                            Comparator.comparingInt(
                                    LeaderboardEntry::
                                            getCompletedMinigames
                            );

                    /*
                     * Completed daily quests.
                     */
                    case DAILY_QUESTS ->
                            Comparator.comparingInt(
                                    LeaderboardEntry::
                                            getCompletedDailyQuests
                            );

                    /*
                     * Completed non-daily quests.
                     */
                    case NON_DAILY_QUESTS ->
                            Comparator.comparingInt(
                                    LeaderboardEntry::
                                            getCompletedNonDailyQuests
                            );

                    /*
                     * Highest scored-game score.
                     */
                    case HIGHEST_SCORE ->
                            Comparator.comparingInt(
                                    LeaderboardEntry::
                                            getHighestScore
                            );

                    /*
                     * USERNAME was already handled above.
                     */
                    case USERNAME ->
                            throw new IllegalStateException(
                                    "Username comparator should have been handled separately."
                            );
                };

        /*
         * DESCENDING:
         *
         * Reverse ONLY the primary value.
         *
         * Example:
         *
         * Score:
         * 500
         * 300
         * 100
         */
        if (direction == SortDirection.DESCENDING) {

            primary = primary.reversed();
        }

        /*
         * IMPORTANT:
         *
         * The username tie-break stays A -> Z,
         * regardless of whether the primary
         * sort is ascending or descending.
         *
         * Example:
         *
         * Amir  -> 300
         * Reza  -> 300
         *
         * In both ascending and descending,
         * Amir appears before Reza when the
         * score is tied.
         */
        return primary.thenComparing(
                username
        );
    }

    /**
     * Case-insensitive username comparator.
     *
     * First:
     * compare lowercase usernames.
     *
     * Then:
     * compare the original username so the
     * result is deterministic even for names
     * that only differ by capitalization.
     */
    private Comparator<LeaderboardEntry> usernameComparator() {

        return Comparator
                .comparing(
                        (LeaderboardEntry entry) ->
                                entry
                                        .getUsername()
                                        .toLowerCase(
                                                Locale.ROOT
                                        )
                )
                .thenComparing(
                        LeaderboardEntry::getUsername
                );
    }
}