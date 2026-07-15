package model.inGame.zombie;

import model.GameEngine;
import model.inGame.plant.Plant;

public final class InstantDestroyZombieAttack implements ZombieAttackComponent {
    private final String eventPrefix;

    public InstantDestroyZombieAttack(String eventPrefix) {
        this.eventPrefix = eventPrefix;
    }

    @Override
    public boolean attack(Zombie zombie, GameEngine engine, double deltaSeconds) {
        Plant target = engine.findCollidingPlant(zombie);
        if (target == null || target.getBooleanState("TRANSFORMED")) {
            return false;
        }
        target.receiveDamage(Integer.MAX_VALUE, engine, zombie);
        engine.recordEvent(eventPrefix + " destroyed " + target.getEffectiveType() + ".");
        return true;
    }
}
