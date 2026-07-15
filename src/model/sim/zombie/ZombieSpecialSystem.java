package model.sim.zombie;

import model.enums.TerrainType;
import model.enums.ZombieType;
import model.inGame.zombie.ZombieArmorPart;
import model.inGame.zombie.ZombieRegistry;
import model.sim.SimulationSystem;
import model.sim.SimulationWorld;
import model.sim.TickContext;
import model.sim.board.PlantInstance;
import model.sim.board.Tile;
import model.sim.sun.Sun;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic special-state transitions for mandatory zombies in the command
 * simulation. The detailed projectile combat model uses the same canonical
 * registry through {@code model.inGame.zombie.ZombieBehaviorFactory}.
 */
public final class ZombieSpecialSystem implements SimulationSystem {
    private interface Ability {
        void tick(ZombieInstance zombie, TickContext context);
    }

    private final ZombieRegistry registry;
    private final Map<ZombieType, Ability> abilities = new EnumMap<>(ZombieType.class);

    public ZombieSpecialSystem() {
        this(ZombieRegistry.getDefault());
    }

    public ZombieSpecialSystem(ZombieRegistry registry) {
        this.registry = registry;
        registerAbilities();
    }

    private void registerAbilities() {
        abilities.put(ZombieType.GARGANTUAR, this::gargantuar);
        abilities.put(ZombieType.TURQUOISE_ZOMBIE, this::turquoise);
        abilities.put(ZombieType.PROSPECTOR, this::prospector);
        abilities.put(ZombieType.PIANIST, this::pianist);
        abilities.put(ZombieType.BARREL_ROLLER, this::barrelRoller);
        abilities.put(ZombieType.RA_ZOMBIE, this::ra);
        abilities.put(ZombieType.TOMBRAISER, this::tombraiser);
        abilities.put(ZombieType.HUNTER, this::hunter);
        abilities.put(ZombieType.SNORKEL, this::snorkel);
        abilities.put(ZombieType.OCTOPUS_ZOMBIE, this::octopus);
    }

    @Override
    public void tick(TickContext context) {
        SimulationWorld world = context.getWorld();
        if (!world.isRunning()) {
            return;
        }
        for (ZombieInstance zombie : new ArrayList<>(world.getZombieInstances())) {
            if (zombie.isDead()) {
                continue;
            }
            zombie.tickEffects(1.0 / TickContext.TICKS_PER_SECOND);
            Ability ability = abilities.get(zombie.getType());
            if (ability != null && !zombie.isDead()) {
                ability.tick(zombie, context);
            }
        }
    }

    private void gargantuar(ZombieInstance zombie, TickContext context) {
        if (zombie.getBooleanState("IMP_THROWN")
                || zombie.getHp() > zombie.getSpec().getHealth() / 2) {
            return;
        }
        zombie.putState("IMP_THROWN", true);
        ZombieSpec impSpec = ZombieSpec.fromDefinition(registry.requireMandatory(ZombieType.IMP));
        context.getWorld().addZombie(new ZombieInstance(impSpec, 2.5, zombie.getRow()));
        context.emit("Gargantuar threw an Imp into the third column from the left.");
    }

    private void turquoise(ZombieInstance zombie, TickContext context) {
        SimulationWorld world = context.getWorld();
        if (!zombie.getBooleanState("STEALING")) {
            if (nearestPlantAhead(world, zombie, 4.0) == null) {
                return;
            }
            zombie.putState("STEALING", true);
            zombie.putState("STEAL_TICKS", 0);
        }
        int ticks = zombie.getIntState("STEAL_TICKS", 0) + 1;
        zombie.putState("STEAL_TICKS", ticks);
        if (ticks % TickContext.TICKS_PER_SECOND == 0 && ticks <= 5 * TickContext.TICKS_PER_SECOND) {
            int stolen = Math.min(25, world.getSunBalance());
            world.addSun(-stolen);
            zombie.putState("STOLEN_SUN", zombie.getIntState("STOLEN_SUN", 0) + stolen);
        }
        if (ticks >= 5 * TickContext.TICKS_PER_SECOND) {
            destroyPlantsAhead(world, zombie, 4, context);
            zombie.putState("STEALING", false);
            zombie.putState("STEAL_TICKS", 0);
            context.emit("Turquoise Zombie fired its four-tile laser.");
        }
    }

    private void prospector(ZombieInstance zombie, TickContext context) {
        if (!zombie.hasState("DYNAMITE_LIT")) {
            zombie.putState("DYNAMITE_LIT", true);
        }
        if (!zombie.getBooleanState("DYNAMITE_LIT") || zombie.getBooleanState("REVERSED")) {
            return;
        }
        int ticks = zombie.getIntState("DYNAMITE_TICKS", 0) + 1;
        zombie.putState("DYNAMITE_TICKS", ticks);
        if (ticks >= 10 * TickContext.TICKS_PER_SECOND) {
            zombie.setX(0.1);
            zombie.setDirection(1);
            zombie.putState("REVERSED", true);
            context.emit("Prospector dynamite exploded and reversed its direction.");
        }
    }

    private void pianist(ZombieInstance zombie, TickContext context) {
        int ticks = zombie.getIntState("PIANO_TICKS", 0) + 1;
        zombie.putState("PIANO_TICKS", ticks);
        if (ticks % (4 * TickContext.TICKS_PER_SECOND) != 0) {
            return;
        }
        List<ZombieInstance> candidates = new ArrayList<>();
        for (ZombieInstance other : context.getWorld().getZombieInstances()) {
            if (other != zombie && !other.isDead()) {
                candidates.add(other);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        ZombieInstance target = candidates.get(context.getRandom().nextInt(candidates.size()));
        List<Integer> rows = new ArrayList<>();
        if (target.getRow() > 0) {
            rows.add(target.getRow() - 1);
        }
        if (target.getRow() + 1 < context.getWorld().getRows()) {
            rows.add(target.getRow() + 1);
        }
        if (!rows.isEmpty()) {
            target.setRow(rows.get(context.getRandom().nextInt(rows.size())));
            context.emit("Pianist moved " + target.getSpec().getName() + " to an adjacent row.");
        }
    }

    private void barrelRoller(ZombieInstance zombie, TickContext context) {
        if (!zombie.getBooleanState("BARREL_BROKEN")
                || zombie.getBooleanState("IMPS_RELEASED")) {
            return;
        }
        zombie.putState("IMPS_RELEASED", true);
        ZombieSpec imp = ZombieSpec.fromDefinition(registry.requireMandatory(ZombieType.IMP));
        context.getWorld().addZombie(new ZombieInstance(imp, zombie.getX() + 0.1, zombie.getRow()));
        context.getWorld().addZombie(new ZombieInstance(imp, zombie.getX() + 0.2, zombie.getRow()));
        context.emit("The barrel broke and released two Imps.");
    }

    private void ra(ZombieInstance zombie, TickContext context) {
        int ticks = zombie.getIntState("RA_TICKS", 0) + 1;
        zombie.putState("RA_TICKS", ticks);
        if (ticks % TickContext.TICKS_PER_SECOND != 0) {
            return;
        }
        Sun target = null;
        double best = Double.MAX_VALUE;
        for (Sun sun : context.getWorld().getSuns()) {
            if (sun.getState() != Sun.State.ON_GROUND || sun.getTileY() != zombie.getRow()) {
                continue;
            }
            double distance = Math.abs(sun.getTileX() + 0.5 - zombie.getX());
            if (distance < best) {
                best = distance;
                target = sun;
            }
        }
        if (target != null) {
            context.getWorld().getSuns().remove(target);
            zombie.putState("RA_STOLEN_SUN",
                    zombie.getIntState("RA_STOLEN_SUN", 0) + target.getValue());
        }
    }

    private void tombraiser(ZombieInstance zombie, TickContext context) {
        int ticks = zombie.getIntState("BONE_TICKS", 0) + 1;
        zombie.putState("BONE_TICKS", ticks);
        if (ticks % (8 * TickContext.TICKS_PER_SECOND) != 0) {
            return;
        }
        List<Tile> valid = new ArrayList<>();
        for (int row = 0; row < context.getWorld().getRows(); row++) {
            for (int column = 0; column < context.getWorld().getColumns(); column++) {
                Tile tile = context.getWorld().getBoard().tileAt(column, row);
                if (!tile.hasAnyPlant() && tile.getTerrain().isPlantableByDefault()) {
                    valid.add(tile);
                }
            }
        }
        int created = 0;
        while (created < 2 && !valid.isEmpty()) {
            int index = context.getRandom().nextInt(valid.size());
            valid.remove(index).setTerrain(TerrainType.GRAVESTONE);
            created++;
        }
        if (created > 0) {
            context.emit("Tombraiser created " + created + " graves.");
        }
    }

    private void hunter(ZombieInstance zombie, TickContext context) {
        int ticks = zombie.getIntState("HUNTER_TICKS", 0) + 1;
        zombie.putState("HUNTER_TICKS", ticks);
        if (ticks % (3 * TickContext.TICKS_PER_SECOND) != 0) {
            return;
        }
        PlantInstance target = nearestPlantInLane(context.getWorld(), zombie.getRow(), zombie.getX());
        if (target != null && target.addFreezeLevel()) {
            context.getWorld().getBoard().tileAt(target.getTileX(), target.getTileY())
                    .setProjectileBlocker("ice", PlantInstance.ICE_HEALTH);
        }
    }

    private void snorkel(ZombieInstance zombie, TickContext context) {
        int x = Math.max(0, Math.min(context.getWorld().getColumns() - 1, zombie.getTileX()));
        Tile tile = context.getWorld().getBoard().tileAt(x, zombie.getRow());
        zombie.putState("SUBMERGED", tile != null
                && tile.getTerrain().requiresWaterCapablePlant() && !tile.hasAnyPlant());
    }

    private void octopus(ZombieInstance zombie, TickContext context) {
        int ticks = zombie.getIntState("OCTOPUS_TICKS", 0) + 1;
        zombie.putState("OCTOPUS_TICKS", ticks);
        if (ticks % (5 * TickContext.TICKS_PER_SECOND) != 0) {
            return;
        }
        PlantInstance target = nearestPlantInLane(context.getWorld(), zombie.getRow(), zombie.getX());
        if (target != null && !target.isOctopused()) {
            target.setOctopused(true);
            context.getWorld().getBoard().tileAt(target.getTileX(), target.getTileY())
                    .setProjectileBlocker("octopus", 600);
            context.emit("Octopus Zombie disabled " + target.getType() + ".");
        }
    }

    private PlantInstance nearestPlantAhead(
            SimulationWorld world, ZombieInstance zombie, double range
    ) {
        PlantInstance result = null;
        double best = Double.MAX_VALUE;
        for (model.sim.Damageable damageable : world.getPlants()) {
            if (!(damageable instanceof PlantInstance plant) || plant.getTileY() != zombie.getRow()) {
                continue;
            }
            double signed = zombie.getDirection() < 0
                    ? zombie.getX() - (plant.getTileX() + 0.5)
                    : (plant.getTileX() + 0.5) - zombie.getX();
            if (signed >= 0.0 && signed <= range && signed < best) {
                best = signed;
                result = plant;
            }
        }
        return result;
    }

    private PlantInstance nearestPlantInLane(SimulationWorld world, int row, double x) {
        PlantInstance result = null;
        double best = Double.MAX_VALUE;
        for (model.sim.Damageable damageable : world.getPlants()) {
            if (!(damageable instanceof PlantInstance plant) || plant.getTileY() != row) {
                continue;
            }
            double distance = Math.abs(plant.getTileX() + 0.5 - x);
            if (distance < best) {
                best = distance;
                result = plant;
            }
        }
        return result;
    }

    private void destroyPlantsAhead(
            SimulationWorld world, ZombieInstance zombie, int count, TickContext context
    ) {
        int start = zombie.getTileX() + zombie.getDirection();
        for (int step = 0; step < count; step++) {
            int column = start + step * zombie.getDirection();
            Tile tile = world.getBoard().tileAt(column, zombie.getRow());
            if (tile == null) {
                continue;
            }
            destroy(world, tile.getStackedPlant(), tile, context);
            destroy(world, tile.getSupportPlant(), tile, context);
        }
    }

    private void destroy(
            SimulationWorld world, PlantInstance plant, Tile tile, TickContext context
    ) {
        if (plant == null) {
            return;
        }
        plant.takeDamage(Integer.MAX_VALUE);
        if (tile.getStackedPlant() == plant) {
            tile.setStackedPlant(null);
        } else if (tile.getSupportPlant() == plant) {
            tile.clearPlants();
        }
        world.getPlants().remove(plant);
        world.recordPlantLost();
        context.emit("Plant " + plant.getType() + " at (" + plant.getTileX()
                + ", " + plant.getTileY() + ") is destroyed.");
    }

    public static void onDeath(
            ZombieInstance zombie, SimulationWorld world, TickContext context
    ) {
        onDeath(zombie, world, context::emit);
    }

    public static void onDeath(
            ZombieInstance zombie,
            SimulationWorld world,
            java.util.function.Consumer<String> eventSink
    ) {
        if (zombie.getType() == ZombieType.RA_ZOMBIE) {
            int returned = zombie.getIntState("RA_STOLEN_SUN", 0);
            world.addSun(returned);
            if (returned > 0) {
                eventSink.accept("Ra Zombie returned " + returned + " stolen sun.");
            }
        } else if (zombie.getType() == ZombieType.TURQUOISE_ZOMBIE) {
            int returned = zombie.getIntState("STOLEN_SUN", 0) / 2;
            world.addSun(returned);
            if (returned > 0) {
                eventSink.accept("Turquoise Zombie dropped " + returned + " stolen sun.");
            }
        } else if (zombie.getType() == ZombieType.BARREL_ROLLER) {
            for (ZombieArmorPart part : zombie.getArmorParts()) {
                if ("barrel".equalsIgnoreCase(part.getName()) && !part.isBroken()) {
                    int x = Math.max(0, Math.min(world.getColumns() - 1, zombie.getTileX()));
                    world.getBoard().tileAt(x, zombie.getRow())
                            .setProjectileBlocker("barrel", part.getHealth());
                    eventSink.accept("Barrel Roller died; its barrel remained on the tile.");
                }
            }
        }
    }
}
