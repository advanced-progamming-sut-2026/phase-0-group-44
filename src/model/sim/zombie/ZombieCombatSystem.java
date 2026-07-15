package model.sim.zombie;

import model.sim.GameOutcome;
import model.sim.SimulationSystem;
import model.sim.SimulationWorld;
import model.sim.TickContext;
import model.sim.board.PlantInstance;
import model.sim.board.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * The per-tick zombie loop: move left, eat the plant ahead when blocked, destroy
 * it, then resume — plus lawn-mower triggering and the loss condition.
 */
public class ZombieCombatSystem implements SimulationSystem {

    /** The x at or below which a zombie has reached the end of its lane. */
    public static final double LANE_END_X = 0.0;

    @Override
    public void tick(TickContext context) {
        SimulationWorld world = context.getWorld();

        if (!world.isRunning()) {
            return;
        }

        for (ZombieInstance zombie : world.getZombieInstances()) {
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
        PlantInstance blocker = foremostPlant(world, zombie);

        if (blocker != null) {
            eat(zombie, blocker, world, context);
            return;
        }

        zombie.setState(ZombieInstance.State.MOVING);
        double perTick = zombie.getSpec().getSpeedTilesPerSecond() / TickContext.TICKS_PER_SECOND;
        zombie.setX(zombie.getX() - perTick);

        if (zombie.getX() <= LANE_END_X) {
            reachEndOfLane(context, world, zombie);
        }
    }

    /** The rightmost plant in the zombie's row at or ahead of its tile. */
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

    private void eat(
            ZombieInstance zombie,
            PlantInstance plant,
            SimulationWorld world,
            TickContext context
    ) {
        zombie.setState(ZombieInstance.State.EATING);

        int perTick = zombie.getSpec().getEatDamagePerSecond() / TickContext.TICKS_PER_SECOND;
        plant.takeDamage(Math.max(perTick, 1));

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

        for (ZombieInstance zombie : world.getZombieInstances()) {
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
