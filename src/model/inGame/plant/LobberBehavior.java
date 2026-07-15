package model.inGame.plant;

import model.GameEngine;
import model.enums.DamageType;
import model.enums.PlantTag;
import model.inGame.projectile.AreaEffect;
import model.inGame.projectile.ButterEffect;
import model.inGame.projectile.FireEffect;
import model.inGame.projectile.IceEffect;
import model.inGame.projectile.NormalEffect;
import model.inGame.projectile.ProjectileEffect;
import model.inGame.projectile.ProjectileFactory;
import model.inGame.zombie.Zombie;

public class LobberBehavior extends AbstractTimedBehavior {
    public enum Mode {
        NORMAL,
        KERNEL,
        SPLASH,
        ICE_SPLASH,
        FIRE_SPLASH
    }

    private final Mode mode;
    private final ProjectileFactory projectileFactory = new ProjectileFactory();

    public LobberBehavior(Mode mode) {
        this.mode = mode;
    }

    @Override
    public void tick(Plant plant, GameEngine engine, double deltaSeconds) {
        if (mode == Mode.FIRE_SPLASH) {
            int warmthRadius = 1 + (int) plant.getStats().getSpecial("WARMTH_RADIUS", 0);
            engine.clearIce(plant.getPosition(), warmthRadius);
        }
        Zombie target = engine.getFirstZombieAhead(plant, 20.0);
        if (target == null || !ready(plant, deltaSeconds, plant.getStats().getActionInterval())) {
            return;
        }
        launch(plant, engine, target, false);
    }

    private void launch(Plant plant, GameEngine engine, Zombie target, boolean food) {
        int damage = boostedDamage(plant, engine);
        ProjectileEffect effect;
        if (mode == Mode.KERNEL) {
            double butterChance = 0.25 + plant.getStats().getSpecial("BUTTER_CHANCE", 0);
            if (engine.getRandom().nextDouble() < butterChance) {
                damage = Math.max(damage, 40 + (plant.getStats().getDamage() - 20));
                effect = new ButterEffect(4.0);
            } else {
                effect = new NormalEffect();
            }
        } else if (mode == Mode.SPLASH || mode == Mode.ICE_SPLASH || mode == Mode.FIRE_SPLASH) {
            DamageType type = mode == Mode.ICE_SPLASH ? DamageType.ICE
                    : mode == Mode.FIRE_SPLASH ? DamageType.FIRE : DamageType.NORMAL;
            ProjectileEffect primary = mode == Mode.ICE_SPLASH ? new IceEffect()
                    : mode == Mode.FIRE_SPLASH ? new FireEffect() : new NormalEffect();
            int splash = Math.max(1, damage / 2 + (int) plant.getStats().getSpecial("SPLASH_DAMAGE", 0));
            effect = new AreaEffect(primary, splash, 1, 1, type);
        } else {
            effect = new NormalEffect();
        }
        engine.spawnProjectile(projectileFactory.lobbed(plant, target, food ? damage * 2 : damage, effect));
    }

    @Override
    public void onPlantFood(Plant plant, GameEngine engine) {
        if (mode == Mode.KERNEL) {
            for (Zombie zombie : engine.getZombies()) {
                if (!zombie.isDead() && !zombie.isHypnotized()) {
                    zombie.applyFreeze(4.0);
                }
            }
            return;
        }
        int count = mode == Mode.FIRE_SPLASH ? 3 : 5;
        for (Zombie target : engine.getRandomZombies(count)) {
            launch(plant, engine, target, true);
        }
    }
}
