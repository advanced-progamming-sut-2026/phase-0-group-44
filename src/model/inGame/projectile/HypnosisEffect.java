package model.inGame.projectile;

import model.GameEngine;
import model.enums.DamageType;
import model.inGame.zombie.Zombie;

/** A Caulipower projectile that converts its target to the player's side. */
public final class HypnosisEffect implements ProjectileEffect {
    @Override
    public void apply(Projectile projectile, Zombie zombie, GameEngine engine) {
        zombie.hypnotize();
        engine.recordEvent(zombie.getName() + " was hypnotized.");
    }

    @Override
    public DamageType damageType() {
        return DamageType.TRUE;
    }
}
