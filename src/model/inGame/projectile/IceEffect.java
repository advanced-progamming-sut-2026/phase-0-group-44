package model.inGame.projectile;

import model.GameEngine;
import model.enums.DamageType;
import model.inGame.zombie.Zombie;

public class IceEffect implements ProjectileEffect {
    private final double slowDuration;

    public IceEffect() {
        this(5.0);
    }

    public IceEffect(double slowDuration) {
        this.slowDuration = slowDuration;
    }

    @Override
    public void apply(Projectile projectile, Zombie zombie, GameEngine engine) {
        zombie.takeDamage(projectile.getDamage(), DamageType.ICE);
        zombie.applySlow(slowDuration);
    }
}
