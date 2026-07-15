package model.user;

import model.enums.PlantType;
import model.inGame.plant.PlantDefinition;

public class PlantCard {
    private static final int[] COIN_COSTS = {0, 1000, 2000, 3000};
    private static final int[] SEED_PACKET_COSTS = {0, 10, 20, 30};

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
            throw new IllegalArgumentException("Seed packet amount cannot be negative.");
        }
        seedPackets += amount;
    }

    public void decreaseSeedPackets(int amount) {
        if (amount < 0 || amount > seedPackets) {
            throw new IllegalArgumentException("Invalid seed packet amount.");
        }
        seedPackets -= amount;
    }

    public boolean canUpgrade() {
        return level >= 1 && level < PlantDefinition.MAX_LEVEL;
    }

    public int getUpgradeCoinCost() {
        return canUpgrade() ? COIN_COSTS[level] : 0;
    }

    public int getUpgradeSeedPacketCost() {
        return canUpgrade() ? SEED_PACKET_COSTS[level] : 0;
    }

    public void upgrade() {
        if (!canUpgrade()) {
            throw new IllegalStateException("Plant is already at maximum level.");
        }
        level++;
    }

    public void applyDefaults() {
        if (level < 1) {
            level = 1;
        } else if (level > PlantDefinition.MAX_LEVEL) {
            level = PlantDefinition.MAX_LEVEL;
        }
        seedPackets = Math.max(0, seedPackets);
    }
}
