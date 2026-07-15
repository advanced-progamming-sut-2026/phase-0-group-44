package model.sim.board;

import model.enums.PlantType;
import model.sim.Damageable;

/**
 * A planted plant on the board. Carries its spec, position, current health, and
 * whether it is currently a support base or the plant stacked on one.
 */
public class PlantInstance implements Damageable {

    private final PlantSpec spec;
    private final int tileX;
    private final int tileY;
    private int hp;
    private boolean stackedOnSupport;
    private int iceHitCount;
    private boolean frozen;
    private boolean octopused;

    public PlantInstance(PlantSpec spec, int tileX, int tileY) {
        this.spec = spec;
        this.tileX = tileX;
        this.tileY = tileY;
        this.hp = spec.getHp();
    }

    public PlantSpec getSpec() {
        return spec;
    }

    public PlantType getType() {
        return spec.getType();
    }

    @Override
    public int getTileX() {
        return tileX;
    }

    @Override
    public int getTileY() {
        return tileY;
    }

    public int getHp() {
        return hp;
    }

    public boolean isStackedOnSupport() {
        return stackedOnSupport;
    }

    public void setStackedOnSupport(boolean stackedOnSupport) {
        this.stackedOnSupport = stackedOnSupport;
    }


    public int getIceHitCount() {
        return iceHitCount;
    }

    public boolean addIceHit() {
        iceHitCount++;
        if (iceHitCount >= 3) {
            frozen = true;
        }
        return frozen;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void clearFrozen() {
        frozen = false;
        iceHitCount = 0;
    }

    public boolean isOctopused() {
        return octopused;
    }

    public void setOctopused(boolean octopused) {
        this.octopused = octopused;
    }

    public boolean isActive() {
        return !frozen && !octopused && !isDead();
    }

    @Override
    public void takeDamage(int amount) {
        hp -= amount;

        if (hp < 0) {
            hp = 0;
        }
    }

    @Override
    public boolean isDead() {
        return hp <= 0;
    }
}
