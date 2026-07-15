package model.sim.adventure;

import model.config.GameWorld;
import model.enums.PlantType;
import model.enums.TerrainType;
import model.level.AdventureLevelConfig;
import model.level.ChapterRules;
import model.level.FrozenZombiePlacement;
import model.level.ProtectedPlantPlacement;
import model.level.SpecialLevelType;
import model.level.TileCoordinate;
import model.sim.SimulationWorld;
import model.sim.board.PlantInstance;
import model.sim.board.PlantSpec;
import model.sim.board.PlantSpecSource;
import model.sim.board.Tile;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpec;
import model.sim.zombie.ZombieSpecSource;
import model.user.User;
import util.RandomSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Applies a level's initial terrain, entities, resources and runtime flags. */
public final class AdventureInitializer {
    private AdventureInitializer() {
    }

    public static void initialize(
            SimulationWorld world,
            AdventureLevelConfig config,
            PlantSpecSource plantSpecs,
            ZombieSpecSource zombieSpecs,
            User user,
            RandomSource random
    ) {
        if (world == null || config == null) {
            return;
        }

        setBaseTerrain(world, config.getWorld());
        ChapterRules rules = config.getChapterRules();
        applyChapterTerrain(world, rules);

        Set<TileCoordinate> protectedTiles = preplaceProtectedPlants(
                world, config, plantSpecs);
        List<PlantType> conveyorPool = acquiredConveyorPool(config, user);
        AdventureRuntimeState state = new AdventureRuntimeState(
                config, protectedTiles, conveyorPool);
        world.setAdventureState(state);
        world.addSun(config.getStartingSun());
        world.setWavesStarted(!config.areWavesInitiallyPaused());

        if (config.getSpecialType() == SpecialLevelType.CONVEYOR_BELT) {
            state.issueConveyorPacket(random);
        }

        if (rules.getWorld() == GameWorld.BIG_WAVE_BEACH) {
            applyInitialWater(world, rules, state);
        }

        preplaceFrozenZombies(world, rules, zombieSpecs);
    }

    private static void setBaseTerrain(SimulationWorld world, GameWorld gameWorld) {
        TerrainType terrain = switch (gameWorld) {
            case ANCIENT_EGYPT -> TerrainType.NORMAL_EGYPT;
            case FROSTBITE_CAVES -> TerrainType.NORMAL_FROSTBITE;
            case BIG_WAVE_BEACH -> TerrainType.NORMAL_BEACH;
            case DARK_AGES -> TerrainType.NORMAL_DARK_AGES;
        };
        for (int y = 0; y < world.getRows(); y++) {
            for (int x = 0; x < world.getColumns(); x++) {
                world.getBoard().tileAt(x, y).setTerrain(terrain);
            }
        }
    }

    private static void applyChapterTerrain(SimulationWorld world, ChapterRules rules) {
        for (TileCoordinate coordinate : rules.getInitialGraves()) {
            Tile tile = world.getBoard().tileAt(coordinate.getX(), coordinate.getY());
            if (tile != null) {
                tile.setTerrain(TerrainType.GRAVESTONE);
            }
        }
        for (TileCoordinate coordinate : rules.getSlipperyUpTiles()) {
            Tile tile = world.getBoard().tileAt(coordinate.getX(), coordinate.getY());
            if (tile != null) {
                tile.setTerrain(TerrainType.SLIPPERY_UP);
            }
        }
        for (TileCoordinate coordinate : rules.getSlipperyDownTiles()) {
            Tile tile = world.getBoard().tileAt(coordinate.getX(), coordinate.getY());
            if (tile != null) {
                tile.setTerrain(TerrainType.SLIPPERY_DOWN);
            }
        }
        for (TileCoordinate coordinate : rules.getLowTideTiles()) {
            Tile tile = world.getBoard().tileAt(coordinate.getX(), coordinate.getY());
            if (tile != null) {
                tile.setTerrain(TerrainType.LOW_TIDE);
            }
        }
        for (TileCoordinate coordinate : rules.getNecromancyTiles()) {
            Tile tile = world.getBoard().tileAt(coordinate.getX(), coordinate.getY());
            if (tile != null) {
                tile.setTerrain(TerrainType.NECROMANCY);
            }
        }
    }

    private static Set<TileCoordinate> preplaceProtectedPlants(
            SimulationWorld world,
            AdventureLevelConfig config,
            PlantSpecSource plantSpecs
    ) {
        Set<TileCoordinate> protectedTiles = new LinkedHashSet<>();
        if (plantSpecs == null) {
            return protectedTiles;
        }
        for (ProtectedPlantPlacement placement : config.getProtectedPlants()) {
            PlantSpec spec = plantSpecs.specOf(placement.getType());
            TileCoordinate coordinate = placement.getCoordinate();
            Tile tile = world.getBoard().tileAt(coordinate.getX(), coordinate.getY());
            if (spec == null || tile == null || tile.hasAnyPlant()) {
                throw new IllegalStateException("Invalid protected plant placement: " + coordinate);
            }
            PlantInstance plant = new PlantInstance(spec, coordinate.getX(), coordinate.getY());
            tile.setSupportPlant(plant);
            world.addPlant(plant);
            protectedTiles.add(coordinate);
        }
        return protectedTiles;
    }

    private static List<PlantType> acquiredConveyorPool(
            AdventureLevelConfig config,
            User user
    ) {
        List<PlantType> result = new ArrayList<>();
        for (PlantType type : config.getConveyorCandidates()) {
            if (user == null || user.getCollection().hasPlant(type)) {
                result.add(type);
            }
        }
        return result;
    }

    private static void applyInitialWater(
            SimulationWorld world,
            ChapterRules rules,
            AdventureRuntimeState state
    ) {
        int columns = rules.waterColumnsForWave(1);
        int start = world.getColumns() - columns;
        for (int y = 0; y < world.getRows(); y++) {
            for (int x = Math.max(0, start); x < world.getColumns(); x++) {
                world.getBoard().tileAt(x, y).setTerrain(TerrainType.WATER);
            }
        }
        state.setWaterColumns(columns);
    }

    private static void preplaceFrozenZombies(
            SimulationWorld world,
            ChapterRules rules,
            ZombieSpecSource zombieSpecs
    ) {
        if (zombieSpecs == null) {
            return;
        }
        for (FrozenZombiePlacement placement : rules.getFrozenZombies()) {
            ZombieSpec spec = findSpec(zombieSpecs, placement.getType());
            if (spec == null) {
                continue;
            }
            ZombieInstance zombie = new ZombieInstance(spec, placement.getX(), placement.getRow());
            zombie.freezeInIce();
            world.addZombie(zombie);
        }
    }

    private static ZombieSpec findSpec(
            ZombieSpecSource source,
            model.enums.ZombieType type
    ) {
        for (ZombieSpec spec : source.availableSpecs()) {
            if (spec.getType() == type) {
                return spec;
            }
        }
        return null;
    }
}
