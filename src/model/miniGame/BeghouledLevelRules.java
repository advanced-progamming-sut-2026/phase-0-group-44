package model.miniGame;

import model.enums.PlantType;
import model.enums.ZombieType;

import java.util.List;

/** Explicit board, match target, wave pressure, and upgrade availability. */
public final class BeghouledLevelRules {
    private final int level;
    private final int targetMatches;
    private final int zombieSpawnIntervalTicks;
    private final int dangerScore;
    private final int timeLimitTicks;
    private final List<PlantType> plantPool;
    private final List<ZombieType> zombieSequence;

    public BeghouledLevelRules(
            int level,
            int targetMatches,
            int zombieSpawnIntervalTicks,
            int dangerScore,
            int timeLimitTicks,
            List<PlantType> plantPool,
            List<ZombieType> zombieSequence
    ) {
        if (level < 1 || level > 3 || targetMatches <= 0
                || zombieSpawnIntervalTicks <= 0 || dangerScore <= 0
                || timeLimitTicks <= 0 || plantPool == null || plantPool.size() != 5
                || plantPool.stream().distinct().count() != 5
                || zombieSequence == null || zombieSequence.isEmpty()) {
            throw new IllegalArgumentException("Invalid Beghouled level rules.");
        }
        this.level = level;
        this.targetMatches = targetMatches;
        this.zombieSpawnIntervalTicks = zombieSpawnIntervalTicks;
        this.dangerScore = dangerScore;
        this.timeLimitTicks = timeLimitTicks;
        this.plantPool = List.copyOf(plantPool);
        this.zombieSequence = List.copyOf(zombieSequence);
    }

    public int getLevel() { return level; }
    public int getTargetMatches() { return targetMatches; }
    public int getZombieSpawnIntervalTicks() { return zombieSpawnIntervalTicks; }
    public int getDangerScore() { return dangerScore; }
    public int getTimeLimitTicks() { return timeLimitTicks; }
    public List<PlantType> getPlantPool() { return plantPool; }
    public List<ZombieType> getZombieSequence() { return zombieSequence; }

    public boolean isHarderThan(BeghouledLevelRules previous) {
        return previous != null
                && level == previous.level + 1
                && targetMatches > previous.targetMatches
                && zombieSpawnIntervalTicks < previous.zombieSpawnIntervalTicks
                && dangerScore > previous.dangerScore
                && timeLimitTicks < previous.timeLimitTicks;
    }
}
