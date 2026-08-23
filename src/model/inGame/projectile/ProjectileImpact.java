package model.inGame.projectile;

import model.enums.DamageType;
import model.enums.PlantType;

import java.util.concurrent.atomic.AtomicLong;

/** Short-lived model marker used only to render projectile collision feedback. */
public final class ProjectileImpact {
    private static final AtomicLong ID_COUNTER = new AtomicLong();
    public static final double DEFAULT_LIFETIME_SECONDS = 0.32;

    private final long id = ID_COUNTER.getAndIncrement();
    private final PlantType sourceType;
    private final ProjectileType projectileType;
    private final DamageType damageType;
    private final boolean butter;
    private final boolean secondary;
    private final int row;
    private final double x;
    private final double lifetimeSeconds;
    private double remainingSeconds;

    public ProjectileImpact(
            PlantType sourceType,
            ProjectileType projectileType,
            DamageType damageType,
            boolean butter,
            int row,
            double x
    ) {
        this(sourceType, projectileType, damageType, butter, false, row, x);
    }

    public ProjectileImpact(
            PlantType sourceType,
            ProjectileType projectileType,
            DamageType damageType,
            boolean butter,
            boolean secondary,
            int row,
            double x
    ) {
        this.sourceType = sourceType;
        this.projectileType = projectileType;
        this.damageType = damageType == null ? DamageType.NORMAL : damageType;
        this.butter = butter;
        this.secondary = secondary;
        this.row = row;
        this.x = x;
        this.lifetimeSeconds = DEFAULT_LIFETIME_SECONDS;
        this.remainingSeconds = lifetimeSeconds;
    }

    public void tick(double deltaSeconds) {
        remainingSeconds = Math.max(0.0, remainingSeconds - Math.max(0.0, deltaSeconds));
    }

    public boolean isExpired() {
        return remainingSeconds <= 0.0;
    }

    public long getId() {
        return id;
    }

    public PlantType getSourceType() {
        return sourceType;
    }

    public ProjectileType getProjectileType() {
        return projectileType;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public boolean isButter() {
        return butter;
    }

    /** True when this marker belongs to splash/AoE damage rather than the primary hit. */
    public boolean isSecondary() {
        return secondary;
    }

    public int getRow() {
        return row;
    }

    public double getX() {
        return x;
    }

    public double getLifetimeSeconds() {
        return lifetimeSeconds;
    }

    public double getRemainingSeconds() {
        return remainingSeconds;
    }
}
