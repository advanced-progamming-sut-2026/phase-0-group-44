package model.inGame.projectile;

import model.GameEngine;
import model.enums.DamageType;
import model.inGame.zombie.Zombie;

public class PoisonEffect implements ProjectileEffect {
    private final int tickDamage;
    private final double duration;

    public PoisonEffect() {
        this(5, 5.0);
    }

    public PoisonEffect(int tickDamage, double duration) {
        this.tickDamage = tickDamage;
        this.duration = duration;
    }

    @Override
    public void apply(Projectile projectile, Zombie zombie, GameEngine engine) {
        zombie.receiveDamage(projectile.getDamage(), DamageType.POISON, engine);
        zombie.applyPoison(tickDamage, duration);
    }

    @Override
    public DamageType damageType() {
        return DamageType.POISON;
    }

}
