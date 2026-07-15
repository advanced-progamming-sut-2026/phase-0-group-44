package model.enums;

/**
 * The terrain kinds the chapters need. Static capability flags live here; state
 * that changes during play (gravestone/ice health, frozen contents, slippery
 * direction) lives on the {@code Tile}.
 */
public enum TerrainType {
    NORMAL_EGYPT(true, false, false),
    GRAVESTONE(false, true, false),
    NORMAL_FROSTBITE(true, false, false),
    SLIPPERY_UP(false, false, false),
    SLIPPERY_DOWN(false, false, false),
    FROZEN(false, false, false),
    NORMAL_BEACH(true, false, false),
    WATER(false, false, true),
    LOW_TIDE(true, false, false),
    NORMAL_DARK_AGES(true, false, false),
    DARK_AGES_GRAVESTONE(false, true, false),
    NECROMANCY(true, false, false);

    private final boolean plantableByDefault;
    private final boolean blocksHorizontalProjectiles;
    private final boolean requiresWaterCapablePlant;

    TerrainType(
            boolean plantableByDefault,
            boolean blocksHorizontalProjectiles,
            boolean requiresWaterCapablePlant
    ) {
        this.plantableByDefault = plantableByDefault;
        this.blocksHorizontalProjectiles = blocksHorizontalProjectiles;
        this.requiresWaterCapablePlant = requiresWaterCapablePlant;
    }

    public boolean isPlantableByDefault() {
        return plantableByDefault;
    }

    public boolean blocksHorizontalProjectiles() {
        return blocksHorizontalProjectiles;
    }

    public boolean requiresWaterCapablePlant() {
        return requiresWaterCapablePlant;
    }

    public boolean isSlippery() {
        return this == SLIPPERY_UP || this == SLIPPERY_DOWN;
    }

    /** Row delta a slippery tile pushes a zombie: -1 up, +1 down, 0 otherwise. */
    public int slipperyRowDelta() {
        if (this == SLIPPERY_UP) {
            return -1;
        }

        if (this == SLIPPERY_DOWN) {
            return 1;
        }

        return 0;
    }
}
