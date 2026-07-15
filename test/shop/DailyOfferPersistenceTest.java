package shop;

import controller.ShopController;
import model.Result;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyOfferPersistenceTest {
    private static final Instant BEFORE_MIDNIGHT =
            Instant.parse("2026-07-16T23:59:00Z");
    private static final Instant AFTER_MIDNIGHT =
            Instant.parse("2026-07-17T00:01:00Z");

    @TempDir
    Path tempDir;

    private PlantRegistry registry;

    @BeforeEach
    void setUp() {
        Store.setUsers(new java.util.ArrayList<>());
        Store.setLoggedInUser(null);
        registry = PlantRegistry.getDefault();
    }

    @Test
    void restartOnTheSameDayKeepsOfferIdentityAndPurchaseState() {
        Path savePath = tempDir.resolve("users.json");
        UserService firstService = service(savePath, BEFORE_MIDNIGHT);
        User user = newShopper(firstService);

        ShopController firstController = new ShopController(
                registry, firstService, new FixedRandom(1));
        firstController.daily();
        String offerId = user.getDailyShop().getOfferId();
        String offerPlant = user.getDailyShop().getOfferPlant();
        assertEquals("2026-07-16", user.getDailyShop().getOfferDate());
        assertEquals(PlantType.PEASHOOTER.name(), offerPlant);

        assertTrue(firstController.buy("daily", 1, null).getStatus());
        assertEquals(10_000 - 1_600, user.getCoins());

        UserService secondService = restart(savePath, BEFORE_MIDNIGHT.plusSeconds(30));
        User reloaded = Store.getLoggedInUser();

        // A different roll must not matter: the persisted offer wins.
        ShopController secondController = new ShopController(
                registry, secondService, new FixedRandom(0));
        secondController.daily();
        assertEquals(offerId, reloaded.getDailyShop().getOfferId());
        assertEquals(offerPlant, reloaded.getDailyShop().getOfferPlant());

        Result<String> secondPurchase = secondController.buy("daily", 1, null);
        assertFalse(secondPurchase.getStatus());
        assertEquals("the daily offer can be purchased only once per day",
                secondPurchase.getMessage());
        assertEquals(10_000 - 1_600, reloaded.getCoins());
        assertEquals(10, reloaded.getCollection()
                .getPlantCard(PlantType.PEASHOOTER).getSeedPackets());
    }

    @Test
    void offerRefreshesAtMidnightAndBecomesPurchasableAgain() {
        Path savePath = tempDir.resolve("users.json");
        UserService eveningService = service(savePath, BEFORE_MIDNIGHT);
        User user = newShopper(eveningService);

        ShopController eveningController = new ShopController(
                registry, eveningService, new FixedRandom(1));
        eveningController.daily();
        String eveningOfferId = user.getDailyShop().getOfferId();
        assertTrue(eveningController.buy("daily", 1, null).getStatus());

        UserService morningService = restart(savePath, AFTER_MIDNIGHT);
        User reloaded = Store.getLoggedInUser();

        ShopController morningController = new ShopController(
                registry, morningService, new FixedRandom(0));
        morningController.daily();

        assertEquals("2026-07-17", reloaded.getDailyShop().getOfferDate());
        assertNotEquals(eveningOfferId, reloaded.getDailyShop().getOfferId());
        assertEquals(PlantType.SUNFLOWER.name(),
                reloaded.getDailyShop().getOfferPlant());

        assertTrue(morningController.buy("daily", 1, null).getStatus());
        assertEquals(10_000 - 2 * 1_600, reloaded.getCoins());
        assertEquals(10, reloaded.getCollection()
                .getPlantCard(PlantType.SUNFLOWER).getSeedPackets());
    }

    private User newShopper(UserService service) {
        User user = new User();
        user.setUsername("midnight-shopper");
        user.setCoins(10_000);
        user.setGems(50);
        user.setStayLoggedIn(true);
        user.applyDefaults();
        user.getCollection().purchasePlant(
                registry.requireMandatory(PlantType.SUNFLOWER));
        user.getCollection().purchasePlant(
                registry.requireMandatory(PlantType.PEASHOOTER));
        service.addUser(user);
        Store.setLoggedInUser(user);
        return user;
    }

    private UserService restart(Path savePath, Instant at) {
        Store.setUsers(new java.util.ArrayList<>());
        Store.setLoggedInUser(null);
        UserService service = service(savePath, at);
        service.loadUsers();
        if (Store.getLoggedInUser() == null) {
            for (User candidate : Store.getUsers()) {
                if ("midnight-shopper".equals(candidate.getUsername())) {
                    Store.setLoggedInUser(candidate);
                }
            }
        }
        return service;
    }

    private UserService service(Path savePath, Instant instant) {
        return new UserService(
                new JsonUserRepository(savePath),
                Clock.fixed(instant, ZoneOffset.UTC)
        );
    }

    /** Always returns the same roll (clamped to the bound). */
    private static final class FixedRandom implements RandomSource {
        private final int value;

        FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return Math.min(value, bound - 1);
        }

        @Override
        public double nextDouble() {
            return 0.0;
        }
    }
}
