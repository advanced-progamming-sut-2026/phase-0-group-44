package model.inGame.zombie;

import model.GameEngine;
import model.inGame.plant.Plant;

/** A Wizard transforms a colliding plant instead of eating it. */
final class WizardZombieAttack implements ZombieAttackComponent {
    @Override
    public boolean attack(Zombie zombie, GameEngine engine, double deltaSeconds) {
        Plant target = engine.findCollidingPlant(zombie);
        if (target == null) {
            return false;
        }
        engine.transformPlantToCat(target, zombie);
        return true;
    }
}
