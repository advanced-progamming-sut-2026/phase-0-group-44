package model.inGame.plant;

public class PlantDefinition {
    private PlantType type;
    private String name;
    private int hp;
    private int rechargeTime;
    private int actionTime;
    private int cost;
    private PlantCategory category;
    private String description;

    public PlantDefinition() {
        // Required for JSON deserialization
    }

    public PlantType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getHp() {
        return hp;
    }

    public int getRechargeTime() {
        return rechargeTime;
    }

    public int getActionTime() {
        return actionTime;
    }

    public int getCost() {
        return cost;
    }

    public PlantCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayText() {
        return "Name: " + name + "\n"
                + "Type: " + type + "\n"
                + "HP: " + hp + "\n"
                + "Recharge Time: " + rechargeTime + "\n"
                + "Action Time: " + actionTime + "\n"
                + "Sun Cost: " + cost + "\n"
                + "Category: " + category + "\n"
                + "Description: " + description;
    }
}

