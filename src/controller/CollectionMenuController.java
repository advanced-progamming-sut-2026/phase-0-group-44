package controller;

import model.Result;
import model.inGame.plant.Plant;
import model.inGame.zombie.Zombie;
import model.user.User;

import java.util.ArrayList;

public class CollectionMenuController {
    private static final int PLANT_PURCHASE_COST = 2000;

    public Result<ArrayList<Plant>> showPlants(User user) {
        Result<ArrayList<Plant>> result = new Result<>();

        ArrayList<Plant> plants = user.getUnlockedPlants();

        result.setStatus(true);
        result.setData(plants);

        if (plants.isEmpty()) {
            result.appendToMessage("No unlocked plants.");
            return result;
        }

        for (Plant plant : plants) {
            result.appendToMessage(plant.getDisplayText());
            result.appendToMessage("\n\n");
        }

        return result;
    }

    public Result<ArrayList<Plant>> showAllPlants() {
        Result<ArrayList<Plant>> result = new Result<>();

        ArrayList<Plant> plants = GameData.getAllPlants();

        result.setStatus(true);
        result.setData(plants);

        if (plants.isEmpty()) {
            result.appendToMessage("No plants found.");
            return result;
        }

        for (Plant plant : plants) {
            result.appendToMessage(plant.getDisplayText());
            result.appendToMessage("\n\n");
        }

        return result;
    }

    public Result<ArrayList<Zombie>> showZombies(User user) {
        Result<ArrayList<Zombie>> result = new Result<>();

        ArrayList<Zombie> zombies = user.getSeenZombies();

        result.setStatus(true);
        result.setData(zombies);

        if (zombies.isEmpty()) {
            result.appendToMessage("No seen zombies.");
            return result;
        }

        for (Zombie zombie : zombies) {
            result.appendToMessage(zombie.getDisplayText());
            result.appendToMessage("\n\n");
        }

        return result;
    }

    public Result<ArrayList<Zombie>> showAllZombies() {
        Result<ArrayList<Zombie>> result = new Result<>();

        ArrayList<Zombie> zombies = GameData.getAllZombies();

        result.setStatus(true);
        result.setData(zombies);

        if (zombies.isEmpty()) {
            result.appendToMessage("No zombies found.");
            return result;
        }

        for (Zombie zombie : zombies) {
            result.appendToMessage(zombie.getDisplayText());
            result.appendToMessage("\n\n");
        }

        return result;
    }

    public Result<Plant> showPlant(User user, String plantName) {
        Result<Plant> result = new Result<>();

        Plant plant = findPlantByName(user.getUnlockedPlants(), plantName);

        if (plant == null) {
            result.setStatus(false);
            result.appendToMessage("Plant not found in your collection.");
            return result;
        }

        result.setStatus(true);
        result.setData(plant);
        result.appendToMessage(plant.getDisplayText());

        return result;
    }

    public Result<Zombie> showZombie(User user, String zombieName) {
        Result<Zombie> result = new Result<>();

        Zombie zombie = findZombieByName(user.getSeenZombies(), zombieName);

        if (zombie == null) {
            result.setStatus(false);
            result.appendToMessage("Zombie not found in your collection.");
            return result;
        }

        result.setStatus(true);
        result.setData(zombie);
        result.appendToMessage(zombie.getDisplayText());

        return result;
    }

    public Result<Plant> purchasePlant(User user, String plantName) {
        Result<Plant> result = new Result<>();

        Plant plant = GameData.findPlantByName(plantName);

        if (plant == null) {
            result.setStatus(false);
            result.appendToMessage("Plant does not exist.");
            return result;
        }

        if (findPlantByName(user.getUnlockedPlants(), plantName) != null) {
            result.setStatus(false);
            result.appendToMessage("You already have this plant.");
            return result;
        }

        if (user.getCoins() < PLANT_PURCHASE_COST) {
            result.setStatus(false);
            result.appendToMessage("Not enough coins.");
            return result;
        }

        user.decreaseCoins(PLANT_PURCHASE_COST);
        user.getUnlockedPlants().add(plant.copy());

        result.setStatus(true);
        result.setData(plant);
        result.appendToMessage("Plant purchased successfully.");

        return result;
    }

    public Result<Plant> upgradePlant(User user, String plantName) {
        Result<Plant> result = new Result<>();

        Plant plant = findPlantByName(user.getUnlockedPlants(), plantName);

        if (plant == null) {
            result.setStatus(false);
            result.appendToMessage("Plant not found in your collection.");
            return result;
        }

        int coinCost = plant.getUpgradeCoinCost();
        int seedPacketCost = plant.getUpgradeSeedPacketCost();

        if (user.getCoins() < coinCost) {
            result.setStatus(false);
            result.appendToMessage("Not enough coins.");
            return result;
        }

        if (plant.getSeedPackets() < seedPacketCost) {
            result.setStatus(false);
            result.appendToMessage("Not enough seed packets.");
            return result;
        }

        user.decreaseCoins(coinCost);
        plant.decreaseSeedPackets(seedPacketCost);
        plant.upgrade();

        result.setStatus(true);
        result.setData(plant);
        result.appendToMessage("Plant upgraded successfully.");

        return result;
    }

    private Plant findPlantByName(ArrayList<Plant> plants, String plantName) {
        for (Plant plant : plants) {
            if (plant.getName().equalsIgnoreCase(plantName)) {
                return plant;
            }
        }

        return null;
    }

    private Zombie findZombieByName(ArrayList<Zombie> zombies, String zombieName) {
        for (Zombie zombie : zombies) {
            if (zombie.getName().equalsIgnoreCase(zombieName)) {
                return zombie;
            }
        }

        return null;
    }
}
