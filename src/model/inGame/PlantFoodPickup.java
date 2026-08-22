package model.inGame;

/**
 * A collectible Plant Food reward dropped onto the lawn by a glowing zombie.
 *
 * <p>The model owns the fall/lifetime and remembers the zombie's continuous
 * death position so the view can make the leaf visibly leave the zombie's body
 * before it lands on a tile.</p>
 */
public final class PlantFoodPickup {
    public static final double FALL_DURATION_SECONDS = 1.45;
    public static final double UNCOLLECTED_LIFETIME_SECONDS = 7.0;

    private final int tileX;
    private final int tileY;
    private final double sourceX;
    private final double totalFallSeconds;
    private double remainingFallSeconds;
    private double remainingVisibleSeconds = UNCOLLECTED_LIFETIME_SECONDS;

    public PlantFoodPickup(int tileX, int tileY) {
        this(tileX, tileY, tileX + 0.5, FALL_DURATION_SECONDS);
    }

    public PlantFoodPickup(int tileX, int tileY, double fallSeconds) {
        this(tileX, tileY, tileX + 0.5, fallSeconds);
    }

    public PlantFoodPickup(int tileX, int tileY, double sourceX, double fallSeconds) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.sourceX = sourceX;
        this.totalFallSeconds = Math.max(0.0, fallSeconds);
        this.remainingFallSeconds = this.totalFallSeconds;
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    /** Continuous board-space x coordinate where the glowing zombie died. */
    public double getSourceX() {
        return sourceX;
    }

    public boolean isAt(int x, int y) {
        return tileX == x && tileY == y;
    }

    public boolean isFalling() {
        return remainingFallSeconds > 1e-9;
    }

    public double getFallDurationSeconds() {
        return totalFallSeconds;
    }

    public double getFallProgress() {
        if (!isFalling() || totalFallSeconds <= 1e-9) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0,
                1.0 - remainingFallSeconds / totalFallSeconds));
    }

    public double getRemainingVisibleSeconds() {
        return remainingVisibleSeconds;
    }

    public void tick(double deltaSeconds) {
        double delta = Math.max(0.0, deltaSeconds);
        if (isFalling()) {
            remainingFallSeconds = Math.max(0.0, remainingFallSeconds - delta);
            if (!isFalling()) {
                remainingVisibleSeconds = UNCOLLECTED_LIFETIME_SECONDS;
            }
            return;
        }
        remainingVisibleSeconds -= delta;
    }

    public boolean isExpired() {
        return !isFalling() && remainingVisibleSeconds <= 1e-9;
    }
}
