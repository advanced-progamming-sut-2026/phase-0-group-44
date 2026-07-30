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
        zombie.receiveDamage(projectile.getDamage(), DamageType.NORMAL, engine);
        zombie.applyStun(stunDuration);
    }

    @Override
    public DamageType damageType() {
        return DamageType.NORMAL;
    }

    @Override
    public String statusEffectSuffix() {
        return " and stunned it for " + stunDuration + "s";
    }

}
