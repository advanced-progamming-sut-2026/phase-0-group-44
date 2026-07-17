package minigame;

import model.Result;
import model.Store;
import model.enums.MenuName;
import model.enums.NewsType;
import model.events.DomainEventBus;
import model.miniGame.MiniGame;
import model.miniGame.MiniGameCatalog;
import model.miniGame.MiniGameCommandExtension;
import model.miniGame.MiniGameId;
import model.miniGame.MiniGameLifecycleState;
import model.miniGame.MiniGameProgress;
import model.miniGame.MiniGameSession;
import model.miniGame.MiniGameSessionFactory;
import model.sim.GameOutcome;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.DomainEventPublisher;
import service.MiniGameService;
import service.NewsService;
import service.UserService;
import util.SeededRandomSource;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniGameLifecycleTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-18T10:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private UserService users;
    private MiniGameService service;
    private User user;

    @BeforeEach
    void setUp() {
        resetStore();
        users = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")), CLOCK);
        service = serviceWith(new MiniGameSessionFactory(new SeededRandomSource()));
        user = new User();
        user.setUsername("player");
        users.addUser(user);
        Store.setLoggedInUser(user);
        Store.setCurrentMenu(MenuName.TRAVEL_LOG);
    }

    @Test
    void selectingStartingAndLosingDoNotAdvanceProgress() {
        assertFalse(service.select(user, "vase breaker", 2).getStatus());

        Result<MiniGameSession> selected = service.select(user, "vase breaker", 1);
        assertTrue(selected.getStatus());
        assertEquals(MiniGameLifecycleState.SELECTED, selected.getData().getState());
        assertEquals(5, selected.getData().getSimulation().getWorld().getRows());
        assertEquals(9, selected.getData().getSimulation().getWorld().getColumns());

        assertTrue(service.start(user).getStatus());
        assertEquals(MenuName.MINIGAME, Store.getCurrentMenu());
        assertEquals(MiniGameLifecycleState.RUNNING,
                Store.getActiveMiniGameSession().getState());
        assertTrue(service.advance(user, 1).getStatus());

        assertTrue(service.finish(user, GameOutcome.LOST).getStatus());
        assertEquals(MenuName.TRAVEL_LOG, Store.getCurrentMenu());
        assertNull(Store.getActiveMiniGameSession());
        assertFalse(progress(user, MiniGameId.VASE_BREAKER).isLevelCompleted(1));
        assertEquals(0, user.getCompletedMiniGames());
    }

    @Test
    void firstWinsProgressThroughThreeLevelsAndCountTheMinigameOnce() {
        win("vase-breaker", 1);
        assertTrue(progress(user, MiniGameId.VASE_BREAKER).isLevelUnlocked(2));
        assertEquals(0, user.getCompletedMiniGames());

        win("vase-breaker", 2);
        assertTrue(progress(user, MiniGameId.VASE_BREAKER).isLevelUnlocked(3));
        assertEquals(0, user.getCompletedMiniGames());

        win("vase-breaker", 3);
        assertEquals(1, user.getCompletedMiniGames());
        assertTrue(progress(user, MiniGameId.BOWLING_WALLNUT).isLevelUnlocked(1));
        assertEquals(1, user.getNewsList().stream()
                .filter(item -> item.getType() == NewsType.MINIGAME_UNLOCKED)
                .count());

        win("vase-breaker", 3);
        assertEquals(1, user.getCompletedMiniGames());
        assertEquals(1, user.getNewsList().stream()
                .filter(item -> item.getType() == NewsType.MINIGAME_UNLOCKED)
                .count());

        win("bowling-wallnut", 1);
        win("bowling-wallnut", 2);
        win("bowling-wallnut", 3);
        assertEquals(2, user.getCompletedMiniGames());
    }

    @Test
    void progressionSurvivesRestartAndUnlocksTheNextLevel() {
        win("vase-breaker", 1);

        resetStore();
        UserService reloaded = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")), CLOCK);
        assertTrue(reloaded.loadUsers().getStatus());
        User restored = reloaded.findByUsername("player");
        MiniGameService restoredService = new MiniGameService(
                MiniGameCatalog.mandatoryDefaults(),
                new MiniGameSessionFactory(new SeededRandomSource()),
                reloaded,
                new NewsService(reloaded),
                new DomainEventPublisher(new DomainEventBus(), reloaded)
        );

        MiniGameProgress restoredProgress = restored.getMiniGameProgress()
                .get(MiniGameId.VASE_BREAKER.getToken());
        assertTrue(restoredProgress.isLevelCompleted(1));
        assertTrue(restoredProgress.isLevelUnlocked(2));
        assertTrue(restoredService.select(restored, "vase-breaker", 2).getStatus());
    }

    @Test
    void strategyCommandExtensionCanFinishWithoutModeConditionals() {
        MiniGameSessionFactory factory = new MiniGameSessionFactory(new SeededRandomSource());
        factory.register(new MiniGame() {
            @Override
            public MiniGameId getId() {
                return MiniGameId.VASE_BREAKER;
            }

            @Override
            public List<MiniGameCommandExtension> commandExtensions() {
                return List.of(new MiniGameCommandExtension() {
                    @Override
                    public boolean supports(String input) {
                        return "objective complete".equals(input);
                    }

                    @Override
                    public Result<String> execute(MiniGameSession session, String input) {
                        session.getSimulation().getWorld().setOutcome(GameOutcome.WON);
                        Result<String> result = new Result<>();
                        result.setStatus(true);
                        result.setData("completed");
                        result.appendToMessage("objective completed");
                        return result;
                    }
                });
            }
        });
        service = serviceWith(factory);

        assertTrue(service.select(user, "vase-breaker", 1).getStatus());
        assertTrue(service.start(user).getStatus());
        assertTrue(service.executeStrategyCommand(user, "objective complete").getStatus());

        assertTrue(progress(user, MiniGameId.VASE_BREAKER).isLevelCompleted(1));
        assertTrue(progress(user, MiniGameId.VASE_BREAKER).isLevelUnlocked(2));
        assertNull(Store.getActiveMiniGameSession());
    }

    @Test
    void timeLimitProducesAValidLossWithoutACompletion() {
        assertTrue(service.select(user, "vase-breaker", 1).getStatus());
        MiniGameSession selected = Store.getActiveMiniGameSession();
        assertTrue(service.start(user).getStatus());

        Result<List<String>> result = service.advance(
                user, selected.getConfig().getTimeLimitTicks());

        assertTrue(result.getStatus());
        assertTrue(result.getMessage().contains("time limit reached"));
        assertFalse(progress(user, MiniGameId.VASE_BREAKER).isLevelCompleted(1));
        assertEquals(0, user.getCompletedMiniGames());
        assertNull(Store.getActiveMiniGameSession());
    }

    private MiniGameService serviceWith(MiniGameSessionFactory factory) {
        DomainEventPublisher events = new DomainEventPublisher(new DomainEventBus(), users);
        return new MiniGameService(
                MiniGameCatalog.mandatoryDefaults(), factory, users,
                new NewsService(users), events);
    }

    private void win(String name, int level) {
        assertTrue(service.select(user, name, level).getStatus());
        assertTrue(service.start(user).getStatus());
        assertTrue(service.finish(user, GameOutcome.WON).getStatus());
    }

    private MiniGameProgress progress(User owner, MiniGameId id) {
        return owner.getMiniGameProgress().get(id.getToken());
    }

    private void resetStore() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        Store.setActiveMiniGameSession(null);
        Store.setCurrentMenu(MenuName.REGISTER);
        Store.setRunning(true);
    }
}
