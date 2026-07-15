package model.inGame.zombie;

import java.util.Objects;

public final class ZombieArmorPart {
    private final String name;
    private final int maxHealth;
    private final boolean magnetic;
    private int health;

    public ZombieArmorPart(String name, int maxHealth, boolean magnetic) {
        if (name == null || name.isBlank() || maxHealth <= 0) {
            throw new IllegalArgumentException("Armor requires a name and positive health.");
        }
        this.name = name.trim();
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.magnetic = magnetic;
    }

    public ZombieArmorPart copy() {
        ZombieArmorPart copy = new ZombieArmorPart(name, maxHealth, magnetic);
        copy.health = health;
        return copy;
    }

    public int absorb(int damage) {
        int absorbed = Math.min(Math.max(0, damage), health);
        health -= absorbed;
        return absorbed;
    }

    public void remove() {
        health = 0;
    }

    public String getName() {
        return name;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getHealth() {
        return health;
    }

    public boolean isMagnetic() {
        return magnetic;
    }

    public boolean isBroken() {
        return health <= 0;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ZombieArmorPart part
                && maxHealth == part.maxHealth && magnetic == part.magnetic
                && name.equals(part.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, maxHealth, magnetic);
    }
}
