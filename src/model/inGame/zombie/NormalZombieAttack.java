package model.inGame.zombie;

import model.GameEngine;
import model.inGame.plant.Plant;

public class NormalZombieAttack implements ZombieAttackComponent {
    private final double multiplier;

    public NormalZombieAttack() {
        this(1.0);
    }

    public NormalZombieAttack(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public boolean attack(Zombie zombie, GameEngine engine, double deltaSeconds) {
        Plant target = engine.findCollidingPlant(zombie);
        if (target == null || target.getBooleanState("TRANSFORMED")) {
            return false;
        }
        double pending = zombie.getDoubleState("ATTACK_DAMAGE", 0.0)
                + zombie.getEatDamagePerSecond() * multiplier * deltaSeconds;
        int wholeDamage = (int) Math.floor(pending);
        zombie.putState("ATTACK_DAMAGE", pending - wholeDamage);
        if (wholeDamage > 0) {
            target.receiveDamage(wholeDamage, engine, zombie);
        }
        return true;
    }
}
