package model.inGame.zombie;

import model.GameEngine;
import model.Position;
import model.inGame.plant.Plant;

public final class DodoZombieMovement extends NormalZombieMovement {
    @Override
    public void move(Zombie zombie, GameEngine engine, double deltaSeconds) {
        Plant plant = engine.findCollidingPlant(zombie);
        if (plant != null && shouldFlyOver(plant)) {
            zombie.moveBy(zombie.getDirection() * 1.05);
            engine.recordEvent("Dodo Rider flew over " + plant.getEffectiveType() + ".");
            return;
        }
        super.move(zombie, engine, deltaSeconds);
    }

    private boolean shouldFlyOver(Plant plant) {
        return DodoZombieAttack.canFlyOver(plant);
    }
}
