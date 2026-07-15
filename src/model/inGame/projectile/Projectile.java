package model.inGame.projectile;

import model.GameEngine;
import model.enums.PlantTag;
import model.enums.PlantType;
import model.inGame.zombie.Zombie;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Projectile {
    private final PlantType sourceType;
    private int row;
    private double x;
    private int direction;
    private int damage;
    private final double speed;
    private final ProjectileType type;
    private ProjectileEffect effect;
    private final CollisionLogic collisionLogic;
    private final Set<Long> hitZombieIds = new HashSet<>();
    private final Set<Integer> transformedColumns = new HashSet<>();
    private int maxHits;
    private double remainingRange;
    private Zombie target;
    private final double landingX;
    private double flightRemaining;
    private boolean active = true;
    private boolean peaProjectile;

    Projectile(PlantType sourceType, int row, double x, int direction, int damage,
               double speed, ProjectileType type, ProjectileEffect effect,
               int maxHits, double range, Zombie target, double flightTime,
               boolean peaProjectile) {
        this.sourceType = sourceType;
        this.row = row;
        this.x = x;
        this.direction = direction >= 0 ? 1 : -1;
        this.damage = Math.max(0, damage);
        this.speed = Math.max(0.1, speed);
        this.type = type;
        this.effect = effect == null ? new NormalEffect() : effect;
        this.maxHits = Math.max(1, maxHits);
        this.remainingRange = Math.max(0.1, range);
        this.target = target;
        this.landingX = target == null ? x + this.direction * range : target.getX();
        this.flightRemaining = Math.max(0.05, flightTime);
        this.peaProjectile = peaProjectile;
        this.collisionLogic = switch (type) {
            case DIRECT -> new NormalCollision();
            case PIERCING -> new PiercingCollision();
            case LOBBED -> new LobberCollision();
            case HOMING -> new HomingCollision();
            case BOUNCING -> new BouncingCollision();
        };
    }

    public void tick(GameEngine engine, double deltaSeconds) {
        if (!active || deltaSeconds <= 0) {
            return;
        }
        collisionLogic.advance(this, engine, deltaSeconds);
    }

    void advanceLinear(GameEngine engine, double deltaSeconds, boolean piercing, boolean bouncing) {
        double oldX = x;
        double distance = speed * deltaSeconds;
        double newX = x + direction * distance;
        remainingRange -= distance;

        Integer blocker = engine.getGameMap().firstBlockingColumn(row, oldX, newX);
        double effectiveNewX = blocker == null ? newX : blocker;
        engine.transformProjectileAlongPath(this, oldX, effectiveNewX);

        List<Zombie> crossed = engine.getZombiesCrossed(row, oldX, effectiveNewX, direction, hitZombieIds);
        for (Zombie zombie : crossed) {
            hit(zombie, engine);
            if (!piercing || hitZombieIds.size() >= maxHits) {
                active = false;
                break;
            }
            if (bouncing) {
                Zombie next = engine.findBounceTarget(zombie, direction, hitZombieIds);
                if (next != null) {
                    row = next.getRow();
                    target = next;
                }
            }
        }
        x = effectiveNewX;
        if (blocker != null || remainingRange <= 0 || x < 0 || x > engine.getGameMap().getColumns()) {
            active = false;
        }
    }

    void advanceLobbed(GameEngine engine, double deltaSeconds) {
        flightRemaining -= deltaSeconds;
        if (flightRemaining > 0) {
            return;
        }
        Zombie landingTarget = null;
        if (target != null && !target.isDead() && target.getRow() == row
                && Math.abs(target.getX() - landingX) <= 0.75) {
            landingTarget = target;
        }
        if (landingTarget == null) {
            landingTarget = engine.findZombieAtLanding(row, landingX);
        }
        if (landingTarget != null) {
            hit(landingTarget, engine);
        }
        active = false;
    }

    void advanceHoming(GameEngine engine, double deltaSeconds) {
        if (target == null || target.isDead()) {
            target = engine.findNearestZombie(row, x, true);
            if (target == null) {
                active = false;
                return;
            }
        }
        flightRemaining -= deltaSeconds;
        if (flightRemaining <= 0) {
            row = target.getRow();
            hit(target, engine);
            active = false;
        }
    }

    private void hit(Zombie zombie, GameEngine engine) {
        if (zombie == null || zombie.isDead() || hitZombieIds.contains(zombie.getId())) {
            return;
        }
        hitZombieIds.add(zombie.getId());
        effect.apply(this, zombie, engine);
        engine.recordEvent(sourceType + " projectile hit " + zombie.getName() + " for " + damage + ".");
    }

    public PlantType getSourceType() {
        return sourceType;
    }

    public int getRow() {
        return row;
    }

    public double getX() {
        return x;
    }

    public int getDirection() {
        return direction;
    }

    public int getDamage() {
        return damage;
    }

    public void multiplyDamage(double multiplier) {
        damage = Math.max(0, (int) Math.round(damage * multiplier));
    }

    public ProjectileType getType() {
        return type;
    }

    public ProjectileEffect getEffect() {
        return effect;
    }

    public void setEffect(ProjectileEffect effect) {
        this.effect = effect;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isPeaProjectile() {
        return peaProjectile;
    }

    public void setPeaProjectile(boolean peaProjectile) {
        this.peaProjectile = peaProjectile;
    }

    public boolean markTransformedAt(int column) {
        return transformedColumns.add(column);
    }
}
