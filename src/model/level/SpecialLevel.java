package model.level;

/** One of the eight mandatory special-level configurations. */
public final class SpecialLevel implements Level {
    private final AdventureLevelConfig config;

    public SpecialLevel(AdventureLevelConfig config) {
        if (config == null || !config.isSpecial() || config.isBossDeferred()) {
            throw new IllegalArgumentException("SpecialLevel requires a special config.");
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

    public SpecialLevelType getSpecialType() {
        return config.getSpecialType();
    }
}
