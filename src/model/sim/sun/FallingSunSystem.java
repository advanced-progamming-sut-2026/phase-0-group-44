package model.sim.sun;

import model.sim.SimulationSystem;
import model.sim.SimulationWorld;
import model.sim.TickContext;
import util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Spawns suns from the sky on the specification's interval, advances each fall,
 * and lands them.
 *
 * <p>A sun takes 5 seconds (50 ticks) to reach the ground. On spawn it picks a
 * random tile and emits the dropping message; on landing it emits the ground
 * message. This system can be disabled for a level that turns sky suns off.</p>
 */
public class FallingSunSystem implements SimulationSystem {

    /** 5 seconds to reach the ground. */
    public static final int FALL_TICKS = 5 * TickContext.TICKS_PER_SECOND;

    private final boolean enabled;
    private long ticksSinceSpawn;

    public FallingSunSystem(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void tick(TickContext context) {
        if (!enabled) {
            return;
        }

        spawnIfDue(context);
        advanceFalls(context);
    }

    private void spawnIfDue(TickContext context) {
        ticksSinceSpawn++;

        long interval = SunDropSchedule.intervalTicks(
                context.getElapsedSeconds(), TickContext.TICKS_PER_SECOND);

        if (ticksSinceSpawn >= interval) {
            spawn(context);
            ticksSinceSpawn = 0;
        }
    }

    private void spawn(TickContext context) {
        SimulationWorld world = context.getWorld();
        RandomSource random = context.getRandom();

        SunType type = SunType.roll(random);
        int x = random.nextInt(world.getColumns());
        int y = random.nextInt(world.getRows());

        Sun sun = Sun.falling(type, x, y, FALL_TICKS);
        world.getSuns().add(sun);

        context.emit("New " + type.getLabel() + " sun is dropping at position ("
                + x + ", " + y + ")");
    }

    private void advanceFalls(TickContext context) {
        List<Sun> landed = new ArrayList<>();

        for (Sun sun : context.getWorld().getSuns()) {
            if (sun.isFalling() && sun.tickFall()) {
                landed.add(sun);
            }
        }

        for (Sun sun : landed) {
            context.emit("Sun reached the ground at position ("
                    + sun.getTileX() + ", " + sun.getTileY() + ")");
        }
    }
}
