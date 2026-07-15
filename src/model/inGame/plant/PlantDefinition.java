package model.inGame.plant;

import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.enums.PlantType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PlantDefinition {
    public static final int MAX_LEVEL = 4;

    private int id;
    private PlantType type;
    private String name;
    private PlantCategory category;
    private Set<PlantTag> tags = EnumSet.noneOf(PlantTag.class);
    private int cost;
    private int baseHp;
    private DamageProfile damageProfile = DamageProfile.parse("0");
    private String baseAbility;
    private String plantFoodEffect;
    private List<PlantUpgrade> upgrades = new ArrayList<>();
    private double actionInterval;
    private double recharge;
    private boolean bonus;

    public PlantDefinition() {
        // Required for Gson compatibility with older callers.
    }

    public PlantDefinition(int id, String name, PlantCategory category, Set<PlantTag> tags,
                           int cost, int baseHp, DamageProfile damageProfile,
                           String baseAbility, String plantFoodEffect,
                           List<PlantUpgrade> upgrades, double actionInterval,
                           double recharge, boolean bonus) {
        this.id = id;
        this.name = name;
        this.type = PlantType.fromCanonicalName(name);
        this.category = category;
        this.tags = tags == null || tags.isEmpty()
                ? EnumSet.noneOf(PlantTag.class) : EnumSet.copyOf(tags);
        this.cost = cost;
        this.baseHp = baseHp;
        this.damageProfile = damageProfile;
        this.baseAbility = baseAbility;
        this.plantFoodEffect = plantFoodEffect;
        this.upgrades = new ArrayList<>(upgrades);
        this.actionInterval = actionInterval;
        this.recharge = recharge;
        this.bonus = bonus;
    }

    public int getId() {
        return id;
    }

    public PlantType getType() {
        if (type == null && name != null) {
            type = PlantType.fromCanonicalName(name);
        }
        return type;
    }

    public String getName() {
        return name;
    }

    public PlantCategory getCategory() {
        return category;
    }

    public PlantCategory getBehaviorCategory() {
        return isMint() ? PlantCategory.MINT : category;
    }

    public Set<PlantTag> getTags() {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(tags);
    }

    public boolean hasTag(PlantTag tag) {
        return tags != null && tags.contains(tag);
    }

    public int getCost() {
        return cost;
    }

    public int getBaseHp() {
        return baseHp;
    }

    /** Compatibility alias retained for the collection view. */
    public int getHp() {
        return baseHp;
    }

    public DamageProfile getDamageProfile() {
        return damageProfile == null ? DamageProfile.parse("0") : damageProfile;
    }

    public int getDamage() {
        return getDamageProfile().getPrimaryDamage();
    }

    public String getBaseAbility() {
        return baseAbility;
    }

    public String getPlantFoodEffect() {
        return plantFoodEffect;
    }

    public List<PlantUpgrade> getUpgrades() {
        return upgrades == null ? Collections.emptyList() : Collections.unmodifiableList(upgrades);
    }

    public double getActionInterval() {
        return actionInterval;
    }

    /** Compatibility alias; timings are no longer rounded internally. */
    public int getActionTime() {
        return (int) Math.round(actionInterval);
    }

    public double getRecharge() {
        return recharge;
    }

    /** Compatibility alias; timings are no longer rounded internally. */
    public int getRechargeTime() {
        return (int) Math.round(recharge);
    }

    public boolean isBonus() {
        return bonus;
    }

    public boolean isMandatory() {
        return !bonus;
    }

    public boolean isMint() {
        return getType().name().endsWith("_MINT");
    }

    public PlantStats statsAtLevel(int level) {
        return PlantStats.resolve(this, level);
    }

    public String getDescription() {
        return baseAbility;
    }

    public String getDisplayText() {
        return "Name: " + name + "\n"
                + "Type: " + getType() + "\n"
                + "Status: " + (bonus ? "BLUE/BONUS" : "MANDATORY") + "\n"
                + "HP: " + baseHp + "\n"
                + "Damage: " + getDamageProfile().getRaw() + "\n"
                + "Recharge Time: " + recharge + "\n"
                + "Action Time: " + actionInterval + "\n"
                + "Sun Cost: " + cost + "\n"
                + "Category: " + category + "\n"
                + "Tags: " + getTags() + "\n"
                + "Ability: " + baseAbility + "\n"
                + "Plant Food: " + plantFoodEffect;
    }
}
