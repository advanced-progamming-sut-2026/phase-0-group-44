package model.inGame.plant;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PlantStats {
    private int hp;
    private int cost;
    private int damage;
    private double actionInterval;
    private double recharge;
    private final Map<String, Double> special = new LinkedHashMap<>();

    private PlantStats(PlantDefinition definition) {
        hp = definition.getBaseHp();
        cost = definition.getCost();
        damage = definition.getDamageProfile().getPrimaryDamage();
        actionInterval = definition.getActionInterval();
        recharge = definition.getRecharge();
    }

    public static PlantStats resolve(PlantDefinition definition, int level) {
        if (level < 1 || level > PlantDefinition.MAX_LEVEL) {
            throw new IllegalArgumentException("Plant level must be between 1 and " + PlantDefinition.MAX_LEVEL + ".");
        }
        PlantStats stats = new PlantStats(definition);
        for (PlantUpgrade upgrade : definition.getUpgrades()) {
            if (upgrade.getLevel() <= level) {
                stats.apply(upgrade.getEffect());
            }
        }
        stats.hp = Math.max(0, stats.hp);
        stats.cost = Math.max(0, stats.cost);
        stats.damage = Math.max(0, stats.damage);
        stats.actionInterval = Math.max(0.05, stats.actionInterval);
        stats.recharge = Math.max(0.0, stats.recharge);
        return stats;
    }


    static PlantStats imitate(PlantDefinition copiedDefinition, PlantDefinition imitaterDefinition, int level) {
        PlantStats stats = new PlantStats(copiedDefinition);
        for (PlantUpgrade upgrade : imitaterDefinition.getUpgrades()) {
            if (upgrade.getLevel() <= level) {
                stats.apply(upgrade.getEffect());
            }
        }
        stats.hp = Math.max(0, stats.hp);
        stats.cost = Math.max(0, stats.cost);
        stats.damage = Math.max(0, stats.damage);
        stats.actionInterval = Math.max(0.05, stats.actionInterval);
        stats.recharge = Math.max(0.0, stats.recharge);
        return stats;
    }

    private void apply(UpgradeEffect effect) {
        switch (effect.key()) {
            case "HP" -> hp += (int) Math.round(effect.amount());
            case "COST" -> cost += (int) Math.round(effect.amount());
            case "DAMAGE" -> damage += (int) Math.round(effect.amount());
            case "ACTION_INTERVAL" -> {
                if (effect.percentage()) {
                    actionInterval *= 1.0 + effect.amount();
                } else {
                    actionInterval += effect.amount();
                }
            }
            case "RECHARGE" -> recharge += effect.amount();
            default -> special.merge(effect.key(), effect.amount(), Double::sum);
        }
    }

    public PlantStats withSpecial(String key, double value) {
        special.putIfAbsent(key, value);
        return this;
    }

    public int getHp() {
        return hp;
    }

    public int getCost() {
        return cost;
    }

    public int getDamage() {
        return damage;
    }

    public double getActionInterval() {
        return actionInterval;
    }

    public double getRecharge() {
        return recharge;
    }

    public double getSpecial(String key, double defaultValue) {
        return special.getOrDefault(key, defaultValue);
    }

    public boolean hasFlag(String key) {
        return special.getOrDefault(key, 0.0) > 0.0;
    }

    public Map<String, Double> getSpecialValues() {
        return Collections.unmodifiableMap(special);
    }
}
