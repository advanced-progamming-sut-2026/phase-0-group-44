package model.miniGame;

import model.sim.GameOutcome;
import model.sim.Simulation;
import model.sim.SimulationWorld;

import java.util.List;

/**
 * Strategy base for minigame-only rule overrides. Shared board and tick logic
 * stay in {@link Simulation}; subclasses only configure the differences.
 */
public abstract class MiniGame {
    public abstract MiniGameId getId();

    /** Called once before the common simulation enters RUNNING. */
    public void configure(
            Simulation simulation,
            SimulationWorld world,
            MiniGameLevelConfig config
    ) {
        world.addSun(config.getStartingResources());
    }

    /**
     * Session-aware setup hook for strategies with attempt-local state. The
     * compatibility overload above remains the default for simple strategies.
     */
    public void configure(MiniGameSession session) {
        configure(session.getSimulation(), session.getSimulation().getWorld(), session.getConfig());
    }

    /** Evaluates an outcome after a shared tick or a strategy command. */
    public GameOutcome evaluate(MiniGameSession session) {
        return session.getSimulation().getWorld().getOutcome();
    }

    /** Commands unique to the strategy; empty for the common framework. */
    public List<MiniGameCommandExtension> commandExtensions() {
        return List.of();
    }
}
