package model.inGame.zombie;

import model.enums.ZombieType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZombieDefinition {
    private ZombieType type;
    private String name;
    private ZombieChapter chapter = ZombieChapter.COMMON;
    private int health;
    private double speedTilesPerSecond;
    private int eatDamagePerSecond;
    private int waveCost;
    private List<ZombieArmorPart> armorParts = new ArrayList<>();
    private ZombieBehaviorKind behaviorKind = ZombieBehaviorKind.NORMAL;
    private boolean bonus;
    private String description = "";

    public ZombieDefinition() {
    }

    public ZombieDefinition(
            ZombieType type,
            String name,
            ZombieChapter chapter,
            int health,
            double speedTilesPerSecond,
            int eatDamagePerSecond,
            int waveCost,
            List<ZombieArmorPart> armorParts,
            ZombieBehaviorKind behaviorKind,
            boolean bonus
    ) {
        if (type == null || name == null || name.isBlank() || chapter == null
                || health <= 0 || speedTilesPerSecond < 0.0
                || eatDamagePerSecond < 0 || waveCost <= 0 || behaviorKind == null) {
            throw new IllegalArgumentException("Invalid canonical zombie definition.");
        }
        this.type = type;
        this.name = name.trim();
        this.chapter = chapter;
        this.health = health;
        this.speedTilesPerSecond = speedTilesPerSecond;
        this.eatDamagePerSecond = eatDamagePerSecond;
        this.waveCost = waveCost;
        this.armorParts = copyArmor(armorParts);
        this.behaviorKind = behaviorKind;
        this.bonus = bonus;
    }

    private static List<ZombieArmorPart> copyArmor(List<ZombieArmorPart> source) {
        List<ZombieArmorPart> result = new ArrayList<>();
        if (source != null) {
            for (ZombieArmorPart part : source) {
                result.add(part.copy());
            }
        }
        return result;
    }

    public List<ZombieArmorPart> createArmorParts() {
        return copyArmor(armorParts);
    }

    public ZombieType getType() {
        return type;
    }

    public int getHealth() {
        return health;
    }

    /** Compatibility total for the old collection screen. */
    public int getArmor() {
        int total = 0;
        for (ZombieArmorPart part : armorParts) {
            total += part.getMaxHealth();
        }
        return total;
    }

    public List<ZombieArmorPart> getArmorParts() {
        return Collections.unmodifiableList(copyArmor(armorParts));
    }

    public int getWaveCost() {
        return waveCost;
    }

    public String getName() {
        return name;
    }

    public ZombieChapter getChapter() {
        return chapter;
    }

    public double getSpeedTilesPerSecond() {
        return speedTilesPerSecond;
    }

    public int getEatDamagePerSecond() {
        return eatDamagePerSecond;
    }

    public ZombieBehaviorKind getBehaviorKind() {
        return behaviorKind;
    }

    public boolean isBonus() {
        return bonus;
    }

    public boolean isMandatory() {
        return !bonus;
    }

    public boolean isFrostbiteNative() {
        return chapter == ZombieChapter.FROSTBITE_CAVES;
    }

    public boolean isFireImmune() {
        return type == ZombieType.DRAGON_IMP;
    }

    public String getDisplayText() {
        return "Name: " + name + "\n"
                + "Type: " + type + "\n"
                + "Chapter: " + chapter + "\n"
                + "Mandatory: " + isMandatory() + "\n"
                + "Health: " + health + "\n"
                + "Armor: " + getArmor() + "\n"
                + "Speed: " + speedTilesPerSecond + "\n"
                + "Eat DPS: " + eatDamagePerSecond + "\n"
                + "Wave Cost: " + waveCost + "\n"
                + "Behavior: " + behaviorKind
                + (description == null || description.isBlank() ? "" : "\nDescription: " + description);
    }
}
