package model.inGame.zombie;

import model.GameEngine;
import model.enums.PlantTag;
import model.inGame.plant.Plant;

/** Lets the Dodo movement component jump only the documented obstacle plants. */
public final class DodoZombieAttack implements ZombieAttackComponent {
    private final NormalZombieAttack normal = new NormalZombieAttack();

    @Override
    public boolean attack(Zombie zombie, GameEngine engine, double deltaSeconds) {
        Plant plant = engine.findCollidingPlant(zombie);
        if (plant != null && canFlyOver(plant)) {
            return false;
        }
        return normal.attack(zombie, engine, deltaSeconds);
    }

    static boolean canFlyOver(Plant plant) {
        if (plant == null || plant.blocksJumping()) {
            return false;
        }
        return plant.getMaxHp() >= 1000
                || plant.getTags().contains(PlantTag.TRAP)
                || plant.getTags().contains(PlantTag.MOVE_ZOMBIES);
    }
}
