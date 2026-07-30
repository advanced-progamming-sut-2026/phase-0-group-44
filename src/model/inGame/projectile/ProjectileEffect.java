package model.inGame.projectile;

import model.GameEngine;
import model.enums.DamageType;
import model.inGame.zombie.Zombie;

public interface ProjectileEffect {
    void apply(Projectile projectile, Zombie zombie, GameEngine engine);

    default DamageType damageType() {
        return DamageType.NORMAL;
    }

    /** Short clause describing any status effect this applies (e.g. " and stunned it for 4.0s"). Empty if none. */
    default String statusEffectSuffix() {
        return "";
    }
}
