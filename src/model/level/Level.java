package model.level;

import model.config.GameWorld;
import model.sim.wave.WaveConfig;

/** A playable or explicitly deferred Adventure level. */
public interface Level {

    String getName();

    LevelSelectionRules getSelectionRules();

    WaveConfig getWaveConfig();

    /** Adventure metadata, or {@code null} for a legacy/test level. */
    default AdventureLevelConfig getAdventureConfig() {
        return null;
    }

    default GameWorld getWorld() {
        AdventureLevelConfig config = getAdventureConfig();
        return config == null ? null : config.getWorld();
    }

    default int getLevelNumber() {
        AdventureLevelConfig config = getAdventureConfig();
        return config == null ? 0 : config.getLevelNumber();
    }

    default boolean isBossDeferred() {
        AdventureLevelConfig config = getAdventureConfig();
        return config != null && config.isBossDeferred();
    }
}
