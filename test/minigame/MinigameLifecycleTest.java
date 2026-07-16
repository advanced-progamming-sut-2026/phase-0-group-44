package minigame;

import model.Store;
import model.miniGame.framework.*;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.NewsService;
import service.UserService;
import service.minigame.MinigameService;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class MinigameLifecycleTest {
    @TempDir Path tempDir;
    private User user;
    private UserService users;
    private MinigameService games;

    @BeforeEach void setUp() {
        Store.setUsers(new ArrayList<>());
        user = new User();
        user.setUsername("kiana");
        user.applyDefaults();
        Store.getUsers().add(user);
        users = new UserService(new JsonUserRepository(tempDir.resolve("users.json")), Clock.systemUTC());
        games = new MinigameService(users, new NewsService(users));
    }

    @Test void selectingStartingWinningAndProgressingAcrossThreeLevels() {
        assertTrue(games.unlock(user, MinigameId.BOWLING_WALLNUT));
        assertEquals(1, games.travelLogPage(user).size());
        assertEquals(1, user.getNewsList().size());

        for (int level = 1; level <= 3; level++) {
            MinigameSession selected = games.select(user, MinigameId.BOWLING_WALLNUT, level, new RecordingStrategy());
            assertEquals(MinigameRunState.SELECTED, selected.getState());
            games.start();
            assertEquals(MinigameRunState.RUNNING, selected.getState());
            games.win(user);
            assertEquals(MinigameRunState.WON, selected.getState());
            assertEquals(level, user.getCompletedMiniGames());
        }
        assertEquals(3, user.getMinigameProgress().get("bowling-wallnut").getCompletedLevels().size());
    }

    @Test void losingDoesNotCompleteOrUnlockNextLevel() {
        games.unlock(user, MinigameId.CONVEYOR_BELT);
        MinigameSession session = games.select(user, MinigameId.CONVEYOR_BELT, 1, new RecordingStrategy());
        games.start();
        games.lose(user);
        assertEquals(MinigameRunState.LOST, session.getState());
        assertEquals(0, user.getCompletedMiniGames());
        assertEquals(1, user.getMinigameProgress().get("conveyor-belt").getHighestUnlockedLevel());
        assertThrows(IllegalStateException.class,
                () -> games.select(user, MinigameId.CONVEYOR_BELT, 2, new RecordingStrategy()));
    }

    @Test void replayedCompletionIsIdempotent() {
        games.unlock(user, MinigameId.VASEBREAKER);
        complete(MinigameId.VASEBREAKER, 1);
        complete(MinigameId.VASEBREAKER, 1);
        assertEquals(1, user.getCompletedMiniGames());
    }

    @Test void progressAndUnlocksSurviveRestart() {
        games.unlock(user, MinigameId.VASEBREAKER);
        complete(MinigameId.VASEBREAKER, 1);
        users.saveUsers();

        Store.setUsers(new ArrayList<>());
        UserService restoredUsers = new UserService(new JsonUserRepository(tempDir.resolve("users.json")), Clock.systemUTC());
        restoredUsers.loadUsers();
        User restored = Store.findUser("kiana");
        assertNotNull(restored);
        assertTrue(restored.getMinigameProgress().get("vasebreaker").isUnlocked());
        assertEquals(2, restored.getMinigameProgress().get("vasebreaker").getHighestUnlockedLevel());
        assertEquals(1, restored.getCompletedMiniGames());
    }

    @Test void everyMandatoryGameHasExactlyThreeExplicitlyHarderLevels() {
        MinigameCatalog catalog = new MinigameCatalog();
        assertEquals(3, catalog.all().size());
        for (MinigameDefinition game : catalog.all()) {
            assertEquals(3, game.getLevels().size());
            MinigameLevelConfig one = game.level(1), two = game.level(2), three = game.level(3);
            assertTrue(two.getWaveCount() > one.getWaveCount());
            assertTrue(three.getWaveCount() > two.getWaveCount());
            assertTrue(two.getSpawnIntervalTicks() < one.getSpawnIntervalTicks());
            assertTrue(three.getSpawnIntervalTicks() < two.getSpawnIntervalTicks());
            assertTrue(two.getZombieHealthMultiplier() > one.getZombieHealthMultiplier());
            assertTrue(three.getZombieHealthMultiplier() > two.getZombieHealthMultiplier());
        }
    }

    @Test void commandExtensionPointDelegatesWithoutModeConditionals() {
        games.unlock(user, MinigameId.BOWLING_WALLNUT);
        MinigameSession session = games.select(user, MinigameId.BOWLING_WALLNUT, 1, new RecordingStrategy());
        session.addCommandHandler(new MinigameCommandHandler() {
            public boolean supports(String command) { return command.equals("roll"); }
            public String execute(MinigameSession ignored, String command) { return "rolled"; }
        });
        assertEquals("rolled", games.extensionCommand("roll"));
        assertThrows(IllegalArgumentException.class, () -> games.extensionCommand("unknown"));
    }

    private void complete(MinigameId id, int level) {
        games.select(user, id, level, new RecordingStrategy());
        games.start();
        games.win(user);
    }

    private static final class RecordingStrategy implements MinigameStrategy { }
}
