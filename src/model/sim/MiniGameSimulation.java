package model.sim;

import model.sim.sun.FallingSunSystem;
import model.sim.sun.SunCollector;
import model.sim.board.CooldownSystem;
import model.sim.sun.SunProducer;
import util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * The one deterministic update pipeline for a running minigame or scored game.
 *
 * <p>{@link #advance(int)} steps the clock one tick at a time and, on each tick,
 * runs every registered system in a fixed order. Plants, projectiles, zombies,
 * effects, cooldowns, waves and level timers register here as they are built;
 * this section wires the sun systems. There is only ever this one clock.</p>
 */
public class MiniGameSimulation {

    private final RandomSource random;
    private final SimulationWorld world;
    private final List<SimulationSystem> systems = new ArrayList<>();
    private final SunCollector sunCollector;

    private long currentTick;

    public MiniGameSimulation(RandomSource random, SimulationWorld world, boolean skySunEnabled) {
        this.random = random;
        this.world = world;
        this.sunCollector = new SunCollector(world);

        // Fixed pipeline order. Later systems (zombies, projectiles, waves, …)
        // are added to this same list so they share this clock.
        systems.add(new CooldownSystem());
        systems.add(new SunProducer.System());
        systems.add(new FallingSunSystem(skySunEnabled));
    }

    public SimulationWorld getWorld() {
        return world;
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public void register(SimulationSystem system) {
        systems.add(system);
    }

    /**
     * Advances the simulation by {@code ticks} ticks and returns the event
     * messages produced, in order. Ten ticks are one in-game second.
     */
    public List<String> advance(int ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("Tick count must be a positive integer.");
        }

        List<String> messages = new ArrayList<>();

        for (int i = 0; i < ticks; i++) {
            TickContext context = new TickContext(currentTick, random, world);

            for (SimulationSystem system : systems) {
                system.tick(context);
            }

            messages.addAll(context.getMessages());
            currentTick++;
        }

        return messages;
    }

    public SunCollector.Outcome collectSun(int x, int y) {
        return sunCollector.collectAt(x, y);
    }

    public int getSunAmount() {
        return world.getSunBalance();
    }

    public void addSun(int amount) {
        world.addSun(amount);
    }
}
