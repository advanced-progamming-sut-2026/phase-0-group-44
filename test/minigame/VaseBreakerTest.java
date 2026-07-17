package minigame;

import model.Result;
import model.Store;
import model.enums.Command;
import model.enums.MenuName;
import model.enums.PlantType;
import model.enums.VaseContentType;
import model.events.DomainEventBus;
import model.miniGame.MiniGameCatalog;
import model.miniGame.MiniGameId;
import model.miniGame.MiniGameProgress;
import model.miniGame.MiniGameSession;
import model.miniGame.MiniGameSessionFactory;
import model.miniGame.SeedPacket;
import model.miniGame.Vase;
import model.miniGame.VaseBreaker;
import model.miniGame.VaseBreakerLevelRules;
import model.miniGame.VaseBreakerState;
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

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaseBreakerTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-18T10:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private UserService users;
    private MiniGameService service;
    private User user;

    @BeforeEach
    void setUp() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        Store.setActiveMiniGameSession(null);
        Store.setCurrentMenu(MenuName.TRAVEL_LOG);
        Store.setRunning(true);
        users = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")), CLOCK);
        service = new MiniGameService(
                MiniGameCatalog.mandatoryDefaults(),
                new MiniGameSessionFactory(new ZeroRandom()),
                users,
                new NewsService(users),
                new DomainEventPublisher(new DomainEventBus(), users));
        user = new User();
        user.setUsername("vase-player");
        users.addUser(user);
        Store.setLoggedInUser(user);
    }

    @Test
    void threeLevelsIncreaseVaseCountCompositionDangerAndExpirationPressure() {
        VaseBreakerLevelRules one = VaseBreaker.rulesFor(1);
        VaseBreakerLevelRules two = VaseBreaker.rulesFor(2);
        VaseBreakerLevelRules three = VaseBreaker.rulesFor(3);

        assertEquals(8, one.getVaseCount());
        assertEquals(12, two.getVaseCount());
        assertEquals(16, three.getVaseCount());
        assertTrue(two.isHarderThan(one));
        assertTrue(three.isHarderThan(two));
        assertEquals(0, MiniGameCatalog.mandatoryDefaults()
                .find(MiniGameId.VASE_BREAKER).getLevel(1).getStartingResources());
        assertFalse(MiniGameCatalog.mandatoryDefaults()
                .find(MiniGameId.VASE_BREAKER).getLevel(1).isSkySunEnabled());
    }

    @Test
    void everyVaseTypeHasItsSpecifiedDeterministicOutcome() {
        unlockLevel(2);
        MiniGameSession session = start(2);
        VaseBreakerState state = state(session);

        Vase empty = vase(state, VaseContentType.EMPTY);
        assertTrue(command("break vase -l (" + empty.getX() + ", " + empty.getY() + ")")
                .getMessage().contains("empty"));

        Vase zombie = vase(state, VaseContentType.ZOMBIE);
        int before = session.getSimulation().getWorld().getZombieInstances().size();
        assertTrue(command("break vase -l (" + zombie.getX() + ", " + zombie.getY() + ")")
                .getStatus());
        assertEquals(before + 1,
                session.getSimulation().getWorld().getZombieInstances().size());

        Vase normalPacket = vase(state, VaseContentType.SEED_PACKET);
        assertTrue(command("break vase -l (" + normalPacket.getX() + ", "
                + normalPacket.getY() + ")").getMessage().contains("dropped packet"));

        Vase plantVase = vase(state, VaseContentType.PLANT_VASE);
        assertTrue(command("break vase -l (" + plantVase.getX() + ", "
                + plantVase.getY() + ")").getMessage().contains("Plant Vase"));
        assertEquals(PlantType.PEASHOOTER, plantVase.getPlantType());

        Vase gargantuar = vase(state, VaseContentType.GARGANTUAR_VASE);
        assertTrue(command("break vase -l (" + gargantuar.getX() + ", "
                + gargantuar.getY() + ")").getMessage().contains("Gargantuar"));
    }

    @Test
    void packetsMustBeCollectedAndPlantedBeforeTheirConfiguredExpiration() {
        MiniGameSession session = start(1);
        VaseBreakerState state = state(session);
        Vase packetVase = vase(state, VaseContentType.SEED_PACKET);
        assertTrue(command("break vase -l (" + packetVase.getX() + ", "
                + packetVase.getY() + ")").getStatus());
        SeedPacket packet = state.getPackets().get(0);

        assertFalse(command("plant packet -i " + packet.getId() + " -l (0, 0)")
                .getStatus());
        assertTrue(command("collect packet -i " + packet.getId()).getStatus());
        assertTrue(service.advance(user, state.getRules().getPacketLifetimeTicks())
                .getMessage().contains("expired"));
        assertEquals(SeedPacket.State.EXPIRED, packet.getState());
        assertFalse(command("plant packet -i " + packet.getId() + " -l (0, 0)")
                .getStatus());
    }

    @Test
    void collectedPacketPlantsOnceWithoutSunOrPlantSelection() {
        MiniGameSession session = start(1);
        VaseBreakerState state = state(session);
        Vase packetVase = vase(state, VaseContentType.SEED_PACKET);
        command("break vase -l (" + packetVase.getX() + ", " + packetVase.getY() + ")");
        SeedPacket packet = state.getPackets().get(0);

        assertEquals(0, session.getSimulation().getWorld().getSunBalance());
        assertTrue(command("collect packet -i " + packet.getId()).getStatus());
        assertTrue(command("plant packet -i " + packet.getId() + " -l (0, 0)")
                .getStatus());
        assertEquals(SeedPacket.State.PLANTED, packet.getState());
        assertEquals(1, session.getSimulation().getWorld().getPlants().size());
        assertEquals(0, session.getSimulation().getWorld().getSunBalance());
        assertFalse(command("plant packet -i " + packet.getId() + " -l (1, 0)")
                .getStatus());
        assertFalse(command("plant plant -t peashooter -l (1, 0)").getStatus());
        assertTrue(service.advance(user, 100).getData().stream()
                .noneMatch(message -> message.contains("sun has fallen")));
    }

    @Test
    void allVasesAndAllThreatsMustBeResolvedToWinAndUnlockNextLevel() {
        MiniGameSession session = start(1);
        VaseBreakerState state = state(session);
        breakEveryVase(state);
        assertNotNull(Store.getActiveMiniGameSession());

        for (ZombieInstance zombie : session.getSimulation().getWorld().getZombieInstances()) {
            zombie.kill();
        }
        assertTrue(service.advance(user, 1).getStatus());

        assertNull(Store.getActiveMiniGameSession());
        MiniGameProgress progress = user.getMiniGameProgress()
                .get(MiniGameId.VASE_BREAKER.getToken());
        assertTrue(progress.isLevelCompleted(1));
        assertTrue(progress.isLevelUnlocked(2));
    }

    @Test
    void standardMowerThenBrainDefenseProducesALoss() {
        unlockLevel(2);
        MiniGameSession session = start(2);
        VaseBreakerState state = state(session);
        List<Vase> threats = state.getVases().stream()
                .filter(vase -> vase.getContentType() == VaseContentType.ZOMBIE)
                .limit(2)
                .toList();

        command("break vase -l (" + threats.get(0).getX() + ", "
                + threats.get(0).getY() + ")");
        ZombieInstance first = session.getSimulation().getWorld().getZombieInstances().get(0);
        int lane = first.getRow();
        first.setX(0.001);
        assertTrue(service.advance(user, 1).getStatus());
        assertTrue(session.getSimulation().getWorld().isLawnMowerUsed(lane));

        command("break vase -l (" + threats.get(1).getX() + ", "
                + threats.get(1).getY() + ")");
        ZombieInstance second = session.getSimulation().getWorld().getZombieInstances().get(0);
        second.setRow(lane);
        second.setX(0.001);
        Result<List<String>> loss = service.advance(user, 1);

        assertTrue(loss.getMessage().contains("ate your brain"));
        assertNull(Store.getActiveMiniGameSession());
        assertFalse(user.getMiniGameProgress().get(MiniGameId.VASE_BREAKER.getToken())
                .isLevelCompleted(2));
    }

    @Test
    void commandSyntaxUsesTheExistingMinigameExtensionEnvelope() {
        assertTrue(Command.MINIGAME_COMMAND.matches(
                "minigame command break vase -l (8, 2)"));
        assertTrue(Command.MINIGAME_COMMAND.matches(
                "minigame command collect packet -i 3"));
        assertTrue(Command.MINIGAME_COMMAND.matches(
                "minigame command plant packet -i 3 -l (1, 2)"));
        MiniGameSession session = start(1);
        String visible = command("show vases").getMessage();
        assertTrue(visible.contains("at ("));
        assertTrue(visible.contains("normal vase"));
        assertFalse(visible.contains("it was empty"));
    }

    private MiniGameSession start(int level) {
        assertTrue(service.select(user, "vase-breaker", level).getStatus());
        assertTrue(service.start(user).getStatus());
        return Store.getActiveMiniGameSession();
    }

    private Result<String> command(String input) {
        return service.executeStrategyCommand(user, input);
    }

    private VaseBreakerState state(MiniGameSession session) {
        VaseBreakerState state = session.getStrategyState(VaseBreakerState.class);
        assertNotNull(state);
        return state;
    }

    private Vase vase(VaseBreakerState state, VaseContentType type) {
        return state.getVases().stream()
                .filter(value -> value.getContentType() == type)
                .findFirst()
                .orElseThrow();
    }

    private void breakEveryVase(VaseBreakerState state) {
        for (Vase vase : state.getVases()) {
            assertTrue(command("break vase -l (" + vase.getX() + ", "
                    + vase.getY() + ")").getStatus());
        }
    }

    private void unlockLevel(int level) {
        user.applyDefaults();
        MiniGameProgress progress = user.getMiniGameProgress().computeIfAbsent(
                MiniGameId.VASE_BREAKER.getToken(), ignored -> new MiniGameProgress());
        progress.applyDefaults();
        progress.unlockMiniGame();
        for (int current = 2; current <= level; current++) {
            progress.unlockLevel(current);
        }
    }

    private static final class ZeroRandom implements RandomSource {
        @Override
        public int nextInt(int bound) {
            return 0;
        }

        @Override
        public double nextDouble() {
            return 0;
        }
    }
}
