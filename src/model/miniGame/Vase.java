package model.miniGame;

import model.enums.PlantType;
import model.enums.VaseContentType;
import model.enums.ZombieType;

public final class Vase {
    private final String id;
    private final int row;
    private final int column;
    private final VaseType type;
    private final VaseContentType contentType;
    private final PlantType plantType;
    private final ZombieType zombieType;
    private boolean broken;

    public Vase(String id, int row, int column, VaseType type, VaseContentType contentType,
                PlantType plantType, ZombieType zombieType) {
        this.id = id; this.row = row; this.column = column; this.type = type;
        this.contentType = contentType; this.plantType = plantType; this.zombieType = zombieType;
        if (type == VaseType.PLANT && contentType != VaseContentType.PLANT_PACKET)
            throw new IllegalArgumentException("Plant Vase must contain a plant packet.");
        if (type == VaseType.GARGANTUAR && contentType != VaseContentType.GARGANTUAR)
            throw new IllegalArgumentException("Gargantuar Vase must contain a Gargantuar.");
    }
    public String getId() { return id; }
    public int getRow() { return row; }
    public int getColumn() { return column; }
    public VaseType getType() { return type; }
    public VaseContentType getContentType() { return contentType; }
    public PlantType getPlantType() { return plantType; }
    public ZombieType getZombieType() { return zombieType; }
    public boolean isBroken() { return broken; }
    public void breakOpen() {
        if (broken) throw new IllegalStateException("Vase is already broken.");
        broken = true;
    }
}
