package model.inGame.zombie;

import model.enums.ZombieType;

public class ZombieDefinition {
    private ZombieType type;
    private String name;
    private int health;
    private int armor;
    private int waveCost;
    private String description;

    public ZombieType getType() {
        return type;
    }

    public int getHealth() {
        return health;
    }

    public int getArmor() {
        return armor;
    }

    public int getWaveCost() {
        return waveCost;
    }

    public String getName() {
        return name;
    }

    public String getDisplayText() {
        return "Name: " + name + "\n"
                + "Type: " + type + "\n"
                + "Health: " + health + "\n"
                + "Armor: " + armor + "\n"
                + "Wave Cost: " + waveCost + "\n"
                + "Description: " + description;
    }
}

