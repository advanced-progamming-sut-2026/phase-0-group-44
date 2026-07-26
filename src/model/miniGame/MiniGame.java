package model.miniGame;

import model.Result;
import model.sim.GameOutcome;
import model.sim.MiniGameSimulation;
import model.sim.SimulationWorld;

import java.util.List;

/**
 * Strategy base for minigame-only rule overrides. Shared board and tick logic
 * stay in {@link MiniGameSimulation}; subclasses only configure the differences.
 */
public abstract class MiniGame {
    public abstract MiniGameId getId();

    /** Optional pre-start state setup, used by modes with a selection phase. */
    public void prepare(MiniGameSession session) {
    }

    /** Validates any strategy-owned selection immediately before start. */
    public Result<String> validateStart(MiniGameSession session) {
        Result<String> result = new Result<>();
        result.setStatus(true);
        result.setData("ready");
        return result;
    }

    /** Called once before the common simulation enters RUNNING. */
    public void configure(
            MiniGameSimulation simulation,
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
