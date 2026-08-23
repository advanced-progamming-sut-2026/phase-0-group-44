package model.inGame.projectile;

import model.GameEngine;
import model.enums.PlantTag;
import model.enums.PlantType;
import model.inGame.zombie.Zombie;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Projectile {
    private static final java.util.concurrent.atomic.AtomicInteger ID_COUNTER =
            new java.util.concurrent.atomic.AtomicInteger();

    private final int id = ID_COUNTER.getAndIncrement();
    private final List<int[]> path = new java.util.ArrayList<>();
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
    private final double spawnX;
    private final int spawnRow;
    private final double landingX;
    private final double flightDuration;
    private double flightRemaining;
    private boolean active = true;
    private boolean peaProjectile;
    private int visualVariant;

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
        this.spawnX = x;
        this.spawnRow = row;
        this.landingX = target == null ? x + this.direction * range : target.getX();
        this.flightDuration = Math.max(0.05, flightTime);
        this.flightRemaining = this.flightDuration;
        this.peaProjectile = peaProjectile;
        this.collisionLogic = switch (type) {
            case DIRECT -> new NormalCollision();
            case PIERCING -> new PiercingCollision();
            case LOBBED -> new LobberCollision();
            case HOMING -> new HomingCollision();
            case BOUNCING -> new BouncingCollision();
        };
        recordPath(row, x);
    }

    private void recordPath(int r, double xPos) {
        int col = (int) Math.floor(xPos);
        if (!path.isEmpty()) {
            int[] last = path.get(path.size() - 1);
            if (last[0] == r && last[1] == col) {
                return;
            }
        }
        path.add(new int[]{r, col});
    }

    /** Every distinct (row, column) cell this projectile has occupied so far, in order. */
    public List<int[]> getPath() {
        return java.util.Collections.unmodifiableList(path);
    }

    /** Stable identity used to give this projectile a consistent display color/label across ticks. */
    public int getId() {
        return id;
    }

    public void tick(GameEngine engine, double deltaSeconds) {
        if (!active || deltaSeconds <= 0) {
            return;
        }
        collisionLogic.advance(this, engine, deltaSeconds);
    }

    void advanceLinear(GameEngine engine, double deltaSeconds, boolean piercing, boolean bouncing) {
        int startRow = row;
        double oldX = x;
        double distance = speed * deltaSeconds;
        double newX = x + direction * distance;
        remainingRange -= distance;

        Integer blocker = engine.getGameMap().firstBlockingColumn(row, oldX, newX);

        // Find whichever the projectile actually reaches first: the blocking
        // obstacle or the nearest not-yet-hit zombie in its path. The search range
        // covers the obstacle's whole tile (not just its floored column boundary) so a
        // zombie standing on the same tile as the obstacle — e.g. spawned there by
        // Necromancy on top of a grave — can still be hit instead of the shot being
        // permanently stuck on the obstacle.
        double obstacleFarEdge = blocker == null ? newX
                : (direction >= 0 ? blocker + 1.0 - 1e-6 : (double) blocker);
        List<Zombie> aheadOfObstacle = engine.getZombiesCrossed(
                row, oldX, obstacleFarEdge, direction, hitZombieIds);
        boolean zombieBlocksFirst = !aheadOfObstacle.isEmpty();

        double effectiveNewX = zombieBlocksFirst
                ? aheadOfObstacle.get(0).getX()
                : (blocker == null ? newX : blocker);
        engine.transformProjectileAlongPath(this, oldX, effectiveNewX);

        if (blocker != null && !zombieBlocksFirst) {
            model.enums.ObstacleType obstacleType =
                    engine.getGameMap().getTile(row, blocker).getObstacle();
            int dealt = engine.damageObstacleAt(row, blocker, damage, effect.damageType());
            boolean destroyed =
                    engine.getGameMap().getTile(row, blocker).getObstacle() == model.enums.ObstacleType.NONE;
            if (dealt > 0) {
                engine.recordProjectileImpact(this, row, blocker + 0.5);
                engine.recordEvent(sourceType + " projectile hit " + obstacleType.name()
                        + " for " + dealt + (destroyed ? "; it was destroyed." : "."));
            }
        }

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

        int fromCol = (int) Math.floor(oldX);
        int toCol = (int) Math.floor(effectiveNewX);
        int step = direction >= 0 ? 1 : -1;
        for (int c = fromCol; step > 0 ? c <= toCol : c >= toCol; c += step) {
            recordPath(startRow, c);
        }
        if (row != startRow) {
            recordPath(row, effectiveNewX);
        }

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
        } else {
            int landingColumn = Math.max(0, Math.min(
                    engine.getGameMap().getColumns() - 1, (int) Math.floor(landingX)));
            model.enums.ObstacleType obstacleType =
                    engine.getGameMap().getTile(row, landingColumn).getObstacle();
            int dealt = engine.damageObstacleAt(row, landingColumn, damage, effect.damageType());
            boolean destroyed = engine.getGameMap().getTile(row, landingColumn).getObstacle()
                    == model.enums.ObstacleType.NONE;
            if (dealt > 0) {
                engine.recordProjectileImpact(this, row, landingColumn + 0.5);
                engine.recordEvent(sourceType + " projectile hit " + obstacleType.name()
                        + " for " + dealt + (destroyed ? "; it was destroyed." : "."));
            }
        }
        active = false;
        recordPath(row, landingX);
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
            recordPath(row, target.getX());
        }
    }

    private void hit(Zombie zombie, GameEngine engine) {
        if (zombie == null || zombie.isDead() || hitZombieIds.contains(zombie.getId())) {
            return;
        }
        hitZombieIds.add(zombie.getId());
        engine.recordProjectileImpact(this, zombie.getRow(), zombie.getX());
        if (zombie.projectileDisposition(this, engine)
                == model.inGame.zombie.ZombieProjectileDisposition.BLOCK) {
            engine.recordEvent(zombie.getName() + " blocked a " + type + " projectile.");
            return;
        }
        effect.apply(this, zombie, engine);
        engine.recordEvent(sourceType + " projectile hit " + zombie.getName() + " for " + damage
                + effect.statusEffectSuffix() + ".");
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

    /** Smooth visual progress for lobbed/homing projectiles. */
    public double getFlightProgress() {
        if (type != ProjectileType.LOBBED && type != ProjectileType.HOMING) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0,
                1.0 - flightRemaining / Math.max(0.05, flightDuration)));
    }

    /** Model-derived x coordinate used by the renderer between discrete simulation ticks. */
    public double getVisualX() {
        if (type != ProjectileType.LOBBED && type != ProjectileType.HOMING) {
            return x;
        }
        double destination = target != null && !target.isDead() ? target.getX() : landingX;
        double progress = getFlightProgress();
        return spawnX + (destination - spawnX) * progress;
    }

    /** Fractional row coordinate for homing shots that can cross lanes. */
    public double getVisualRow() {
        if (type != ProjectileType.HOMING || target == null || target.isDead()) {
            return row;
        }
        double progress = getFlightProgress();
        return spawnRow + (target.getRow() - spawnRow) * progress;
    }

    /** 0..1 arc height used by lobbed projectiles. */
    public double getVisualArc() {
        if (type != ProjectileType.LOBBED) {
            return 0.0;
        }
        return Math.sin(Math.PI * getFlightProgress());
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

    /**
     * Optional presentation variant selected by the source plant. Gameplay
     * collision/damage never depends on this value; it only lets the renderer
     * choose the matching supplied projectile artwork (for example Bowling
     * Bulb's cyan/blue/orange bulbs).
     */
    public int getVisualVariant() {
        return visualVariant;
    }

    public void setVisualVariant(int visualVariant) {
        this.visualVariant = Math.max(0, visualVariant);
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
