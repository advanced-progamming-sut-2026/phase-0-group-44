package model.inGame.plant;

import model.GameEngine;
import model.inGame.projectile.NormalEffect;
import model.inGame.projectile.ProjectileFactory;

/** Legacy adapter; mandatory plants use ShooterBehavior through PlantBehaviorFactory. */
class ShooterAttack implements AttackBehavior {
    private final ProjectileFactory projectileFactory = new ProjectileFactory();

    @Override
    public void attack(Plant plant, GameEngine engine) {
        if (plant.getPosition() == null || engine.getFirstZombieAhead(plant, 20.0) == null) {
            return;
        }
        engine.spawnProjectile(projectileFactory.direct(
                plant,
                plant.getPosition().getRow(),
                1,
                plant.getStats().getDamage(),
                new NormalEffect(),
                20.0
        ));
    }
}