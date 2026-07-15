package model.sim;

/**
 * One participant in the single update pipeline. Every active system — plants,
 * projectiles, zombies, effects, cooldowns, waves, dropping suns, level timers —
 * advances through this same interface, so there is only one clock.
 */
public interface SimulationSystem {

    void tick(TickContext context);
}
