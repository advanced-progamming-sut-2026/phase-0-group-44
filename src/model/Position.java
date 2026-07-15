package model;

import java.util.Objects;

public final class Position {
    private final int row;
    private final int column;

    public Position(int row, int column) {
        if (row < 0 || column < 0) {
            throw new IllegalArgumentException("Board coordinates cannot be negative.");
        }
        this.row = row;
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public Position offset(int rowDelta, int columnDelta) {
        return new Position(row + rowDelta, column + columnDelta);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Position position)) {
            return false;
        }
        return row == position.row && column == position.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }

    @Override
    public String toString() {
        return "(" + row + ", " + column + ")";
    }
}
