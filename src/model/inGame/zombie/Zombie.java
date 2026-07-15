package model.inGame.zombie;

import model.GameEngine;
import model.Position;
import model.enums.DamageType;
import model.enums.ZombieType;
import model.inGame.projectile.Projectile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Zombie {
    private static final AtomicLong IDS = new AtomicLong();

    private final long id = IDS.incrementAndGet();
    private final ZombieDefinition definition;
    private final CompositeZombieBehavior behavior;
    private final List<ZombieArmorPart> armorParts;
    private final Map<ZombieEffectType, ZombieEffectState> effects =
            new EnumMap<>(ZombieEffectType.class);
    private final Map<String, Object> state = new HashMap<>();
    private final Map<String, Double> timers = new HashMap<>();

    private String name;
    private int row;
    private double x;
    private int maxHealth;
    private int health;
    private int direction = -1;
    private double runtimeSpeedMultiplier = 1.0;
    private boolean deathHandled;

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
        this.definition = null;
        this.name = name == null ? "Zombie" : name;
        this.row = row;
        this.x = x;
        this.maxHealth = health;
        this.health = health;
        this.armorParts = new ArrayList<>();
        if (armor > 0) {
            this.armorParts.add(new ZombieArmorPart("armor", armor, metalArmor));
        }
        // Legacy ad-hoc zombies are passive combat targets. Canonical zombies
        // created by ZombieFactory receive movement and attack components.
        this.behavior = new CompositeZombieBehavior(null, null, List.of());
    }

    public Zombie(
            ZombieDefinition definition,
            int row,
            double x,
            CompositeZombieBehavior behavior
    ) {
        if (definition == null || behavior == null || row < 0 || x < 0) {
            throw new IllegalArgumentException("Invalid canonical zombie runtime.");
        }
        this.definition = definition;
        this.behavior = behavior;
        this.name = definition.getName();
        this.row = row;
        this.x = x;
        this.maxHealth = definition.getHealth();
        this.health = definition.getHealth();
        this.armorParts = definition.createArmorParts();
        if (definition.getType() == ZombieType.ALL_STAR) {
            state.put("CHARGING", true);
            runtimeSpeedMultiplier = 5.0;
        }
        if (definition.getType() == ZombieType.EXPLORER) {
            state.put("TORCH_LIT", true);
        }
        if (definition.getType() == ZombieType.PROSPECTOR) {
            state.put("DYNAMITE_LIT", true);
        }
    }

    public void tick(double deltaSeconds) {
        advanceEffects(deltaSeconds);
    }

    public void tick(GameEngine engine, double deltaSeconds) {
        if (deltaSeconds <= 0.0 || isDead()) {
            return;
        }
        advanceEffects(deltaSeconds);
        if (!isDead()) {
            behavior.tick(this, engine, deltaSeconds);
        }
    }

    private void advanceEffects(double deltaSeconds) {
        if (deltaSeconds <= 0.0 || isDead()) {
            return;
        }
        ZombieEffectState poison = effects.get(ZombieEffectType.POISONED);
        if (poison != null) {
            int ticks = poison.advanceAndCountWholeSeconds(deltaSeconds);
            for (int i = 0; i < ticks && !isDead(); i++) {
                takeDamage(poison.getMagnitude(), DamageType.POISON);
            }
        }
        for (ZombieEffectState effect : new ArrayList<>(effects.values())) {
            if (effect != poison) {
                effect.advanceAndCountWholeSeconds(deltaSeconds);
            }
            if (effect.isExpired()) {
                effects.remove(effect.getType());
            }
        }
    }

    public int receiveDamage(int amount, DamageType damageType, GameEngine engine) {
        if (amount <= 0 || isDead()) {
            return 0;
        }
        List<String> armorBefore = activeArmorNames();
        int dealt = takeDamage(amount, damageType);
        for (String nameBefore : armorBefore) {
            ZombieArmorPart part = findArmorPart(nameBefore);
            if (part != null && part.isBroken()) {
                behavior.onArmorBroken(this, engine, nameBefore);
            }
        }
        behavior.onDamaged(this, engine, dealt,
                damageType == null ? DamageType.NORMAL : damageType);
        return dealt;
    }

    public int takeDamage(int amount, DamageType damageType) {
        if (amount <= 0 || isDead()) {
            return 0;
        }
        DamageType type = damageType == null ? DamageType.NORMAL : damageType;
        if (type == DamageType.FIRE) {
            thaw();
            if (definition != null && definition.isFireImmune()) {
                return 0;
            }
        }
        int remaining = amount;
        int dealt = 0;
        if (type != DamageType.POISON && type != DamageType.TRUE) {
            for (ZombieArmorPart part : armorParts) {
                if (remaining <= 0) {
                    break;
                }
                int absorbed = part.absorb(remaining);
                remaining -= absorbed;
                dealt += absorbed;
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
        if (definition != null && definition.isFrostbiteNative()) {
            return;
        }
        applyEffect(ZombieEffectType.FROZEN, seconds, 0);
    }

    public void applySlow(double seconds) {
        if (definition != null && definition.isFrostbiteNative()) {
            return;
        }
        if (!isFrozen()) {
            applyEffect(ZombieEffectType.CHILLED, seconds, 0);
        }
    }

    public void applyStun(double seconds) {
        applyEffect(ZombieEffectType.STUNNED, seconds, 0);
    }

    public void applyPoison(int damagePerSecond, double seconds) {
        applyEffect(ZombieEffectType.POISONED, seconds, damagePerSecond);
    }

    private void applyEffect(ZombieEffectType type, double seconds, int magnitude) {
        ZombieEffectState existing = effects.get(type);
        if (existing == null) {
            effects.put(type, new ZombieEffectState(type, seconds, magnitude));
        } else {
            existing.refresh(seconds, magnitude);
        }
    }

    public void thaw() {
        effects.remove(ZombieEffectType.FROZEN);
        effects.remove(ZombieEffectType.CHILLED);
    }

    public boolean removeMetalArmor() {
        for (ZombieArmorPart part : armorParts) {
            if (part.isMagnetic() && !part.isBroken()) {
                part.remove();
                return true;
            }
        }
        return false;
    }

    public void hypnotize() {
        effects.put(ZombieEffectType.HYPNOTIZED,
                new ZombieEffectState(ZombieEffectType.HYPNOTIZED, Double.MAX_VALUE, 0));
    }

    public void moveToRow(int newRow) {
        if (newRow < 0) {
            throw new IllegalArgumentException("Zombie row cannot be negative.");
        }
        row = newRow;
    }

    public void moveBy(double deltaX) {
        if (!isFrozen() && !isStunned()) {
            x = Math.max(0.0, x + deltaX * (isSlowed() ? 0.5 : 1.0));
        }
    }

    public void onIceHit(GameEngine engine) {
        behavior.onIceHit(this, engine);
    }

    public void onFireHit(GameEngine engine) {
        behavior.onFireHit(this, engine);
    }

    public ZombieProjectileDisposition projectileDisposition(Projectile projectile, GameEngine engine) {
        return behavior.projectileDisposition(this, projectile, engine);
    }

    public void handleDeath(GameEngine engine) {
        if (!deathHandled) {
            deathHandled = true;
            behavior.onDeath(this, engine);
        }
    }

    public boolean advanceTimer(String key, double deltaSeconds, double interval) {
        return advanceTimerCount(key, deltaSeconds, interval) > 0;
    }

    public int advanceTimerCount(String key, double deltaSeconds, double interval) {
        if (key == null || key.isBlank() || deltaSeconds < 0.0 || interval <= 0.0) {
            throw new IllegalArgumentException("Timer key, duration, and interval must be valid.");
        }
        double elapsed = timers.getOrDefault(key, 0.0) + deltaSeconds;
        int activations = (int) Math.floor((elapsed + 1e-9) / interval);
        timers.put(key, Math.max(0.0, elapsed - activations * interval));
        return activations;
    }

    public void putState(String key, Object value) {
        if (value == null) {
            state.remove(key);
        } else {
            state.put(key, value);
        }
    }

    public boolean hasState(String key) {
        return state.containsKey(key);
    }

    public boolean getBooleanState(String key) {
        return Boolean.TRUE.equals(state.get(key));
    }

    public int getIntState(String key, int defaultValue) {
        Object value = state.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    public double getDoubleState(String key, double defaultValue) {
        Object value = state.get(key);
        return value instanceof Number number ? number.doubleValue() : defaultValue;
    }

    public void setRuntimeSpeedMultiplier(double multiplier) {
        runtimeSpeedMultiplier = Math.max(0.0, multiplier);
    }

    public long getId() {
        return id;
    }

    public ZombieDefinition getDefinition() {
        return definition;
    }

    public ZombieType getType() {
        return definition == null ? ZombieType.NORMAL : definition.getType();
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

    public void setX(double x) {
        this.x = Math.max(0.0, x);
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction >= 0 ? 1 : -1;
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
        int total = 0;
        for (ZombieArmorPart part : armorParts) {
            total += part.getHealth();
        }
        return total;
    }

    public List<ZombieArmorPart> getArmorParts() {
        List<ZombieArmorPart> result = new ArrayList<>();
        for (ZombieArmorPart part : armorParts) {
            result.add(part.copy());
        }
        return Collections.unmodifiableList(result);
    }

    public ZombieArmorPart findArmorPart(String armorName) {
        if (armorName == null) {
            return null;
        }
        for (ZombieArmorPart part : armorParts) {
            if (part.getName().equalsIgnoreCase(armorName)) {
                return part;
            }
        }
        return null;
    }

    private List<String> activeArmorNames() {
        List<String> result = new ArrayList<>();
        for (ZombieArmorPart part : armorParts) {
            if (!part.isBroken()) {
                result.add(part.getName());
            }
        }
        return result;
    }

    public boolean hasMetalArmor() {
        for (ZombieArmorPart part : armorParts) {
            if (part.isMagnetic() && !part.isBroken()) {
                return true;
            }
        }
        return false;
    }

    public boolean isHypnotized() {
        return effects.containsKey(ZombieEffectType.HYPNOTIZED);
    }

    public boolean isFrozen() {
        return effects.containsKey(ZombieEffectType.FROZEN);
    }

    public boolean isSlowed() {
        return effects.containsKey(ZombieEffectType.CHILLED);
    }

    public boolean isStunned() {
        return effects.containsKey(ZombieEffectType.STUNNED);
    }

    public double getFrozenSeconds() {
        return remaining(ZombieEffectType.FROZEN);
    }

    public double getSlowedSeconds() {
        return remaining(ZombieEffectType.CHILLED);
    }

    public Map<ZombieEffectType, Double> getActiveEffects() {
        Map<ZombieEffectType, Double> result = new EnumMap<>(ZombieEffectType.class);
        for (Map.Entry<ZombieEffectType, ZombieEffectState> entry : effects.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getRemainingSeconds());
        }
        return Collections.unmodifiableMap(result);
    }

    private double remaining(ZombieEffectType type) {
        ZombieEffectState state = effects.get(type);
        return state == null ? 0.0 : state.getRemainingSeconds();
    }

    public double getSpeedTilesPerSecond() {
        double base = definition == null ? 0.185 : definition.getSpeedTilesPerSecond();
        if (getBooleanState("ENRAGED")) {
            base *= 2.5;
        }
        return base * runtimeSpeedMultiplier;
    }

    public int getEatDamagePerSecond() {
        int base = definition == null ? 100 : definition.getEatDamagePerSecond();
        if (getBooleanState("ENRAGED")) {
            base = (int) Math.round(base * 2.5);
        }
        return base;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public String infoText() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append(':').append('\n')
                .append("position: ").append(String.format(java.util.Locale.ROOT, "%.2f", x))
                .append(", ").append(row).append('\n')
                .append("health: ").append(health).append('/').append(maxHealth).append('\n')
                .append("armor:").append('\n');
        for (ZombieArmorPart part : armorParts) {
            if (!part.isBroken()) {
                builder.append(part.getName()).append(": ").append(part.getHealth()).append('\n');
            }
        }
        builder.append("effects:").append('\n');
        for (Map.Entry<ZombieEffectType, Double> effect : getActiveEffects().entrySet()) {
            builder.append(effect.getKey().name().toLowerCase(java.util.Locale.ROOT))
                    .append(": ")
                    .append(String.format(java.util.Locale.ROOT, "%.1fs", effect.getValue()))
                    .append('\n');
        }
        return builder.toString().stripTrailing();
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
