package model.level;

import model.enums.PlantType;

/** One plant pre-placed and protected by a Save Our Seeds level. */
public final class ProtectedPlantPlacement {
    private final PlantType type;
    private final TileCoordinate coordinate;

    public ProtectedPlantPlacement(PlantType type, int x, int y) {
        if (type == null) {
            throw new IllegalArgumentException("Protected plant type is required.");
        }
        this.type = type;
        this.coordinate = new TileCoordinate(x, y);
    }

    public PlantType getType() {
        return type;
    }

    public TileCoordinate getCoordinate() {
        return coordinate;
    }
}
