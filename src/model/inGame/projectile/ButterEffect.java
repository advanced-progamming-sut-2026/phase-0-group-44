package model.inGame.projectile;

import model.GameEngine;
import model.enums.DamageType;
import model.inGame.zombie.Zombie;

public class ButterEffect implements ProjectileEffect {
    private final double stunDuration;

    public ButterEffect(double stunDuration) {
        this.stunDuration = stunDuration;
    }

    @Override
    public void apply(Projectile projectile, Zombie zombie, GameEngine engine) {
        zombie.takeDamage(projectile.getDamage(), DamageType.NORMAL);
        zombie.applyFreeze(stunDuration);
    }
}
