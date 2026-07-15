package model.sim.adventure;

import model.config.GameWorld;
import model.enums.PlantType;
import model.enums.TerrainType;
import model.enums.ZombieType;
import model.level.AdventureLevelConfig;
import model.level.ChapterRules;
import model.level.SpecialLevelType;
import model.level.TileCoordinate;
import model.level.TimedWarObjective;
import model.sim.GameOutcome;
import model.sim.SimulationSystem;
import model.sim.SimulationWorld;
import model.sim.TickContext;
import model.sim.board.PlantInstance;
import model.sim.board.Tile;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpec;
import model.sim.zombie.ZombieSpecSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Runs every chapter and special-level transition on the shared simulation clock. */
public final class AdventureRuleSystem implements SimulationSystem {
    private static final int ADJACENT_FIRE_MELT_PER_TICK = 6;

    private final ZombieSpecSource zombieSpecs;

    public AdventureRuleSystem(ZombieSpecSource zombieSpecs) {
        this.zombieSpecs = zombieSpecs;
    }

    @Override
    public void tick(TickContext context) {
        SimulationWorld world = context.getWorld();
        AdventureRuntimeState state = world.getAdventureState();
        if (state == null || !world.isRunning()) {
            return;
        }

        state.tickElapsed();
        processNewWave(context, world, state);
        tickConveyor(context, state);
        meltIceNearFire(world, context);
        checkSpecialConditions(context, world, state);
    }

    private void processNewWave(
            TickContext context,
            SimulationWorld world,
            AdventureRuntimeState state
    ) {
        int wave = world.getCurrentWave();
        if (wave <= 0 || wave <= state.getLastProcessedWave()) {
            return;
        }
        state.setLastProcessedWave(wave);
        ChapterRules rules = state.getConfig().getChapterRules();
        if (rules.getWorld() == GameWorld.FROSTBITE_CAVES) {
            applyIcyWind(context, world, rules);
        } else if (rules.getWorld() == GameWorld.BIG_WAVE_BEACH) {
            applyWaterLevel(context, world, state, rules, wave);
            spawnLowTideZombies(context, world, rules, state.getWaterColumns());
        } else if (rules.getWorld() == GameWorld.DARK_AGES) {
            createDarkAgesGraves(context, world, rules);
            runNecromancy(context, world, rules);
        }
    }

    private void tickConveyor(TickContext context, AdventureRuntimeState state) {
        if (state.getConfig().getSpecialType() != SpecialLevelType.CONVEYOR_BELT
                || !state.conveyorPacketDue()) {
            return;
        }
        PlantType type = state.issueConveyorPacket(context.getRandom());
        if (type != null) {
            context.emit("Conveyor supplied " + type + ".");
        }
    }

    private void checkSpecialConditions(
            TickContext context,
            SimulationWorld world,
            AdventureRuntimeState state
    ) {
        AdventureLevelConfig config = state.getConfig();
        SpecialLevelType type = config.getSpecialType();
        if (type == null) {
            return;
        }
        switch (type) {
            case SAVE_OUR_SEEDS -> checkProtectedPlants(context, world, state);
            case TIMED_WAR -> checkTimedWar(context, world, state);
            case DEAD_LINE -> checkDeadLine(context, world, config);
            case LOVE_YOUR_PLANTS -> checkPlantLossLimit(context, world, config);
            default -> {
                // Other special types alter setup/selection/planting rather than tick outcome.
            }
        }
    }

    private void checkProtectedPlants(
            TickContext context,
            SimulationWorld world,
            AdventureRuntimeState state
    ) {
        for (TileCoordinate coordinate : state.getProtectedTiles()) {
            Tile tile = world.getBoard().tileAt(coordinate.getX(), coordinate.getY());
            if (tile == null || !tile.hasAnyPlant()) {
                world.setOutcome(GameOutcome.LOST);
                context.emit("A protected plant was lost; LOSER!!!");
                return;
            }
        }
    }

    private void checkTimedWar(
            TickContext context,
            SimulationWorld world,
            AdventureRuntimeState state
    ) {
        AdventureLevelConfig config = state.getConfig();
        int progress = config.getTimedWarObjective() == TimedWarObjective.ZOMBIE_KILLS
                ? world.getZombieKillCount() : world.getProducedSunTotal();
        if (progress >= config.getTimedWarTarget()) {
            world.setOutcome(GameOutcome.WON);
            context.emit("Timed War target completed.");
            return;
        }
        if (state.getElapsedTicks() >= config.getTimedWarTicks()) {
            world.setOutcome(GameOutcome.LOST);
            context.emit("Timed War timer expired; LOSER!!!");
        }
    }

    private void checkDeadLine(
            TickContext context,
            SimulationWorld world,
            AdventureLevelConfig config
    ) {
        for (ZombieInstance zombie : world.getZombieInstances()) {
            if (!zombie.isDead() && zombie.getX() < config.getDeadLineColumn()) {
                world.setOutcome(GameOutcome.LOST);
                context.emit("A zombie crossed the dead line; LOSER!!!");
                return;
            }
        }
    }

    private void checkPlantLossLimit(
            TickContext context,
            SimulationWorld world,
            AdventureLevelConfig config
    ) {
        if (world.getPlantLossCount() >= config.getMaximumPlantLosses()) {
            world.setOutcome(GameOutcome.LOST);
            context.emit("Too many plants were lost; LOSER!!!");
        }
    }

    private void applyIcyWind(
            TickContext context,
            SimulationWorld world,
            ChapterRules rules
    ) {
        int count = Math.min(world.getRows(), rules.getIcyWindRowsPerWave());
        Set<Integer> rows = new LinkedHashSet<>();
        while (rows.size() < count) {
            rows.add(context.getRandom().nextInt(world.getRows()));
        }
        for (model.sim.Damageable damageable : world.getPlants()) {
            if (!(damageable instanceof PlantInstance plant)
                    || !rows.contains(plant.getTileY())
                    || plant.getSpec().isFire()) {
                continue;
            }
            applyFreezeLevel(world, plant);
        }
        if (!rows.isEmpty()) {
            context.emit("Icy wind affected rows " + rows + ".");
        }
    }

    private void applyFreezeLevel(SimulationWorld world, PlantInstance plant) {
        if (plant.addFreezeLevel()) {
            Tile tile = world.getBoard().tileAt(plant.getTileX(), plant.getTileY());
            if (tile != null) {
                tile.setProjectileBlocker("ice", PlantInstance.ICE_HEALTH);
            }
        }
    }

    private void meltIceNearFire(SimulationWorld world, TickContext context) {
        for (model.sim.Damageable damageable : new ArrayList<>(world.getPlants())) {
            if (!(damageable instanceof PlantInstance plant) || !plant.isFrozen()) {
                continue;
            }
            boolean hasFireNeighbour = false;
            for (Tile neighbour : world.getBoard().neighboursOf(
                    plant.getTileX(), plant.getTileY())) {
                if (isFire(neighbour.getSupportPlant()) || isFire(neighbour.getStackedPlant())) {
                    hasFireNeighbour = true;
                    break;
                }
            }
            if (!hasFireNeighbour) {
                continue;
            }
            boolean melted = plant.damageIce(ADJACENT_FIRE_MELT_PER_TICK, false);
            Tile tile = world.getBoard().tileAt(plant.getTileX(), plant.getTileY());
            if (tile != null) {
                tile.damageProjectileBlocker(ADJACENT_FIRE_MELT_PER_TICK);
            }
            if (melted) {
                context.emit("Adjacent fire melted the ice at ("
                        + plant.getTileX() + ", " + plant.getTileY() + ").");
            }
        }
    }

    private boolean isFire(PlantInstance plant) {
        return plant != null && plant.getSpec().isFire() && !plant.isDead();
    }

    private void applyWaterLevel(
            TickContext context,
            SimulationWorld world,
            AdventureRuntimeState state,
            ChapterRules rules,
            int wave
    ) {
        int targetColumns = rules.waterColumnsForWave(wave);
        int waterStart = world.getColumns() - targetColumns;
        for (int y = 0; y < world.getRows(); y++) {
            for (int x = 0; x < world.getColumns(); x++) {
                Tile tile = world.getBoard().tileAt(x, y);
                boolean flooded = x >= waterStart;
                if (flooded) {
                    if (tile.getTerrain() != TerrainType.WATER) {
                        destroyPlantsExposedToWater(context, world, tile);
                    }
                    tile.setTerrain(TerrainType.WATER);
                } else if (rules.getLowTideTiles().contains(new TileCoordinate(x, y))) {
                    tile.setTerrain(TerrainType.LOW_TIDE);
                } else {
                    tile.setTerrain(TerrainType.NORMAL_BEACH);
                }
            }
        }
        state.setWaterColumns(targetColumns);
        context.emit("Water level now covers " + targetColumns + " right-side columns.");
    }

    private void destroyPlantsExposedToWater(
            TickContext context,
            SimulationWorld world,
            Tile tile
    ) {
        PlantInstance support = tile.getSupportPlant();
        if (support == null || support.getSpec().isWaterCapable()
                || support.getSpec().providesSupport()) {
            return;
        }
        destroyPlant(context, world, tile, support);
        PlantInstance stacked = tile.getStackedPlant();
        if (stacked != null) {
            destroyPlant(context, world, tile, stacked);
        }
    }

    private void destroyPlant(
            TickContext context,
            SimulationWorld world,
            Tile tile,
            PlantInstance plant
    ) {
        if (plant == null) {
            return;
        }
        if (tile.getStackedPlant() == plant) {
            tile.setStackedPlant(null);
        } else if (tile.getSupportPlant() == plant) {
            tile.setSupportPlant(null);
        }
        world.getPlants().remove(plant);
        world.recordPlantLost();
        context.emit("Plant " + plant.getType() + " at (" + plant.getTileX()
                + ", " + plant.getTileY() + ") drowned.");
    }

    private void spawnLowTideZombies(
            TickContext context,
            SimulationWorld world,
            ChapterRules rules,
            int waterColumns
    ) {
        if (zombieSpecs == null || waterColumns <= 0) {
            return;
        }
        int waterStart = world.getColumns() - waterColumns;
        ZombieSpec normal = findSpec(ZombieType.NORMAL);
        if (normal == null) {
            return;
        }
        for (TileCoordinate coordinate : rules.getLowTideTiles()) {
            if (coordinate.getX() < waterStart) {
                continue;
            }
            ZombieInstance zombie = new ZombieInstance(
                    normal, coordinate.getX() + 0.5, coordinate.getY());
            world.addZombie(zombie);
            context.emit("A zombie emerged from flooded low tide at " + coordinate + ".");
        }
    }

    private void createDarkAgesGraves(
            TickContext context,
            SimulationWorld world,
            ChapterRules rules
    ) {
        List<Tile> valid = new ArrayList<>();
        for (int y = 0; y < world.getRows(); y++) {
            for (int x = 0; x < world.getColumns(); x++) {
                Tile tile = world.getBoard().tileAt(x, y);
                if (!tile.hasAnyPlant() && !tile.isGravestone()
                        && (tile.getTerrain() == TerrainType.NORMAL_DARK_AGES
                        || tile.getTerrain() == TerrainType.NECROMANCY)) {
                    valid.add(tile);
                }
            }
        }
        int count = Math.min(rules.getDarkGravesPerWave(), valid.size());
        for (int i = 0; i < count; i++) {
            Tile tile = valid.remove(context.getRandom().nextInt(valid.size()));
            tile.setTerrain(TerrainType.DARK_AGES_GRAVESTONE);
            GraveReward reward = switch (context.getRandom().nextInt(3)) {
                case 1 -> GraveReward.SUN_50;
                case 2 -> GraveReward.PLANT_FOOD;
                default -> GraveReward.NONE;
            };
            tile.setGraveReward(reward);
            context.emit("A Dark Ages grave appeared at (" + tile.getColumn()
                    + ", " + tile.getRow() + ") containing " + reward + ".");
        }
    }

    private void runNecromancy(
            TickContext context,
            SimulationWorld world,
            ChapterRules rules
    ) {
        ZombieSpec normal = findSpec(ZombieType.NORMAL);
        if (normal == null) {
            return;
        }
        for (TileCoordinate coordinate : rules.getNecromancyTiles()) {
            Tile tile = world.getBoard().tileAt(coordinate.getX(), coordinate.getY());
            if (tile == null || !tile.isGravestone()) {
                continue;
            }
            world.addZombie(new ZombieInstance(
                    normal, coordinate.getX() + 0.5, coordinate.getY()));
            context.emit("Necromancy spawned a zombie beneath grave " + coordinate + ".");
        }
    }

    private ZombieSpec findSpec(ZombieType type) {
        if (zombieSpecs == null) {
            return null;
        }
        for (ZombieSpec spec : zombieSpecs.availableSpecs()) {
            if (spec.getType() == type) {
                return spec;
            }
        }
        return null;
    }
}
