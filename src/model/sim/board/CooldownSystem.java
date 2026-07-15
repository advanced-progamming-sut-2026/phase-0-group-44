package model.sim.board;

import model.sim.SimulationSystem;
import model.sim.TickContext;

/** Ticks down every plant's recharge each tick, through the one pipeline. */
public class CooldownSystem implements SimulationSystem {

    @Override
    public void tick(TickContext context) {
        context.getWorld().tickCooldowns();
    }
}
