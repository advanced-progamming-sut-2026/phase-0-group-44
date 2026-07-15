package adventure;

import model.config.AdventureCatalog;
import model.config.GameWorld;
import model.enums.PlantType;
import model.enums.TerrainType;
import model.inGame.plant.PlantRegistry;
import model.inGame.zombie.ZombieRegistry;
import model.level.AdventureLevelConfig;
import model.level.ChapterRules;
import model.sim.Simulation;
import model.sim.SimulationWorld;
import model.sim.adventure.AdventureInitializer;
import model.sim.adventure.AdventureRuleSystem;
import model.sim.board.DefaultPlantSpecSource;
import model.sim.board.PlantInstance;
import model.sim.board.PlantSpecSource;
import model.sim.zombie.DefaultZombieSpecSource;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpecSource;
import org.junit.jupiter.api.Test;
import util.SeededRandomSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureChapterRulesTest {
    private final PlantSpecSource plants =
            new DefaultPlantSpecSource(PlantRegistry.getDefault());
    private final ZombieSpecSource zombies =
            new DefaultZombieSpecSource(ZombieRegistry.getDefault());

    @Test
    void ancientEgyptStartsWithSevenHundredHealthProjectileBlockingGraves() {
        SimulationWorld world = initialize(
                AdventureCatalog.level(GameWorld.ANCIENT_EGYPT, 1).getAdventureConfig());

        var grave = world.getBoard().tileAt(3, 1);
        assertEquals(TerrainType.GRAVESTONE, grave.getTerrain());
        assertEquals(700, grave.getTerrainHealth());
        assertTrue(grave.getTerrain().blocksHorizontalProjectiles());
        assertTrue(world.getAdventureState().getConfig().getChapterRules()
                .hasFinalWaveTornadoes());
    }

    @Test
    void frostbiteUsesThreeFreezeLevelsSixHundredIceAndFrozenZombies() {
        ChapterRules rules = ChapterRules.builder(GameWorld.FROSTBITE_CAVES)
                .icyWindRowsPerWave(5)
                .build();
        AdventureLevelConfig config = AdventureLevelConfig.builder(
                        GameWorld.FROSTBITE_CAVES, 1, "freeze test")
                .chapterRules(rules)
                .build();
        SimulationWorld world = initialize(config);
        PlantInstance plant = new PlantInstance(plants.specOf(PlantType.PEASHOOTER), 1, 1);
        world.getBoard().tileAt(1, 1).setSupportPlant(plant);
        world.addPlant(plant);
        Simulation simulation = new Simulation(new SeededRandomSource(1), world, false);
        simulation.register(new AdventureRuleSystem(zombies));

        for (int wave = 1; wave <= 3; wave++) {
            world.setCurrentWave(wave);
            simulation.advance(1);
        }

        assertEquals(3, plant.getFreezeLevel());
        assertTrue(plant.isFrozen());
        assertEquals(600, plant.getIceHealth());
        assertTrue(world.getBoard().tileAt(1, 1).hasProjectileBlocker());

        SimulationWorld canonical = initialize(
                AdventureCatalog.level(GameWorld.FROSTBITE_CAVES, 1).getAdventureConfig());
        assertTrue(canonical.getZombieInstances().stream()
                .anyMatch(ZombieInstance::isEncasedInIce));
    }

    @Test
    void beachWaterScheduleIsBoundedAndChangesAtWaveStart() {
        AdventureLevelConfig config = AdventureCatalog.level(
                GameWorld.BIG_WAVE_BEACH, 1).getAdventureConfig();
        SimulationWorld world = initialize(config);
        assertEquals(3, world.getAdventureState().getWaterColumns());
        assertEquals(TerrainType.WATER, world.getBoard().tileAt(8, 0).getTerrain());

        Simulation simulation = new Simulation(new SeededRandomSource(2), world, false);
        simulation.register(new AdventureRuleSystem(zombies));
        world.setCurrentWave(2);
        simulation.advance(1);

        assertEquals(5, world.getAdventureState().getWaterColumns());
        assertEquals(TerrainType.WATER, world.getBoard().tileAt(4, 0).getTerrain());
        assertFalse(world.getAdventureState().getWaterColumns()
                > config.getChapterRules().getMaximumWaterColumns());
    }

    @Test
    void darkAgesCreatesVisibleRewardGravesAndHasNoSkySun() {
        AdventureLevelConfig config = AdventureCatalog.level(
                GameWorld.DARK_AGES, 1).getAdventureConfig();
        SimulationWorld world = initialize(config);
        Simulation simulation = new Simulation(new SeededRandomSource(3), world, false);
        simulation.register(new AdventureRuleSystem(zombies));
        world.setCurrentWave(1);
        simulation.advance(1);

        long graves = java.util.stream.IntStream.range(0, world.getRows())
                .boxed()
                .flatMap(y -> java.util.stream.IntStream.range(0, world.getColumns())
                        .mapToObj(x -> world.getBoard().tileAt(x, y)))
                .filter(tile -> tile.getTerrain() == TerrainType.DARK_AGES_GRAVESTONE)
                .count();
        assertEquals(2, graves);
        assertFalse(config.isSkySunEnabled());
    }

    private SimulationWorld initialize(AdventureLevelConfig config) {
        SimulationWorld world = new SimulationWorld();
        AdventureInitializer.initialize(
                world, config, plants, zombies, null, new SeededRandomSource(0));
        return world;
    }
}
