package plant;

import controller.CollectionMenuController;
import model.Result;
import model.Store;
import model.enums.PlantType;
import model.inGame.plant.PlantCollectionView;
import model.inGame.plant.PlantRegistry;
import model.inGame.plant.PlantSelection;
import model.inGame.zombie.ZombieRegistry;
import model.user.Collection;
import model.user.PlantCard;
import model.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.UserService;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantCollectionIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void collectionAndSelectionSupportBlueBonusRows() {
        Collection collection = new Collection();
        collection.purchasePlant(PlantType.PEASHOOTER);
        collection.purchasePlant(PlantRegistry.getDefault().require(PlantType.PEA_POD));
        collection.purchasePlant(PlantRegistry.getDefault().require(PlantType.CAT_TAIL));

        PlantSelection selection = new PlantSelection(PlantRegistry.getDefault(), 3);
        selection.select(PlantType.PEASHOOTER, collection);
        selection.select(PlantType.PEA_POD, collection);
        selection.select(PlantType.CAT_TAIL, collection);

        assertTrue(selection.getTypes().contains(PlantType.PEASHOOTER));
        assertTrue(selection.getTypes().contains(PlantType.PEA_POD));
        assertTrue(selection.getTypes().contains(PlantType.CAT_TAIL));
    }


    @Test
    void bonusPlantPurchaseIsAtomicAndSurvivesRestart() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        User user = new User();
        user.setUsername("bonus-owner");
        user.setCoins(4000);
        user.applyDefaults();

        Path savePath = tempDir.resolve("users.json");
        UserService service = new UserService(
                new JsonUserRepository(savePath), Clock.systemUTC());
        service.addUser(user);
        CollectionMenuController controller = new CollectionMenuController(
                PlantRegistry.getDefault(), ZombieRegistry.getDefault(), service);

        Result<PlantCollectionView> purchase = controller.purchasePlant(user, "Pea Pod");
        assertTrue(purchase.getStatus());
        assertEquals(2000, user.getCoins());
        assertTrue(user.getCollection().hasPlant(PlantType.PEA_POD));

        Result<PlantCollectionView> duplicate = controller.purchasePlant(user, "Pea Pod");
        assertFalse(duplicate.getStatus());
        assertEquals(2000, user.getCoins());

        Store.setUsers(new ArrayList<>());
        UserService reloaded = new UserService(
                new JsonUserRepository(savePath), Clock.systemUTC());
        reloaded.loadUsers();
        User restored = reloaded.findByUsername("bonus-owner");
        assertTrue(restored.getCollection().hasPlant(PlantType.PEA_POD));
        assertEquals(2000, restored.getCoins());
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
