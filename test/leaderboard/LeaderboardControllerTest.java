package leaderboard;

import controller.GameMenuController;
import model.Result;
import model.Store;
import model.leaderboard.LeaderboardEntry;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.UserService;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardControllerTest {
    @TempDir Path tempDir;
    private GameMenuController controller;

    @BeforeEach
    void setUp() {
        Store.setUsers(new ArrayList<>());
        User user = new User();
        user.setUsername("player");
        user.applyDefaults();
        Store.getUsers().add(user);
        Store.setLoggedInUser(user);
        UserService users = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")), Clock.systemUTC());
        controller = new GameMenuController(users);
    }

    @Test void defaultRouteShowsAllColumns() {
        Result<List<LeaderboardEntry>> result = controller.leaderboard();
        assertTrue(result.getStatus());
        assertTrue(result.getMessage().contains("chapter-level"));
        assertTrue(result.getMessage().contains("highest score"));
        assertTrue(result.getMessage().contains("player"));
    }

    @Test void rejectsUnknownSortInputs() {
        assertFalse(controller.leaderboard("coins", "desc").getStatus());
        assertFalse(controller.leaderboard("progress", "sideways").getStatus());
    }
}
