package model.miniGame.framework;

import model.inGame.GameSession;
import model.sim.Simulation;

public interface MinigameStrategy {
    default void configure(GameSession gameSession, Simulation simulation, MinigameLevelConfig config) { }
    default void onStart(MinigameSession session) { }
    default void onTick(MinigameSession session) { }
    default void onWin(MinigameSession session) { }
    default void onLoss(MinigameSession session) { }
}
