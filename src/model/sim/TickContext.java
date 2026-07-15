package model.sim;

import util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * What each system receives on every tick: the current tick, the elapsed game
 * time, the shared randomness source, the world, and a sink for the exact event
 * messages the specification prescribes.
 */
public class TickContext {

    /** 10 ticks make one in-game second; 1 tick is the smallest simulation unit. */
    public static final int TICKS_PER_SECOND = 10;

    private final long currentTick;
    private final RandomSource random;
    private final SimulationWorld world;
    private final List<String> messages;

    public TickContext(long currentTick, RandomSource random, SimulationWorld world) {
        this.currentTick = currentTick;
        this.random = random;
        this.world = world;
        this.messages = new ArrayList<>();
    }

    public long getCurrentTick() {
        return currentTick;
    }

    /** Elapsed game time in seconds at the start of this tick. */
    public double getElapsedSeconds() {
        return currentTick / (double) TICKS_PER_SECOND;
    }

    public RandomSource getRandom() {
        return random;
    }

    public SimulationWorld getWorld() {
        return world;
    }

    public void emit(String message) {
        messages.add(message);
    }

    public List<String> getMessages() {
        return messages;
    }
}
