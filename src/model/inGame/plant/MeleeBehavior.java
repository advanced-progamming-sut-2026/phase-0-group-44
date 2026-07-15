package model.inGame.plant;

import model.GameEngine;
import model.Position;
import model.enums.DamageType;
import model.inGame.zombie.Zombie;

public class MeleeBehavior extends AbstractTimedBehavior {
    public enum Mode {
        BONK_CHOY,
        PHAT_BEET
    }

    private final Mode mode;

    public MeleeBehavior(Mode mode) {
        this.mode = mode;
    }

    @Override
    public void tick(Plant plant, GameEngine engine, double deltaSeconds) {
        if (!hasTarget(plant, engine) || !ready(plant, deltaSeconds, plant.getStats().getActionInterval())) {
            return;
        }
        attack(plant, engine, 1.0);
    }

    private boolean hasTarget(Plant plant, GameEngine engine) {
        if (mode == Mode.PHAT_BEET) {
            return !engine.getZombiesInArea(plant.getPosition(), 1, 1).isEmpty();
        }
        for (Zombie zombie : engine.getZombiesInArea(plant.getPosition(), 0, 1)) {
            if (zombie.getRow() == plant.getPosition().getRow()) {
                return true;
            }
        }
        return false;
    }

    private void attack(Plant plant, GameEngine engine, double multiplier) {
        int damage = (int) Math.round(boostedDamage(plant, engine) * multiplier);
        if (mode == Mode.PHAT_BEET) {
            engine.damageArea(plant.getPosition(), 1, 1, damage, DamageType.NORMAL);
        } else {
            for (Zombie zombie : engine.getZombiesInArea(plant.getPosition(), 0, 1)) {
                if (zombie.getRow() == plant.getPosition().getRow()) {
                    zombie.takeDamage(damage, DamageType.NORMAL);
                }
            }
        }
    }

    @Override
    public void onPlantFood(Plant plant, GameEngine engine) {
        if (mode == Mode.BONK_CHOY) {
            for (int i = 0; i < 9; i++) {
                engine.damageArea(plant.getPosition(), 1, 1,
                        boostedDamage(plant, engine), DamageType.NORMAL);
            }
        } else {
            attack(plant, engine, 9.0);
        }
    }
}
