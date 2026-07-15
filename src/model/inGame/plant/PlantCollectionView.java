package model.inGame.plant;

import model.user.PlantCard;

public class PlantCollectionView {
    private final PlantDefinition definition;
    private final PlantCard card;

    public PlantCollectionView(
            PlantDefinition definition,
            PlantCard card
    ) {
        this.definition = definition;
        this.card = card;
    }

    public PlantDefinition getDefinition() {
        return definition;
    }

    public PlantCard getCard() {
        return card;
    }

    public String getDisplayText() {
        PlantStats stats = definition.statsAtLevel(card.getLevel());
        return definition.getDisplayText() + "\n"
                + "Level: " + card.getLevel() + "\n"
                + "Resolved HP: " + stats.getHp() + "\n"
                + "Resolved Damage: " + stats.getDamage() + "\n"
                + "Resolved Sun Cost: " + stats.getCost() + "\n"
                + "Resolved Action Interval: " + stats.getActionInterval() + "\n"
                + "Resolved Recharge: " + stats.getRecharge() + "\n"
                + "Seed Packets: " + card.getSeedPackets() + "\n"
                + "Upgrade Coin Cost: " + card.getUpgradeCoinCost() + "\n"
                + "Upgrade Seed Packet Cost: " + card.getUpgradeSeedPacketCost();
    }
}

