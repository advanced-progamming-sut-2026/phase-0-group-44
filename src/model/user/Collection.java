package model.user;

import model.enums.PlantType;
import model.enums.ZombieType;
import model.inGame.plant.PlantDefinition;

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

        seenZombies = EnumSet.noneOf(ZombieType.class);

    }

    public Map<PlantType, PlantCard> getOwnedPlants() {
        return ownedPlants;
    }

    public Set<ZombieType> getSeenZombies() {
        return seenZombies;
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

    public void purchasePlant(PlantDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Plant definition is null."
            );
        }

        purchasePlant(definition.getType());
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


    /**
     * Recreates any container a save file did not contain, so a reloaded
     * collection behaves exactly like a freshly created one.
     */
    public void applyDefaults() {
        if (ownedPlants == null) {
            ownedPlants = new EnumMap<>(PlantType.class);
        }

        if (seenZombies == null) {
            seenZombies = EnumSet.noneOf(ZombieType.class);
        }

        if (zombieCards == null) {
            zombieCards = new ArrayList<>();
        }

        for (PlantCard card : ownedPlants.values()) {
            if (card != null) {
                card.applyDefaults();
            }
        }
    }
}
