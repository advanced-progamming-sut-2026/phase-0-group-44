package model.inGame.plant;

import model.GameEngine;
import model.inGame.projectile.NormalEffect;
import model.inGame.projectile.ProjectileFactory;
import model.inGame.zombie.Zombie;

public class PiercingBehavior extends AbstractTimedBehavior {
    public enum Mode {
        CACTUS,
        FUME
    }

    private final Mode mode;
    private final ProjectileFactory projectileFactory = new ProjectileFactory();

    public PiercingBehavior(Mode mode) {
        this.mode = mode;
    }

    @Override
    public void tick(Plant plant, GameEngine engine, double deltaSeconds) {
        double range = (mode == Mode.FUME ? 4.0 : 20.0) + plant.getStats().getSpecial("RANGE", 0);
        if (engine.getFirstZombieAhead(plant, range) == null
                || !ready(plant, deltaSeconds, plant.getStats().getActionInterval())) {
            return;
        }
        int hits = mode == Mode.CACTUS ? 3 + (int) plant.getStats().getSpecial("PIERCE", 0) : 99;
        engine.spawnProjectile(projectileFactory.piercing(plant, plant.getPosition().getRow(), 1,
                boostedDamage(plant, engine), new NormalEffect(), hits, range));
    }

    @Override
    public void onPlantFood(Plant plant, GameEngine engine) {
        if (mode == Mode.FUME) {
            for (Zombie zombie : engine.getZombiesInLane(plant.getPosition().getRow())) {
                zombie.takeDamage(100, model.enums.DamageType.NORMAL);
                zombie.moveBy(1.0);
            }
        } else {
            engine.spawnProjectile(projectileFactory.piercing(plant, plant.getPosition().getRow(), 1,
                    Math.max(100, plant.getStats().getDamage() * 4), new NormalEffect(), 99, 20.0));
        }
    }
}
