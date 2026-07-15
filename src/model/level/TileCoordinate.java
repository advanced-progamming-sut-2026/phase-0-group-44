package model.level;

import java.util.Objects;

/** Immutable x/y board coordinate used by data-driven level configuration. */
public final class TileCoordinate {
    private final int x;
    private final int y;

    public TileCoordinate(int x, int y) {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Tile coordinates cannot be negative.");
        }
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TileCoordinate coordinate)) {
            return false;
        }
        return x == coordinate.x && y == coordinate.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
