package model.miniGame;

import controller.MiniGameBoardController;
import model.Result;
import model.enums.PlantType;
import model.inGame.PlantSelection;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRegistry;
import model.inGame.zombie.ZombieDefinition;
import model.inGame.zombie.ZombieRegistry;
import model.sim.GameOutcome;
import model.sim.SimulationSystem;
import model.sim.SimulationWorld;
import model.sim.TickContext;
import model.sim.board.DefaultPlantSpecSource;
import model.sim.board.PlantInstance;
import model.sim.board.Tile;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpec;
import util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

/** Selection, finite waves, canonical plant combat, and trait overrides for Zombotany. */
public final class ZombotanyState {
    public static final int JALAPENO_TRIGGER_TICKS = 100;
    public static final int PEASHOOTER_FIRE_INTERVAL_TICKS = 20;
    public static final double PEA_SPEED_TILES_PER_SECOND = 3.0;

    private final ZombotanyLevelRules rules;
    private final RandomSource random;
    private final PlantSelection selection = new PlantSelection();
    private Set<PlantType> availablePlants;
    private final PlantRegistry plantRegistry = PlantRegistry.getDefault();
    private final ZombieRegistry zombieRegistry = ZombieRegistry.getDefault();
    private final DefaultPlantSpecSource plantSpecs = new DefaultPlantSpecSource(plantRegistry);
    private final Map<ZombieInstance, VariantState> variants = new IdentityHashMap<>();
    private final Map<PlantInstance, Long> nextPlantAttackTick = new IdentityHashMap<>();
    private final List<ZombotanyProjectile> projectiles = new ArrayList<>();
    private int spawned;
    private long nextSpawnTick = 1;
    private boolean initialized;

    public ZombotanyState(ZombotanyLevelRules rules, RandomSource random) {
        if (rules == null || random == null) {
            throw new IllegalArgumentException("Zombotany state dependencies are required.");
        }
        this.rules = rules;
        this.random = random;
        this.availablePlants = new LinkedHashSet<>();
        for (PlantDefinition definition : plantRegistry.findMandatory()) {
            this.availablePlants.add(definition.getType());
        }
    }

    public ZombotanyLevelRules getRules() { return rules; }
    public PlantSelection getSelection() { return selection; }
    public List<ZombotanyProjectile> getProjectiles() {
        return Collections.unmodifiableList(projectiles);
    }
    public Map<ZombieInstance, ZombotanyTrait> getVariants() {
        Map<ZombieInstance, ZombotanyTrait> result = new IdentityHashMap<>();
        variants.forEach((zombie, state) -> result.put(zombie, state.trait));
        return Collections.unmodifiableMap(result);
    }

    /** Production selection is restricted to the owner's unlocked mandatory plants. */
    public void restrictAvailablePlants(Set<PlantType> owned) {
        if (initialized) {
            throw new IllegalStateException("Plant availability must be set before start.");
        }
        availablePlants = new LinkedHashSet<>();
        if (owned != null) {
            for (PlantType type : owned) {
                try {
                    plantRegistry.requireMandatory(type);
                    availablePlants.add(type);
                } catch (RuntimeException ignored) {
                    // Bonus/unknown rows are deliberately excluded.
                }
            }
        }
        for (PlantType chosen : new ArrayList<>(selection.getChosen())) {
            if (!availablePlants.contains(chosen)) selection.remove(chosen);
        }
    }

    public Result<String> addSelectedPlant(String token) {
        Result<String> result = new Result<>();
        PlantType type = parsePlant(token);
        if (type == null) {
            result.appendToMessage("unknown plant");
            return result;
        }
        try {
            plantRegistry.requireMandatory(type);
        } catch (RuntimeException exception) {
            result.appendToMessage("that plant is not available in mandatory Zombotany");
            return result;
        }
        if (!availablePlants.contains(type)) {
            result.appendToMessage("that plant is not unlocked for this profile");
            return result;
        }
        if (selection.contains(type)) {
            result.appendToMessage("this plant is already selected");
            return result;
        }
        if (selection.size() >= rules.getSelectionCapacity()) {
            result.appendToMessage("all selection slots are full");
            return result;
        }
        selection.add(type);
        result.setStatus(true);
        result.setData(type.name());
        result.appendToMessage("added " + type.name());
        return result;
    }

    public Result<String> removeSelectedPlant(String token) {
        Result<String> result = new Result<>();
        PlantType type = parsePlant(token);
        if (type == null || !selection.contains(type)) {
            result.appendToMessage("this plant is not selected");
            return result;
        }
        selection.remove(type);
        result.setStatus(true);
        result.setData(type.name());
        result.appendToMessage("removed " + type.name());
        return result;
    }

    public String selectionStatus() {
        return "selected plants (" + selection.size() + "/"
                + rules.getSelectionCapacity() + "): "
                + (selection.isEmpty() ? "none" : selection.getChosen());
    }

    public String availablePlants() {
        return "available mandatory plants: " + availablePlants;
    }

    public void initialize() {
        initialized = true;
    }

    public Result<String> plant(MiniGameSession session, String token, int x, int y) {
        Result<String> result = new Result<>();
        if (!initialized) {
            result.appendToMessage("Zombotany has not started");
            return result;
        }
        PlantType type = parsePlant(token);
        MiniGameBoardController board = new MiniGameBoardController(
                session.getSimulation().getWorld(), selection, plantSpecs);
        return board.plantPlant(type, x, y);
    }

    public Result<String> collectSun(MiniGameSession session, int x, int y) {
        Result<String> result = new Result<>();
        var outcome = session.getSimulation().collectSun(x, y);
        if (!outcome.isCollected()) {
            result.appendToMessage("no sun at (" + x + ", " + y + ")");
            return result;
        }
        result.setStatus(true);
        result.setData(String.valueOf(session.getSimulation().getSunAmount()));
        result.appendToMessage(outcome.isExploded()
                ? "radioactive sun exploded"
                : "collected " + outcome.getGained() + " sun; total "
                + session.getSimulation().getSunAmount());
        return result;
    }

    public SimulationSystem traitAndPlantSystem() {
        return context -> {
            if (!context.getWorld().isRunning()) return;
            long completedTick = context.getCurrentTick() + 1;
            if (spawned < rules.getZombieCount() && completedTick >= nextSpawnTick) {
                spawn(context, completedTick);
                nextSpawnTick = completedTick + rules.getSpawnIntervalTicks();
            }
            handleJalapenos(context, completedTick);
            handleSquashes(context);
            firePeas(context, completedTick);
            movePeas(context);
            attackWithPlants(context, completedTick);
        };
    }

    public SimulationSystem cleanupSystem() {
        return context -> {
            variants.keySet().removeIf(zombie -> zombie.isDead()
                    || !context.getWorld().getZombies().contains(zombie));
            projectiles.removeIf(projectile -> !projectile.isActive());
            nextPlantAttackTick.keySet().removeIf(PlantInstance::isDead);
        };
    }

    public GameOutcome evaluate(SimulationWorld world) {
        if (world.getOutcome() == GameOutcome.LOST) return GameOutcome.LOST;
        if (spawned >= rules.getZombieCount() && world.getZombieInstances().isEmpty()) {
            world.setOutcome(GameOutcome.WON);
            return GameOutcome.WON;
        }
        return GameOutcome.RUNNING;
    }

    public String status(MiniGameSession session) {
        SimulationWorld world = session.getSimulation().getWorld();
        return "Zombotany level " + rules.getLevel()
                + "\nselected: " + selection.getChosen()
                + "\nsun: " + world.getSunBalance()
                + "\nspawned: " + spawned + "/" + rules.getZombieCount()
                + "\nlive variants: " + variants.size()
                + "\npea projectiles: " + projectiles.size()
                + "\ntick: " + session.getSimulation().getCurrentTick();
    }

    private void spawn(TickContext context, long completedTick) {
        ZombotanyTrait trait = rules.getSequence().get(spawned % rules.getSequence().size());
        int row = spawned % context.getWorld().getRows();
        spawned++;
        ZombieInstance zombie = createVariant(trait, context.getWorld(), row);
        context.getWorld().addZombie(zombie);
        variants.put(zombie, new VariantState(trait, completedTick));
        context.emit(displayName(trait) + " entered row " + row + ".");
    }

    private ZombieInstance createVariant(
            ZombotanyTrait trait,
            SimulationWorld world,
            int row
    ) {
        ZombieDefinition normal = zombieRegistry.requireMandatory(model.enums.ZombieType.NORMAL);
        int health = normal.getHealth();
        double speed = normal.getSpeedTilesPerSecond();
        int eat = normal.getEatDamagePerSecond();
        if (trait == ZombotanyTrait.WALL_NUT) {
            health = plantRegistry.requireMandatory(PlantType.WALL_NUT).getHp();
        } else if (trait == ZombotanyTrait.SQUASH) {
            speed *= 3.5;
            eat = 0;
        }
        ZombieSpec spec = ZombieSpec.builder(displayName(trait))
                .health(health)
                .speedTilesPerSecond(speed)
                .eatDamagePerSecond(eat)
                .waveCost(normal.getWaveCost())
                .build();
        return new ZombieInstance(spec, world.getColumns() - 0.01, row);
    }

    private void handleJalapenos(TickContext context, long completedTick) {
        for (Map.Entry<ZombieInstance, VariantState> entry : variants.entrySet()) {
            ZombieInstance zombie = entry.getKey();
            VariantState state = entry.getValue();
            if (state.trait != ZombotanyTrait.JALAPENO || state.triggered
                    || zombie.isDead() || completedTick - state.spawnTick < JALAPENO_TRIGGER_TICKS) {
                continue;
            }
            state.triggered = true;
            int destroyed = 0;
            for (int x = 0; x < context.getWorld().getColumns(); x++) {
                Tile tile = context.getWorld().getBoard().tileAt(x, zombie.getRow());
                PlantInstance plant = topPlant(tile);
                if (plant != null) {
                    destroyPlant(context, plant);
                    destroyed++;
                }
            }
            context.emit("Jalapeno Zombie burned row " + zombie.getRow()
                    + " and destroyed " + destroyed + " plant(s).");
        }
    }

    private void handleSquashes(TickContext context) {
        for (Map.Entry<ZombieInstance, VariantState> entry : new ArrayList<>(variants.entrySet())) {
            ZombieInstance zombie = entry.getKey();
            if (entry.getValue().trait != ZombotanyTrait.SQUASH || zombie.isDead()) continue;
            int x = zombie.getTileX();
            if (x < 0 || x >= context.getWorld().getColumns()) continue;
            PlantInstance plant = topPlant(
                    context.getWorld().getBoard().tileAt(x, zombie.getRow()));
            if (plant == null) continue;
            destroyPlant(context, plant);
            zombie.kill();
            context.emit("Squash Zombie destroyed itself and " + plant.getType()
                    + " at (" + x + ", " + zombie.getRow() + ").");
        }
    }

    private void firePeas(TickContext context, long completedTick) {
        int damage = plantRegistry.requireMandatory(PlantType.PEASHOOTER).getDamage();
        for (Map.Entry<ZombieInstance, VariantState> entry : variants.entrySet()) {
            ZombieInstance zombie = entry.getKey();
            VariantState state = entry.getValue();
            if (state.trait != ZombotanyTrait.PEASHOOTER || zombie.isDead()
                    || completedTick < state.nextShotTick
                    || !hasPlantLeft(context.getWorld(), zombie)) {
                continue;
            }
            state.nextShotTick = completedTick + PEASHOOTER_FIRE_INTERVAL_TICKS;
            projectiles.add(new ZombotanyProjectile(
                    zombie.getX() - 0.1, zombie.getRow(), damage, PEA_SPEED_TILES_PER_SECOND));
            context.emit("Peashooter Zombie fired a pea left in row " + zombie.getRow() + ".");
        }
    }

    private void movePeas(TickContext context) {
        for (ZombotanyProjectile projectile : projectiles) {
            if (!projectile.isActive()) continue;
            projectile.advanceOneTick();
            if (!projectile.isActive()) continue;
            int x = (int) Math.floor(projectile.getX());
            if (x < 0 || x >= context.getWorld().getColumns()) continue;
            PlantInstance plant = topPlant(
                    context.getWorld().getBoard().tileAt(x, projectile.getRow()));
            if (plant == null) continue;
            plant.takeDamage(projectile.getDamage());
            projectile.deactivate();
            context.emit("Peashooter Zombie pea hit " + plant.getType()
                    + " for " + projectile.getDamage() + ".");
            if (plant.isDead()) destroyPlant(context, plant);
        }
    }

    private void attackWithPlants(TickContext context, long completedTick) {
        for (model.sim.Damageable damageable : new ArrayList<>(context.getWorld().getPlants())) {
            if (!(damageable instanceof PlantInstance plant) || !plant.isActive()) continue;
            long due = nextPlantAttackTick.getOrDefault(plant, 1L);
            if (completedTick < due) continue;
            PlantDefinition definition = plantRegistry.requireMandatory(plant.getType());
            int interval = Math.max(1, (int) Math.round(
                    definition.getActionInterval() * TickContext.TICKS_PER_SECOND));
            nextPlantAttackTick.put(plant, completedTick + interval);
            int damage = definition.getDamage()
                    * Math.max(1, definition.getDamageProfile().getProjectileCount());
            if (damage <= 0) continue;
            ZombieInstance target = nearestTarget(context.getWorld(), plant);
            if (target == null) continue;
            target.takeDamage(damage);
            context.emit(plant.getType() + " hit " + target.getSpec().getName()
                    + " for " + damage + ".");
        }
    }

    private ZombieInstance nearestTarget(SimulationWorld world, PlantInstance plant) {
        ZombieInstance nearest = null;
        for (ZombieInstance zombie : world.getZombieInstances()) {
            if (zombie.isDead() || zombie.getRow() != plant.getTileY()
                    || zombie.getX() < plant.getTileX()) continue;
            if (nearest == null || zombie.getX() < nearest.getX()) nearest = zombie;
        }
        return nearest;
    }

    private boolean hasPlantLeft(SimulationWorld world, ZombieInstance zombie) {
        for (model.sim.Damageable damageable : world.getPlants()) {
            if (damageable instanceof PlantInstance plant && !plant.isDead()
                    && plant.getTileY() == zombie.getRow()
                    && plant.getTileX() < zombie.getX()) return true;
        }
        return false;
    }

    private void destroyPlant(TickContext context, PlantInstance plant) {
        Tile tile = context.getWorld().getBoard().tileAt(plant.getTileX(), plant.getTileY());
        if (tile != null) {
            if (tile.getStackedPlant() == plant) tile.setStackedPlant(null);
            else if (tile.getSupportPlant() == plant) tile.clearPlants();
        }
        context.getWorld().getPlants().remove(plant);
        context.getWorld().recordPlantLost();
        context.emit("Plant " + plant.getType() + " at (" + plant.getTileX()
                + ", " + plant.getTileY() + ") is destroyed.");
    }

    private PlantInstance topPlant(Tile tile) {
        if (tile == null) return null;
        return tile.getStackedPlant() != null ? tile.getStackedPlant() : tile.getSupportPlant();
    }

    private PlantType parsePlant(String token) {
        try {
            return PlantType.fromCanonicalName(token);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String displayName(ZombotanyTrait trait) {
        return switch (trait) {
            case PEASHOOTER -> "Peashooter Zombie";
            case WALL_NUT -> "Wall-nut Zombie";
            case JALAPENO -> "Jalapeno Zombie";
            case SQUASH -> "Squash Zombie";
        };
    }

    private static final class VariantState {
        private final ZombotanyTrait trait;
        private final long spawnTick;
        private long nextShotTick;
        private boolean triggered;

        private VariantState(ZombotanyTrait trait, long spawnTick) {
            this.trait = trait;
            this.spawnTick = spawnTick;
            this.nextShotTick = spawnTick;
        }
    }
}
