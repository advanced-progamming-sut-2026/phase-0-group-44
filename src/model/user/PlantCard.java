package model.user;

import model.enums.PlantType;

public class PlantCard {
    private PlantType type;
    private int level;
    private int seedPackets;
    private boolean unlocked;

    public PlantCard() {
    }

    public PlantCard(PlantType type) {
        this.type = type;
        this.level = 1;
        this.seedPackets = 0;
        this.unlocked = true;
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

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public void addSeedPackets(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException(
                    "Seed packet amount cannot be negative."
            );
        }

        seedPackets += amount;
    }

    public void decreaseSeedPackets(int amount) {
        if (amount < 0 || amount > seedPackets) {
            throw new IllegalArgumentException(
                    "Invalid seed packet amount."
            );
        }

        seedPackets -= amount;
    }

    public int getUpgradeCoinCost() {
        return level * 1000;
    }

    public int getUpgradeSeedPacketCost() {
        return level * 10;
    }

    public void upgrade() {
        level++;
    }
}
