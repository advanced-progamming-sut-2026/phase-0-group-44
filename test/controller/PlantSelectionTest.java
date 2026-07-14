package controller;

import model.Result;
import model.Store;
import model.enums.MenuName;
import model.enums.PlantType;
import model.inGame.GameSession;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRepository;
import model.level.Level;
import model.level.LevelSelectionRules;
import model.level.NormalLevel;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.GreenhouseBoostService;
import service.UserService;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plant-selection flow. Plant definitions are supplied by an in-memory fake
 * repository, so the selection logic is verified without depending on the JSON
 * data load.
 */
class PlantSelectionTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-14T09:30:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private UserService userService;
    private PlantRepository plantRepository;
    private PlantSelectionController controller;
    private User user;

    @BeforeEach
    void setUp() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        Store.setCurrentMenu(MenuName.GAME);
        Store.setActiveSession(null);

        userService = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")), CLOCK);
        plantRepository = new FakePlantRepository();

        user = new User();
        user.setUsername("kasra");
        user.applyDefaults();
        userService.addUser(user);
        Store.setLoggedInUser(user);

        controller = new PlantSelectionController(plantRepository, userService);
    }

    private void ownAllSixPlants() {
        for (PlantType type : PlantType.values()) {
            user.getCollection().purchasePlant(type);
        }
    }

    private void begin(Level level) {
        controller.begin(level);
    }

    private Level normal() {
        return new NormalLevel("Egypt 1-1");
    }

    // ---------- show ----------

    @Test
    void showAllListsEveryDefinition() {
        begin(normal());

        Result<java.util.List<PlantDefinition>> result = controller.showAllPlants();

        assertTrue(result.getStatus());
        assertEquals(PlantType.values().length, result.getData().size());
    }

    @Test
    void showAvailableListsOnlyOwnedPlantsThisLevelAllows() {
        user.getCollection().purchasePlant(PlantType.SUNFLOWER);
        user.getCollection().purchasePlant(PlantType.PEASHOOTER);

        Level restricted = new NormalLevel("Egypt 1-2",
                LevelSelectionRules.restrictedTo(8, EnumSet.of(PlantType.SUNFLOWER)));
        begin(restricted);

        Result<java.util.List<PlantDefinition>> result = controller.showAvailablePlants();

        assertTrue(result.getStatus());
        assertEquals(1, result.getData().size());
        assertEquals(PlantType.SUNFLOWER, result.getData().get(0).getType());
    }

    // ---------- add: rejections ----------

    @Test
    void addRejectsAnUnknownPlant() {
        ownAllSixPlants();
        begin(normal());

        Result<String> result = controller.addPlant("Nonexistent");

        assertFalse(result.getStatus());
        assertTrue(controller.getSelection().isEmpty());
    }

    @Test
    void addRejectsAPlantThePlayerDoesNotOwn() {
        begin(normal());

        Result<String> result = controller.addPlant("SUNFLOWER");

        assertFalse(result.getStatus());
        assertEquals("you do not own this plant", result.getMessage());
        assertTrue(controller.getSelection().isEmpty());
    }

    @Test
    void addRejectsAPlantThisLevelDisallows() {
        ownAllSixPlants();
        Level restricted = new NormalLevel("Egypt 1-3",
                LevelSelectionRules.restrictedTo(8, EnumSet.of(PlantType.SUNFLOWER)));
        begin(restricted);

        Result<String> result = controller.addPlant("PEASHOOTER");

        assertFalse(result.getStatus());
        assertEquals("this level does not allow this plant", result.getMessage());
        assertFalse(controller.getSelection().contains(PlantType.PEASHOOTER));
    }

    @Test
    void addRejectsADuplicateSelection() {
        ownAllSixPlants();
        begin(normal());
        controller.addPlant("SUNFLOWER");

        Result<String> result = controller.addPlant("SUNFLOWER");

        assertFalse(result.getStatus());
        assertEquals("this plant is already selected", result.getMessage());
        assertEquals(1, controller.getSelection().size());
    }

    @Test
    void addRejectsWhenSelectionIsFull() {
        ownAllSixPlants();
        Level small = new NormalLevel("tiny", LevelSelectionRules.withCapacity(2));
        begin(small);

        assertTrue(controller.addPlant("SUNFLOWER").getStatus());
        assertTrue(controller.addPlant("PEASHOOTER").getStatus());

        Result<String> result = controller.addPlant("WALL_NUT");

        assertFalse(result.getStatus());
        assertEquals("all selection slots are full", result.getMessage());
        assertEquals(2, controller.getSelection().size());
    }

    @Test
    void aNormalLevelHasEightSlots() {
        assertEquals(8, new NormalLevel("x").getSelectionRules().getCapacity());
    }

    // ---------- remove ----------

    @Test
    void removeRejectsUnknownOrUnselectedPlants() {
        ownAllSixPlants();
        begin(normal());
        controller.addPlant("SUNFLOWER");

        assertFalse(controller.removePlant("Nonexistent").getStatus());
        assertFalse(controller.removePlant("PEASHOOTER").getStatus());
        assertEquals(1, controller.getSelection().size());

        Result<String> ok = controller.removePlant("SUNFLOWER");
        assertTrue(ok.getStatus());
        assertTrue(controller.getSelection().isEmpty());
    }

    // ---------- boost ----------

    @Test
    void boostChargesTwoDiamondsOnlyOnSuccess() {
        ownAllSixPlants();
        user.setGems(5);
        begin(normal());
        controller.addPlant("SUNFLOWER");

        Result<String> ok = controller.boostPlant("SUNFLOWER");
        assertTrue(ok.getStatus());
        assertEquals(3, user.getGems());
        assertTrue(controller.getSelection().isDiamondBoosted(PlantType.SUNFLOWER));
    }

    @Test
    void boostDoesNotChargeWhenItFails() {
        ownAllSixPlants();
        user.setGems(5);
        begin(normal());

        // not selected yet
        assertFalse(controller.boostPlant("SUNFLOWER").getStatus());
        assertEquals(5, user.getGems());

        // unknown plant
        assertFalse(controller.boostPlant("Nonexistent").getStatus());
        assertEquals(5, user.getGems());

        // already boosted
        controller.addPlant("SUNFLOWER");
        controller.boostPlant("SUNFLOWER");
        assertEquals(3, user.getGems());
        assertFalse(controller.boostPlant("SUNFLOWER").getStatus());
        assertEquals(3, user.getGems());
    }

    @Test
    void boostFailsWithoutEnoughDiamondsAndDoesNotCharge() {
        ownAllSixPlants();
        user.setGems(1);
        begin(normal());
        controller.addPlant("SUNFLOWER");

        Result<String> result = controller.boostPlant("SUNFLOWER");

        assertFalse(result.getStatus());
        assertEquals("not enough diamonds", result.getMessage());
        assertEquals(1, user.getGems());
        assertFalse(controller.getSelection().isDiamondBoosted(PlantType.SUNFLOWER));
    }

    @Test
    void aDiamondBoostedPlantGetsImmediatePlantFoodInTheSession() {
        ownAllSixPlants();
        user.setGems(2);
        begin(normal());
        controller.addPlant("SUNFLOWER");
        controller.boostPlant("SUNFLOWER");

        GameSession session = controller.startGame().getData();

        assertTrue(session.hasImmediatePlantFood(PlantType.SUNFLOWER));
        assertFalse(session.hasImmediatePlantFood(PlantType.PEASHOOTER));
    }

    // ---------- stored greenhouse boost ----------

    @Test
    void aStoredGreenhouseBoostIsConsumedOnFirstUseOnly() {
        ownAllSixPlants();
        user.addStoredPlantBoost(PlantType.SUNFLOWER);
        begin(normal());
        controller.addPlant("SUNFLOWER");

        GameSession session = controller.startGame().getData();
        assertTrue(session.hasPendingGreenhouseBoost(PlantType.SUNFLOWER));

        GreenhouseBoostService boosts = new GreenhouseBoostService(userService);

        assertTrue(boosts.consumeOnFirstUse(session, user, PlantType.SUNFLOWER),
                "first use consumes the stored boost");
        assertFalse(boosts.consumeOnFirstUse(session, user, PlantType.SUNFLOWER),
                "a later use does not consume again");

        assertFalse(user.getPlantBoosts().containsKey(PlantType.SUNFLOWER));

        UserService reloaded = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")), CLOCK);
        reloaded.loadUsers();
        assertFalse(reloaded.findByUsername("kasra")
                .getPlantBoosts().containsKey(PlantType.SUNFLOWER));
    }

    @Test
    void aPlantHasAtMostOneStoredGreenhouseBoost() {
        assertTrue(user.addStoredPlantBoost(PlantType.SUNFLOWER));
        assertFalse(user.addStoredPlantBoost(PlantType.SUNFLOWER));
        assertEquals(1, user.getPlantBoosts().get(PlantType.SUNFLOWER).intValue());
    }

    // ---------- start game ----------

    @Test
    void startGameRequiresAtLeastOnePlantOnANormalLevel() {
        ownAllSixPlants();
        begin(normal());

        Result<GameSession> result = controller.startGame();

        assertFalse(result.getStatus());
        assertEquals("select at least one plant", result.getMessage());
    }

    @Test
    void startGameEntersTheGameplayContext() {
        ownAllSixPlants();
        begin(normal());
        controller.addPlant("SUNFLOWER");

        Result<GameSession> result = controller.startGame();

        assertTrue(result.getStatus());
        assertEquals(MenuName.GAMEPLAY, Store.getCurrentMenu());
        assertEquals(Store.getActiveSession(), result.getData());
    }

    @Test
    void sessionsAreIsolatedFromLaterSelectionEdits() {
        ownAllSixPlants();
        begin(normal());
        controller.addPlant("SUNFLOWER");

        GameSession first = controller.startGame().getData();

        // Continue editing the same controller's selection and start again.
        controller.addPlant("PEASHOOTER");
        GameSession second = controller.startGame().getData();

        assertNotSame(first, second);
        assertEquals(1, first.getSelection().size());
        assertEquals(2, second.getSelection().size());
        assertFalse(first.getSelection().contains(PlantType.PEASHOOTER));
        assertTrue(second.getSelection().contains(PlantType.PEASHOOTER));
    }

    @Test
    void twoSessionsDoNotShareStoredBoostConsumption() {
        ownAllSixPlants();
        user.addStoredPlantBoost(PlantType.SUNFLOWER);
        begin(normal());
        controller.addPlant("SUNFLOWER");

        GameSession first = controller.startGame().getData();
        GameSession second = controller.startGame().getData();

        // Consuming in the first session must not clear the pending flag in the second.
        first.useAndConsumeGreenhouseBoost(PlantType.SUNFLOWER);

        assertFalse(first.hasPendingGreenhouseBoost(PlantType.SUNFLOWER));
        assertTrue(second.hasPendingGreenhouseBoost(PlantType.SUNFLOWER));
    }

    /** In-memory plant repository: one definition per {@link PlantType}. */
    private static final class FakePlantRepository implements PlantRepository {
        private final ArrayList<PlantDefinition> definitions = new ArrayList<>();

        FakePlantRepository() {
            for (PlantType type : PlantType.values()) {
                definitions.add(new TestPlantDefinition(type));
            }
        }

        @Override
        public void load() {
        }

        @Override
        public ArrayList<PlantDefinition> findAll() {
            return new ArrayList<>(definitions);
        }

        @Override
        public PlantDefinition findByType(PlantType type) {
            for (PlantDefinition definition : definitions) {
                if (definition.getType() == type) {
                    return definition;
                }
            }

            return null;
        }

        @Override
        public PlantDefinition findByName(String name) {
            for (PlantDefinition definition : definitions) {
                if (definition.getType().name().equalsIgnoreCase(name)) {
                    return definition;
                }
            }

            return null;
        }
    }

    /** A definition whose type is set without needing the JSON schema. */
    private static final class TestPlantDefinition extends PlantDefinition {
        private final PlantType type;

        TestPlantDefinition(PlantType type) {
            this.type = type;
        }

        @Override
        public PlantType getType() {
            return type;
        }

        @Override
        public String getName() {
            return type.name();
        }
    }
}
