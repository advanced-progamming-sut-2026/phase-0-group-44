package model.sim.adventure;

import model.GameEngine;
import model.Position;
import model.config.GameWorld;
import model.enums.ObstacleType;
import model.enums.PlantType;
import model.enums.TerrainType;
import model.inGame.plant.Plant;
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
            GameEngine engine,
            AdventureLevelConfig config,
            User user,
            RandomSource random
    ) {
        if (engine == null || config == null) {
            return;
        }

        setBaseTerrain(engine, config.getWorld());
        ChapterRules rules = config.getChapterRules();
        applyChapterTerrain(engine, rules);

        Set<TileCoordinate> protectedTiles = preplaceProtectedPlants(engine, config);
        List<PlantType> conveyorPool = acquiredConveyorPool(config, user);
        AdventureRuntimeState state = new AdventureRuntimeState(config, protectedTiles, conveyorPool);
        engine.setAdventureState(state);
        engine.addSun(config.getStartingSun());
        engine.setWavesStarted(!config.areWavesInitiallyPaused());

        if (config.getSpecialType() == SpecialLevelType.CONVEYOR_BELT) {
            state.issueConveyorPacket(random);
        }

        if (rules.getWorld() == GameWorld.BIG_WAVE_BEACH) {
            applyInitialWater(engine, rules, state);
        }

        preplaceFrozenZombies(engine, rules);
    }

    private static void setBaseTerrain(GameEngine engine, GameWorld gameWorld) {
        TerrainType terrain = switch (gameWorld) {
            case ANCIENT_EGYPT -> TerrainType.NORMAL_EGYPT;
            case FROSTBITE_CAVES -> TerrainType.NORMAL_FROSTBITE;
            case BIG_WAVE_BEACH -> TerrainType.NORMAL_BEACH;
            case DARK_AGES -> TerrainType.NORMAL_DARK_AGES;
        };
        var map = engine.getGameMap();
        for (int row = 0; row < map.getRows(); row++) {
            for (int column = 0; column < map.getColumns(); column++) {
                map.getTile(row, column).setTerrain(terrain);
            }
        }
    }

    /** {@code NORMAL_EGYPT} chapter graves are an obstacle overlay, not a terrain type (world A). */
    private static void applyChapterTerrain(GameEngine engine, ChapterRules rules) {
        var map = engine.getGameMap();
        for (TileCoordinate coordinate : rules.getInitialGraves()) {
            Position pos = new Position(coordinate.getY(), coordinate.getX());
            if (map.isInside(pos)) {
                map.setObstacle(pos, ObstacleType.GRAVE, 700, "");
            }
        }
        for (TileCoordinate coordinate : rules.getSlipperyUpTiles()) {
            setTerrainIfInside(map, coordinate, TerrainType.SLIPPERY_UP);
        }
        for (TileCoordinate coordinate : rules.getSlipperyDownTiles()) {
            setTerrainIfInside(map, coordinate, TerrainType.SLIPPERY_DOWN);
        }
        for (TileCoordinate coordinate : rules.getLowTideTiles()) {
            setTerrainIfInside(map, coordinate, TerrainType.LOW_TIDE);
        }
        for (TileCoordinate coordinate : rules.getNecromancyTiles()) {
            setTerrainIfInside(map, coordinate, TerrainType.NECROMANCY);
        }
    }

    private static void setTerrainIfInside(model.inGame.GameMap map, TileCoordinate coordinate, TerrainType terrain) {
        Position pos = new Position(coordinate.getY(), coordinate.getX());
        if (map.isInside(pos)) {
            map.getTile(pos).setTerrain(terrain);
        }
    }

    private static Set<TileCoordinate> preplaceProtectedPlants(GameEngine engine, AdventureLevelConfig config) {
        Set<TileCoordinate> protectedTiles = new LinkedHashSet<>();
        var map = engine.getGameMap();
        for (ProtectedPlantPlacement placement : config.getProtectedPlants()) {
            TileCoordinate coordinate = placement.getCoordinate();
            Position pos = new Position(coordinate.getY(), coordinate.getX());
            if (!map.isInside(pos) || map.getTile(pos).hasAnyPlant()) {
                throw new IllegalStateException("Invalid protected plant placement: " + coordinate);
            }
            Plant plant = engine.getPlantFactory().create(placement.getType(), 1);
            engine.placePlantForFree(plant, pos);
            protectedTiles.add(coordinate);
        }
        return protectedTiles;
    }

    private static List<PlantType> acquiredConveyorPool(AdventureLevelConfig config, User user) {
        List<PlantType> result = new ArrayList<>();
        for (PlantType type : config.getConveyorCandidates()) {
            if (user == null || user.getCollection().hasPlant(type)) {
                result.add(type);
            }
        }
        return result;
    }

    private static void applyInitialWater(GameEngine engine, ChapterRules rules, AdventureRuntimeState state) {
        var map = engine.getGameMap();
        int columns = rules.waterColumnsForWave(1);
        int start = map.getColumns() - columns;
        for (int row = 0; row < map.getRows(); row++) {
            for (int column = Math.max(0, start); column < map.getColumns(); column++) {
                map.getTile(row, column).setTerrain(TerrainType.WATER);
            }
        }
        state.setWaterColumns(columns);
    }

    private static void preplaceFrozenZombies(GameEngine engine, ChapterRules rules) {
        for (FrozenZombiePlacement placement : rules.getFrozenZombies()) {
            model.inGame.zombie.Zombie zombie =
                    engine.spawnZombie(placement.getType(), placement.getRow(), placement.getX());
            zombie.applyFreeze(Double.MAX_VALUE);
        }
    }
}