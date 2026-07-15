package model.sim.zombie;

import model.enums.PlantType;
import model.enums.ZombieType;
import model.sim.GameOutcome;
import model.sim.SimulationSystem;
import model.sim.SimulationWorld;
import model.sim.TickContext;
import model.sim.board.PlantInstance;
import model.sim.board.Tile;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Movement, collision attacks, lawn mowers, and death cleanup. */
public class ZombieCombatSystem implements SimulationSystem {

    public static final double LANE_END_X = 0.0;

    private static final Set<PlantType> DODO_FLY_OVER = EnumSet.of(
            PlantType.WALL_NUT,
            PlantType.ENDURIAN,
            PlantType.GARLIC,
            PlantType.SWEET_POTATO,
            PlantType.EXPLODE_O_NUT,
            PlantType.PUMPKIN,
            PlantType.POTATO_MINE,
            PlantType.PRIMAL_POTATO_MINE,
            PlantType.ICEBERG_LETTUCE
    );

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
            step(context, world, zombie);
            if (!world.isRunning()) {
                return;
            }
        }
        removeDead(context, world);
    }

    private void step(TickContext context, SimulationWorld world, ZombieInstance zombie) {
        if (zombie.isFrozen()) {
            return;
        }
        PlantInstance blocker = foremostPlant(world, zombie);
        if (blocker != null) {
            if (zombie.getType() == ZombieType.DODO_RIDER && canDodoFlyOver(blocker)) {
                zombie.setX(zombie.getX() + zombie.getDirection() * 1.05);
                context.emit("Dodo Rider flew over " + blocker.getType() + ".");
                return;
            }
            attack(zombie, blocker, world, context);
            return;
        }

        zombie.setState(ZombieInstance.State.MOVING);
        double perTick = zombie.effectiveSpeed() / TickContext.TICKS_PER_SECOND;
        zombie.setX(zombie.getX() + zombie.getDirection() * perTick);

        if (zombie.getDirection() < 0 && zombie.getX() <= LANE_END_X) {
            reachEndOfLane(context, world, zombie);
        } else if (zombie.getDirection() > 0 && zombie.getX() >= world.getColumns()) {
            zombie.setX(world.getColumns() - 0.01);
        }
    }

    private boolean canDodoFlyOver(PlantInstance plant) {
        return plant.getType() != PlantType.TALL_NUT && DODO_FLY_OVER.contains(plant.getType());
    }

    private PlantInstance foremostPlant(SimulationWorld world, ZombieInstance zombie) {
        int tileX = zombie.getTileX();
        int row = zombie.getRow();
        if (tileX < 0 || tileX >= world.getBoard().getColumns()) {
            return null;
        }
        Tile tile = world.getBoard().tileAt(tileX, row);
        if (tile == null) {
            return null;
        }
        if (tile.getStackedPlant() != null) {
            return tile.getStackedPlant();
        }
        return tile.getSupportPlant();
    }

    private void attack(
            ZombieInstance zombie,
            PlantInstance plant,
            SimulationWorld world,
            TickContext context
    ) {
        zombie.setState(ZombieInstance.State.EATING);
        ZombieType type = zombie.getType();
        boolean instant = type == ZombieType.GARGANTUAR
                || type == ZombieType.PIANIST
                || type == ZombieType.EXPLORER && zombie.getBooleanState("TORCH_LIT")
                || type == ZombieType.ALL_STAR && zombie.getBooleanState("CHARGING");
        if (instant) {
            plant.takeDamage(Integer.MAX_VALUE);
            if (type == ZombieType.ALL_STAR) {
                zombie.putState("CHARGING", false);
                zombie.putState("POST_CHARGE", true);
            }
        } else {
            int perTick = zombie.effectiveEatDamage() / TickContext.TICKS_PER_SECOND;
            plant.takeDamage(Math.max(perTick, 1));
        }
        if (plant.isDead()) {
            destroyPlant(world, context, plant);
        }
    }

    private void destroyPlant(SimulationWorld world, TickContext context, PlantInstance plant) {
        Tile tile = world.getBoard().tileAt(plant.getTileX(), plant.getTileY());
        if (tile != null) {
            if (tile.getStackedPlant() == plant) {
                tile.setStackedPlant(null);
            } else if (tile.getSupportPlant() == plant) {
                tile.clearPlants();
            }
        }
        world.getPlants().remove(plant);
        context.emit("Plant " + plant.getType().name()
                + " at (" + plant.getTileX() + ", " + plant.getTileY() + ") is destroyed.");
    }

    private void reachEndOfLane(TickContext context, SimulationWorld world, ZombieInstance zombie) {
        int row = zombie.getRow();
        if (!world.isLawnMowerUsed(row)) {
            triggerLawnMower(context, world, row);
            return;
        }
        world.setOutcome(GameOutcome.LOST);
        context.emit("The zombie ate your brain; LOSER!!!");
    }

    private void triggerLawnMower(TickContext context, SimulationWorld world, int row) {
        world.useLawnMower(row);
        List<String> killedNames = new ArrayList<>();
        for (ZombieInstance zombie : new ArrayList<>(world.getZombieInstances())) {
            if (zombie.getRow() == row && !zombie.isBoss() && !zombie.isDead()) {
                killedNames.add(zombie.getSpec().getName());
                recordAndKill(world, zombie, false, context);
            }
        }
        context.emit("The lawn mower in the row " + row + "is triggered and killed these zombies: "
                + String.join(", ", killedNames));
    }

    private void removeDead(TickContext context, SimulationWorld world) {
        List<ZombieInstance> dead = new ArrayList<>();
        for (ZombieInstance zombie : world.getZombieInstances()) {
            if (zombie.isDead()) {
                dead.add(zombie);
            }
        }
        for (ZombieInstance zombie : dead) {
            ZombieSpecialSystem.onDeath(zombie, world, context);
            emitDeath(context, zombie);
            world.recordDeath(new ZombieDeath(
                    zombie.getSpec(), zombie.getTileX(), zombie.getRow(),
                    zombie.isGlowing(), false));
            world.getZombies().remove(zombie);
        }
    }

    private void recordAndKill(
            SimulationWorld world,
            ZombieInstance zombie,
            boolean cheatKill,
            TickContext context
    ) {
        zombie.kill();
        ZombieSpecialSystem.onDeath(zombie, world, context);
        emitDeath(context, zombie);
        world.recordDeath(new ZombieDeath(
                zombie.getSpec(), zombie.getTileX(), zombie.getRow(),
                zombie.isGlowing(), cheatKill));
        world.getZombies().remove(zombie);
    }

    private void emitDeath(TickContext context, ZombieInstance zombie) {
        context.emit("Zombie of type " + zombie.getSpec().getName()
                + " is dead at (" + zombie.getTileX() + ", " + zombie.getRow() + ")");
    }
}
