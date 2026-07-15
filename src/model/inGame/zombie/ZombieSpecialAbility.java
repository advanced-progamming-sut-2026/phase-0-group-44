package model.inGame.zombie;

import model.GameEngine;
import model.enums.DamageType;
import model.inGame.projectile.Projectile;

public interface ZombieSpecialAbility {
    default void tickBeforeMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
    }

    default void tickAfterMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
    }

    default void onDamaged(
            Zombie zombie,
            GameEngine engine,
            int damage,
            DamageType damageType
    ) {
    }

    default void onArmorBroken(Zombie zombie, GameEngine engine, String armorName) {
    }

    default void onDeath(Zombie zombie, GameEngine engine) {
    }

    default void onIceHit(Zombie zombie, GameEngine engine) {
    }

    default void onFireHit(Zombie zombie, GameEngine engine) {
    }

    default ZombieProjectileDisposition projectileDisposition(
            Zombie zombie,
            Projectile projectile,
            GameEngine engine
    ) {
        return ZombieProjectileDisposition.ACCEPT;
    }
}
