package model.inGame.projectile;

import model.GameEngine;
import model.enums.DamageType;
import model.inGame.zombie.Zombie;

public class FireEffect implements ProjectileEffect {
    @Override
    public void apply(Projectile projectile, Zombie zombie, GameEngine engine) {
        zombie.takeDamage(projectile.getDamage(), DamageType.FIRE);
        zombie.thaw();
    }
}
