package shop;

import controller.ShopController;
import model.Result;
import model.Store;
import model.enums.PlantType;
import model.inGame.plant.PlantRegistry;
import model.miniGame.GreenHouse;
import model.shop.ShopItem;
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

class ShopControllerTest {
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
        user.setUsername("shopper");
        user.setCoins(10_000);
        user.setGems(50);
        user.applyDefaults();
        user.getCollection().purchasePlant(
                registry.requireMandatory(PlantType.SUNFLOWER));
        user.getCollection().purchasePlant(
                registry.requireMandatory(PlantType.PEASHOOTER));
        userService.addUser(user);
        Store.setLoggedInUser(user);
    }

    @Test
    void potPurchaseUnlocksSlotsChargesCoinsAndEnforcesTwentyPotCap() {
        ShopController controller = controller(new ScriptedRandom().withInt(0));

        assertTrue(controller.buy("pot", 2, null).getStatus());
        assertEquals(GreenHouse.INITIAL_UNLOCKED_SLOTS + 2,
                user.getGreenHouse().getSlotCount());
        assertEquals(10_000 - 2 * 2_000, user.getCoins());

        int coinsBefore = user.getCoins();
        Result<String> overflow = controller.buy("pot", 14, null);
        assertFalse(overflow.getStatus());
        assertEquals("cannot own more than 20 pots", overflow.getMessage());
        assertEquals(coinsBefore, user.getCoins());
        assertEquals(GreenHouse.INITIAL_UNLOCKED_SLOTS + 2,
                user.getGreenHouse().getSlotCount());
    }

    @Test
    void plantFoodIsCappedAtThreeStoredAndChargedInDiamonds() {
        ShopController controller = controller(new ScriptedRandom().withInt(0));

        assertTrue(controller.buy("plant-food", 3, null).getStatus());
        assertEquals(3, user.getPlantFood());
        assertEquals(50 - 9, user.getGems());

        Result<String> full = controller.buy("plant-food", 1, null);
        assertFalse(full.getStatus());
        assertEquals("cannot store more than 3 plant foods", full.getMessage());
        assertEquals(3, user.getPlantFood());
        assertEquals(41, user.getGems());
    }

    @Test
    void randomBundleGrantsFivePacketsToTheRolledUnlockedPlant() {
        ShopController controller = controller(
                new ScriptedRandom().withInt(0).withInt(1).withInt(0));

        assertTrue(controller.buy("random-seeds", 2, null).getStatus());
        assertEquals(9_000 - 1_000, user.getCoins());
        assertEquals(5, user.getCollection()
                .getPlantCard(PlantType.PEASHOOTER).getSeedPackets());
        assertEquals(5, user.getCollection()
                .getPlantCard(PlantType.SUNFLOWER).getSeedPackets());
    }

    @Test
    void selectedBundleValidatesFlagAndUnlockStateBeforeCharging() {
        ShopController controller = controller(new ScriptedRandom().withInt(0));

        Result<String> missingFlag = controller.buy("selected-seeds", 1, null);
        assertEquals("selected seed bundle requires -t <plant_type>",
                missingFlag.getMessage());

        Result<String> unknown = controller.buy("selected-seeds", 1, "Nonsense");
        assertEquals("no plant of type \"Nonsense\"", unknown.getMessage());

        Result<String> locked = controller.buy("selected-seeds", 1, "Wall-nut");
        assertTrue(locked.getMessage().contains("is not unlocked"));
        assertEquals(50, user.getGems());

        assertTrue(controller.buy("selected-seeds", 2, "Peashooter").getStatus());
        assertEquals(50 - 10, user.getGems());
        assertEquals(20, user.getCollection()
                .getPlantCard(PlantType.PEASHOOTER).getSeedPackets());
    }

    @Test
    void currencyConversionTradesFiveDiamondsForFiveHundredCoins() {
        ShopController controller = controller(new ScriptedRandom().withInt(0));

        assertTrue(controller.buy("coin-pack", 2, null).getStatus());
        assertEquals(50 - 10, user.getGems());
        assertEquals(10_000 + 1_000, user.getCoins());
    }

    @Test
    void rejectionsNeverPartiallyChargeOrGrant() {
        ShopController controller = controller(new ScriptedRandom().withInt(0));
        user.setCoins(500);
        user.setGems(1);

        assertEquals("count must be a positive number",
                controller.buy("pot", 0, null).getMessage());
        assertEquals("count must be a positive number",
                controller.buy("pot", -3, null).getMessage());
        assertEquals("no shop item with id \"rocket\"",
                controller.buy("rocket", 1, null).getMessage());
        assertEquals("not enough coins",
                controller.buy("pot", 1, null).getMessage());
        assertEquals("not enough diamonds",
                controller.buy("plant-food", 1, null).getMessage());
        assertEquals("cannot own more than 20 pots",
                controller.buy("pot", Integer.MAX_VALUE, null).getMessage());
        assertEquals("not enough diamonds",
                controller.buy("coin-pack", Integer.MAX_VALUE, null).getMessage());

        assertEquals(500, user.getCoins());
        assertEquals(1, user.getGems());
        assertEquals(0, user.getPlantFood());
        assertEquals(GreenHouse.INITIAL_UNLOCKED_SLOTS,
                user.getGreenHouse().getSlotCount());
        assertEquals(0, user.getCollection()
                .getPlantCard(PlantType.PEASHOOTER).getSeedPackets());
    }

    @Test
    void dailyOfferUsesInjectedRollAndAllowsExactlyOnePurchasePerDay() {
        ShopController controller = controller(new ScriptedRandom().withInt(1));

        Result<String> daily = controller.daily();
        assertTrue(daily.getStatus());
        assertTrue(daily.getData().contains("Peashooter"));
        assertTrue(daily.getData().contains("1600 coins"));
        assertTrue(daily.getData().contains("available"));
        assertEquals("2026-07-16", user.getDailyShop().getOfferDate());

        Result<String> multiple = controller.buy("daily", 2, null);
        assertEquals("the daily offer can be purchased only once per day",
                multiple.getMessage());
        assertEquals(10_000, user.getCoins());

        assertTrue(controller.buy("daily", 1, null).getStatus());
        assertEquals(10_000 - 1_600, user.getCoins());
        assertEquals(10, user.getCollection()
                .getPlantCard(PlantType.PEASHOOTER).getSeedPackets());

        Result<String> again = controller.buy("daily", 1, null);
        assertEquals("the daily offer can be purchased only once per day",
                again.getMessage());
        assertEquals(10_000 - 1_600, user.getCoins());
        assertEquals(10, user.getCollection()
                .getPlantCard(PlantType.PEASHOOTER).getSeedPackets());
        assertTrue(controller.daily().getData().contains("already purchased today"));
    }

    @Test
    void listShowsWholeCatalogAndDailyLine() {
        ShopController controller = controller(new ScriptedRandom().withInt(0));

        String listing = controller.list().getData();
        assertTrue(listing.contains("pot: 2000 coins"));
        assertTrue(listing.contains("plant-food: 3 diamonds"));
        assertTrue(listing.contains("random-seeds: 1000 coins"));
        assertTrue(listing.contains("selected-seeds: 5 diamonds"));
        assertTrue(listing.contains("coin-pack: 5 diamonds"));
        assertTrue(listing.contains("daily: " + ShopItem.DAILY_PACKETS
                + " seed packets for Sunflower"));
    }

    private ShopController controller(RandomSource random) {
        return new ShopController(registry, userService, random);
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
            Integer next = integers.poll();
            return next == null ? 0 : Math.min(next, bound - 1);
        }

        @Override
        public double nextDouble() {
            Double next = doubles.poll();
            return next == null ? 0.0 : next;
        }
    }
}
