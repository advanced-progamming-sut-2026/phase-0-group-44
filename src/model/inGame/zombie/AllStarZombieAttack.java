package model.inGame.zombie;

import model.GameEngine;
import model.inGame.plant.Plant;

public final class AllStarZombieAttack implements ZombieAttackComponent {
    private final NormalZombieAttack slowAttack = new NormalZombieAttack(0.5);

    @Override
    public boolean attack(Zombie zombie, GameEngine engine, double deltaSeconds) {
        if (!zombie.getBooleanState("CHARGING")) {
            return slowAttack.attack(zombie, engine, deltaSeconds);
        }
        Plant plant = engine.findCollidingPlant(zombie);
        Zombie hypnotized = engine.findCollidingHypnotizedZombie(zombie);
        if (plant == null && hypnotized == null) {
            return false;
        }
        if (plant != null && !plant.getBooleanState("TRANSFORMED")) {
            plant.receiveDamage(Integer.MAX_VALUE, engine, zombie);
            engine.recordEvent("All-Star charge destroyed " + plant.getEffectiveType() + ".");
        } else if (hypnotized != null) {
            hypnotized.receiveDamage(Integer.MAX_VALUE, model.enums.DamageType.TRUE, engine);
            engine.recordEvent("All-Star charge destroyed a hypnotized zombie.");
        }
        zombie.putState("CHARGING", false);
        zombie.setRuntimeSpeedMultiplier(0.35);
        return true;
    }
}
