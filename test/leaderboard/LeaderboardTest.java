package leaderboard;

import controller.GameMenuController;
import controller.MainMenuController;
import model.Result;
import model.Store;
import model.enums.Command;
import model.user.User;
import model.utility.Leaderboard;
import model.utility.LeaderboardColumn;
import model.utility.LeaderboardEntry;
import model.utility.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.LeaderboardService;
import service.UserService;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardTest {
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private Path savePath;
    private UserService users;
    private LeaderboardService leaderboard;

    @BeforeEach
    void setUp() {
        savePath = tempDir.resolve("users.json");
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        users = new UserService(new JsonUserRepository(savePath), CLOCK);
        leaderboard = new LeaderboardService(users);

        register(profile("alpha", 2, 1, 3, 5, 1, 0));
        register(profile("bravo", 1, 4, 1, 7, 4, 900));
        register(profile("charlie", 2, 3, 2, 2, 6, 400));
        register(profile("delta", 0, 0, 5, 4, 3, 100));
    }

    @Test
    void sortsUsernameAscending() {
        assertOrder(LeaderboardColumn.USERNAME, SortDirection.ASCENDING,
                "alpha", "bravo", "charlie", "delta");
    }

    @Test
    void sortsUsernameDescending() {
        assertOrder(LeaderboardColumn.USERNAME, SortDirection.DESCENDING,
                "delta", "charlie", "bravo", "alpha");
    }

    @Test
    void sortsChapterAndLevelAscending() {
        assertOrder(LeaderboardColumn.PROGRESS, SortDirection.ASCENDING,
                "delta", "bravo", "alpha", "charlie");
    }

    @Test
    void sortsChapterAndLevelDescending() {
        assertOrder(LeaderboardColumn.PROGRESS, SortDirection.DESCENDING,
                "charlie", "alpha", "bravo", "delta");
    }

    @Test
    void sortsCompletedMinigamesAscending() {
        assertOrder(LeaderboardColumn.MINIGAMES, SortDirection.ASCENDING,
                "bravo", "charlie", "alpha", "delta");
    }

    @Test
    void sortsCompletedMinigamesDescending() {
        assertOrder(LeaderboardColumn.MINIGAMES, SortDirection.DESCENDING,
                "delta", "alpha", "charlie", "bravo");
    }

    @Test
    void sortsDailyQuestCountAscending() {
        assertOrder(LeaderboardColumn.DAILY_QUESTS, SortDirection.ASCENDING,
                "charlie", "delta", "alpha", "bravo");
    }

    @Test
    void sortsDailyQuestCountDescending() {
        assertOrder(LeaderboardColumn.DAILY_QUESTS, SortDirection.DESCENDING,
                "bravo", "alpha", "delta", "charlie");
    }

    @Test
    void sortsNonDailyQuestCountAscending() {
        assertOrder(LeaderboardColumn.NON_DAILY_QUESTS, SortDirection.ASCENDING,
                "alpha", "delta", "bravo", "charlie");
    }

    @Test
    void sortsNonDailyQuestCountDescending() {
        assertOrder(LeaderboardColumn.NON_DAILY_QUESTS, SortDirection.DESCENDING,
                "charlie", "bravo", "delta", "alpha");
    }

    @Test
    void sortsHighestScoreAscendingAndIncludesUnsetZero() {
        assertOrder(LeaderboardColumn.HIGHEST_SCORE, SortDirection.ASCENDING,
                "alpha", "delta", "charlie", "bravo");
        assertEquals(0, leaderboard.getLeaderboard(
                LeaderboardColumn.HIGHEST_SCORE,
                SortDirection.ASCENDING).getEntries().get(0).getHighestScore());
    }

    @Test
    void sortsHighestScoreDescending() {
        assertOrder(LeaderboardColumn.HIGHEST_SCORE, SortDirection.DESCENDING,
                "bravo", "charlie", "delta", "alpha");
    }

    @Test
    void equalPrimaryValuesAlwaysUseUsernameAscendingAsTieBreak() {
        for (User user : Store.getUsers()) {
            user.setCompletedDailyQuests(5);
        }

        assertOrder(LeaderboardColumn.DAILY_QUESTS, SortDirection.ASCENDING,
                "alpha", "bravo", "charlie", "delta");
        assertOrder(LeaderboardColumn.DAILY_QUESTS, SortDirection.DESCENDING,
                "alpha", "bravo", "charlie", "delta");
    }

    @Test
    void defaultLeaderboardUsesOverallProgressRatherThanScoredGameScore() {
        Leaderboard defaultView = leaderboard.getLeaderboard();

        assertEquals(LeaderboardColumn.PROGRESS, defaultView.getSortColumn());
        assertEquals(SortDirection.DESCENDING, defaultView.getSortDirection());
        assertEquals(List.of("charlie", "alpha", "bravo", "delta"), usernames(defaultView));
        assertEquals(0, find(defaultView, "alpha").getHighestScore(),
                "a profile does not need the bonus scored-game implementation");
    }

    @Test
    void allProfileColumnsSurviveRestartAndFeedTheLeaderboard() {
        UserService reloadedUsers = new UserService(new JsonUserRepository(savePath), CLOCK);
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        assertTrue(reloadedUsers.loadUsers().getStatus());

        Leaderboard restored = new LeaderboardService(reloadedUsers).getLeaderboard(
                LeaderboardColumn.PROGRESS, SortDirection.DESCENDING);
        LeaderboardEntry charlie = find(restored, "charlie");

        assertEquals(4, restored.getEntries().size());
        assertEquals(2, charlie.getLatestCompletedChapter());
        assertEquals(3, charlie.getLatestCompletedLevel());
        assertEquals(2, charlie.getCompletedMinigames());
        assertEquals(2, charlie.getCompletedDailyQuests());
        assertEquals(6, charlie.getCompletedNonDailyQuests());
        assertEquals(400, charlie.getHighestScore());
    }

    @Test
    void canonicalGameRouteAndMainCompatibilityRouteReturnTheSameGlobalData() {
        Store.setLoggedInUser(users.findByUsername("alpha"));
        Result<Leaderboard> fromGame = new GameMenuController(users)
                .leaderboard("daily-quests", "desc");
        Result<Leaderboard> fromMain = new MainMenuController(users)
                .leaderboard("daily-quests", "desc");

        assertTrue(fromGame.getStatus());
        assertTrue(fromMain.getStatus());
        assertEquals(usernames(fromGame.getData()), usernames(fromMain.getData()));
        assertEquals(4, fromGame.getData().getEntries().size());
    }

    @Test
    void leaderboardCommandKeepsDefaultFormAndAddsExplicitSortForm() {
        assertTrue(Command.MENU_LEADERBOARD.matches("menu leaderboard"));
        assertTrue(Command.MENU_LEADERBOARD_SORT.matches(
                "menu leaderboard -s non-daily-quests -o asc"));
        assertFalse(Command.MENU_LEADERBOARD_SORT.matches(
                "menu leaderboard -s score"));
    }

    @Test
    void invalidColumnAndDirectionAreRejectedWithoutDroppingUsers() {
        Store.setLoggedInUser(users.findByUsername("alpha"));
        GameMenuController controller = new GameMenuController(users);

        assertFalse(controller.leaderboard("coins", "asc").getStatus());
        assertFalse(controller.leaderboard("progress", "sideways").getStatus());
        assertEquals(4, Store.getUsers().size());
    }

    private void assertOrder(
            LeaderboardColumn column,
            SortDirection direction,
            String... expected
    ) {
        Leaderboard result = leaderboard.getLeaderboard(column, direction);
        assertEquals(List.of(expected), usernames(result));
    }

    private List<String> usernames(Leaderboard result) {
        return result.getEntries().stream().map(LeaderboardEntry::getUsername).toList();
    }

    private LeaderboardEntry find(Leaderboard result, String username) {
        return result.getEntries().stream()
                .filter(entry -> username.equals(entry.getUsername()))
                .findFirst()
                .orElseThrow();
    }

    private void register(User user) {
        assertTrue(users.addUser(user).getStatus());
    }

    private User profile(
            String username,
            int chapter,
            int level,
            int minigames,
            int daily,
            int nonDaily,
            int score
    ) {
        User user = new User();
        user.setUsername(username);
        user.applyDefaults();
        user.setLatestCompletedChapter(chapter);
        user.setLatestCompletedLevel(level);
        user.setCompletedMiniGames(minigames);
        user.setCompletedDailyQuests(daily);
        user.setCompletedNonDailyQuests(nonDaily);
        user.setHighestMewPoint(score);
        return user;
    }
}
