package model.sim.adventure;

import model.GameEngine;
import model.Tile;
import model.inGame.GameOutcome;
import model.Position;
import model.config.GameWorld;
import model.enums.DamageType;
import model.enums.ObstacleType;
import model.enums.PlantTag;
import model.enums.PlantType;
import model.enums.TerrainType;
import model.enums.ZombieType;
import model.inGame.GameMap;
import model.inGame.plant.Plant;
import model.inGame.zombie.Zombie;
import model.level.AdventureLevelConfig;
import model.level.ChapterRules;
import model.level.SpecialLevelType;
import model.level.TileCoordinate;
import model.level.TimedWarObjective;
import util.RandomSourceAdapter;

import java.util.*;

/** Runs every chapter and special-level transition on GameEngine's tick clock. */
public final class AdventureRuleSystem {
    private static final int ADJACENT_FIRE_MELT_PER_TICK = 6;

    /** Called once per simulation tick (1/10 second), from GameEngine.advance(). */
    public void tick(GameEngine engine) {
        AdventureRuntimeState state = engine.getAdventureState();
        if (state == null || !engine.isRunning()) {
            return;
        }

        // Gameplay may advance several simulation ticks in one rendered frame.
        // The runtime delay is deliberately longer than that batch so the low-tide
        // warning reaches BattlefieldAnnouncementLayer before these zombies spawn.
        if (state.tickLowTideSpawnDelay()) {
            spawnPendingLowTideZombies(engine, state);
        }
        if (state.tickNecromancySpawnDelay()) {
            spawnPendingNecromancyZombies(engine, state);
        }

        state.tickElapsed();
        processNewWave(engine, state);
        tickConveyor(engine, state);
        meltIceNearFire(engine);
        checkSpecialConditions(engine, state);
    }

    private void processNewWave(GameEngine engine, AdventureRuntimeState state) {
        int wave = engine.getCurrentWave();
        if (wave <= 0 || wave <= state.getLastProcessedWave()) {
            return;
        }
        state.setLastProcessedWave(wave);
        ChapterRules rules = state.getConfig().getChapterRules();
        if (rules.getWorld() == GameWorld.FROSTBITE_CAVES) {
            applyIcyWind(engine, rules);
        } else if (rules.getWorld() == GameWorld.BIG_WAVE_BEACH) {
            applyWaterLevel(engine, state, rules, wave);
            queueLowTideZombies(engine, rules, state);
        } else if (rules.getWorld() == GameWorld.DARK_AGES) {
            createDarkAgesGraves(engine, rules);
            queueNecromancy(engine, rules, state);
        }
    }

    private void tickConveyor(GameEngine engine, AdventureRuntimeState state) {
        if (state.getConfig().getSpecialType() != SpecialLevelType.CONVEYOR_BELT
                || !state.conveyorPacketDue()) {
            return;
        }
        PlantType type = state.issueConveyorPacket(RandomSourceAdapter.wrap(engine.getRandom()));
        if (type != null) {
            engine.recordEvent("Conveyor supplied " + type + ".");
        }
    }

    private void checkSpecialConditions(GameEngine engine, AdventureRuntimeState state) {
        AdventureLevelConfig config = state.getConfig();
        SpecialLevelType type = config.getSpecialType();
        if (type == null) {
            return;
        }
        switch (type) {
            case SAVE_OUR_SEEDS -> checkProtectedPlants(engine, state);
            case TIMED_WAR -> checkTimedWar(engine, state);
            case DEAD_LINE -> checkDeadLine(engine, config);
            case LOVE_YOUR_PLANTS -> checkPlantLossLimit(engine, config);
            default -> {
                // Other special types alter setup/selection/planting rather than tick outcome.
            }
        }
    }

    private void checkProtectedPlants(GameEngine engine, AdventureRuntimeState state) {
        GameMap map = engine.getGameMap();
        for (TileCoordinate coordinate : state.getProtectedTiles()) {
            Position pos = new Position(coordinate.getY(), coordinate.getX());
            Tile tile = map.isInside(pos) ? map.getTile(pos) : null;
            if (tile == null || !tile.hasAnyPlant()) {
                engine.setOutcome(GameOutcome.LOST);
                engine.recordEvent("A protected plant was lost; LOSER!!!");
                return;
            }
        }
    }

    private void checkTimedWar(GameEngine engine, AdventureRuntimeState state) {
        AdventureLevelConfig config = state.getConfig();
        int progress = config.getTimedWarObjective() == TimedWarObjective.ZOMBIE_KILLS
                ? engine.getZombieKillCount() : engine.getProducedSunTotal();
        if (progress >= config.getTimedWarTarget()) {
            engine.setOutcome(GameOutcome.WON);
            engine.recordEvent("Timed War target completed.");
            return;
        }
        if (state.getElapsedTicks() >= config.getTimedWarTicks()) {
            engine.setOutcome(GameOutcome.LOST);
            engine.recordEvent("Timed War timer expired; LOSER!!!");
        }
    }

    private void checkDeadLine(GameEngine engine, AdventureLevelConfig config) {
        for (Zombie zombie : engine.getZombies()) {
            if (!zombie.isDead() && zombie.getX() < config.getDeadLineColumn()) {
                engine.setOutcome(GameOutcome.LOST);
                engine.recordEvent("A zombie crossed the dead line; LOSER!!!");
                return;
            }
        }
    }

    private void checkPlantLossLimit(GameEngine engine, AdventureLevelConfig config) {
        if (engine.getPlantLossCount() >= config.getMaximumPlantLosses()) {
            engine.setOutcome(GameOutcome.LOST);
            engine.recordEvent("Too many plants were lost; LOSER!!!");
        }
    }

    private void applyIcyWind(GameEngine engine, ChapterRules rules) {
        GameMap map = engine.getGameMap();
        Random random = engine.getRandom();
        int count = Math.min(map.getRows(), rules.getIcyWindRowsPerWave());
        Set<Integer> rows = new LinkedHashSet<>();
        while (rows.size() < count) {
            rows.add(random.nextInt(map.getRows()));
        }
        for (Plant plant : map.getPlants()) {
            if (plant.getPosition() == null || !rows.contains(plant.getPosition().getRow())
                    || plant.getEffectiveDefinition().hasTag(PlantTag.FIRE)) {
                continue;
            }
            engine.addIceHit(plant);
        }
        if (!rows.isEmpty()) {
            engine.recordEvent("Icy wind affected rows " + rows + ".");
        }
    }

    private void meltIceNearFire(GameEngine engine) {
        GameMap map = engine.getGameMap();
        for (Plant plant : new ArrayList<>(map.getPlants())) {
            if (plant.getPosition() == null || !plant.getBooleanState("FROZEN")) {
                continue;
            }
            boolean hasFireNeighbour = false;
            for (Tile neighbour : engine.neighboursOf(plant.getPosition())) {
                if (isFire(neighbour.getSupportPlant()) || isFire(neighbour.getPrimaryPlant())
                        || isFire(neighbour.getArmorPlant())) {
                    hasFireNeighbour = true;
                    break;
                }
            }
            if (!hasFireNeighbour) {
                continue;
            }
            int row = plant.getPosition().getRow();
            int column = plant.getPosition().getColumn();
            engine.damageObstacleAt(row, column, ADJACENT_FIRE_MELT_PER_TICK, DamageType.NORMAL);
            if (!plant.getBooleanState("FROZEN")) {
                engine.recordEvent("Adjacent fire melted the ice at (" + column + ", " + row + ").");
            }
        }
    }

    private boolean isFire(Plant plant) {
        return plant != null && !plant.isDead() && plant.getEffectiveDefinition().hasTag(PlantTag.FIRE);
    }

    private void applyWaterLevel(GameEngine engine, AdventureRuntimeState state, ChapterRules rules, int wave) {
        GameMap map = engine.getGameMap();
        int targetColumns = rules.waterColumnsForWave(wave);
        int waterStart = map.getColumns() - targetColumns;
        for (int row = 0; row < map.getRows(); row++) {
            for (int column = 0; column < map.getColumns(); column++) {
                Position pos = new Position(row, column);
                Tile tile = map.getTile(pos);
                boolean flooded = column >= waterStart;
                if (flooded) {
                    if (tile.getTerrain() != TerrainType.WATER) {
                        destroyPlantsExposedToWater(engine, tile);
                    }
                    tile.setTerrain(TerrainType.WATER);
                } else if (rules.getLowTideTiles().contains(new TileCoordinate(column, row))) {
                    tile.setTerrain(TerrainType.LOW_TIDE);
                } else {
                    tile.setTerrain(TerrainType.NORMAL_BEACH);
                }
            }
        }
        state.setWaterColumns(targetColumns);
        engine.recordEvent("Water level now covers " + targetColumns + " right-side columns.");
    }

    private void destroyPlantsExposedToWater(GameEngine engine, Tile tile) {
        Plant support = tile.getSupportPlant();
        if (support != null && !waterSafe(support)) {
            engine.destroyPlant(support, "drowned.");
        }
        Plant primary = tile.getPrimaryPlant();
        if (primary != null && !waterSafe(primary)) {
            engine.destroyPlant(primary, "drowned.");
        }
    }

    private boolean waterSafe(Plant plant) {
        return plant.getEffectiveDefinition().hasTag(PlantTag.WATER)
                || plant.getEffectiveType() == PlantType.LILY_PAD;
    }

    private void queueLowTideZombies(
            GameEngine engine,
            ChapterRules rules,
            AdventureRuntimeState state
    ) {
        int waterColumns = state.getWaterColumns();
        if (waterColumns <= 0) {
            return;
        }

        int waterStart = engine.getGameMap().getColumns() - waterColumns;
        List<TileCoordinate> queued = new ArrayList<>();
        for (TileCoordinate coordinate : rules.getLowTideTiles()) {
            if (coordinate.getX() < waterStart) {
                continue;
            }
            state.queueLowTideSpawn(coordinate);
            queued.add(coordinate);
        }

        if (!queued.isEmpty()) {
            engine.recordEvent("Low tide zombies incoming at " + queued + ".");
        }
    }

    private void spawnPendingLowTideZombies(
            GameEngine engine,
            AdventureRuntimeState state
    ) {
        for (TileCoordinate coordinate : state.drainLowTideSpawns()) {
            engine.spawnZombie(
                    ZombieType.NORMAL,
                    coordinate.getY(),
                    coordinate.getX() + 0.5
            );
            engine.recordEvent("A zombie emerged from low tide at " + coordinate + ".");
        }
    }

    private void createDarkAgesGraves(GameEngine engine, ChapterRules rules) {
        GameMap map = engine.getGameMap();
        Random random = engine.getRandom();
        List<Position> valid = new ArrayList<>();
        for (int row = 0; row < map.getRows(); row++) {
            for (int column = 0; column < map.getColumns(); column++) {
                Position pos = new Position(row, column);
                Tile tile = map.getTile(pos);
                if (!tile.hasAnyPlant() && !engine.isGrave(tile)
                        && (tile.getTerrain() == TerrainType.NORMAL_DARK_AGES
                        || tile.getTerrain() == TerrainType.NECROMANCY)) {
                    valid.add(pos);
                }
            }
        }

        int count = Math.min(rules.getDarkGravesPerWave(), valid.size());
        if (count <= 0) {
            return;
        }

        /*
         * Make the chapter's necromancy mechanic reliably observable. The old
         * implementation placed all graves uniformly across ~45 cells, so the two
         * configured necromancy cells almost never contained a grave and the
         * mechanic could go an entire level without occurring.
         *
         * Prefer ONE free configured necromancy cell, then place the remaining
         * graves randomly exactly as before. This preserves the phase-1 count and
         * random grave payloads while making the chapter identity testable.
         */
        Position preferred = chooseFreeNecromancyPosition(map, rules, valid, random);
        int placed = 0;
        if (preferred != null) {
            valid.remove(preferred);
            placeDarkAgesGrave(engine, map, random, preferred);
            placed++;
        }

        while (placed < count && !valid.isEmpty()) {
            Position pos = valid.remove(random.nextInt(valid.size()));
            placeDarkAgesGrave(engine, map, random, pos);
            placed++;
        }
    }

    private Position chooseFreeNecromancyPosition(
            GameMap map,
            ChapterRules rules,
            List<Position> valid,
            Random random
    ) {
        List<Position> candidates = new ArrayList<>();
        for (TileCoordinate coordinate : rules.getNecromancyTiles()) {
            Position pos = new Position(coordinate.getY(), coordinate.getX());
            if (map.isInside(pos) && valid.contains(pos)) {
                candidates.add(pos);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private void placeDarkAgesGrave(
            GameEngine engine,
            GameMap map,
            Random random,
            Position pos
    ) {
        String payload = switch (random.nextInt(3)) {
            case 1 -> "SUN_50";
            case 2 -> "PLANT_FOOD";
            default -> "";
        };
        map.setObstacle(pos, ObstacleType.GRAVE, 700, payload);
        engine.recordEvent("A Dark Ages grave appeared at (" + pos.getColumn()
                + ", " + pos.getRow() + ") containing "
                + (payload.isEmpty() ? "nothing" : payload) + ".");
    }

    private void queueNecromancy(
            GameEngine engine,
            ChapterRules rules,
            AdventureRuntimeState state
    ) {
        GameMap map = engine.getGameMap();
        for (TileCoordinate coordinate : rules.getNecromancyTiles()) {
            Position pos = new Position(coordinate.getY(), coordinate.getX());
            if (!map.isInside(pos) || !engine.isGrave(map.getTile(pos))) {
                continue;
            }
            state.queueNecromancySpawn(coordinate);
            engine.recordEvent("Necromancy is stirring beneath grave " + coordinate + ".");
        }
    }

    private void spawnPendingNecromancyZombies(
            GameEngine engine,
            AdventureRuntimeState state
    ) {
        GameMap map = engine.getGameMap();
        for (TileCoordinate coordinate : state.drainNecromancySpawns()) {
            Position pos = new Position(coordinate.getY(), coordinate.getX());
            // A defensive check keeps the delayed visual flow safe if some future
            // mechanic removes the grave between warning and spawn.
            if (!map.isInside(pos) || !engine.isGrave(map.getTile(pos))) {
                continue;
            }
            engine.spawnZombie(ZombieType.NORMAL, coordinate.getY(), coordinate.getX() + 0.5);
            engine.recordEvent("Necromancy spawned a zombie beneath grave " + coordinate + ".");
        }
    }
}