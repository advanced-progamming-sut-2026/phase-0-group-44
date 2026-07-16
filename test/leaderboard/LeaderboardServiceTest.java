package leaderboard;

import model.Store;
import model.leaderboard.LeaderboardColumn;
import model.leaderboard.LeaderboardEntry;
import model.leaderboard.SortDirection;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.LeaderboardService;
import service.UserService;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaderboardServiceTest {
    @TempDir Path tempDir;
    private LeaderboardService leaderboard;

    @BeforeEach
    void setUp() {
        Store.setUsers(new ArrayList<>());
        Store.getUsers().add(user("amy", 1, 4, 3, 8, 2, 0));
        Store.getUsers().add(user("bob", 2, 1, 5, 2, 7, 900));
        Store.getUsers().add(user("cara", 2, 1, 1, 8, 4, 300));
        Store.getUsers().add(user("dave", 1, 3, 5, 1, 7, 900));
        leaderboard = new LeaderboardService();
    }

    @Test void sortsProgressAscendingAndDescending() {
        assertOrder(LeaderboardColumn.PROGRESS, SortDirection.ASC, "dave", "amy", "bob", "cara");
        assertOrder(LeaderboardColumn.PROGRESS, SortDirection.DESC, "bob", "cara", "amy", "dave");
    }

    @Test void sortsMinigamesAscendingAndDescending() {
        assertOrder(LeaderboardColumn.MINIGAMES, SortDirection.ASC, "cara", "amy", "bob", "dave");
        assertOrder(LeaderboardColumn.MINIGAMES, SortDirection.DESC, "bob", "dave", "amy", "cara");
    }

    @Test void sortsDailyQuestsAscendingAndDescending() {
        assertOrder(LeaderboardColumn.DAILY_QUESTS, SortDirection.ASC, "dave", "bob", "amy", "cara");
        assertOrder(LeaderboardColumn.DAILY_QUESTS, SortDirection.DESC, "amy", "cara", "bob", "dave");
    }

    @Test void sortsNonDailyQuestsAscendingAndDescending() {
        assertOrder(LeaderboardColumn.NON_DAILY_QUESTS, SortDirection.ASC, "amy", "cara", "bob", "dave");
        assertOrder(LeaderboardColumn.NON_DAILY_QUESTS, SortDirection.DESC, "bob", "dave", "cara", "amy");
    }

    @Test void sortsHighestScoreAscendingAndDescendingAndAllowsZero() {
        assertOrder(LeaderboardColumn.HIGHEST_SCORE, SortDirection.ASC, "amy", "cara", "bob", "dave");
        assertOrder(LeaderboardColumn.HIGHEST_SCORE, SortDirection.DESC, "bob", "dave", "cara", "amy");
    }

    @Test void leaderboardUsesProfilesReloadedFromPersistence() {
        Path save = tempDir.resolve("users.json");
        UserService writer = new UserService(new JsonUserRepository(save), Clock.systemUTC());
        writer.saveUsers();

        Store.setUsers(new ArrayList<>());
        UserService reader = new UserService(new JsonUserRepository(save), Clock.systemUTC());
        reader.loadUsers();

        List<LeaderboardEntry> entries = new LeaderboardService().getLeaderboard(
                LeaderboardColumn.HIGHEST_SCORE, SortDirection.DESC);
        assertEquals(List.of("bob", "dave", "cara", "amy"), usernames(entries));
        assertEquals(0, entries.get(3).getHighestScore());
    }

    private void assertOrder(LeaderboardColumn column, SortDirection direction, String... expected) {
        assertEquals(List.of(expected), usernames(leaderboard.getLeaderboard(column, direction)));
    }

    private List<String> usernames(List<LeaderboardEntry> entries) {
        return entries.stream().map(LeaderboardEntry::getUsername).toList();
    }

    private User user(String name, int chapter, int level, int minigames,
                      int daily, int nonDaily, int score) {
        User user = new User();
        user.setUsername(name);
        user.setLatestCompletedChapter(chapter);
        user.setLatestCompletedLevel(level);
        user.setCompletedMiniGames(minigames);
        user.setCompletedDailyQuests(daily);
        user.setCompletedNonDailyQuests(nonDaily);
        user.setHighestMewPoint(score);
        user.applyDefaults();
        return user;
    }
}
