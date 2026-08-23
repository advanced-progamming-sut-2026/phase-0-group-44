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
        Integer obstacleColumn = target == null ? firstBlockingColumnAhead(plant, engine, 20.0) : null;
        if (target == null && obstacleColumn == null) {
            return;
        }
        if (!ready(plant, deltaSeconds, plant.getStats().getActionInterval())) {
            return;
        }
        if (target != null) {
            launch(plant, engine, target, false);
        } else {
            launchAtObstacle(plant, engine, obstacleColumn);
        }
    }

    /**
     * True if a destructible obstacle (grave, ice, barrel, etc.) sits within
     * range ahead of the plant — lobbers should proactively clear these
     * rather than only ever requiring a live zombie target.
     */
    private Integer firstBlockingColumnAhead(Plant plant, GameEngine engine, double range) {
        int row = plant.getPosition().getRow();
        int startColumn = plant.getPosition().getColumn() + 1;
        int maxColumn = Math.min(engine.getGameMap().getColumns() - 1,
                (int) Math.floor(plant.getPosition().getColumn() + range));
        for (int column = startColumn; column <= maxColumn; column++) {
            if (engine.getGameMap().getTile(row, column).blocksDirectProjectiles()) {
                return column;
            }
        }
        return null;
    }

    private void launch(Plant plant, GameEngine engine, Zombie target, boolean food) {
        plant.markAttacked();
        int[] damageHolder = {boostedDamage(plant, engine)};
        ProjectileEffect effect = resolveEffect(plant, engine, damageHolder);
        int damage = food ? damageHolder[0] * 2 : damageHolder[0];
        engine.spawnProjectile(projectileFactory.lobbed(plant, target, damage, effect));
    }

    private void launchAtObstacle(Plant plant, GameEngine engine, int column) {
        plant.markAttacked();
        int[] damageHolder = {boostedDamage(plant, engine)};
        ProjectileEffect effect = resolveEffect(plant, engine, damageHolder);
        engine.spawnProjectile(projectileFactory.lobbedAt(
                plant, plant.getPosition().getRow(), column + 0.5, damageHolder[0], effect));
    }

    /** Shared damage/effect selection for both a live-zombie lob and an obstacle lob. */
    private ProjectileEffect resolveEffect(Plant plant, GameEngine engine, int[] damageHolder) {
        int damage = damageHolder[0];
        ProjectileEffect effect;
        if (mode == Mode.KERNEL) {
            double butterChance = 0.25 + plant.getStats().getSpecial("BUTTER_CHANCE", 0);
            boolean butter = engine.getRandom().nextDouble() < butterChance;
            plant.putState("LAST_KERNEL_BUTTER", butter);
            if (butter) {
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
        damageHolder[0] = damage;
        return effect;
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
