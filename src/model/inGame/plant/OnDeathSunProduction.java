package model.inGame.plant;

import model.GameEngine;

/** Legacy strategy for Sun Bean-like integrations. */
class OnDeathSunProduction implements SunProduceBehavior {
    private boolean produced;

    @Override
    public void produce(Plant plant, GameEngine engine) {
        if (!produced && plant.isDead()) {
            engine.addSun(50);
            produced = true;
        }
    }
}
