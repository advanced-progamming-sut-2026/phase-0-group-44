package model.inGame.projectile;

import model.GameEngine;
import model.enums.DamageType;
import model.inGame.zombie.Zombie;

public interface ProjectileEffect {
    void apply(Projectile projectile, Zombie zombie, GameEngine engine);

    default DamageType damageType() {
        return DamageType.NORMAL;
    }
}
