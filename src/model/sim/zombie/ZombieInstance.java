package model.sim.zombie;

import model.enums.DamageType;
import model.enums.ZombieType;
import model.inGame.zombie.ZombieArmorPart;
import model.inGame.zombie.ZombieDefinition;
import model.inGame.zombie.ZombieEffectState;
import model.inGame.zombie.ZombieEffectType;
import model.sim.Damageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A continuous-position zombie used by the command-driven simulation. */
public class ZombieInstance implements Damageable {

    public enum State {
        MOVING,
        EATING,
        DEAD
    }

    private final ZombieSpec spec;
    private final List<ZombieArmorPart> armorParts;
    private final Map<ZombieEffectType, ZombieEffectState> effects =
            new EnumMap<>(ZombieEffectType.class);
    private final Map<String, Object> runtimeState = new HashMap<>();
    private double x;
    private int row;
    private int hp;
    private State state;
    private boolean glowing;
    private int direction = -1;
    private boolean encasedInIce;
    private int encasingIceHealth;

    public ZombieInstance(ZombieSpec spec, double x, int row) {
        this.spec = spec;
        this.x = x;
        this.row = row;
        this.hp = spec.getHealth();
        this.state = State.MOVING;
        ZombieDefinition definition = spec.getDefinition();
        this.armorParts = definition == null
                ? new ArrayList<>() : definition.createArmorParts();
        if (getType() == ZombieType.ALL_STAR) {
            runtimeState.put("CHARGING", true);
        }
        if (getType() == ZombieType.EXPLORER) {
            runtimeState.put("TORCH_LIT", true);
        }
        if (getType() == ZombieType.PROSPECTOR) {
            runtimeState.put("DYNAMITE_LIT", true);
        }
    }

    public ZombieSpec getSpec() {
        return spec;
    }

    public ZombieDefinition getDefinition() {
        return spec.getDefinition();
    }

    public ZombieType getType() {
        return spec.getType();
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

    public void setRow(int row) {
        this.row = row;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction >= 0 ? 1 : -1;
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

    public int getArmorHealth() {
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
        takeDamage(amount, DamageType.NORMAL);
    }

    public int takeDamage(int amount, DamageType type) {
        if (amount <= 0 || isDead()) {
            return 0;
        }
        if (type == DamageType.FIRE && getType() == ZombieType.DRAGON_IMP) {
            return 0;
        }
        int remaining = amount;
        int dealt = 0;
        if (encasedInIce) {
            if (type == DamageType.FIRE) {
                encasedInIce = false;
                encasingIceHealth = 0;
            } else {
                int absorbed = Math.min(encasingIceHealth, remaining);
                encasingIceHealth -= absorbed;
                remaining -= absorbed;
                dealt += absorbed;
                if (encasingIceHealth <= 0) {
                    encasedInIce = false;
                    encasingIceHealth = 0;
                }
                if (remaining <= 0) {
                    return dealt;
                }
            }
        }
        if (type != DamageType.POISON && type != DamageType.TRUE) {
            for (ZombieArmorPart part : armorParts) {
                boolean wasIntact = !part.isBroken();
                int absorbed = part.absorb(remaining);
                remaining -= absorbed;
                dealt += absorbed;
                if (wasIntact && part.isBroken()) {
                    if ("newspaper".equalsIgnoreCase(part.getName())) {
                        runtimeState.put("ENRAGED", true);
                    } else if ("barrel".equalsIgnoreCase(part.getName())) {
                        runtimeState.put("BARREL_BROKEN", true);
                    }
                }
                if (remaining <= 0) {
                    break;
                }
            }
        }
        int baseDamage = Math.min(hp, remaining);
        hp -= baseDamage;
        dealt += baseDamage;
        if (hp <= 0) {
            hp = 0;
            state = State.DEAD;
        }
        return dealt;
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

    public void applyEffect(ZombieEffectType type, double seconds, int magnitude) {
        if ((type == ZombieEffectType.CHILLED || type == ZombieEffectType.FROZEN)
                && getDefinition() != null && getDefinition().isFrostbiteNative()) {
            return;
        }
        ZombieEffectState effect = effects.get(type);
        if (effect == null) {
            effects.put(type, new ZombieEffectState(type, seconds, magnitude));
        } else {
            effect.refresh(seconds, magnitude);
        }
    }

    public void tickEffects(double deltaSeconds) {
        ZombieEffectState poison = effects.get(ZombieEffectType.POISONED);
        if (poison != null) {
            int ticks = poison.advanceAndCountWholeSeconds(deltaSeconds);
            for (int i = 0; i < ticks; i++) {
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

    public boolean isFrozen() {
        return encasedInIce || effects.containsKey(ZombieEffectType.FROZEN);
    }

    public void freezeInIce() {
        encasedInIce = true;
        encasingIceHealth = 600;
    }

    public boolean isEncasedInIce() {
        return encasedInIce;
    }

    public int getEncasingIceHealth() {
        return encasingIceHealth;
    }

    public void damageEncasingIce(int amount, boolean fire) {
        if (!encasedInIce) {
            return;
        }
        if (fire) {
            encasedInIce = false;
            encasingIceHealth = 0;
            return;
        }
        encasingIceHealth -= Math.max(0, amount);
        if (encasingIceHealth <= 0) {
            encasedInIce = false;
            encasingIceHealth = 0;
        }
    }

    public boolean isSlowed() {
        return effects.containsKey(ZombieEffectType.CHILLED);
    }

    public Map<ZombieEffectType, Double> getActiveEffects() {
        Map<ZombieEffectType, Double> result = new EnumMap<>(ZombieEffectType.class);
        for (Map.Entry<ZombieEffectType, ZombieEffectState> entry : effects.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getRemainingSeconds());
        }
        return Collections.unmodifiableMap(result);
    }

    public boolean getBooleanState(String key) {
        return Boolean.TRUE.equals(runtimeState.get(key));
    }

    public boolean hasState(String key) {
        return runtimeState.containsKey(key);
    }

    public void onIceHit() {
        if (getType() == ZombieType.PROSPECTOR && !getBooleanState("REVERSED")) {
            runtimeState.put("DYNAMITE_LIT", false);
        }
        if (getType() == ZombieType.EXPLORER) {
            runtimeState.put("TORCH_LIT", false);
        }
    }

    public void onFireHit() {
        if (getType() == ZombieType.EXPLORER) {
            runtimeState.put("TORCH_LIT", true);
        }
    }

    public void putState(String key, Object value) {
        if (value == null) {
            runtimeState.remove(key);
        } else {
            runtimeState.put(key, value);
        }
    }

    public int getIntState(String key, int defaultValue) {
        Object value = runtimeState.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    public double getDoubleState(String key, double defaultValue) {
        Object value = runtimeState.get(key);
        return value instanceof Number number ? number.doubleValue() : defaultValue;
    }

    public double effectiveSpeed() {
        double speed = spec.getSpeedTilesPerSecond();
        if (getType() == ZombieType.IMP || getType() == ZombieType.DRAGON_IMP) {
            speed *= 1.5;
        }
        if (getBooleanState("ENRAGED")) {
            speed *= 2.5;
        }
        if (getBooleanState("CHARGING")) {
            speed *= 5.0;
        } else if (getBooleanState("POST_CHARGE")) {
            speed *= 0.35;
        }
        if (getType() == ZombieType.GARGANTUAR) {
            speed *= 0.35;
        }
        if (isSlowed()) {
            speed *= 0.5;
        }
        return isFrozen() ? 0.0 : speed;
    }

    public int effectiveEatDamage() {
        double damage = spec.getEatDamagePerSecond();
        if (getType() == ZombieType.IMP || getType() == ZombieType.DRAGON_IMP) {
            damage *= 1.5;
        }
        if (getBooleanState("ENRAGED")) {
            damage *= 2.5;
        }
        return (int) Math.round(damage);
    }

    @Override
    public boolean isDead() {
        return hp <= 0;
    }

    public void kill() {
        hp = 0;
        state = State.DEAD;
    }

    public String infoText() {
        StringBuilder builder = new StringBuilder();
        builder.append(spec.getName()).append(':').append('\n')
                .append("type: ").append(getType()).append('\n')
                .append("position: ")
                .append(String.format(Locale.ROOT, "%.2f", x)).append(", ").append(row).append('\n')
                .append("health: ").append(hp).append('/').append(spec.getHealth()).append('\n')
                .append("armor:").append('\n');
        for (ZombieArmorPart part : armorParts) {
            if (!part.isBroken()) {
                builder.append(part.getName()).append(": ").append(part.getHealth()).append('\n');
            }
        }
        builder.append("effects:").append('\n');
        for (Map.Entry<ZombieEffectType, Double> effect : getActiveEffects().entrySet()) {
            builder.append(effect.getKey().name().toLowerCase(Locale.ROOT)).append(": ")
                    .append(String.format(Locale.ROOT, "%.1fs", effect.getValue())).append('\n');
        }
        return builder.toString().stripTrailing();
    }
}
