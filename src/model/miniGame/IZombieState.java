package model.miniGame;

import model.Result;
import model.enums.PlantType;
import model.enums.ZombieType;
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
import model.sim.board.PlantSpec;
import model.sim.board.Tile;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpec;
import model.inGame.zombie.ZombieEffectType;
import util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Attempt-local I, Zombie board, economy, objectives, and plant fire. */
public final class IZombieState {
    private final IZombieLevelRules rules;
    private final RandomSource random;
    private final PlantRegistry plantRegistry = PlantRegistry.getDefault();
    private final ZombieRegistry zombieRegistry = ZombieRegistry.getDefault();
    private final DefaultPlantSpecSource plantSpecs = new DefaultPlantSpecSource(plantRegistry);
    private final List<Brain> brains = new ArrayList<>();
    private final List<SunProducingZombie> producers = new ArrayList<>();
    private final List<PlantInstance> preplacedPlants = new ArrayList<>();
    private final List<ZombieInstance> playerZombies = new ArrayList<>();
    private final Map<PlantInstance, Long> nextPlantAttackTick = new IdentityHashMap<>();

    public IZombieState(IZombieLevelRules rules, SimulationWorld world, RandomSource random) {
        if (rules == null || world == null || random == null
                || rules.getRedLineColumn() >= world.getColumns()
                || rules.getPreplacedPlantCount() > rules.getRedLineColumn() * world.getRows()) {
            throw new IllegalArgumentException("I, Zombie state requires valid rules and board space.");
        }
        this.rules = rules;
        this.random = random;
        for (int row = 0; row < world.getRows(); row++) {
            brains.add(new Brain(row));
        }
        preplacePlants(world);
        createSunProducers(world);
    }

    public IZombieLevelRules getRules() { return rules; }
    public List<Brain> getBrains() { return Collections.unmodifiableList(brains); }
    public List<SunProducingZombie> getProducers() { return Collections.unmodifiableList(producers); }
    public List<PlantInstance> getPreplacedPlants() { return Collections.unmodifiableList(preplacedPlants); }
    public List<ZombieInstance> getPlayerZombies() { return Collections.unmodifiableList(playerZombies); }

    public int getEatenBrainCount() {
        return (int) brains.stream().filter(Brain::isEaten).count();
    }

    public int getLiveProducerCount() {
        return (int) producers.stream().filter(producer -> !producer.getZombie().isDead()).count();
    }

    public Result<String> placeZombie(MiniGameSession session, String token, int x, int y) {
        Result<String> result = new Result<>();
        ZombieType type = ZombieType.fromToken(token);
        int price = type == null ? -1 : rules.priceOf(type);
        if (price <= 0) {
            result.appendToMessage("that zombie is not selectable in this level");
            return result;
        }
        SimulationWorld world = session.getSimulation().getWorld();
        if (!world.isInsideBoard(x, y)) {
            result.appendToMessage("invalid tile");
            return result;
        }
        if (x < rules.getRedLineColumn()) {
            result.appendToMessage("zombies may be placed only at or right of red-line column "
                    + rules.getRedLineColumn());
            return result;
        }
        if (world.getSunBalance() < price) {
            result.appendToMessage("not enough sun; " + type.name() + " costs " + price);
            return result;
        }
        ZombieDefinition definition = zombieRegistry.requireMandatory(type);
        ZombieInstance zombie = new ZombieInstance(
                ZombieSpec.fromDefinition(definition), x + 0.5, y);
        world.addSun(-price);
        world.addZombie(zombie);
        playerZombies.add(zombie);
        result.setStatus(true);
        result.setData(type.name());
        result.appendToMessage("placed " + definition.getName() + " at (" + x + ", " + y
                + ") for " + price + " sun; remaining sun " + world.getSunBalance());
        return result;
    }

    public void consumeBrain(
            TickContext context,
            SimulationWorld world,
            ZombieInstance zombie
    ) {
        Brain brain = brainAt(zombie.getRow());
        if (brain != null && brain.eat()) {
            context.emit(zombie.getSpec().getName() + " ate the brain in row "
                    + zombie.getRow() + ".");
        }
        zombie.kill();
        world.getZombies().remove(zombie);
        if (getEatenBrainCount() == brains.size()) {
            world.setOutcome(GameOutcome.WON);
            context.emit("All five brains were eaten.");
        }
    }

    public GameOutcome evaluate(SimulationWorld world) {
        if (getEatenBrainCount() == brains.size()) {
            world.setOutcome(GameOutcome.WON);
            return GameOutcome.WON;
        }
        if (world.getOutcome() == GameOutcome.LOST) {
            return GameOutcome.LOST;
        }
        if (isDeadEnd(world)) {
            world.setOutcome(GameOutcome.LOST);
            return GameOutcome.LOST;
        }
        return GameOutcome.RUNNING;
    }

    public SimulationSystem tickSystem() {
        return context -> {
            produceSun(context);
            attackWithPlants(context);
            if (isDeadEnd(context.getWorld())) {
                context.getWorld().setOutcome(GameOutcome.LOST);
                context.emit("No usable zombie can be afforded and every placed zombie was destroyed.");
            }
        };
    }

    public String status(MiniGameSession session) {
        SimulationWorld world = session.getSimulation().getWorld();
        StringBuilder output = new StringBuilder();
        output.append("I, Zombie level ").append(rules.getLevel()).append('\n')
                .append("sun: ").append(world.getSunBalance()).append('\n')
                .append("legal zombie columns: ").append(rules.getRedLineColumn())
                .append('-').append(world.getColumns() - 1).append('\n')
                .append("brains eaten: ").append(getEatenBrainCount()).append('/')
                .append(brains.size()).append('\n')
                .append("sun producers alive: ").append(getLiveProducerCount()).append('/')
                .append(producers.size()).append('\n')
                .append("available zombies:\n");
        for (Map.Entry<ZombieType, Integer> entry : rules.getZombiePrices().entrySet()) {
            output.append("- ").append(entry.getKey().name())
                    .append(": ").append(entry.getValue()).append(" sun\n");
        }
        output.append("placed attack zombies alive: ")
                .append(playerZombies.stream().filter(zombie -> !zombie.isDead()).count())
                .append("; pre-placed plants alive: ")
                .append(preplacedPlants.stream().filter(plant -> !plant.isDead()).count())
                .append(';').append(" tick ").append(session.getSimulation().getCurrentTick());
        return output.toString();
    }

    private void preplacePlants(SimulationWorld world) {
        List<int[]> positions = new ArrayList<>();
        for (int x = 0; x < rules.getRedLineColumn(); x++) {
            for (int row = 0; row < world.getRows(); row++) {
                positions.add(new int[]{x, row});
            }
        }
        shuffle(positions);
        for (int index = 0; index < rules.getPreplacedPlantCount(); index++) {
            int[] position = positions.get(index);
            PlantType type = rules.getPlantPool().get(random.nextInt(rules.getPlantPool().size()));
            PlantSpec spec = plantSpecs.specOf(type);
            if (spec == null) {
                throw new IllegalStateException("Missing canonical plant data for " + type);
            }
            PlantInstance plant = new PlantInstance(spec, position[0], position[1]);
            Tile tile = world.getBoard().tileAt(position[0], position[1]);
            tile.setStackedPlant(plant);
            world.addPlant(plant);
            preplacedPlants.add(plant);
            PlantDefinition definition = plantRegistry.requireMandatory(type);
            int interval = Math.max(1, (int) Math.round(
                    definition.getActionInterval() * TickContext.TICKS_PER_SECOND));
            nextPlantAttackTick.put(plant, (long) interval);
        }
    }

    private void createSunProducers(SimulationWorld world) {
        ZombieDefinition bucket = zombieRegistry.requireMandatory(ZombieType.BUCKETHEAD);
        int equivalentHealth = bucket.getHealth() + bucket.getArmor();
        for (int row = 0; row < world.getRows(); row++) {
            ZombieSpec spec = ZombieSpec.builder("Sun-producing Zombie")
                    .health(equivalentHealth)
                    .speedTilesPerSecond(0.0)
                    .eatDamagePerSecond(0)
                    .waveCost(1)
                    .build();
            ZombieInstance zombie = new ZombieInstance(
                    spec, world.getColumns() - 0.25, row);
            zombie.putState("I_ZOMBIE_SUN_PRODUCER", true);
            world.addZombie(zombie);
            producers.add(new SunProducingZombie(zombie, rules));
        }
    }

    private void produceSun(TickContext context) {
        long completedTick = context.getCurrentTick() + 1;
        for (SunProducingZombie producer : producers) {
            int amount = producer.produceIfDue(completedTick);
            if (amount > 0) {
                context.getWorld().addSun(amount);
                context.getWorld().recordProducedSun(amount);
                context.emit("Sun-producing Zombie in row "
                        + producer.getZombie().getRow() + " produced " + amount + " sun.");
            }
        }
    }

    private void attackWithPlants(TickContext context) {
        for (PlantInstance plant : preplacedPlants) {
            if (!plant.isActive()) {
                continue;
            }
            long due = nextPlantAttackTick.getOrDefault(plant, 0L);
            if (context.getCurrentTick() + 1 < due) {
                continue;
            }
            PlantDefinition definition = plantRegistry.requireMandatory(plant.getType());
            int interval = Math.max(1, (int) Math.round(
                    definition.getActionInterval() * TickContext.TICKS_PER_SECOND));
            nextPlantAttackTick.put(plant, context.getCurrentTick() + 1 + interval);
            int damage = definition.getDamage()
                    * Math.max(1, definition.getDamageProfile().getProjectileCount());
            if (damage <= 0) {
                continue;
            }
            ZombieInstance target = nearestTarget(context.getWorld(), plant);
            if (target == null) {
                continue;
            }
            target.takeDamage(damage);
            if (plant.getType() == PlantType.SNOW_PEA) {
                target.applyEffect(ZombieEffectType.CHILLED, 2.0, 1);
                target.onIceHit();
            }
            context.emit(plant.getType().name() + " hit " + target.getSpec().getName()
                    + " for " + damage + ".");
        }
    }

    private ZombieInstance nearestTarget(SimulationWorld world, PlantInstance plant) {
        ZombieInstance nearest = null;
        for (ZombieInstance zombie : world.getZombieInstances()) {
            if (zombie.isDead() || zombie.getRow() != plant.getTileY()
                    || zombie.getX() < plant.getTileX()) {
                continue;
            }
            if (plant.getType() == PlantType.BONK_CHOY
                    && zombie.getX() - plant.getTileX() > 1.25) {
                continue;
            }
            if (nearest == null || zombie.getX() < nearest.getX()) {
                nearest = zombie;
            }
        }
        return nearest;
    }

    private boolean isDeadEnd(SimulationWorld world) {
        if (getLiveProducerCount() > 0) {
            return false;
        }
        boolean livePlaced = playerZombies.stream().anyMatch(zombie -> !zombie.isDead());
        return !livePlaced && world.getSunBalance() < rules.minimumZombiePrice();
    }

    private Brain brainAt(int row) {
        return row < 0 || row >= brains.size() ? null : brains.get(row);
    }

    private <T> void shuffle(List<T> values) {
        for (int index = values.size() - 1; index > 0; index--) {
            int other = random.nextInt(index + 1);
            T value = values.get(index);
            values.set(index, values.get(other));
            values.set(other, value);
        }
    }
}
