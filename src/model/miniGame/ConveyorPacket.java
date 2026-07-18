package model.miniGame;

/** One selectable bowling plant currently visible on the conveyor belt. */
public final class ConveyorPacket {
    private final int id;
    private final BowlingPlantType plantType;
    private boolean used;

    public ConveyorPacket(int id, BowlingPlantType plantType) {
        if (id <= 0 || plantType == null) {
            throw new IllegalArgumentException("A conveyor packet requires a positive id and plant type.");
        }
        this.id = id;
        this.plantType = plantType;
    }

    public int getId() {
        return id;
    }

    public BowlingPlantType getPlantType() {
        return plantType;
    }

    public boolean isAvailable() {
        return !used;
    }

    public boolean use() {
        if (used) {
            return false;
        }
        used = true;
        return true;
    }
}
