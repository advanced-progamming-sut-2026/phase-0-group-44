package model.miniGame;

import model.enums.PlantType;
import model.enums.VaseContentType;
import model.enums.ZombieType;

/** One immutable-position Vasebreaker vase and its hidden configured content. */
public final class Vase {
    private final int id;
    private final int x;
    private final int y;
    private final VaseContentType contentType;
    private final PlantType plantType;
    private final ZombieType zombieType;
    private boolean broken;

    public Vase(
            int id,
            int x,
            int y,
            VaseContentType contentType,
            PlantType plantType,
            ZombieType zombieType
    ) {
        if (id <= 0 || x < 0 || y < 0 || contentType == null) {
            throw new IllegalArgumentException("Invalid vase definition.");
        }
        if ((contentType == VaseContentType.SEED_PACKET
                || contentType == VaseContentType.PLANT_VASE) && plantType == null) {
            throw new IllegalArgumentException("Plant vases require a plant packet.");
        }
        if (contentType == VaseContentType.ZOMBIE && zombieType == null) {
            throw new IllegalArgumentException("Zombie vases require a zombie type.");
        }
        if (contentType == VaseContentType.GARGANTUAR_VASE
                && zombieType != ZombieType.GARGANTUAR) {
            throw new IllegalArgumentException("A Gargantuar Vase must contain Gargantuar.");
        }
        this.id = id;
        this.x = x;
        this.y = y;
        this.contentType = contentType;
        this.plantType = plantType;
        this.zombieType = zombieType;
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public VaseContentType getContentType() {
        return contentType;
    }

    public PlantType getPlantType() {
        return plantType;
    }

    public ZombieType getZombieType() {
        return zombieType;
    }

    public boolean isBroken() {
        return broken;
    }

    public boolean breakOnce() {
        if (broken) {
            return false;
        }
        broken = true;
        return true;
    }
}
