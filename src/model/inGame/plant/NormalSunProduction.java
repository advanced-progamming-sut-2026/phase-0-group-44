package model.inGame.plant;

import model.GameEngine;

/** Legacy strategy kept for source compatibility; new plants use SunProducerBehavior. */
public class NormalSunProduction implements SunProduceBehavior {
    private final int amount;

    public NormalSunProduction() {
        this(50);
    }

    public NormalSunProduction(int amount) {
        this.amount = Math.max(0, amount);
    }

    @Override
    public void produce(Plant plant, GameEngine engine) {
        engine.addSun(amount);
    }
}
