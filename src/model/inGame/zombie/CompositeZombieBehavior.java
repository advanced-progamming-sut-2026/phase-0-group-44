package model.inGame.zombie;

import model.GameEngine;
import model.enums.DamageType;
import model.inGame.projectile.Projectile;

import java.util.ArrayList;
import java.util.List;

public final class CompositeZombieBehavior {
    private final ZombieMovementComponent movement;
    private final ZombieAttackComponent attack;
    private final List<ZombieSpecialAbility> abilities;

    public CompositeZombieBehavior(
            ZombieMovementComponent movement,
            ZombieAttackComponent attack,
            List<ZombieSpecialAbility> abilities
    ) {
        this.movement = movement;
        this.attack = attack;
        this.abilities = abilities == null ? List.of() : new ArrayList<>(abilities);
    }

    public void tick(Zombie zombie, GameEngine engine, double deltaSeconds) {
        for (ZombieSpecialAbility ability : abilities) {
            ability.tickBeforeMovement(zombie, engine, deltaSeconds);
        }
        if (!zombie.isDead() && !zombie.isFrozen() && !zombie.isStunned()) {
            boolean blocked = attack != null && attack.attack(zombie, engine, deltaSeconds);
            if (!blocked && movement != null) {
                movement.move(zombie, engine, deltaSeconds);
            }
        }
        for (ZombieSpecialAbility ability : abilities) {
            ability.tickAfterMovement(zombie, engine, deltaSeconds);
        }
    }

    public void onDamaged(Zombie zombie, GameEngine engine, int damage, DamageType type) {
        for (ZombieSpecialAbility ability : abilities) {
            ability.onDamaged(zombie, engine, damage, type);
        }
    }

    public void onArmorBroken(Zombie zombie, GameEngine engine, String armorName) {
        for (ZombieSpecialAbility ability : abilities) {
            ability.onArmorBroken(zombie, engine, armorName);
        }
    }

    public void onDeath(Zombie zombie, GameEngine engine) {
        for (ZombieSpecialAbility ability : abilities) {
            ability.onDeath(zombie, engine);
        }
    }

    public void onIceHit(Zombie zombie, GameEngine engine) {
        for (ZombieSpecialAbility ability : abilities) {
            ability.onIceHit(zombie, engine);
        }
    }

    public void onFireHit(Zombie zombie, GameEngine engine) {
        for (ZombieSpecialAbility ability : abilities) {
            ability.onFireHit(zombie, engine);
        }
    }

    public ZombieProjectileDisposition projectileDisposition(
            Zombie zombie,
            Projectile projectile,
            GameEngine engine
    ) {
        for (ZombieSpecialAbility ability : abilities) {
            if (ability.projectileDisposition(zombie, projectile, engine)
                    == ZombieProjectileDisposition.BLOCK) {
                return ZombieProjectileDisposition.BLOCK;
            }
        }
        return ZombieProjectileDisposition.ACCEPT;
    }
}
