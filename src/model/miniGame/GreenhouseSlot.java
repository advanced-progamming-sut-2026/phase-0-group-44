package model.miniGame;

import model.enums.PlantType;

/**
 * One greenhouse slot and its planted contents. The planting instant is stored
 * as an epoch second so that readiness can later be decided against an injected
 * clock rather than against {@code System.currentTimeMillis()}.
 */
public class GreenhouseSlot {
    private int index;
    private PlantType plantType;
    private long plantedAtEpochSecond;

    public GreenhouseSlot() {
    }

    public GreenhouseSlot(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public PlantType getPlantType() {
        return plantType;
    }

    public long getPlantedAtEpochSecond() {
        return plantedAtEpochSecond;
    }

    public boolean isOccupied() {
        return plantType != null;
    }

    public void plant(PlantType plantType, long plantedAtEpochSecond) {
        if (plantType == null) {
            throw new IllegalArgumentException("Plant type is null.");
        }

        this.plantType = plantType;
        this.plantedAtEpochSecond = plantedAtEpochSecond;
    }

    public void clear() {
        this.plantType = null;
        this.plantedAtEpochSecond = 0L;
    }
}
