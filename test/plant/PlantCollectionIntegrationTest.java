package plant;

import model.enums.PlantType;
import model.inGame.plant.PlantRegistry;
import model.inGame.plant.PlantSelection;
import model.user.Collection;
import model.user.PlantCard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantCollectionIntegrationTest {

    @Test
    void collectionAndSelectionRejectBlueBonusRows() {
        Collection collection = new Collection();
        collection.purchasePlant(PlantType.PEASHOOTER);

        PlantSelection selection = new PlantSelection(PlantRegistry.getDefault(), 2);
        selection.select(PlantType.PEASHOOTER, collection);

        assertTrue(selection.getTypes().contains(PlantType.PEASHOOTER));
        assertThrows(
                UnsupportedOperationException.class,
                () -> collection.purchasePlant(
                        PlantRegistry.getDefault().require(
                                PlantType.PEA_POD
                        )
                )
        );
        assertThrows(UnsupportedOperationException.class,
                () -> PlantRegistry.getDefault().requireMandatory(PlantType.CAT_TAIL));
    }

    @Test
    void upgradeEconomyRisesAndStopsAtCanonicalMaximumLevel() {
        PlantCard card = new PlantCard(PlantType.PEASHOOTER);
        card.addSeedPackets(100);

        assertEquals(1000, card.getUpgradeCoinCost());
        assertEquals(10, card.getUpgradeSeedPacketCost());
        card.upgrade();
        assertEquals(2000, card.getUpgradeCoinCost());
        assertEquals(20, card.getUpgradeSeedPacketCost());
        card.upgrade();
        assertEquals(3000, card.getUpgradeCoinCost());
        assertEquals(30, card.getUpgradeSeedPacketCost());
        card.upgrade();

        assertEquals(4, card.getLevel());
        assertEquals(0, card.getUpgradeCoinCost());
        assertEquals(0, card.getUpgradeSeedPacketCost());
        assertThrows(IllegalStateException.class, card::upgrade);
    }
}
