package model.miniGame;

import model.sim.zombie.ZombieInstance;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** A moving, non-board-plant bowling entity owned entirely by the minigame strategy. */
public final class BowlingBall {
    private final int id;
    private final BowlingPlantType type;
    private final double speedTilesPerSecond;
    private final Set<ZombieInstance> contactedZombies =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private double x;
    private double y;
    private int verticalDirection;
    private int deflectionCount;
    private boolean active = true;

    public BowlingBall(
            int id,
            BowlingPlantType type,
            double x,
            double y,
            double speedTilesPerSecond
    ) {
        if (id <= 0 || type == null || x < 0 || y < 0 || speedTilesPerSecond <= 0) {
            throw new IllegalArgumentException("Invalid bowling ball.");
        }
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.speedTilesPerSecond = speedTilesPerSecond;
    }

    public int getId() {
        return id;
    }

    public BowlingPlantType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getSpeedTilesPerSecond() {
        return speedTilesPerSecond;
    }

    public int getVerticalDirection() {
        return verticalDirection;
    }

    public int getDeflectionCount() {
        return deflectionCount;
    }

    public double getHeadingDegrees() {
        if (verticalDirection > 0) {
            return 45.0;
        }
        if (verticalDirection < 0) {
            return -45.0;
        }
        return 0.0;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }

    public boolean hasContacted(ZombieInstance zombie) {
        return contactedZombies.contains(zombie);
    }

    public boolean markContact(ZombieInstance zombie) {
        return contactedZombies.add(zombie);
    }

    /**
     * First contact changes the straight heading by 45 degrees. Each later
     * contact reverses the diagonal vertical component, a 90-degree change.
     */
    public void deflectAfterContact(int rows) {
        if (verticalDirection == 0) {
            double center = (rows - 1) / 2.0;
            verticalDirection = y <= center ? 1 : -1;
        } else {
            verticalDirection *= -1;
        }
        deflectionCount++;
    }

    /** Top/bottom contact follows the same later-contact 90-degree behavior. */
    public void deflectAtBoundary(boolean topBoundary) {
        verticalDirection = topBoundary ? 1 : -1;
        deflectionCount++;
    }
}
