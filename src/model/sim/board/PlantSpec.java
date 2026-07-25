package model.sim.board;

import model.enums.PlantType;

/**
 * The gameplay-facing facts about a plant that planting and the board need: its
 * sun cost, recharge, health, and capabilities.
 *
 * <p>These come from the plant data layer once it loads. Until then a
 * {@link PlantSpecSource} supplies them, so the board mechanics are exercised
 * against explicit, injectable values rather than hardcoded per-plant numbers.</p>
 */
public final class PlantSpec {

    private final PlantType type;
    private final int sunCost;
    private final int rechargeTicks;
    private final int hp;
    private final boolean waterCapable;
    private final boolean providesSupport;
    private final boolean stacksOnSupport;
    private final boolean fire;
    private final boolean hasPlantFoodEffect;
    private int sunProductionAmount;      // 0 means "not a sun producer"
    private int productionIntervalTicks;

    private PlantSpec(Builder builder) {
        this.type = builder.type;
        this.sunCost = builder.sunCost;
        this.rechargeTicks = builder.rechargeTicks;
        this.hp = builder.hp;
        this.waterCapable = builder.waterCapable;
        this.providesSupport = builder.providesSupport;
        this.stacksOnSupport = builder.stacksOnSupport;
        this.fire = builder.fire;
        this.hasPlantFoodEffect = builder.hasPlantFoodEffect;
        this.sunProductionAmount = builder.sunProductionAmount;
        this.productionIntervalTicks = builder.productionIntervalTicks;
    }

    public int getSunProductionAmount() {
        return sunProductionAmount;
    }

    public int getProductionIntervalTicks() {
        return productionIntervalTicks;
    }

    public boolean isSunProducer() {
        return sunProductionAmount > 0 && productionIntervalTicks > 0;
    }

    public PlantType getType() {
        return type;
    }

    public int getSunCost() {
        return sunCost;
    }

    public int getRechargeTicks() {
        return rechargeTicks;
    }

    public int getHp() {
        return hp;
    }

    /** Whether the plant may be placed directly on water. */
    public boolean isWaterCapable() {
        return waterCapable;
    }

    /** Whether the plant is a support (a Lily Pad-style base another plant stacks on). */
    public boolean providesSupport() {
        return providesSupport;
    }

    /** Whether the plant may be stacked on top of a support. */
    public boolean stacksOnSupport() {
        return stacksOnSupport;
    }

    /** Whether the plant is a fire plant (melts adjacent ice). */
    public boolean isFire() {
        return fire;
    }

    public boolean hasPlantFoodEffect() {
        return hasPlantFoodEffect;
    }

    public static Builder builder(PlantType type) {
        return new Builder(type);
    }

    /** Fluent builder; every capability defaults to false. */
    public static final class Builder {
        private final PlantType type;
        private int sunCost;
        private int rechargeTicks;
        private int hp;
        private boolean waterCapable;
        private boolean providesSupport;
        private boolean stacksOnSupport;
        private boolean fire;
        private boolean hasPlantFoodEffect;
        private int sunProductionAmount;      // 0 means "not a sun producer"
        private int productionIntervalTicks;

        private Builder(PlantType type) {
            this.type = type;
        }

        public Builder sunProductionAmount(int amount) {
            this.sunProductionAmount = amount;
            return this;
        }

        public Builder productionIntervalTicks(int ticks) {
            this.productionIntervalTicks = ticks;
            return this;
        }

        public Builder sunCost(int sunCost) {
            this.sunCost = sunCost;
            return this;
        }

        public Builder rechargeTicks(int rechargeTicks) {
            this.rechargeTicks = rechargeTicks;
            return this;
        }

        public Builder hp(int hp) {
            this.hp = hp;
            return this;
        }

        public Builder waterCapable(boolean value) {
            this.waterCapable = value;
            return this;
        }

        public Builder providesSupport(boolean value) {
            this.providesSupport = value;
            return this;
        }

        public Builder stacksOnSupport(boolean value) {
            this.stacksOnSupport = value;
            return this;
        }

        public Builder fire(boolean value) {
            this.fire = value;
            return this;
        }

        public Builder hasPlantFoodEffect(boolean value) {
            this.hasPlantFoodEffect = value;
            return this;
        }

        public PlantSpec build() {
            return new PlantSpec(this);
        }
    }
}
