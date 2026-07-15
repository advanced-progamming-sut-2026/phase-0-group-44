package model.inGame.zombie;

import model.Position;
import model.enums.DamageType;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Zombie {
    private static final AtomicLong IDS = new AtomicLong();

    private final long id = IDS.incrementAndGet();
    private String name;
    private int row;
    private double x;
    private int maxHealth;
    private int health;
    private int armor;
    private boolean metalArmor;
    private boolean hypnotized;
    private double frozenSeconds;
    private double slowedSeconds;
    private double poisonSeconds;
    private double poisonAccumulator;
    private int poisonDamagePerSecond;

    public Zombie() {
        this("Zombie", 0, 8.0, 200, 0, false);
    }

    public Zombie(String name, int row, double x, int health) {
        this(name, row, x, health, 0, false);
    }

    public Zombie(String name, int row, double x, int health, int armor, boolean metalArmor) {
        if (row < 0 || x < 0 || health <= 0 || armor < 0) {
            throw new IllegalArgumentException("Invalid zombie statistics or position.");
        }
        this.name = name == null ? "Zombie" : name;
        this.row = row;
        this.x = x;
        this.maxHealth = health;
        this.health = health;
        this.armor = armor;
        this.metalArmor = metalArmor && armor > 0;
    }

    public void tick(double deltaSeconds) {
        if (deltaSeconds <= 0 || isDead()) {
            return;
        }
        frozenSeconds = Math.max(0.0, frozenSeconds - deltaSeconds);
        slowedSeconds = Math.max(0.0, slowedSeconds - deltaSeconds);
        if (poisonSeconds > 0.0) {
            poisonSeconds = Math.max(0.0, poisonSeconds - deltaSeconds);
            poisonAccumulator += deltaSeconds;
            while (poisonAccumulator >= 1.0 && !isDead()) {
                poisonAccumulator -= 1.0;
                takeDamage(poisonDamagePerSecond, DamageType.POISON);
            }
        }
    }

    public int takeDamage(int amount, DamageType damageType) {
        if (amount <= 0 || isDead()) {
            return 0;
        }
        DamageType type = damageType == null ? DamageType.NORMAL : damageType;
        if (type == DamageType.FIRE) {
            thaw();
        }
        int remaining = amount;
        int dealt = 0;
        if (type != DamageType.POISON && type != DamageType.TRUE && armor > 0) {
            int absorbed = Math.min(armor, remaining);
            armor -= absorbed;
            remaining -= absorbed;
            dealt += absorbed;
            if (armor == 0) {
                metalArmor = false;
            }
        }
        if (remaining > 0) {
            int healthDamage = Math.min(health, remaining);
            health -= healthDamage;
            dealt += healthDamage;
        }
        return dealt;
    }

    public void applyFreeze(double seconds) {
        frozenSeconds = Math.max(frozenSeconds, Math.max(0.0, seconds));
    }

    public void applySlow(double seconds) {
        if (frozenSeconds <= 0.0) {
            slowedSeconds = Math.max(slowedSeconds, Math.max(0.0, seconds));
        }
    }

    public void applyPoison(int damagePerSecond, double seconds) {
        poisonDamagePerSecond = Math.max(poisonDamagePerSecond, Math.max(0, damagePerSecond));
        poisonSeconds = Math.max(poisonSeconds, Math.max(0.0, seconds));
    }

    public void thaw() {
        frozenSeconds = 0.0;
        slowedSeconds = 0.0;
    }

    public boolean removeMetalArmor() {
        if (!metalArmor || armor <= 0) {
            return false;
        }
        armor = 0;
        metalArmor = false;
        return true;
    }

    public void hypnotize() {
        hypnotized = true;
    }

    public void moveToRow(int newRow) {
        if (newRow < 0) {
            throw new IllegalArgumentException("Zombie row cannot be negative.");
        }
        row = newRow;
    }

    public void moveBy(double deltaX) {
        if (!isFrozen()) {
            x = Math.max(0.0, x + deltaX * (isSlowed() ? 0.5 : 1.0));
        }
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getRow() {
        return row;
    }

    public double getX() {
        return x;
    }

    public int getColumn() {
        return (int) Math.floor(x);
    }

    public Position getPosition() {
        return new Position(row, Math.max(0, getColumn()));
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getHealth() {
        return health;
    }

    public int getArmor() {
        return armor;
    }

    public boolean hasMetalArmor() {
        return metalArmor && armor > 0;
    }

    public boolean isHypnotized() {
        return hypnotized;
    }

    public boolean isFrozen() {
        return frozenSeconds > 0.0;
    }

    public boolean isSlowed() {
        return slowedSeconds > 0.0;
    }

    public double getFrozenSeconds() {
        return frozenSeconds;
    }

    public double getSlowedSeconds() {
        return slowedSeconds;
    }

    public boolean isDead() {
        return health <= 0;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Zombie zombie && id == zombie.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
