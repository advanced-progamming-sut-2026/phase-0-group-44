package minigame;

import model.Result;
import model.Store;
import model.enums.MenuName;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.events.DomainEventBus;
import model.inGame.zombie.ZombieDefinition;
import model.inGame.zombie.ZombieRegistry;
import model.miniGame.Brain;
import model.miniGame.IZombie;
import model.miniGame.IZombieLevelRules;
import model.miniGame.IZombieState;
import model.miniGame.MiniGameCatalog;
import model.miniGame.MiniGameDefinition;
import model.miniGame.MiniGameId;
import model.miniGame.MiniGameLifecycleState;
import model.miniGame.MiniGameProgress;
import model.miniGame.MiniGameSession;
import model.miniGame.MiniGameSessionFactory;
import model.miniGame.SunProducingZombie;
import model.sim.GameOutcome;
import model.sim.board.PlantInstance;
import model.sim.zombie.ZombieInstance;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.DomainEventPublisher;
import service.MiniGameService;
import service.NewsService;
import service.UserService;
import util.RandomSource;
import util.SeededRandomSource;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IZombieTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-18T10:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @BeforeEach
    void resetStore() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        Store.setActiveMiniGameSession(null);
        Store.setCurrentMenu(MenuName.TRAVEL_LOG);
        Store.setRunning(true);
    }

    @Test
    void threeLevelsTightenTheRegionIncreasePlantsAndUseElevenZombieTypes() {
        IZombieLevelRules first = IZombie.rulesFor(1);
        IZombieLevelRules second = IZombie.rulesFor(2);
        IZombieLevelRules third = IZombie.rulesFor(3);

        assertTrue(second.isHarderThan(first));
        assertTrue(third.isHarderThan(second));
        assertEquals(5, first.getZombiePrices().size());
        assertEquals(5, second.getZombiePrices().size());
        assertEquals(5, third.getZombiePrices().size());
        assertEquals(8, first.getPreplacedPlantCount());
        assertEquals(12, second.getPreplacedPlantCount());
        assertEquals(16, third.getPreplacedPlantCount());
        assertEquals(5, first.getRedLineColumn());
        assertEquals(6, second.getRedLineColumn());
        assertEquals(7, third.getRedLineColumn());

        Set<ZombieType> distinct = new HashSet<>();
        distinct.addAll(first.getZombiePrices().keySet());
        distinct.addAll(second.getZombiePrices().keySet());
        distinct.addAll(third.getZombiePrices().keySet());
        assertEquals(11, distinct.size());
        assertFalse(first.getZombiePrices().keySet().equals(second.getZombiePrices().keySet()));
        assertFalse(second.getZombiePrices().keySet().equals(third.getZombiePrices().keySet()));

        MiniGameDefinition definition = MiniGameCatalog.phaseOneDefaults()
                .find(MiniGameId.I_ZOMBIE);
        for (int level = 1; level <= 3; level++) {
            assertEquals(150, definition.getLevel(level).getStartingResources());
            assertFalse(definition.getLevel(level).isSkySunEnabled());
        }
    }

    @Test
    void plantsAreRandomlyPreplacedOnlyLeftOfTheRedLine() {
        MiniGameSession session = session(2, new SeededRandomSource(17));
        IZombieState state = state(session);

        assertEquals(12, state.getPreplacedPlants().size());
        for (PlantInstance plant : state.getPreplacedPlants()) {
            assertTrue(plant.getTileX() < state.getRules().getRedLineColumn());
            assertTrue(plant.getTileY() >= 0 && plant.getTileY() < 5);
        }
        assertEquals(5, state.getProducers().size());
        assertEquals(5, state.getBrains().size());
    }

    @Test
    void placementHonorsTheRedLineRosterAndAtomicSunCost() {
        MiniGameSession session = session(1, new SeededRandomSource(7));
        IZombieState state = state(session);

        assertEquals(150, session.getSimulation().getWorld().getSunBalance());
        assertFalse(session.executeStrategyCommand(
                "place zombie -t normal -l (4, 2)").getStatus());
        assertEquals(150, session.getSimulation().getWorld().getSunBalance());
        assertFalse(session.executeStrategyCommand(
                "place zombie -t wizard -l (5, 2)").getStatus());
        assertEquals(150, session.getSimulation().getWorld().getSunBalance());

        assertTrue(session.executeStrategyCommand(
                "place zombie -t normal -l (5, 2)").getStatus());
        assertEquals(100, session.getSimulation().getWorld().getSunBalance());
        assertEquals(1, state.getPlayerZombies().size());
        assertFalse(session.executeStrategyCommand(
                "place zombie -t buckethead -l (5, 2)").getStatus());
        assertEquals(100, session.getSimulation().getWorld().getSunBalance());
        assertFalse(session.executeStrategyCommand(
                "plant plant -t peashooter -l (1, 2)").getStatus());
    }

    @Test
    void eachRowStartsWithANonReplaceableBucketheadHealthSunProducer() {
        MiniGameSession session = session(1, new ZeroRandom());
        IZombieState state = state(session);
        ZombieDefinition bucket = ZombieRegistry.getDefault()
                .requireMandatory(ZombieType.BUCKETHEAD);
        int bucketEquivalent = bucket.getHealth() + bucket.getArmor();

        for (int row = 0; row < 5; row++) {
            SunProducingZombie producer = state.getProducers().get(row);
            assertEquals(row, producer.getZombie().getRow());
            assertEquals(bucketEquivalent, producer.getZombie().getHp());
            assertEquals(240, producer.intervalAt(0));
            assertEquals(220, producer.intervalAt(600));
            assertEquals(200, producer.intervalAt(1200));
        }
        assertFalse(session.executeStrategyCommand(
                "place zombie -t sun-producing zombie -l (5, 0)").getStatus());
    }

    @Test
    void livingProducersGenerateSunAndKilledOnesNeverReturn() {
        MiniGameSession session = session(1, new ZeroRandom());
        IZombieState state = state(session);
        state.getProducers().get(0).getZombie().kill();

        session.advance(239);
        assertEquals(150, session.getSimulation().getWorld().getSunBalance());
        session.advance(1);

        assertEquals(250, session.getSimulation().getWorld().getSunBalance());
        assertEquals(0, state.getProducers().get(0).getProductionCount());
        for (int row = 1; row < 5; row++) {
            assertEquals(1, state.getProducers().get(row).getProductionCount());
        }
    }

    @Test
    void canonicalPlantsAttackPlacedZombiesWhileZombiesStillEatPlants() {
        MiniGameSession session = session(1, new ZeroRandom());
        IZombieState state = state(session);
        PlantInstance shooter = state.getPreplacedPlants().stream()
                .filter(plant -> plant.getType() == PlantType.PEASHOOTER)
                .findFirst().orElseThrow();

        session.getSimulation().getWorld().addSun(1000);
        assertTrue(state.placeZombie(session, "buckethead", 5, shooter.getTileY()).getStatus());
        ZombieInstance target = state.getPlayerZombies().get(0);
        int before = target.getHp() + target.getArmorHealth();
        session.advance(15);
        assertTrue(target.getHp() + target.getArmorHealth() < before);

        target.setX(shooter.getTileX() + 0.1);
        int plantHp = shooter.getHp();
        session.advance(1);
        assertTrue(shooter.getHp() < plantHp);
    }

    @Test
    void reachingEachRowConsumesOneBrainAndAllFiveBrainsWin() {
        MiniGameSession session = session(1, new ZeroRandom());
        IZombieState state = state(session);
        session.getSimulation().getWorld().addSun(1000);

        for (int row = 0; row < 5; row++) {
            assertTrue(state.placeZombie(session, "normal", 5, row).getStatus());
            ZombieInstance zombie = state.getPlayerZombies()
                    .get(state.getPlayerZombies().size() - 1);
            zombie.setX(-0.001);
            Result<java.util.List<String>> advanced = session.advance(1);
            assertTrue(advanced.getMessage().contains("ate the brain in row " + row));
            assertTrue(state.getBrains().get(row).isEaten());
        }

        assertEquals(5, state.getEatenBrainCount());
        assertEquals(MiniGameLifecycleState.WON, session.getState());
    }

    @Test
    void deadEndLossRequiresNoProducersNoPlacedZombiesAndNoAffordableChoice() {
        MiniGameSession session = session(1, new ZeroRandom());
        IZombieState state = state(session);
        session.getSimulation().getWorld().addSun(-150);
        assertTrue(session.advance(1).getStatus());
        assertEquals(MiniGameLifecycleState.RUNNING, session.getState());

        for (SunProducingZombie producer : state.getProducers()) {
            producer.getZombie().kill();
        }
        Result<java.util.List<String>> result = session.advance(1);

        assertEquals(MiniGameLifecycleState.LOST, session.getState());
        assertTrue(result.getMessage().contains("No usable zombie can be afforded"));
    }

    @Test
    void serviceProgressesAcrossAllThreeIZombieLevelsAndPersists() {
        UserService users = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")), CLOCK);
        MiniGameService service = new MiniGameService(
                MiniGameCatalog.phaseOneDefaults(),
                new MiniGameSessionFactory(new SeededRandomSource(5)),
                users,
                new NewsService(users),
                new DomainEventPublisher(new DomainEventBus(), users));
        User user = new User();
        user.setUsername("zombie-player");
        users.addUser(user);
        user.getMiniGameProgress().computeIfAbsent(
                MiniGameId.I_ZOMBIE.getToken(), ignored -> new MiniGameProgress())
                .unlockMiniGame();

        for (int level = 1; level <= 3; level++) {
            assertTrue(service.select(user, "i-zombie", level).getStatus());
            assertTrue(service.start(user).getStatus());
            assertTrue(service.finish(user, GameOutcome.WON).getStatus());
            if (level < 3) {
                assertTrue(user.getMiniGameProgress().get(MiniGameId.I_ZOMBIE.getToken())
                        .isLevelUnlocked(level + 1));
            }
        }
        assertEquals(1, user.getCompletedMiniGames());

        UserService reloaded = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")), CLOCK);
        assertTrue(reloaded.loadUsers().getStatus());
        User restored = reloaded.findByUsername("zombie-player");
        MiniGameProgress progress = restored.getMiniGameProgress()
                .get(MiniGameId.I_ZOMBIE.getToken());
        assertTrue(progress.isLevelCompleted(1));
        assertTrue(progress.isLevelCompleted(2));
        assertTrue(progress.isLevelCompleted(3));
        assertEquals(1, restored.getCompletedMiniGames());
    }

    private MiniGameSession session(int level, RandomSource random) {
        MiniGameDefinition definition = MiniGameCatalog.phaseOneDefaults()
                .find(MiniGameId.I_ZOMBIE);
        MiniGameSession session = new MiniGameSessionFactory(random)
                .create("izombie", definition, definition.getLevel(level));
        assertTrue(session.start().getStatus());
        return session;
    }

    private IZombieState state(MiniGameSession session) {
        return session.getStrategyState(IZombieState.class);
    }

    private static final class ZeroRandom implements RandomSource {
        @Override
        public int nextInt(int bound) {
            return 0;
        }

        @Override
        public double nextDouble() {
            return 0.0;
        }
    }
}
