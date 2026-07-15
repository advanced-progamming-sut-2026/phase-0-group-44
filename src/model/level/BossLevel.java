package model.level;

/** Phase-1 placeholder for a level-4 boss. It is visible but never playable. */
public final class BossLevel implements Level {
    private final AdventureLevelConfig config;

    public BossLevel(AdventureLevelConfig config) {
        if (config == null || !config.isBossDeferred()) {
            throw new IllegalArgumentException("BossLevel requires a deferred boss config.");
        }
        this.config = config;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public LevelSelectionRules getSelectionRules() {
        return config.getSelectionRules();
    }

    @Override
    public model.sim.wave.WaveConfig getWaveConfig() {
        return config.getWaveConfig();
    }

    @Override
    public AdventureLevelConfig getAdventureConfig() {
        return config;
    }
}
