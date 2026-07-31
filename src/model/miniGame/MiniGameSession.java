package model.miniGame;

import model.Result;
import model.sim.GameOutcome;
import model.sim.MiniGameSimulation;

import java.util.ArrayList;
import java.util.List;

/** One non-persisted play attempt backed by the common simulation pipeline. */
public final class MiniGameSession {
    private final String ownerUsername;
    private final MiniGameDefinition definition;
    private final MiniGameLevelConfig config;
    private final MiniGame rules;
    private final MiniGameSimulation simulation;
    private MiniGameLifecycleState state = MiniGameLifecycleState.SELECTED;
    private boolean settled;
    private Object strategyState;

    public MiniGameSession(
            String ownerUsername,
            MiniGameDefinition definition,
            MiniGameLevelConfig config,
            MiniGame rules,
            MiniGameSimulation simulation
    ) {
        if (ownerUsername == null || ownerUsername.isBlank()
                || definition == null || config == null || rules == null || simulation == null
                || definition.getId() != config.getMiniGameId()
                || rules.getId() != definition.getId()) {
            throw new IllegalArgumentException("A minigame session requires matching components.");
        }
        this.ownerUsername = ownerUsername;
        this.definition = definition;
        this.config = config;
        this.rules = rules;
        this.simulation = simulation;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public MiniGameDefinition getDefinition() {
        return definition;
    }

    public MiniGameLevelConfig getConfig() {
        return config;
    }

    public MiniGame getRules() {
        return rules;
    }

    public MiniGameSimulation getSimulation() {
        return simulation;
    }

    public MiniGameLifecycleState getState() {
        return state;
    }

    public Object getStrategyState() {
        return strategyState;
    }

    public <T> T getStrategyState(Class<T> type) {
        return type != null && type.isInstance(strategyState)
                ? type.cast(strategyState) : null;
    }

    public void setStrategyState(Object strategyState) {
        if (state != MiniGameLifecycleState.SELECTED) {
            throw new IllegalStateException("Strategy state must be installed before the session starts.");
        }
        this.strategyState = strategyState;
    }

    public boolean markSettled() {
        if (settled || (state != MiniGameLifecycleState.WON
                && state != MiniGameLifecycleState.LOST)) {
            return false;
        }
        settled = true;
        return true;
    }

    public Result<MiniGameSession> start() {
        Result<MiniGameSession> result = new Result<>();
        if (state != MiniGameLifecycleState.SELECTED) {
            result.appendToMessage("the selected minigame has already started or finished");
            return result;
        }
        Result<String> validation = rules.validateStart(this);
        if (validation == null || !validation.getStatus()) {
            result.appendToMessage(validation == null || validation.getMessage().isBlank()
                    ? "minigame is not ready to start" : validation.getMessage());
            return result;
        }
        rules.configure(this);
        state = MiniGameLifecycleState.RUNNING;
        result.setStatus(true);
        result.setData(this);
        result.appendToMessage("started " + definition.getDisplayName()
                + " level " + config.getLevelNumber());
        return result;
    }

    public Result<List<String>> advance(int ticks) {
        Result<List<String>> result = new Result<>();
        if (state != MiniGameLifecycleState.RUNNING) {
            result.appendToMessage("no minigame is currently running");
            return result;
        }
        if (ticks <= 0) {
            result.appendToMessage("tick count must be a positive integer");
            return result;
        }
        List<String> messages = new ArrayList<>(simulation.advance(ticks));
        if (simulation.getCurrentTick() >= config.getTimeLimitTicks()
                && simulation.getWorld().getOutcome() == GameOutcome.RUNNING) {
            simulation.getWorld().setOutcome(GameOutcome.LOST);
            messages.add("minigame time limit reached");
        }
        GameOutcome outcome = rules.evaluate(this);
        if (outcome == GameOutcome.WON) {
            state = MiniGameLifecycleState.WON;
        } else if (outcome == GameOutcome.LOST) {
            state = MiniGameLifecycleState.LOST;
        }
        result.setStatus(true);
        result.setData(List.copyOf(messages));
        result.appendToMessage(messages.isEmpty()
                ? "advanced " + ticks + " tick(s)"
                : String.join("\n", messages));
        return result;
    }

    public Result<String> executeStrategyCommand(String input) {
        Result<String> result = new Result<>();
        if (state != MiniGameLifecycleState.RUNNING
                && state != MiniGameLifecycleState.SELECTED) {
            result.appendToMessage("no minigame is selected or running");
            return result;
        }
        for (MiniGameCommandExtension extension : rules.commandExtensions()) {
            if (extension.supports(input)) {
                if (state == MiniGameLifecycleState.SELECTED
                        && !extension.availableBeforeStart()) {
                    result.appendToMessage("this command is available only after the minigame starts");
                    return result;
                }
                Result<String> executed = extension.execute(this, input);
                if (state == MiniGameLifecycleState.RUNNING) {
                    GameOutcome outcome = rules.evaluate(this);
                    if (outcome == GameOutcome.WON) {
                        state = MiniGameLifecycleState.WON;
                    } else if (outcome == GameOutcome.LOST) {
                        state = MiniGameLifecycleState.LOST;
                    }
                }
                return executed;
            }
        }
        result.appendToMessage("command is not supported by this minigame");
        return result;
    }

    /** Transition used by objective systems and lifecycle tests. */
    public boolean finish(GameOutcome outcome) {
        if (state != MiniGameLifecycleState.RUNNING
                || (outcome != GameOutcome.WON && outcome != GameOutcome.LOST)) {
            return false;
        }
        simulation.getWorld().setOutcome(outcome);
        state = outcome == GameOutcome.WON
                ? MiniGameLifecycleState.WON : MiniGameLifecycleState.LOST;
        return true;
    }
}
