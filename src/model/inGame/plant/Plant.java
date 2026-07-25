package model.inGame.plant;

import model.GameEngine;
import model.Position;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.enums.PlantType;
import model.inGame.zombie.Zombie;
import model.sim.Damageable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Plant implements Damageable {
    private final PlantDefinition definition;
    private final PlantDefinition effectiveDefinition;
    private final int level;
    private final PlantStats stats;
    private final PlantBehavior behavior;
    private int hp;
    private int armor;
    private Position position;
    private double ageSeconds;
    private boolean expired;
    private boolean deathHandled;
    private final Map<String, Double> timers = new HashMap<>();
    private final Map<String, Object> state = new HashMap<>();

    public Plant(PlantDefinition definition, PlantDefinition effectiveDefinition,
                 int level, PlantStats stats, PlantBehavior behavior) {
        if (definition == null || effectiveDefinition == null || stats == null || behavior == null) {
            throw new IllegalArgumentException("Plant runtime dependencies cannot be null.");
        }
        this.definition = definition;
        this.effectiveDefinition = effectiveDefinition;
        this.level = level;
        this.stats = stats;
        this.behavior = behavior;
        this.hp = stats.getHp();
    }

    /** Compatibility constructor retained for older code. */
    public Plant(PlantType type, int hp, int rechargeTime, int actionTime, int cost,
                 model.enums.PlantCategory category, SunProduceBehavior sunProduceBehavior,
                 AttackBehavior attackBehavior, SpecialAbility specialAbility) {
        this(PlantRegistry.getDefault().require(type),
                PlantRegistry.getDefault().require(type),
                1,
                PlantRegistry.getDefault().require(type).statsAtLevel(1),
                new PlantBehavior() {
                    @Override
                    public void tick(Plant plant, GameEngine engine, double deltaSeconds) {
                        if (sunProduceBehavior != null) {
                            sunProduceBehavior.produce(plant, engine);
                        }
                        if (attackBehavior != null) {
                            attackBehavior.attack(plant, engine);
                        }
                    }

                    @Override
                    public void onPlantFood(Plant plant, GameEngine engine) {
                        if (specialAbility != null) {
                            specialAbility.activate(plant, engine);
                        }
                    }
                });
        this.hp = Math.max(0, hp);
    }

    public void onPlant(GameEngine engine) {
        behavior.onPlant(this, engine);
    }

    public void tick(GameEngine engine, double deltaSeconds) {
        if (isDead() || deltaSeconds <= 0) {
            return;
        }
        ageSeconds += deltaSeconds;
        if (getBooleanState("FROZEN") || getBooleanState("OCTOPUSED")
                || getBooleanState("TRANSFORMED")) {
            return;
        }
        behavior.tick(this, engine, deltaSeconds);
        if (isDead() && !deathHandled) {
            handleDeath(engine);
        }
    }

    /** One-second compatibility tick. */
    public void act(GameEngine engine) {
        tick(engine, 1.0);
    }

    public int receiveDamage(int amount, GameEngine engine, Zombie attacker) {
        if (amount <= 0 || isDead()) {
            return 0;
        }
        int remaining = amount;
        int oldArmor = armor;
        int absorbed = Math.min(armor, remaining);
        armor -= absorbed;
        remaining -= absorbed;
        int hpDamage = Math.min(hp, remaining);
        hp -= hpDamage;
        int total = absorbed + hpDamage;
        if (oldArmor > 0 && armor == 0) {
            behavior.onArmorBroken(this, engine);
        }
        behavior.onDamaged(this, engine, attacker, total);
        if (isDead() && !deathHandled) {
            handleDeath(engine);
        }
        return total;
    }

    public void usePlantFood(GameEngine engine) {
        if (!isDead()) {
            behavior.onPlantFood(this, engine);
        }
    }

    public void expire(GameEngine engine) {
        expired = true;
        hp = 0;
        if (!deathHandled) {
            handleDeath(engine);
        }
    }

    private void handleDeath(GameEngine engine) {
        deathHandled = true;
        behavior.onDeath(this, engine);
    }

    public boolean advanceTimer(String key, double deltaSeconds, double interval) {
        if (interval <= 0) {
            return true;
        }
        double current = timers.getOrDefault(key, 0.0) + deltaSeconds;
        if (current + 1e-9 >= interval) {
            timers.put(key, current - interval);
            return true;
        }
        timers.put(key, current);
        return false;
    }

    public void resetTimer(String key) {
        timers.put(key, 0.0);
    }

    public double getTimer(String key) {
        return timers.getOrDefault(key, 0.0);
    }

    public void putState(String key, Object value) {
        if (value == null) {
            state.remove(key);
        } else {
            state.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getState(String key, Class<T> type, T defaultValue) {
        Object value = state.get(key);
        return type.isInstance(value) ? (T) value : defaultValue;
    }

    public boolean getBooleanState(String key) {
        return Boolean.TRUE.equals(state.get(key));
    }

    public void addArmor(int amount) {
        armor = Math.max(0, armor + amount);
    }

    public void healToFull() {
        hp = stats.getHp();
    }

    public PlantDefinition getDefinition() {
        return definition;
    }

    public PlantDefinition getEffectiveDefinition() {
        return effectiveDefinition;
    }

    public PlantType getType() {
        return definition.getType();
    }

    public PlantType getEffectiveType() {
        return effectiveDefinition.getType();
    }

    public int getLevel() {
        return level;
    }

    public PlantStats getStats() {
        return stats;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return stats.getHp();
    }

    public int getArmor() {
        return armor;
    }

    public int getRechargeTime() {
        return (int) Math.round(stats.getRecharge());
    }

    public int getActionTime() {
        return (int) Math.round(stats.getActionInterval());
    }

    public int getCost() {
        return stats.getCost();
    }

    public PlantCategory getCategory() {
        return effectiveDefinition.getCategory();
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Set<PlantTag> getTags() {
        return Collections.unmodifiableSet(effectiveDefinition.getTags());
    }

    public double getAgeSeconds() {
        return ageSeconds;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean blocksJumping() {
        return getEffectiveType() == PlantType.TALL_NUT && !isDead();
    }

    public boolean isDead() {
        return expired || hp <= 0 && !getBooleanState("ACTIVE_ZERO_HP");
    }

    @Override
    public int getTileX() {
        return position == null ? -1 : position.getColumn();
    }

    @Override
    public int getTileY() {
        return position == null ? -1 : position.getRow();
    }

    @Override
    public void takeDamage(int amount) {
        // این متد از قبل وجود داره؛ فقط باید override اضافه بشه
        if (amount <= 0 || isDead()) return;
        hp = Math.max(0, hp - amount);
    }
}
