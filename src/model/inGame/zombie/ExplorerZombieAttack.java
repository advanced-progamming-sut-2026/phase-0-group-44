package model.inGame.zombie;

import model.GameEngine;
import model.inGame.plant.Plant;

public final class ExplorerZombieAttack implements ZombieAttackComponent {
    private final NormalZombieAttack normal = new NormalZombieAttack();

    @Override
    public boolean attack(Zombie zombie, GameEngine engine, double deltaSeconds) {
        Plant target = engine.findPlantWithin(zombie.getRow(), zombie.getX(), zombie.getDirection(), 0.99);
        if (target == null || target.getBooleanState("TRANSFORMED")) {
            return false;
        }
        if (zombie.getBooleanState("TORCH_LIT")) {
            target.receiveDamage(Integer.MAX_VALUE, engine, zombie);
            engine.recordEvent("Explorer torch burned " + target.getEffectiveType() + ".");
            return true;
        }
        return normal.attack(zombie, engine, deltaSeconds);
    }
}
