package model.miniGame;

/** One row objective in I, Zombie. */
public final class Brain {
    private final int row;
    private boolean eaten;

    public Brain(int row) {
        if (row < 0) {
            throw new IllegalArgumentException("Brain row cannot be negative.");
        }
        this.row = row;
    }

    public int getRow() { return row; }
    public boolean isEaten() { return eaten; }

    public boolean eat() {
        if (eaten) {
            return false;
        }
        eaten = true;
        return true;
    }
}
