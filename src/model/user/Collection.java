package model.user;

import model.enums.PlantType;
import model.enums.ZombieType;
import model.inGame.plant.Plant;
import model.inGame.zombie.Zombie;

import java.util.ArrayList;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class Collection {
    private Map<PlantType, PlantCard> ownedPlants;
    private Set<ZombieType> seenZombies;
    private ArrayList<ZombieCard> zombieCards;

    public Collection() {
        ownedPlants =
                new EnumMap<PlantType, PlantCard>(PlantType.class);

        seenZombies =
                EnumSet.noneOf(ZombieType.class);

        zombieCards = new ArrayList<ZombieCard>();
    }

    public Map<PlantType, PlantCard> getOwnedPlants() {
        return ownedPlants;
    }

    public Set<ZombieType> getSeenZombies() {
        return seenZombies;
    }

    public ArrayList<ZombieCard> getZombieCards() {
        return zombieCards;
    }

    public boolean hasPlant(PlantType type) {
        PlantCard card = ownedPlants.get(type);

        return card != null && card.isUnlocked();
    }

    public PlantCard getPlantCard(PlantType type) {
        return ownedPlants.get(type);
    }

    public void purchasePlant(PlantType type) {
        if (type == null) {
            throw new IllegalArgumentException("Plant type is null.");
        }

        if (hasPlant(type)) {
            throw new IllegalStateException(
                    "Plant is already purchased."
            );
        }

        ownedPlants.put(type, new PlantCard(type));
    }

    public void upgradePlant(PlantType type) {
        PlantCard card = ownedPlants.get(type);

        if (card == null || !card.isUnlocked()) {
            throw new IllegalStateException(
                    "Plant is not unlocked."
            );
        }

        card.upgrade();
    }

    public void markZombieAsSeen(ZombieType type) {
        if (type == null || seenZombies.contains(type)) {
            return;
        }

        seenZombies.add(type);
        zombieCards.add(new ZombieCard(type, true, true));
    }

    public ZombieCard getZombieCard(ZombieType type) {
        for (ZombieCard card : zombieCards) {
            if (card.getType() == type) {
                return card;
            }
        }

        return null;
    }
}


