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
        return definition.getDisplayText() + "\n"
                + "Level: " + card.getLevel() + "\n"
                + "Seed Packets: " + card.getSeedPackets() + "\n"
                + "Upgrade Coin Cost: "
                + card.getUpgradeCoinCost() + "\n"
                + "Upgrade Seed Packet Cost: "
                + card.getUpgradeSeedPacketCost();
    }
}

