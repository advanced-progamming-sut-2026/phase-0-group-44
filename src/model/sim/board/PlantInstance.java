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
    private final int tileX;
    private final int tileY;
    private int hp;
    private boolean stackedOnSupport;
    private int freezeLevel;
    private int iceHealth;
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
        return freezeLevel;
    }

    public int getFreezeLevel() {
        return freezeLevel;
    }

    public int getIceHealth() {
        return iceHealth;
    }

    public boolean addIceHit() {
        return addFreezeLevel();
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

    /** Fire removes the ice immediately; other damage reduces its 600 health. */
    public boolean damageIce(int amount, boolean fire) {
        if (!frozen) {
            return false;
        }
        if (fire) {
            clearFrozen();
            return true;
        }
        iceHealth -= Math.max(0, amount);
        if (iceHealth <= 0) {
            clearFrozen();
            return true;
        }
        return false;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void clearFrozen() {
        frozen = false;
        freezeLevel = 0;
        iceHealth = 0;
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
