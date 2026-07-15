package model.inGame.projectile;

import model.GameEngine;
import model.enums.DamageType;
import model.inGame.zombie.Zombie;

public class FireEffect implements ProjectileEffect {
    @Override
    public void apply(Projectile projectile, Zombie zombie, GameEngine engine) {
        zombie.onFireHit(engine);
        zombie.receiveDamage(projectile.getDamage(), DamageType.FIRE, engine);
        zombie.thaw();
    }

    @Override
    public DamageType damageType() {
        return DamageType.FIRE;
    }

}
