package model.inGame.projectile;

import model.GameEngine;
import model.enums.DamageType;
import model.inGame.zombie.Zombie;

public class NormalEffect implements ProjectileEffect {
    @Override
    public void apply(Projectile projectile, Zombie zombie, GameEngine engine) {
        zombie.receiveDamage(projectile.getDamage(), DamageType.NORMAL, engine);
    }

    @Override
    public DamageType damageType() {
        return DamageType.NORMAL;
    }

}
