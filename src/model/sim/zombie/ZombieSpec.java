package model.sim.zombie;

/**
 * The gameplay facts about a zombie kind: its name, wave cost, health, movement
 * speed, how fast it eats, and whether it is a boss.
 *
 * <p>These come from the zombie data layer once it loads; until then a
 * {@link ZombieSpecSource} supplies them, so the combat and wave logic run
 * against explicit, injectable values rather than hardcoded numbers.</p>
 */
public final class ZombieSpec {

    private final String name;
    private final int waveCost;
    private final int health;
    private final double speedTilesPerSecond;
    private final int eatDamagePerSecond;
    private final boolean boss;

    private ZombieSpec(Builder builder) {
        this.name = builder.name;
        this.waveCost = builder.waveCost;
        this.health = builder.health;
        this.speedTilesPerSecond = builder.speedTilesPerSecond;
        this.eatDamagePerSecond = builder.eatDamagePerSecond;
        this.boss = builder.boss;
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

    public static Builder builder(String name) {
        return new Builder(name);
    }

    /** Fluent builder; numeric fields default to 0 and boss to false. */
    public static final class Builder {
        private final String name;
        private int waveCost;
        private int health;
        private double speedTilesPerSecond;
        private int eatDamagePerSecond;
        private boolean boss;

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

        public ZombieSpec build() {
            return new ZombieSpec(this);
        }
    }
}
