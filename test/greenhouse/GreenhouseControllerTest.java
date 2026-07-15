package greenhouse;

import controller.GreenhouseController;
import model.Result;
import model.Store;
import model.enums.PlantType;
import model.inGame.plant.PlantRegistry;
import model.miniGame.GreenhouseSlot;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.UserService;
import util.RandomSource;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreenhouseControllerTest {
    private static final Instant NOW = Instant.parse("2026-07-16T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private PlantRegistry registry;
    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        registry = PlantRegistry.getDefault();
        userService = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")), CLOCK
        );

        user = new User();
        user.setUsername("greenhouse-user");
        user.setCoins(100);
        user.setGems(10);
        user.applyDefaults();
        user.getCollection().purchasePlant(registry.requireMandatory(PlantType.PEASHOOTER));
        userService.addUser(user);
        Store.setLoggedInUser(user);
    }

    @Test
    void plantingUsesInjectedFiftyFiftyRollAndCanonicalEligiblePlants() {
        ScriptedRandom random = new ScriptedRandom()
                .withDouble(0.25)
                .withDouble(0.75)
                .withInt(0);
        GreenhouseController controller = controller(random);

        assertTrue(controller.plantPot(1, 1).getStatus());
        GreenhouseSlot marigold = user.getGreenHouse().getSlot(1, 1);
        assertTrue(marigold.isMarigold());
        assertEquals(NOW.getEpochSecond() + 2 * 3600L,
                marigold.getReadyAtEpochSecond());

        assertTrue(controller.plantPot(2, 1).getStatus());
        GreenhouseSlot plant = user.getGreenHouse().getSlot(2, 1);
        assertEquals(PlantType.PEASHOOTER, plant.getPlantType());
        assertEquals(NOW.getEpochSecond() + 8 * 3600L,
                plant.getReadyAtEpochSecond());
    }

    @Test
    void invalidLockedAndOccupiedSlotsDoNotMutateGreenhouse() {
        GreenhouseController controller = controller(new ScriptedRandom().withDouble(0.1));

        assertEquals("greenhouse coordinates are out of range",
                controller.plantPot(0, 1).getMessage());
        assertEquals("greenhouse slot is locked",
                controller.plantPot(1, 2).getMessage());
        assertTrue(controller.plantPot(1, 1).getStatus());
        assertEquals("greenhouse slot is occupied",
                controller.plantPot(1, 1).getMessage());
        assertEquals(1, user.getGreenHouse().getSlots().stream()
                .filter(GreenhouseSlot::isOccupied).count());
    }

    @Test
    void statusReportsAllSlotsAndReadyGrowingEmptyAndLockedStates() {
        GreenhouseController controller = controller(new ScriptedRandom().withDouble(0.1));
        controller.plantPot(1, 1);
        user.getGreenHouse().getSlot(2, 1).plantMarigold(
                NOW.minusSeconds(7200).getEpochSecond(), NOW.getEpochSecond()
        );

        String status = controller.showGreenhouse().getData();

        assertEquals(20, status.lines().count());
        assertTrue(status.contains("(1, 1): Marigold growing; remaining 02:00:00"));
        assertTrue(status.contains("(2, 1): Marigold ready"));
        assertTrue(status.contains("(3, 1): empty"));
        assertTrue(status.contains("(1, 2): locked"));
    }

    @Test
    void harvestRewardsMarigoldAndCapsStoredPlantBoostAtOne() {
        GreenhouseController controller = controller(new ScriptedRandom());
        GreenhouseSlot marigold = user.getGreenHouse().getSlot(1, 1);
        marigold.plantMarigold(NOW.minusSeconds(7200).getEpochSecond(), NOW.getEpochSecond());

        assertTrue(controller.collect(1, 1).getStatus());
        assertEquals(600, user.getCoins());
        assertFalse(marigold.isOccupied());

        GreenhouseSlot plant = user.getGreenHouse().getSlot(2, 1);
        plant.plant(PlantType.PEASHOOTER,
                NOW.minusSeconds(28800).getEpochSecond(), NOW.getEpochSecond());
        assertTrue(controller.collect(2, 1).getStatus());
        assertEquals(1, user.getPlantBoosts().get(PlantType.PEASHOOTER));

        plant.plant(PlantType.PEASHOOTER,
                NOW.minusSeconds(28800).getEpochSecond(), NOW.getEpochSecond());
        Result<String> duplicate = controller.collect(2, 1);
        assertTrue(duplicate.getStatus());
        assertTrue(duplicate.getMessage().contains("boost already stored"));
        assertEquals(1, user.getPlantBoosts().get(PlantType.PEASHOOTER));
        assertFalse(plant.isOccupied());
    }

    @Test
    void growRoundsRemainingHoursUpAndNeverChargesOnFailure() {
        GreenhouseController controller = controller(new ScriptedRandom());
        GreenhouseSlot slot = user.getGreenHouse().getSlot(1, 1);
        slot.plant(PlantType.PEASHOOTER, NOW.getEpochSecond(),
                NOW.plusSeconds(2 * 3600L + 1800L).getEpochSecond());

        Result<Integer> grown = controller.grow(1, 1);
        assertTrue(grown.getStatus());
        assertEquals(3, grown.getData());
        assertEquals(7, user.getGems());
        assertTrue(slot.isReady(NOW));

        int gemsBefore = user.getGems();
        assertEquals("the plant is already ready", controller.grow(1, 1).getMessage());
        assertEquals(gemsBefore, user.getGems());

        slot.clear();
        slot.plant(PlantType.PEASHOOTER, NOW.getEpochSecond(),
                NOW.plusSeconds(8 * 3600L).getEpochSecond());
        user.setGems(2);
        assertEquals("not enough diamonds", controller.grow(1, 1).getMessage());
        assertEquals(2, user.getGems());
        assertFalse(slot.isReady(NOW));
    }

    private GreenhouseController controller(RandomSource random) {
        return new GreenhouseController(registry, userService, random);
    }

    private static final class ScriptedRandom implements RandomSource {
        private final Queue<Double> doubles = new ArrayDeque<>();
        private final Queue<Integer> integers = new ArrayDeque<>();

        ScriptedRandom withDouble(double value) {
            doubles.add(value);
            return this;
        }

        ScriptedRandom withInt(int value) {
            integers.add(value);
            return this;
        }

        @Override
        public int nextInt(int bound) {
            int value = integers.isEmpty() ? 0 : integers.remove();
            return Math.floorMod(value, bound);
        }

        @Override
        public double nextDouble() {
            return doubles.isEmpty() ? 0.0 : doubles.remove();
        }
    }
}
