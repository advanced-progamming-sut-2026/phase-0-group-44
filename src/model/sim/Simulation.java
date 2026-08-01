package model.sim;

import model.GameEngine;
import java.util.List;

/**
 * لایه‌ی سازگاریِ نازک: GameplayController رو بدونِ تغییرِ اساسی به GameEngine وصل می‌کنه.
 * getWorld() مستقیم خودِ GameEngine رو برمی‌گردونه.
 */
public class Simulation {

    private final GameEngine engine;

    public Simulation(GameEngine engine) {
        this.engine = engine;
    }

    public GameEngine getWorld() {
        return engine;
    }

    public List<String> advance(int ticks) {
        return engine.advance(ticks);
    }

    public GameEngine.SunCollectionOutcome collectSun(int x, int y) {
        return engine.collectSunAt(x, y);
    }

    public int cheatCollectAllSuns() {
        return engine.cheatCollectAllSuns();
    }

    public int getSunAmount() {
        return engine.getSun();
    }

    public void addSun(int amount) {
        engine.addSun(amount);
    }
}
