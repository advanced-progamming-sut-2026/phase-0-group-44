package model.inGame.plant;

import model.GameEngine;

class NoSunProduction implements SunProduceBehavior {
    @Override
    public void produce(Plant plant, GameEngine engine) {
        // does not produce sun
    }
}
