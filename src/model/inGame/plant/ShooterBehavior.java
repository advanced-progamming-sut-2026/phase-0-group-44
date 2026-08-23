package model.inGame.plant;

import model.GameEngine;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.enums.PlantType;
import model.inGame.projectile.FireEffect;
import model.inGame.projectile.IceEffect;
import model.inGame.projectile.NormalEffect;
import model.inGame.projectile.ProjectileEffect;
import model.inGame.projectile.ProjectileFactory;
import model.inGame.projectile.Projectile;
import model.inGame.zombie.Zombie;

public class ShooterBehavior extends AbstractTimedBehavior {
    public enum Mode {
        FORWARD,
        REPEATER,
        THREE_LANES,
        DIAGONAL,
        SPLIT,
        CHARGED,
        BOWLING,
        GATLING,
        SHORT_RANGE
    }

    private final Mode mode;
    private final ProjectileFactory projectileFactory = new ProjectileFactory();

    public ShooterBehavior(Mode mode) {
        this.mode = mode;
    }

    @Override
    public void tick(Plant plant, GameEngine engine, double deltaSeconds) {
        if (mode == Mode.SHORT_RANGE) {
            double lifespan = 60 + plant.getStats().getSpecial("LIFESPAN", 0);
            double resetAt = plant.getState("LIFESPAN_RESET_AT", Double.class, 0.0);
            if (plant.getAgeSeconds() - resetAt >= lifespan) {
                plant.expire(engine);
                return;
            }
        }
        if (!hasTarget(plant, engine) || !ready(plant, deltaSeconds, effectiveInterval(plant))) {
            return;
        }
        fireVolley(plant, engine, false);
        if (plant.getStats().getSpecial("PLANT_FOOD_CHANCE", 0) > 0
                && engine.getRandom().nextDouble() < plant.getStats().getSpecial("PLANT_FOOD_CHANCE", 0)) {
            onPlantFood(plant, engine);
        }
    }

    private double effectiveInterval(Plant plant) {
        if (mode != Mode.BOWLING) {
            return plant.getStats().getActionInterval();
        }
        return Math.max(0.25, plant.getStats().getActionInterval()
                + plant.getStats().getSpecial("REGEN_INTERVAL", 0));
    }

    private boolean hasTarget(Plant plant, GameEngine engine) {
        double range = range(plant);
        if (mode == Mode.THREE_LANES || mode == Mode.DIAGONAL) {
            for (int row = Math.max(0, plant.getPosition().getRow() - 1);
                 row <= Math.min(engine.getGameMap().getRows() - 1, plant.getPosition().getRow() + 1); row++) {
                if (!engine.getZombiesInLane(row).isEmpty() || hasBlockingObstacleAhead(plant, engine, range, row)) {
                    return true;
                }
            }
            return false;
        }
        return engine.getFirstZombieAhead(plant, range) != null
                || mode == Mode.SPLIT && engine.getFirstZombieBehind(plant, range) != null
                || hasBlockingObstacleAhead(plant, engine, range, plant.getPosition().getRow());
    }

    /**
     * True if a destructible obstacle (grave, ice, barrel, etc.) sits within
     * range ahead of the plant in {@code row}, even with no zombie present —
     * plants should proactively clear these rather than only damage them as a
     * side effect of shooting through them at a zombie behind.
     */
    private boolean hasBlockingObstacleAhead(Plant plant, GameEngine engine, double range, int row) {
        int startColumn = plant.getPosition().getColumn() + 1;
        int maxColumn = Math.min(engine.getGameMap().getColumns() - 1,
                (int) Math.floor(plant.getPosition().getColumn() + range));
        for (int column = startColumn; column <= maxColumn; column++) {
            if (engine.getGameMap().getTile(row, column).blocksDirectProjectiles()) {
                return true;
            }
        }
        return false;
    }

    private void fireVolley(Plant plant, GameEngine engine, boolean food) {
        plant.markAttacked();
        int damage = boostedDamage(plant, engine);
        int row = plant.getPosition().getRow();
        ProjectileEffect effect = projectileEffect(plant);
        double range = range(plant);
        switch (mode) {
            case FORWARD, CHARGED, SHORT_RANGE -> fireDirect(plant, engine, row, 1, damage, effect, range);
            case REPEATER -> {
                fireDirect(plant, engine, row, 1, damage, effect, range);
                fireDirect(plant, engine, row, 1, damage, effect, range);
            }
            case THREE_LANES -> {
                for (int targetRow = row - 1; targetRow <= row + 1; targetRow++) {
                    if (targetRow >= 0 && targetRow < engine.getGameMap().getRows()) {
                        fireDirect(plant, engine, targetRow, 1, damage, effect, range);
                    }
                }
            }
            case DIAGONAL -> {
                for (int targetRow : new int[]{row - 1, row + 1}) {
                    if (targetRow >= 0 && targetRow < engine.getGameMap().getRows()) {
                        fireDirect(plant, engine, targetRow, 1, damage, effect, range);
                        fireDirect(plant, engine, targetRow, -1, damage, effect, range);
                    }
                }
            }
            case SPLIT -> {
                fireDirect(plant, engine, row, 1, damage, effect, range);
                fireDirect(plant, engine, row, -1, damage, effect, range);
                fireDirect(plant, engine, row, -1, damage, effect, range);
            }
            case GATLING -> {
                for (int i = 0; i < 4; i++) {
                    fireDirect(plant, engine, row, 1, damage, effect, range);
                }
            }
            case BOWLING -> {
                BowlingBulbShot shot = nextBowlingBulbShot(plant, damage);
                if (shot.damage() > 0) {
                    plant.putState("LAST_BOWLING_VARIANT", shot.variant());
                    Projectile projectile = projectileFactory.bouncing(
                            plant, row, shot.damage(), effect, 3, range);
                    projectile.setVisualVariant(shot.variant());
                    engine.spawnProjectile(projectile);
                }
            }
        }
    }

    private BowlingBulbShot nextBowlingBulbShot(Plant plant, int upgradedCyanDamage) {
        double regenerationDelta = plant.getStats().getSpecial("REGEN_INTERVAL", 0);
        double now = plant.getAgeSeconds();
        double orangeCooldown = Math.max(1.0, 10.0 + regenerationDelta);
        double blueCooldown = Math.max(1.0, 5.0 + regenerationDelta);
        double cyanCooldown = Math.max(0.25, 2.0 + regenerationDelta);
        double lastOrange = plant.getState("BOWLING_LAST_ORANGE", Double.class, -10_000.0);
        double lastBlue = plant.getState("BOWLING_LAST_BLUE", Double.class, -10_000.0);
        double lastCyan = plant.getState("BOWLING_LAST_CYAN", Double.class, -10_000.0);
        int damageUpgrade = upgradedCyanDamage - 40;
        if (now - lastOrange >= orangeCooldown) {
            plant.putState("BOWLING_LAST_ORANGE", now);
            return new BowlingBulbShot(180 + damageUpgrade, 3);
        }
        if (now - lastBlue >= blueCooldown) {
            plant.putState("BOWLING_LAST_BLUE", now);
            return new BowlingBulbShot(120 + damageUpgrade, 2);
        }
        if (now - lastCyan >= cyanCooldown) {
            plant.putState("BOWLING_LAST_CYAN", now);
            return new BowlingBulbShot(40 + damageUpgrade, 1);
        }
        return BowlingBulbShot.NONE;
    }

    private record BowlingBulbShot(int damage, int variant) {
        private static final BowlingBulbShot NONE = new BowlingBulbShot(0, 0);
    }

    private void fireDirect(Plant plant, GameEngine engine, int row, int direction,
                            int damage, ProjectileEffect effect, double range) {
        engine.spawnProjectile(projectileFactory.direct(plant, row, direction, damage, effect, range));
    }

    private ProjectileEffect projectileEffect(Plant plant) {
        if (plant.getEffectiveDefinition().hasTag(PlantTag.FIRE)) {
            return new FireEffect();
        }
        if (plant.getEffectiveDefinition().hasTag(PlantTag.ICE)) {
            return new IceEffect(5 + plant.getStats().getSpecial("CHILL_DURATION", 0));
        }
        return new NormalEffect();
    }

    private double range(Plant plant) {
        double base = mode == Mode.SHORT_RANGE ? 3.0 : 20.0;
        return base + plant.getStats().getSpecial("RANGE", 0);
    }

    @Override
    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public void onPlantFood(Plant plant, GameEngine engine) {
        if (mode == Mode.CHARGED) {
            engine.damageLane(plant.getPosition().getRow(), 2000, model.enums.DamageType.NORMAL);
            return;
        }
        if (mode == Mode.BOWLING) {
            int damage = 300 + (plant.getStats().getDamage() - 40);
            ProjectileEffect explosive = new model.inGame.projectile.AreaEffect(
                    new NormalEffect(), damage / 2, 1, 1, model.enums.DamageType.NORMAL);
            // Plant Food rolls all three supplied bulb visuals together.
            // Damage remains identical; the variant only selects artwork.
            for (int i = 0; i < 3; i++) {
                Projectile projectile = projectileFactory.bouncing(
                        plant, plant.getPosition().getRow(), damage, explosive, 5, 20.0);
                projectile.setVisualVariant(i + 1);
                engine.spawnProjectile(projectile);
            }
            plant.putState("LAST_BOWLING_VARIANT", 3);
            return;
        }
        if (mode == Mode.THREE_LANES) {
            int damage = boostedDamage(plant, engine);
            for (int volley = 0; volley < 3; volley++) {
                for (int row = 0; row < engine.getGameMap().getRows(); row++) {
                    fireDirect(plant, engine, row, 1, damage, projectileEffect(plant), 20.0);
                }
            }
            return;
        }
        if (mode == Mode.SHORT_RANGE) {
            for (Plant other : engine.getGameMap().getPlants()) {
                if (other.getEffectiveType() == plant.getEffectiveType()) {
                    other.putState("LIFESPAN_RESET_AT", other.getAgeSeconds());
                }
            }
        }
        if (plant.getEffectiveType() == PlantType.SNOW_PEA) {
            for (Zombie zombie : engine.getZombiesInLane(plant.getPosition().getRow())) {
                zombie.applyFreeze(2.0);
            }
        }
        int volleys = mode == Mode.GATLING ? 5 : 3;
        for (int i = 0; i < volleys; i++) {
            fireVolley(plant, engine, true);
        }
        if (mode == Mode.REPEATER) {
            fireDirect(plant, engine, plant.getPosition().getRow(), 1,
                    plant.getStats().getDamage() * 20, projectileEffect(plant), 20.0);
        } else if (mode == Mode.GATLING) {
            for (int i = 0; i < 4; i++) {
                fireDirect(plant, engine, plant.getPosition().getRow(), 1,
                        plant.getStats().getDamage() * 20, projectileEffect(plant), 20.0);
            }
        }
    }
}
