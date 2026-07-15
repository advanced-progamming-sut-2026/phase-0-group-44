package model.sim.zombie;

import model.enums.ZombieType;
import model.inGame.zombie.ZombieDefinition;

/**
 * Immutable wave/runtime facts for a zombie kind. Canonical definitions are
 * attached when the registry supplies them; tests may still build lightweight
 * ad-hoc specs through the builder.
 */
public final class ZombieSpec {

    private final String name;
    private final int waveCost;
    private final int health;
    private final double speedTilesPerSecond;
    private final int eatDamagePerSecond;
    private final boolean boss;
    private final ZombieDefinition definition;

    private ZombieSpec(Builder builder) {
        this.name = builder.name;
        this.waveCost = builder.waveCost;
        this.health = builder.health;
        this.speedTilesPerSecond = builder.speedTilesPerSecond;
        this.eatDamagePerSecond = builder.eatDamagePerSecond;
        this.boss = builder.boss;
        this.definition = builder.definition;
    }

    public String getName() {
        return name;
    }

    public int getWaveCost() {
        return waveCost;
    }

    public int getHealth() {
        return health;
    }

    public double getSpeedTilesPerSecond() {
        return speedTilesPerSecond;
    }

    public int getEatDamagePerSecond() {
        return eatDamagePerSecond;
    }

    public boolean isBoss() {
        return boss;
    }

    public ZombieDefinition getDefinition() {
        return definition;
    }

    public ZombieType getType() {
        return definition == null ? ZombieType.NORMAL : definition.getType();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static ZombieSpec fromDefinition(ZombieDefinition definition) {
        return builder(definition.getName())
                .definition(definition)
                .waveCost(definition.getWaveCost())
                .health(definition.getHealth())
                .speedTilesPerSecond(definition.getSpeedTilesPerSecond())
                .eatDamagePerSecond(definition.getEatDamagePerSecond())
                .build();
    }

    public static final class Builder {
        private final String name;
        private int waveCost;
        private int health;
        private double speedTilesPerSecond;
        private int eatDamagePerSecond;
        private boolean boss;
        private ZombieDefinition definition;

        private Builder(String name) {
            this.name = name;
        }

        public Builder waveCost(int waveCost) {
            this.waveCost = waveCost;
            return this;
        }

        public Builder health(int health) {
            this.health = health;
            return this;
        }

        public Builder speedTilesPerSecond(double speed) {
            this.speedTilesPerSecond = speed;
            return this;
        }

        public Builder eatDamagePerSecond(int eatDamagePerSecond) {
            this.eatDamagePerSecond = eatDamagePerSecond;
            return this;
        }

        public Builder boss(boolean boss) {
            this.boss = boss;
            return this;
        }

        public Builder definition(ZombieDefinition definition) {
            this.definition = definition;
            return this;
        }

        public ZombieSpec build() {
            return new ZombieSpec(this);
        }
    }
}
