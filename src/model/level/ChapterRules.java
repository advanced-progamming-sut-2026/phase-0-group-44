package model.level;

import model.config.GameWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable, data-driven chapter mechanics shared by every level in one world.
 * Empty/default values mean that a chapter does not use that mechanic.
 */
public final class ChapterRules {
    private final GameWorld world;
    private final List<TileCoordinate> initialGraves;
    private final boolean finalWaveTornadoes;
    private final int icyWindRowsPerWave;
    private final Set<TileCoordinate> slipperyUpTiles;
    private final Set<TileCoordinate> slipperyDownTiles;
    private final List<FrozenZombiePlacement> frozenZombies;
    private final int maximumWaterColumns;
    private final int[] waterColumnsByWave;
    private final Set<TileCoordinate> lowTideTiles;
    private final int darkGravesPerWave;
    private final Set<TileCoordinate> necromancyTiles;

    private ChapterRules(Builder builder) {
        this.world = builder.world;
        this.initialGraves = List.copyOf(builder.initialGraves);
        this.finalWaveTornadoes = builder.finalWaveTornadoes;
        this.icyWindRowsPerWave = builder.icyWindRowsPerWave;
        this.slipperyUpTiles = Collections.unmodifiableSet(
                new LinkedHashSet<>(builder.slipperyUpTiles));
        this.slipperyDownTiles = Collections.unmodifiableSet(
                new LinkedHashSet<>(builder.slipperyDownTiles));
        this.frozenZombies = List.copyOf(builder.frozenZombies);
        this.maximumWaterColumns = builder.maximumWaterColumns;
        this.waterColumnsByWave = builder.waterColumnsByWave.clone();
        this.lowTideTiles = Collections.unmodifiableSet(
                new LinkedHashSet<>(builder.lowTideTiles));
        this.darkGravesPerWave = builder.darkGravesPerWave;
        this.necromancyTiles = Collections.unmodifiableSet(
                new LinkedHashSet<>(builder.necromancyTiles));
    }

    public static Builder builder(GameWorld world) {
        return new Builder(world);
    }

    public GameWorld getWorld() {
        return world;
    }

    public List<TileCoordinate> getInitialGraves() {
        return initialGraves;
    }

    public boolean hasFinalWaveTornadoes() {
        return finalWaveTornadoes;
    }

    public int getIcyWindRowsPerWave() {
        return icyWindRowsPerWave;
    }

    public Set<TileCoordinate> getSlipperyUpTiles() {
        return slipperyUpTiles;
    }

    public Set<TileCoordinate> getSlipperyDownTiles() {
        return slipperyDownTiles;
    }

    public List<FrozenZombiePlacement> getFrozenZombies() {
        return frozenZombies;
    }

    public int getMaximumWaterColumns() {
        return maximumWaterColumns;
    }

    public int waterColumnsForWave(int waveNumber) {
        if (waterColumnsByWave.length == 0) {
            return 0;
        }
        int index = Math.max(0, Math.min(waterColumnsByWave.length - 1, waveNumber - 1));
        return Math.min(maximumWaterColumns, waterColumnsByWave[index]);
    }

    public Set<TileCoordinate> getLowTideTiles() {
        return lowTideTiles;
    }

    public int getDarkGravesPerWave() {
        return darkGravesPerWave;
    }

    public Set<TileCoordinate> getNecromancyTiles() {
        return necromancyTiles;
    }

    public static final class Builder {
        private final GameWorld world;
        private final List<TileCoordinate> initialGraves = new ArrayList<>();
        private boolean finalWaveTornadoes;
        private int icyWindRowsPerWave;
        private final Set<TileCoordinate> slipperyUpTiles = new LinkedHashSet<>();
        private final Set<TileCoordinate> slipperyDownTiles = new LinkedHashSet<>();
        private final List<FrozenZombiePlacement> frozenZombies = new ArrayList<>();
        private int maximumWaterColumns;
        private int[] waterColumnsByWave = new int[0];
        private final Set<TileCoordinate> lowTideTiles = new LinkedHashSet<>();
        private int darkGravesPerWave;
        private final Set<TileCoordinate> necromancyTiles = new LinkedHashSet<>();

        private Builder(GameWorld world) {
            if (world == null) {
                throw new IllegalArgumentException("Chapter world is required.");
            }
            this.world = world;
        }

        public Builder initialGrave(int x, int y) {
            initialGraves.add(new TileCoordinate(x, y));
            return this;
        }

        public Builder finalWaveTornadoes() {
            finalWaveTornadoes = true;
            return this;
        }

        public Builder icyWindRowsPerWave(int count) {
            icyWindRowsPerWave = Math.max(0, count);
            return this;
        }

        public Builder slipperyUp(int x, int y) {
            slipperyUpTiles.add(new TileCoordinate(x, y));
            return this;
        }

        public Builder slipperyDown(int x, int y) {
            slipperyDownTiles.add(new TileCoordinate(x, y));
            return this;
        }

        public Builder frozenZombie(FrozenZombiePlacement placement) {
            frozenZombies.add(placement);
            return this;
        }

        public Builder waterSchedule(int maximumColumns, int... columnsByWave) {
            maximumWaterColumns = Math.max(0, maximumColumns);
            waterColumnsByWave = columnsByWave == null ? new int[0] : columnsByWave.clone();
            return this;
        }

        public Builder lowTide(int x, int y) {
            lowTideTiles.add(new TileCoordinate(x, y));
            return this;
        }

        public Builder darkGravesPerWave(int count) {
            darkGravesPerWave = Math.max(0, count);
            return this;
        }

        public Builder necromancy(int x, int y) {
            necromancyTiles.add(new TileCoordinate(x, y));
            return this;
        }

        public ChapterRules build() {
            return new ChapterRules(this);
        }
    }
}
