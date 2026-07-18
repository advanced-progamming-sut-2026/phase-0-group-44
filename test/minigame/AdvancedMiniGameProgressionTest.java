package minigame;

import controller.App;
import model.Store;
import model.miniGame.MiniGameId;
import model.miniGame.MiniGameProgress;
import model.sim.GameOutcome;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedMiniGameProgressionTest {
    @TempDir Path tempDir;

    @BeforeEach void reset() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        Store.setActiveMiniGameSession(null);
    }

    @Test
    void beghouledAndZombotanyProgressAcrossAllThreeLevels() {
        App app = new App(new JsonUserRepository(tempDir.resolve("users.json")), Clock.systemUTC());
        User user = new User(); user.setUsername("matcher");
        user.getCollection().purchasePlant(model.enums.PlantType.PEASHOOTER);
        app.getUserService().addUser(user);
        Store.setLoggedInUser(user);
        user.getMiniGameProgress().put(MiniGameId.BEGHOULED.getToken(),
                MiniGameProgress.unlockedAtLevelOne());

        for (int level = 1; level <= 3; level++) {
            assertTrue(app.getMiniGameController().select(user, "beghouled", level).getStatus());
            assertTrue(app.getMiniGameController().start(user).getStatus());
            assertTrue(app.getMiniGameController().finish(user, GameOutcome.WON).getStatus());
        }
        assertTrue(user.getMiniGameProgress().get(MiniGameId.ZOMBOTANY.getToken())
                .isLevelUnlocked(1));

        for (int level = 1; level <= 3; level++) {
            assertTrue(app.getMiniGameController().select(user, "zombotany", level).getStatus());
            assertTrue(app.getMiniGameController()
                    .executeStrategyCommand(user, "add plant -t peashooter").getStatus());
            assertTrue(app.getMiniGameController().start(user).getStatus());
            assertTrue(app.getMiniGameController().finish(user, GameOutcome.WON).getStatus());
        }
        assertEquals(2, user.getCompletedMiniGames());
        assertTrue(user.getMiniGameProgress().get(MiniGameId.ZOMBOTANY.getToken())
                .isFullyCompleted());
    }
}
