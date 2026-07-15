package model.level;

import model.enums.ZombieType;

/** One chapter zombie that starts encased in 600-health ice. */
public final class FrozenZombiePlacement {
    private final ZombieType type;
    private final double x;
    private final int row;

    public FrozenZombiePlacement(ZombieType type, double x, int row) {
        if (type == null || x < 0.0 || row < 0) {
            throw new IllegalArgumentException("Invalid frozen-zombie placement.");
        }
        this.type = type;
        this.x = x;
        this.row = row;
    }

    public ZombieType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public int getRow() {
        return row;
    }
}
