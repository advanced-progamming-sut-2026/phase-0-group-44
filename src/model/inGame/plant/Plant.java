package model.inGame.plant;

public class Plant {
    private String name;
    private PlantType type;
    private int level;
    private int seedPackets;

    private int sunCost;
    private int cooldown;
    private int health;
    private int damage;
    private String description;

    private int upgradeCoinCost;
    private int upgradeSeedPacketCost;

    public Plant() {
        // Needed for JSON libraries like Gson/Jackson
    }

    public Plant(String name, PlantType type, int level, int seedPackets,
                 int sunCost, int cooldown, int health, int damage,
                 String description, int upgradeCoinCost, int upgradeSeedPacketCost) {
        this.name = name;
        this.type = type;
        this.level = level;
        this.seedPackets = seedPackets;
        this.sunCost = sunCost;
        this.cooldown = cooldown;
        this.health = health;
        this.damage = damage;
        this.description = description;
        this.upgradeCoinCost = upgradeCoinCost;
        this.upgradeSeedPacketCost = upgradeSeedPacketCost;
    }

    public String getName() {
        return name;
    }

    public PlantType getType() {
        return type;
    }

    public int getLevel() {
        return level;
    }

    public int getSeedPackets() {
        return seedPackets;
    }

    public int getUpgradeCoinCost() {
        return upgradeCoinCost;
    }

    public int getUpgradeSeedPacketCost() {
        return upgradeSeedPacketCost;
    }

    public void decreaseSeedPackets(int amount) {
        this.seedPackets -= amount;
    }

    public void addSeedPackets(int amount) {
        this.seedPackets += amount;
    }

    public void upgrade() {
        this.level++;

        // اگر خواستی با ارتقا ویژگی‌ها هم بهتر شوند:
        this.health += 50;
        this.damage += 5;

        // می‌تونی هزینه ارتقای بعدی رو هم بیشتر کنی
        this.upgradeCoinCost += 500;
        this.upgradeSeedPacketCost += 5;
    }

    public Plant copy() {
        return new Plant(
                this.name,
                this.type,
                1,
                0,
                this.sunCost,
                this.cooldown,
                this.health,
                this.damage,
                this.description,
                this.upgradeCoinCost,
                this.upgradeSeedPacketCost
        );
    }

    public String getDisplayText() {
        return "Name: " + name + "\n"
                + "Type: " + type + "\n"
                + "Level: " + level + "\n"
                + "Seed Packets: " + seedPackets + "\n"
                + "Sun Cost: " + sunCost + "\n"
                + "Cooldown: " + cooldown + "\n"
                + "Health: " + health + "\n"
                + "Damage: " + damage + "\n"
                + "Description: " + description + "\n"
                + "Upgrade Coin Cost: " + upgradeCoinCost + "\n"
                + "Upgrade Seed Packet Cost: " + upgradeSeedPacketCost;
    }
}
