package minigame;

import controller.App;
import model.Store;
import model.enums.MenuName;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniGameTravelLogTest {
    @TempDir
    Path tempDir;

    private App app;

    @BeforeEach
    void setUp() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        Store.setActiveMiniGameSession(null);
        Store.setCurrentMenu(MenuName.REGISTER);
        Store.setRunning(true);
        app = new App(
                new JsonUserRepository(tempDir.resolve("users.json")),
                Clock.fixed(Instant.parse("2026-07-18T10:00:00Z"), ZoneOffset.UTC)
        );
        User user = new User();
        user.setUsername("traveler");
        app.getUserService().addUser(user);
        Store.setLoggedInUser(user);
        Store.setCurrentMenu(MenuName.TRAVEL_LOG);
    }

    @Test
    void minigamesCanOnlyBeSelectedFromTheirTravelLogPage() {
        assertFalse(app.getTravelController()
                .selectMiniGame("vase-breaker", 1).getStatus());

        String page = app.getTravelController().page("minigame").getMessage();
        assertTrue(page.contains("Vase Breaker"));
        assertTrue(page.contains("Bowling Wall-nut"));
        assertFalse(page.contains("Beghouled"));
        assertFalse(page.contains("Zombotany"));

        assertFalse(app.getTravelController()
                .selectMiniGame("Beghouled", 1).getStatus());
        assertTrue(app.getTravelController()
                .selectMiniGame("Vase Breaker", 1).getStatus());
        assertTrue(app.getTravelController().startMiniGame().getStatus());
        assertEquals(MenuName.MINIGAME, Store.getCurrentMenu());
    }
}
