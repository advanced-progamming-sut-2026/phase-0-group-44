package greenhouse;

import controller.GreenhouseController;
import model.Store;
import model.enums.PlantType;
import model.inGame.plant.PlantRegistry;
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
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreenhousePersistenceTest {
    private static final Instant PLANTED_AT = Instant.parse("2026-07-16T00:00:00Z");

    @TempDir
    Path tempDir;

    @BeforeEach
    void clearStore() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
    }

    @Test
    void timestampBasedGrowthContinuesAcrossRestart() {
        Path savePath = tempDir.resolve("users.json");
        PlantRegistry registry = PlantRegistry.getDefault();
        UserService firstService = service(savePath, PLANTED_AT);

        User user = new User();
        user.setUsername("persisted-grower");
        user.applyDefaults();
        user.getCollection().purchasePlant(registry.requireMandatory(PlantType.PEASHOOTER));
        firstService.addUser(user);
        Store.setLoggedInUser(user);

        GreenhouseController firstController = new GreenhouseController(
                registry, firstService, new PlantRollRandom()
        );
        assertTrue(firstController.plantPot(1, 1).getStatus());
        long readyAt = user.getGreenHouse().getSlot(1, 1).getReadyAtEpochSecond();
        assertEquals(PLANTED_AT.plusSeconds(8 * 3600L).getEpochSecond(), readyAt);

        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);

        Instant afterRestart = PLANTED_AT.plusSeconds(8 * 3600L);
        UserService secondService = service(savePath, afterRestart);
        assertTrue(secondService.loadUsers().getStatus());
        User restored = secondService.findByUsername("persisted-grower");
        Store.setLoggedInUser(restored);

        GreenhouseController secondController = new GreenhouseController(
                registry, secondService, new PlantRollRandom()
        );
        assertTrue(secondController.showGreenhouse().getMessage()
                .contains("(1, 1): Peashooter ready"));
        assertTrue(secondController.collect(1, 1).getStatus());
        assertEquals(1, restored.getPlantBoosts().get(PlantType.PEASHOOTER));
    }

    private UserService service(Path path, Instant instant) {
        return new UserService(
                new JsonUserRepository(path),
                Clock.fixed(instant, ZoneOffset.UTC)
        );
    }

    private static final class PlantRollRandom implements RandomSource {
        @Override
        public int nextInt(int bound) {
            return 0;
        }

        @Override
        public double nextDouble() {
            return 0.75;
        }
    }
}
