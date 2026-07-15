package adventure;

import controller.BoardController;
import controller.GameplayController;
import model.config.AdventureCatalog;
import model.config.GameWorld;
import model.enums.PlantCategory;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.inGame.PlantSelection;
import model.inGame.plant.PlantRegistry;
import model.inGame.zombie.ZombieRegistry;
import model.level.AdventureLevelConfig;
import model.level.SpecialLevelType;
import model.sim.GameOutcome;
import model.sim.Simulation;
import model.sim.SimulationWorld;
import model.sim.adventure.AdventureInitializer;
import model.sim.adventure.AdventureRuleSystem;
import model.sim.board.DefaultPlantSpecSource;
import model.sim.board.PlantSpecSource;
import model.sim.zombie.DefaultZombieSpecSource;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpecSource;
import model.user.User;
import org.junit.jupiter.api.Test;
import util.SeededRandomSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialLevelRulesTest {
    private final PlantSpecSource plants =
            new DefaultPlantSpecSource(PlantRegistry.getDefault());
    private final ZombieSpecSource zombies =
            new DefaultZombieSpecSource(ZombieRegistry.getDefault());

    @Test
    void conveyorStartsWithAPacketAndSuppliesAnotherEveryTwelveSeconds() {
        User user = userOwning(PlantType.SUNFLOWER, PlantType.PEASHOOTER);
        AdventureLevelConfig config = AdventureCatalog.level(
                GameWorld.ANCIENT_EGYPT, 2).getAdventureConfig();
        SimulationWorld world = initialize(config, user);
        int initial = packetCount(world.getAdventureState().getConveyorPackets());
        assertEquals(1, initial);

        Simulation simulation = new Simulation(new SeededRandomSource(1), world, false);
        simulation.register(new AdventureRuleSystem(zombies));
        simulation.advance(12 * 10);

        assertEquals(2, packetCount(world.getAdventureState().getConveyorPackets()));
    }

    @Test
    void lockedPlantsSupportsForcedPlantsAndFamilyExclusion() {
        var rules = AdventureCatalog.level(GameWorld.ANCIENT_EGYPT, 3)
                .getSelectionRules();
        assertTrue(rules.isForced(PlantType.SUNFLOWER));
        assertFalse(rules.allows(PlantType.POTATO_MINE));
        assertTrue(rules.getExcludedCategories().contains(PlantCategory.EXPLOSIVE));
    }

    @Test
    void saveOurSeedsDeadLineTimedWarAndPlantLossLimitCauseDeterministicOutcomes() {
        AdventureLevelConfig save = AdventureCatalog.level(
                GameWorld.FROSTBITE_CAVES, 2).getAdventureConfig();
        SimulationWorld saveWorld = initialize(save, null);
        saveWorld.getBoard().tileAt(1, 1).clearPlants();
        runRuleTick(saveWorld);
        assertEquals(GameOutcome.LOST, saveWorld.getOutcome());

        AdventureLevelConfig timed = AdventureCatalog.level(
                GameWorld.FROSTBITE_CAVES, 3).getAdventureConfig();
        SimulationWorld timedWorld = initialize(timed, null);
        for (int i = 0; i < 5; i++) {
            timedWorld.recordZombieKill();
        }
        runRuleTick(timedWorld);
        assertEquals(GameOutcome.WON, timedWorld.getOutcome());

        AdventureLevelConfig deadLine = AdventureCatalog.level(
                GameWorld.BIG_WAVE_BEACH, 3).getAdventureConfig();
        SimulationWorld deadLineWorld = initialize(deadLine, null);
        ZombieInstance zombie = new ZombieInstance(
                zombies.availableSpecs().stream()
                        .filter(spec -> spec.getType() == ZombieType.NORMAL)
                        .findFirst().orElseThrow(),
                1.5, 0);
        deadLineWorld.addZombie(zombie);
        runRuleTick(deadLineWorld);
        assertEquals(GameOutcome.LOST, deadLineWorld.getOutcome());

        AdventureLevelConfig love = AdventureCatalog.level(
                GameWorld.DARK_AGES, 2).getAdventureConfig();
        SimulationWorld loveWorld = initialize(love, null);
        for (int i = 0; i < 5; i++) {
            loveWorld.recordPlantLost();
        }
        runRuleTick(loveWorld);
        assertEquals(GameOutcome.LOST, loveWorld.getOutcome());
    }

    @Test
    void plantWhatYouGetPausesWavesUsesFixedSunAndSkipsPreWaveCooldown() {
        User user = userOwning(PlantType.PEASHOOTER);
        AdventureLevelConfig config = AdventureCatalog.level(
                GameWorld.DARK_AGES, 3).getAdventureConfig();
        SimulationWorld world = initialize(config, user);
        PlantSelection selection = new PlantSelection();
        selection.add(PlantType.PEASHOOTER);
        BoardController board = new BoardController(world, selection, plants);

        assertEquals(800, world.getSunBalance());
        assertFalse(world.areWavesStarted());
        assertFalse(config.isSkySunEnabled());
        assertFalse(config.getSelectionRules().allows(PlantType.SUNFLOWER));
        assertTrue(board.plantPlant(PlantType.PEASHOOTER, 0, 0).getStatus());
        assertTrue(board.plantPlant(PlantType.PEASHOOTER, 1, 0).getStatus());
        assertFalse(world.isOnCooldown(PlantType.PEASHOOTER));

        GameplayController gameplay = new GameplayController(
                new Simulation(new SeededRandomSource(1), world, false));
        assertTrue(gameplay.startZombieWaves().getStatus());
        assertTrue(world.areWavesStarted());
    }

    @Test
    void nightOpsDisablesSkySunAndBossRowsRemainDeferred() {
        assertFalse(AdventureCatalog.level(GameWorld.BIG_WAVE_BEACH, 2)
                .getAdventureConfig().isSkySunEnabled());
        for (GameWorld world : GameWorld.values()) {
            assertTrue(AdventureCatalog.level(world, 4).isBossDeferred());
        }
        assertEquals(SpecialLevelType.NIGHT_OPS,
                AdventureCatalog.level(GameWorld.BIG_WAVE_BEACH, 2)
                        .getAdventureConfig().getSpecialType());
    }

    private SimulationWorld initialize(AdventureLevelConfig config, User user) {
        SimulationWorld world = new SimulationWorld();
        AdventureInitializer.initialize(
                world, config, plants, zombies, user, new SeededRandomSource(0));
        return world;
    }

    private void runRuleTick(SimulationWorld world) {
        Simulation simulation = new Simulation(new SeededRandomSource(0), world, false);
        simulation.register(new AdventureRuleSystem(zombies));
        simulation.advance(1);
    }

    private User userOwning(PlantType... types) {
        User user = new User();
        user.applyDefaults();
        for (PlantType type : types) {
            user.getCollection().purchasePlant(type);
        }
        return user;
    }

    private int packetCount(Map<PlantType, Integer> packets) {
        return packets.values().stream().mapToInt(Integer::intValue).sum();
    }
}
