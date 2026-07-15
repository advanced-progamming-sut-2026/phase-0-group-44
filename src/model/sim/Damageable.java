package model.sim;

/** Anything on the board that can be found by tile and take damage. */
public interface Damageable {

    int getTileX();

    int getTileY();

    void takeDamage(int amount);

    boolean isDead();
}
