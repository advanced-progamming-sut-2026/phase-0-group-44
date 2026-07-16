package model.miniGame;

import model.miniGame.framework.MinigameSession;
import model.miniGame.framework.MinigameStrategy;
import model.sim.GameOutcome;

public final class VasebreakerStrategy implements MinigameStrategy {
    private final VaseBreaker game;
    public VasebreakerStrategy(int level) { this.game = new VaseBreaker(VasebreakerLevels.level(level)); }
    public VaseBreaker getGame() { return game; }
    @Override public void onTick(MinigameSession session) {
        long tick = session.getSimulation() == null ? 0 : session.getSimulation().getCurrentTick();
        game.expirePackets(tick);
        if (session.getSimulation() != null && session.getSimulation().getWorld().getOutcome() == GameOutcome.LOST) {
            session.lose();
        } else if (game.isWon()) {
            session.win();
        }
    }
}
