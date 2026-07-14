package model.inGame.plant;

import model.GameEngine;
import model.inGame.projectile.ProjectileFactory;

class ShooterAttack implements AttackBehavior {
    private ProjectileFactory projectileFactory = new ProjectileFactory();

    @Override
    public void attack(Plant plant, GameEngine engine) {
        // TODO: use projectileFactory to spawn a projectile from plant's position
    }
}