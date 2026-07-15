package model.sim.zombie;

import model.sim.Damageable;

/**
 * A zombie on the board. Its horizontal position is continuous; its row is an
 * integer lane. It moves left, and when blocked it eats the plant ahead.
 */
public class ZombieInstance implements Damageable {

    /** What the zombie is currently doing. */
    public enum State {
        MOVING,
        EATING,
        DEAD
    }

    private final ZombieSpec spec;
    private double x;
    private final int row;
    private int hp;
    private State state;
    private boolean glowing;

    public ZombieInstance(ZombieSpec spec, double x, int row) {
        this.spec = spec;
        this.x = x;
        this.row = row;
        this.hp = spec.getHealth();
        this.state = State.MOVING;
    }

    public ZombieSpec getSpec() {
        return spec;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public int getRow() {
        return row;
    }

    @Override
    public int getTileX() {
        return (int) Math.floor(x);
    }

    @Override
    public int getTileY() {
        return row;
    }

    public int getHp() {
        return hp;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isBoss() {
        return spec.isBoss();
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    @Override
    public void takeDamage(int amount) {
        hp -= amount;

        if (hp <= 0) {
            hp = 0;
            state = State.DEAD;
        }
    }

    @Override
    public boolean isDead() {
        return hp <= 0;
    }

    /** Marks the zombie dead immediately (lawn mower / nuke). */
    public void kill() {
        hp = 0;
        state = State.DEAD;
    }
}
