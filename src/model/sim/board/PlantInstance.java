package model.sim.board;

import model.enums.PlantType;
import model.sim.Damageable;

/**
 * A planted plant on the board. Carries its spec, position, current health, and
 * whether it is currently a support base or the plant stacked on one.
 */
public class PlantInstance implements Damageable {

    public static final int ICE_HEALTH = 600;

    private final PlantSpec spec;
    private int tileX;
    private int tileY;
    private int hp;
    private boolean stackedOnSupport;
    private int freezeLevel;
    private int iceHealth;
    private boolean frozen;
    private boolean octopused;
    private boolean transformed;
    private long transformedBy = -1L;

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

    public void setStackedOnSupport(boolean stackedOnSupport) {
        this.stackedOnSupport = stackedOnSupport;
    }

    public int getFreezeLevel() {
        return freezeLevel;
    }

    public int getIceHealth() {
        return iceHealth;
    }

    /** Icy wind and Hunter attacks share the same three-level freeze system. */
    public boolean addFreezeLevel() {
        if (freezeLevel < 3) {
            freezeLevel++;
        }
        if (freezeLevel >= 3) {
            frozen = true;
            if (iceHealth <= 0) {
                iceHealth = ICE_HEALTH;
            }
        }
        return frozen;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isOctopused() {
        return octopused;
    }

    public void setOctopused(boolean octopused) {
        this.octopused = octopused;
    }

    public boolean isTransformed() {
        return transformed;
    }

    public long getTransformedBy() {
        return transformedBy;
    }

    public void transform(long wizardId) {
        transformed = true;
        transformedBy = wizardId;
    }

    public void restoreFromTransformation() {
        transformed = false;
        transformedBy = -1L;
    }

    public void moveTo(int x, int y) {
        tileX = x;
        tileY = y;
    }

    public boolean isActive() {
        return !frozen && !octopused && !transformed && !isDead();
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
