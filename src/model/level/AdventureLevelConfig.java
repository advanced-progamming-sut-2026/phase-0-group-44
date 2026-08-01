package model.level;

import model.config.GameWorld;
import model.enums.PlantCategory;
import model.enums.PlantType;
import model.sim.TickContext;
import model.sim.wave.WaveConfig;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Immutable canonical configuration for one Adventure level. */
public final class AdventureLevelConfig {
    public static final int DEFAULT_STARTING_SUN = 50;

    private final GameWorld world;
    private final int levelNumber;
    private final String name;
    private final SpecialLevelType specialType;
    private final boolean bossDeferred;
    private final LevelSelectionRules selectionRules;
    private final WaveConfig waveConfig;
    private final ChapterRules chapterRules;
    private final boolean skySunEnabled;
    private final int startingSun;
    private final boolean wavesInitiallyPaused;
    private final int conveyorIntervalTicks;
    private final Set<PlantType> conveyorCandidates;
    private final List<ProtectedPlantPlacement> protectedPlants;
    private final TimedWarObjective timedWarObjective;
    private final int timedWarTarget;
    private final int timedWarTicks;
    private final int deadLineColumn;
    private final int maximumPlantLosses;
    private final Set<PlantCategory> excludedPlantCategories;

    private AdventureLevelConfig(Builder builder) {
        world = builder.world;
        levelNumber = builder.levelNumber;
        name = builder.name;
        specialType = builder.specialType;
        bossDeferred = builder.bossDeferred;
        selectionRules = builder.selectionRules;
        waveConfig = builder.waveConfig;
        chapterRules = builder.chapterRules;
        skySunEnabled = builder.skySunEnabled;
        startingSun = builder.startingSun;
        wavesInitiallyPaused = builder.wavesInitiallyPaused;
        conveyorIntervalTicks = builder.conveyorIntervalTicks;
        conveyorCandidates = Collections.unmodifiableSet(
                new LinkedHashSet<>(builder.conveyorCandidates));
        protectedPlants = List.copyOf(builder.protectedPlants);
        timedWarObjective = builder.timedWarObjective;
        timedWarTarget = builder.timedWarTarget;
        timedWarTicks = builder.timedWarTicks;
        deadLineColumn = builder.deadLineColumn;
        maximumPlantLosses = builder.maximumPlantLosses;
        excludedPlantCategories = builder.excludedPlantCategories.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(builder.excludedPlantCategories));
    }

    public static Builder builder(GameWorld world, int levelNumber, String name) {
        return new Builder(world, levelNumber, name);
    }

    public GameWorld getWorld() {
        return world;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public String getName() {
        return name;
    }

    public SpecialLevelType getSpecialType() {
        return specialType;
    }

    public boolean isSpecial() {
        return specialType != null;
    }

    public boolean isBossDeferred() {
        return bossDeferred;
    }

    public LevelSelectionRules getSelectionRules() {
        return selectionRules;
    }

    public WaveConfig getWaveConfig() {
        return waveConfig;
    }

    public ChapterRules getChapterRules() {
        return chapterRules;
    }

    public boolean isSkySunEnabled() {
        return skySunEnabled;
    }

    public int getStartingSun() {
        return startingSun;
    }

    public boolean areWavesInitiallyPaused() {
        return wavesInitiallyPaused;
    }

    public int getConveyorIntervalTicks() {
        return conveyorIntervalTicks;
    }

    public Set<PlantType> getConveyorCandidates() {
        return conveyorCandidates;
    }

    public List<ProtectedPlantPlacement> getProtectedPlants() {
        return protectedPlants;
    }

    public TimedWarObjective getTimedWarObjective() {
        return timedWarObjective;
    }

    public int getTimedWarTarget() {
        return timedWarTarget;
    }

    public int getTimedWarTicks() {
        return timedWarTicks;
    }

    public int getDeadLineColumn() {
        return deadLineColumn;
    }

    public int getMaximumPlantLosses() {
        return maximumPlantLosses;
    }

    public static final class Builder {
        private final GameWorld world;
        private final int levelNumber;
        private final String name;
        private SpecialLevelType specialType;
        private boolean bossDeferred;
        private LevelSelectionRules selectionRules = LevelSelectionRules.normal();
        private WaveConfig waveConfig = WaveConfig.of(3, 100);
        private ChapterRules chapterRules;
        private boolean skySunEnabled = true;
        private int startingSun = DEFAULT_STARTING_SUN;
        private boolean wavesInitiallyPaused;
        private int conveyorIntervalTicks = 12 * TickContext.TICKS_PER_SECOND;
        private final Set<PlantType> conveyorCandidates = new LinkedHashSet<>();
        private List<ProtectedPlantPlacement> protectedPlants = List.of();
        private TimedWarObjective timedWarObjective;
        private int timedWarTarget;
        private int timedWarTicks;
        private int deadLineColumn = -1;
        private int maximumPlantLosses = -1;
        private final Set<PlantCategory> excludedPlantCategories =
                EnumSet.noneOf(PlantCategory.class);

        private Builder(GameWorld world, int levelNumber, String name) {
            if (world == null || levelNumber < 1 || levelNumber > 4
                    || name == null || name.isBlank()) {
                throw new IllegalArgumentException("Invalid Adventure level identity.");
            }
            this.world = world;
            this.levelNumber = levelNumber;
            this.name = name;
        }

        public Builder special(SpecialLevelType type) {
            specialType = type;
            return this;
        }

        public Builder bossDeferred() {
            bossDeferred = true;
            return this;
        }

        public Builder selectionRules(LevelSelectionRules rules) {
            selectionRules = rules;
            return this;
        }

        public Builder waveConfig(WaveConfig config) {
            waveConfig = config;
            return this;
        }

        public Builder chapterRules(ChapterRules rules) {
            chapterRules = rules;
            return this;
        }

        public Builder skySunEnabled(boolean enabled) {
            skySunEnabled = enabled;
            return this;
        }

        public Builder startingSun(int amount) {
            startingSun = Math.max(0, amount);
            return this;
        }

        public Builder wavesInitiallyPaused(boolean paused) {
            wavesInitiallyPaused = paused;
            return this;
        }

        public Builder conveyorCandidates(PlantType... types) {
            if (types != null) {
                Collections.addAll(conveyorCandidates, types);
            }
            return this;
        }


        public Builder protectedPlants(List<ProtectedPlantPlacement> placements) {
            protectedPlants = placements == null ? List.of() : List.copyOf(placements);
            return this;
        }

        public Builder timedWar(TimedWarObjective objective, int target, int seconds) {
            timedWarObjective = objective;
            timedWarTarget = Math.max(1, target);
            timedWarTicks = Math.max(1, seconds * TickContext.TICKS_PER_SECOND);
            return this;
        }

        public Builder deadLineColumn(int column) {
            deadLineColumn = column;
            return this;
        }

        public Builder maximumPlantLosses(int count) {
            maximumPlantLosses = Math.max(1, count);
            return this;
        }

        public Builder excludeCategory(PlantCategory category) {
            if (category != null) {
                excludedPlantCategories.add(category);
            }
            return this;
        }

        public AdventureLevelConfig build() {
            if (chapterRules == null) {
                chapterRules = ChapterRules.builder(world).build();
            }
            if (bossDeferred && levelNumber != 4) {
                throw new IllegalStateException("Only level 4 may be a deferred boss.");
            }
            return new AdventureLevelConfig(this);
        }
    }
}
