package model.utility;

import java.util.List;

/** A sorted immutable snapshot of all registered local users. */
public final class Leaderboard {
    private final List<LeaderboardEntry> entries;
    private final LeaderboardColumn sortColumn;
    private final SortDirection sortDirection;

    /** Retains source compatibility with the original empty model stub. */
    public Leaderboard() {
        this(List.of(), LeaderboardColumn.PROGRESS, SortDirection.DESCENDING);
    }

    public Leaderboard(
            List<LeaderboardEntry> entries,
            LeaderboardColumn sortColumn,
            SortDirection sortDirection
    ) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
        this.sortColumn = sortColumn == null ? LeaderboardColumn.PROGRESS : sortColumn;
        this.sortDirection = sortDirection == null
                ? SortDirection.DESCENDING : sortDirection;
    }

    public List<LeaderboardEntry> getEntries() {
        return entries;
    }

    /** Compatibility with the original UML name. */
    public List<LeaderboardEntry> getTopPlayers() {
        return entries;
    }

    public LeaderboardColumn getSortColumn() {
        return sortColumn;
    }

    public SortDirection getSortDirection() {
        return sortDirection;
    }

    public String format() {
        StringBuilder output = new StringBuilder();
        output.append("leaderboard sorted by ")
                .append(sortColumn.getToken())
                .append(' ')
                .append(sortDirection.getToken())
                .append('\n');
        output.append("username | chapter-level | minigames | daily quests | non-daily quests | highest score");
        for (LeaderboardEntry entry : entries) {
            output.append('\n')
                    .append(entry.getUsername()).append(" | ")
                    .append(entry.getProgressLabel()).append(" | ")
                    .append(entry.getCompletedMinigames()).append(" | ")
                    .append(entry.getCompletedDailyQuests()).append(" | ")
                    .append(entry.getCompletedNonDailyQuests()).append(" | ")
                    .append(entry.getHighestScore());
        }
        return output.toString();
    }
}
