package model.inGame.zombie;

import model.GameEngine;
import model.Position;
import model.inGame.plant.Plant;

public final class DodoZombieMovement extends NormalZombieMovement {
    @Override
    public void move(Zombie zombie, GameEngine engine, double deltaSeconds) {
        ZombieAbilityVisuals.tick(
                zombie,
                "DODO_FLY_VISUAL",
                "FLYING",
                deltaSeconds
        );
        Plant plant = engine.findCollidingPlant(zombie);
        if (plant != null && shouldFlyOver(plant)) {
            ZombieAbilityVisuals.start(
                    zombie,
                    "DODO_FLY_VISUAL",
                    "FLYING",
                    0.70
            );
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
