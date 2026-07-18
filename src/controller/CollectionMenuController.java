package controller;

import model.Result;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.inGame.plant.PlantCollectionView;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRepository;
import model.inGame.zombie.ZombieDefinition;
import model.inGame.zombie.ZombieRepository;
import model.user.Collection;
import model.user.PlantCard;
import model.user.User;
import service.UserService;

import java.util.ArrayList;
import java.util.Map;

public class CollectionMenuController {
    private static final int PLANT_PURCHASE_COST = 2000;

    private final PlantRepository plantRepository;
    private final ZombieRepository zombieRepository;
    private final UserService userService;

    public CollectionMenuController(
            PlantRepository plantRepository,
            ZombieRepository zombieRepository,
            UserService userService
    ) {
        this.plantRepository = plantRepository;
        this.zombieRepository = zombieRepository;
        this.userService = userService;
    }

    public Result<ArrayList<PlantCollectionView>> showPlants(
            User user
    ) {
        Result<ArrayList<PlantCollectionView>> result =
                new Result<ArrayList<PlantCollectionView>>();

        if (user == null) {
            return failure(result, "No user is logged in.");
        }

        ArrayList<PlantCollectionView> views =
                new ArrayList<PlantCollectionView>();

        for (Map.Entry<PlantType, PlantCard> entry
                : user.getCollection().getOwnedPlants().entrySet()) {

            PlantCard card = entry.getValue();

            if (!card.isUnlocked()) {
                continue;
            }

            PlantDefinition definition = plantRepository.findByType(entry.getKey());

            if (definition != null) {
                views.add(
                        new PlantCollectionView(definition, card)
                );
            }
        }

        result.setStatus(true);
        result.setData(views);

        if (views.isEmpty()) {
            result.appendToMessage("No unlocked plants.");
            return result;
        }

        for (int i = 0; i < views.size(); i++) {
            result.appendToMessage(views.get(i).getDisplayText());

            if (i < views.size() - 1) {
                result.appendToMessage("\n\n");
            }
        }

        return result;
    }

    public Result<ArrayList<PlantDefinition>> showAllPlants() {
        Result<ArrayList<PlantDefinition>> result =
                new Result<ArrayList<PlantDefinition>>();

        ArrayList<PlantDefinition> plants =
                plantRepository.findAll();

        result.setStatus(true);
        result.setData(plants);

        if (plants.isEmpty()) {
            result.appendToMessage("No plants found.");
            return result;
        }

        for (int i = 0; i < plants.size(); i++) {
            result.appendToMessage(
                    plants.get(i).getDisplayText()
            );

            if (i < plants.size() - 1) {
                result.appendToMessage("\n\n");
            }
        }

        return result;
    }

    public Result<PlantCollectionView> showPlant(
            User user,
            String plantName
    ) {
        Result<PlantCollectionView> result =
                new Result<PlantCollectionView>();

        if (user == null) {
            return failure(result, "No user is logged in.");
        }

        PlantDefinition definition =
                plantRepository.findByName(plantName);

        if (definition == null) {
            return failure(result, "Plant does not exist.");
        }

        PlantCard card = user.getCollection().getPlantCard(definition.getType());

        if (card == null || !card.isUnlocked()) {
            return failure(
                    result,
                    "Plant not found in your collection."
            );
        }

        PlantCollectionView view =
                new PlantCollectionView(definition, card);

        result.setStatus(true);
        result.setData(view);
        result.appendToMessage(view.getDisplayText());

        return result;
    }

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public Result<PlantCollectionView> purchasePlant(
            User user,
            String plantName
    ) {
        Result<PlantCollectionView> result =
                new Result<PlantCollectionView>();

        if (user == null) {
            return failure(result, "No user is logged in.");
        }

        PlantDefinition definition =
                plantRepository.findByName(plantName);

        if (definition == null) {
            return failure(result, "Plant does not exist.");
        }
        Collection collection = user.getCollection();

        if (collection.hasPlant(definition.getType())) {
            return failure(
                    result,
                    "You already have this plant."
            );
        }

        if (user.getCoins() < PLANT_PURCHASE_COST) {
            return failure(result, "Not enough coins.");
        }

        user.decreaseCoins(PLANT_PURCHASE_COST);
        collection.purchasePlant(definition);
        userService.updateUser(user);

        PlantCard card = collection.getPlantCard(
                definition.getType()
        );

        PlantCollectionView view =
                new PlantCollectionView(definition, card);

        result.setStatus(true);
        result.setData(view);
        result.appendToMessage(
                "Plant purchased successfully."
        );

        return result;
    }

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public Result<PlantCollectionView> upgradePlant(
            User user,
            String plantName
    ) {
        Result<PlantCollectionView> result =
                new Result<PlantCollectionView>();

        if (user == null) {
            return failure(result, "No user is logged in.");
        }

        PlantDefinition definition = plantRepository.findByName(plantName);

        if (definition == null) {
            return failure(result, "Plant does not exist.");
        }

        PlantCard card = user.getCollection().getPlantCard(definition.getType());

        if (card == null || !card.isUnlocked()) {
            return failure(
                    result,
                    "Plant not found in your collection."
            );
        }
        if (!card.canUpgrade()) {
            return failure(
                    result,
                    "Plant is already at maximum level."
            );
        }

        int coinCost = card.getUpgradeCoinCost();
        int seedPacketCost =
                card.getUpgradeSeedPacketCost();

        if (user.getCoins() < coinCost) {
            return failure(result, "Not enough coins.");
        }

        if (card.getSeedPackets() < seedPacketCost) {
            return failure(
                    result,
                    "Not enough seed packets."
            );
        }

        user.decreaseCoins(coinCost);
        card.decreaseSeedPackets(seedPacketCost);
        user.getCollection().upgradePlant(
                definition.getType()
        );
        userService.updateUser(user);

        PlantCollectionView view =
                new PlantCollectionView(definition, card);

        result.setStatus(true);
        result.setData(view);
        result.appendToMessage(
                "Plant upgraded successfully."
        );

        return result;
    }

    public Result<ArrayList<ZombieDefinition>> showZombies(
            User user
    ) {
        Result<ArrayList<ZombieDefinition>> result =
                new Result<ArrayList<ZombieDefinition>>();

        if (user == null) {
            return failure(result, "No user is logged in.");
        }

        ArrayList<ZombieDefinition> zombies =
                new ArrayList<ZombieDefinition>();

        for (ZombieType type
                : user.getCollection().getSeenZombies()) {

            ZombieDefinition definition = zombieRepository.findByType(type);

            if (definition != null) {
                zombies.add(definition);
            }
        }

        result.setStatus(true);
        result.setData(zombies);

        if (zombies.isEmpty()) {
            result.appendToMessage("No seen zombies.");
            return result;
        }

        appendZombies(result, zombies);
        return result;
    }

    public Result<ArrayList<ZombieDefinition>> showAllZombies() {
        Result<ArrayList<ZombieDefinition>> result =
                new Result<ArrayList<ZombieDefinition>>();

        ArrayList<ZombieDefinition> zombies =
                zombieRepository.findAll();

        result.setStatus(true);
        result.setData(zombies);

        if (zombies.isEmpty()) {
            result.appendToMessage("No zombies found.");
            return result;
        }

        appendZombies(result, zombies);
        return result;
    }

    public Result<ZombieDefinition> showZombie(
            User user,
            String zombieName
    ) {
        Result<ZombieDefinition> result =
                new Result<ZombieDefinition>();

        if (user == null) {
            return failure(result, "No user is logged in.");
        }

        ZombieDefinition zombie =
                zombieRepository.findByName(zombieName);

        if (zombie == null) {
            return failure(result, "Zombie does not exist.");
        }

        if (!user.getCollection()
                .getSeenZombies()
                .contains(zombie.getType())) {

            return failure(
                    result,
                    "Zombie not found in your collection."
            );
        }

        result.setStatus(true);
        result.setData(zombie);
        result.appendToMessage(zombie.getDisplayText());

        return result;
    }

    private void appendZombies(
            Result<ArrayList<ZombieDefinition>> result,
            ArrayList<ZombieDefinition> zombies
    ) {
        for (int i = 0; i < zombies.size(); i++) {
            result.appendToMessage(
                    zombies.get(i).getDisplayText()
            );

            if (i < zombies.size() - 1) {
                result.appendToMessage("\n\n");
            }
        }
    }

    private <T> Result<T> failure(
            Result<T> result,
            String message
    ) {
        result.setStatus(false);
        result.appendToMessage(message);
        return result;
    }
}

