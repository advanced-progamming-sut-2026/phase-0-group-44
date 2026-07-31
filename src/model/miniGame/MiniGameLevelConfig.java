package model.miniGame;

/**
 * Explicit, data-only difficulty configuration for one mandatory minigame level.
 * Strategies interpret the generic challenge values without leaking mode checks
 * into the shared simulation.
 */
public final class MiniGameLevelConfig {
    private final MiniGameId miniGameId;
    private final int levelNumber;
    private final int difficultyRank;
    private final int objectiveTarget;
    private final int threatBudget;
    private final int startingResources;
    private final int timeLimitTicks;
    private final int rows;
    private final int columns;
    private final boolean skySunEnabled;

    public MiniGameLevelConfig(
            MiniGameId miniGameId,
            int levelNumber,
            int difficultyRank,
            int objectiveTarget,
            int threatBudget,
            int startingResources,
            int timeLimitTicks,
            int rows,
            int columns,
            boolean skySunEnabled
    ) {
        if (miniGameId == null || levelNumber < 1 || levelNumber > 3
                || difficultyRank < 1 || objectiveTarget <= 0 || threatBudget <= 0
                || startingResources < 0 || timeLimitTicks <= 0
                || rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("Invalid minigame level configuration.");
        }
        this.miniGameId = miniGameId;
        this.levelNumber = levelNumber;
        this.difficultyRank = difficultyRank;
        this.objectiveTarget = objectiveTarget;
        this.threatBudget = threatBudget;
        this.startingResources = startingResources;
        this.timeLimitTicks = timeLimitTicks;
        this.rows = rows;
        this.columns = columns;
        this.skySunEnabled = skySunEnabled;
    }

    public MiniGameId getMiniGameId() {
        return miniGameId;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public int getObjectiveTarget() {
        return objectiveTarget;
    }

    public int getThreatBudget() {
        return threatBudget;
    }

    public int getStartingResources() {
        return startingResources;
    }

    public int getTimeLimitTicks() {
        return timeLimitTicks;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public boolean isSkySunEnabled() {
        return skySunEnabled;
    }

    /** Strict monotonic difficulty contract used by the catalog and tests. */
    public boolean isHarderThan(MiniGameLevelConfig previous) {
        if (previous == null || miniGameId != previous.miniGameId) {
            return false;
        }
        return levelNumber == previous.levelNumber + 1
                && difficultyRank > previous.difficultyRank
                && objectiveTarget > previous.objectiveTarget
                && threatBudget > previous.threatBudget
                && startingResources <= previous.startingResources
                && timeLimitTicks < previous.timeLimitTicks;
    }
}
