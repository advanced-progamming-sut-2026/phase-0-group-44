package model.miniGame;

import java.util.List;

/** Explicit finite-wave and pacing configuration for one Zombotany level. */
public final class ZombotanyLevelRules {
    private final int level;
    private final int zombieCount;
    private final int spawnIntervalTicks;
    private final int dangerScore;
    private final int timeLimitTicks;
    private final int selectionCapacity;
    private final List<ZombotanyTrait> sequence;

    public ZombotanyLevelRules(
            int level,
            int zombieCount,
            int spawnIntervalTicks,
            int dangerScore,
            int timeLimitTicks,
            int selectionCapacity,
            List<ZombotanyTrait> sequence
    ) {
        if (level < 1 || level > 3 || zombieCount <= 0 || spawnIntervalTicks <= 0
                || dangerScore <= 0 || timeLimitTicks <= 0 || selectionCapacity <= 0
                || sequence == null || sequence.isEmpty()
                || !sequence.containsAll(List.of(
                        ZombotanyTrait.PEASHOOTER,
                        ZombotanyTrait.WALL_NUT,
                        ZombotanyTrait.JALAPENO,
                        ZombotanyTrait.SQUASH))) {
            throw new IllegalArgumentException("Invalid Zombotany level rules.");
        }
        this.level = level;
        this.zombieCount = zombieCount;
        this.spawnIntervalTicks = spawnIntervalTicks;
        this.dangerScore = dangerScore;
        this.timeLimitTicks = timeLimitTicks;
        this.selectionCapacity = selectionCapacity;
        this.sequence = List.copyOf(sequence);
    }

    public int getLevel() { return level; }
    public int getZombieCount() { return zombieCount; }
    public int getSpawnIntervalTicks() { return spawnIntervalTicks; }
    public int getDangerScore() { return dangerScore; }
    public int getTimeLimitTicks() { return timeLimitTicks; }
    public int getSelectionCapacity() { return selectionCapacity; }
    public List<ZombotanyTrait> getSequence() { return sequence; }

    public boolean isHarderThan(ZombotanyLevelRules previous) {
        return previous != null
                && level == previous.level + 1
                && zombieCount > previous.zombieCount
                && spawnIntervalTicks < previous.spawnIntervalTicks
                && dangerScore > previous.dangerScore
                && timeLimitTicks < previous.timeLimitTicks;
    }
}
