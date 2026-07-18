package scored;

import model.Store;
import model.enums.MenuName;
import model.menu.MenuGraph;
import model.scored.*;
import model.sim.GameOutcome;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.ScoredGameService;
import service.UserService;

import java.nio.file.Path;
import java.time.*;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ScoredGameTest {
    @TempDir Path tempDir;

    @BeforeEach void reset() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        Store.setActiveScoredGameSession(null);
    }

    @Test
    void everyUserReceivesTheSameDateSeededSequence() {
        DailyZombieSequenceGenerator generator = new DailyZombieSequenceGenerator();
        LocalDate date = LocalDate.of(2026, 7, 18);
        assertEquals(generator.generate(date), generator.generate(date));
        assertNotEquals(generator.generate(date).spawns(), generator.generate(date.plusDays(1)).spawns());
        assertEquals(30, generator.generate(date).spawns().size());
    }

    @Test
    void fiveIndependentPatternsContributeToMewPoint() {
        ScoredGameScore score = new ScoredGameScore();
        score.recordQuickKill(10, 30);
        score.recordProjectileKillCount(2);
        score.recordSimultaneousKillCount(2);
        assertTrue(score.finalizeScore(GameOutcome.WON, 123, 0, false));
        assertEquals(300, score.getPoints(ScorePattern.QUICK_KILL));
        assertEquals(200, score.getPoints(ScorePattern.MULTI_KILL_PROJECTILE));
        assertEquals(300, score.getPoints(ScorePattern.SIMULTANEOUS_KILLS));
        assertEquals(123, score.getPoints(ScorePattern.SUN_EFFICIENCY));
        assertEquals(1000, score.getPoints(ScorePattern.PERFECT_DEFENSE));
        assertFalse(score.finalizeScore(GameOutcome.WON, 999, 0, false));
    }

    @Test
    void highestScorePersistsAndNeverMovesBackward() {
        Path save = tempDir.resolve("users.json");
        Clock clock = Clock.fixed(Instant.parse("2026-07-18T08:00:00Z"), ZoneOffset.UTC);
        UserService users = new UserService(new JsonUserRepository(save), clock);
        User user = new User(); user.setUsername("scorer"); users.addUser(user);
        ScoredGameService service = new ScoredGameService(users, clock);
        assertTrue(service.recordHighestScore(user, 900));
        assertFalse(service.recordHighestScore(user, 500));

        Store.setUsers(new ArrayList<>());
        UserService reloaded = new UserService(new JsonUserRepository(save), clock);
        reloaded.loadUsers();
        assertEquals(900, reloaded.findByUsername("scorer").getHighestMewPoint());
    }

    @Test
    void mainAndGameBothExposeTheCompatibilityRoute() {
        assertTrue(MenuGraph.canEnter(MenuName.MAIN, MenuName.SCORED_GAME));
        assertTrue(MenuGraph.canEnter(MenuName.GAME, MenuName.SCORED_GAME));
    }
}
