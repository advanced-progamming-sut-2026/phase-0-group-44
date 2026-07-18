package model.miniGame;

import model.enums.ZombieType;

import java.util.List;

/** Explicit three-level Wall-nut Bowling composition and pacing. */
public final class WallNutBowlingLevelRules {
    private final int level;
    private final int redLineColumn;
    private final int conveyorCapacity;
    private final int initialPackets;
    private final int conveyorIntervalTicks;
    private final int zombieSpawnIntervalTicks;
    private final int dangerScore;
    private final int timeLimitTicks;
    private final double ballSpeedTilesPerSecond;
    private final List<BowlingPlantType> conveyorPattern;
    private final List<ZombieType> zombieSequence;

    public WallNutBowlingLevelRules(
            int level,
            int redLineColumn,
            int conveyorCapacity,
            int initialPackets,
            int conveyorIntervalTicks,
            int zombieSpawnIntervalTicks,
            int dangerScore,
            int timeLimitTicks,
            double ballSpeedTilesPerSecond,
            List<BowlingPlantType> conveyorPattern,
            List<ZombieType> zombieSequence
    ) {
        if (level < 1 || level > 3 || redLineColumn < 0 || conveyorCapacity <= 0
                || initialPackets < 0 || initialPackets > conveyorCapacity
                || conveyorIntervalTicks <= 0 || zombieSpawnIntervalTicks <= 0
                || dangerScore <= 0 || timeLimitTicks <= 0 || ballSpeedTilesPerSecond <= 0
                || conveyorPattern == null || conveyorPattern.isEmpty()
                || zombieSequence == null || zombieSequence.isEmpty()) {
            throw new IllegalArgumentException("Invalid Wall-nut Bowling level rules.");
        }
        this.level = level;
        this.redLineColumn = redLineColumn;
        this.conveyorCapacity = conveyorCapacity;
        this.initialPackets = initialPackets;
        this.conveyorIntervalTicks = conveyorIntervalTicks;
        this.zombieSpawnIntervalTicks = zombieSpawnIntervalTicks;
        this.dangerScore = dangerScore;
        this.timeLimitTicks = timeLimitTicks;
        this.ballSpeedTilesPerSecond = ballSpeedTilesPerSecond;
        this.conveyorPattern = List.copyOf(conveyorPattern);
        this.zombieSequence = List.copyOf(zombieSequence);
    }

    public int getLevel() {
        return level;
    }

    public int getRedLineColumn() {
        return redLineColumn;
    }

    public int getConveyorCapacity() {
        return conveyorCapacity;
    }

    public int getInitialPackets() {
        return initialPackets;
    }

    public int getConveyorIntervalTicks() {
        return conveyorIntervalTicks;
    }

    public int getZombieSpawnIntervalTicks() {
        return zombieSpawnIntervalTicks;
    }

    public int getDangerScore() {
        return dangerScore;
    }

    public int getTimeLimitTicks() {
        return timeLimitTicks;
    }

    public double getBallSpeedTilesPerSecond() {
        return ballSpeedTilesPerSecond;
    }

    public List<BowlingPlantType> getConveyorPattern() {
        return conveyorPattern;
    }

    public List<ZombieType> getZombieSequence() {
        return zombieSequence;
    }

    public int getZombieCount() {
        return zombieSequence.size();
    }

    public boolean isHarderThan(WallNutBowlingLevelRules previous) {
        return previous != null
                && level == previous.level + 1
                && redLineColumn <= previous.redLineColumn
                && initialPackets < previous.initialPackets
                && conveyorIntervalTicks > previous.conveyorIntervalTicks
                && zombieSpawnIntervalTicks < previous.zombieSpawnIntervalTicks
                && dangerScore > previous.dangerScore
                && timeLimitTicks < previous.timeLimitTicks
                && getZombieCount() > previous.getZombieCount();
    }
}
