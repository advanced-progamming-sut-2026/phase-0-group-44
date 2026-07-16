package model.miniGame.framework;

import model.inGame.GameSession;
import model.sim.Simulation;
import java.util.ArrayList;
import java.util.List;

public final class MinigameSession {
    private final MinigameDefinition definition;
    private final MinigameLevelConfig levelConfig;
    private final MinigameStrategy strategy;
    private final GameSession gameSession;
    private final Simulation simulation;
    private final List<MinigameCommandHandler> commandHandlers = new ArrayList<>();
    private MinigameRunState state = MinigameRunState.SELECTED;

    public MinigameSession(MinigameDefinition definition, MinigameLevelConfig levelConfig,
                           MinigameStrategy strategy, GameSession gameSession, Simulation simulation) {
        this.definition = definition; this.levelConfig = levelConfig; this.strategy = strategy;
        this.gameSession = gameSession; this.simulation = simulation;
        strategy.configure(gameSession, simulation, levelConfig);
    }
    public MinigameDefinition getDefinition() { return definition; }
    public MinigameLevelConfig getLevelConfig() { return levelConfig; }
    public MinigameRunState getState() { return state; }
    public GameSession getGameSession() { return gameSession; }
    public Simulation getSimulation() { return simulation; }
    public void addCommandHandler(MinigameCommandHandler handler) { if (handler != null) commandHandlers.add(handler); }
    public void start() { require(MinigameRunState.SELECTED); state = MinigameRunState.RUNNING; strategy.onStart(this); }
    public void tick() { require(MinigameRunState.RUNNING); strategy.onTick(this); }
    public void win() { require(MinigameRunState.RUNNING); state = MinigameRunState.WON; strategy.onWin(this); }
    public void lose() { require(MinigameRunState.RUNNING); state = MinigameRunState.LOST; strategy.onLoss(this); }
    public String executeExtension(String command) {
        for (MinigameCommandHandler handler : commandHandlers) if (handler.supports(command)) return handler.execute(this, command);
        throw new IllegalArgumentException("Unsupported minigame command.");
    }
    private void require(MinigameRunState expected) {
        if (state != expected) throw new IllegalStateException("Expected minigame state " + expected + " but was " + state + ".");
    }
}
