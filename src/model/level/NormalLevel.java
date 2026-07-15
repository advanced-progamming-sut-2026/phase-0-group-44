package model.level;

/** A standard level, either legacy/test-only or backed by Adventure config. */
public class NormalLevel implements Level {

    private static final model.sim.wave.WaveConfig DEFAULT_WAVES =
            model.sim.wave.WaveConfig.of(3, 100);

    private final String name;
    private final LevelSelectionRules selectionRules;
    private final model.sim.wave.WaveConfig waveConfig;
    private final AdventureLevelConfig adventureConfig;

    public NormalLevel(String name) {
        this(name, LevelSelectionRules.normal(), DEFAULT_WAVES);
    }

    public NormalLevel(String name, LevelSelectionRules selectionRules) {
        this(name, selectionRules, DEFAULT_WAVES);
    }

    public NormalLevel(
            String name,
            LevelSelectionRules selectionRules,
            model.sim.wave.WaveConfig waveConfig
    ) {
        this.name = name;
        this.selectionRules = selectionRules;
        this.waveConfig = waveConfig;
        this.adventureConfig = null;
    }

    public NormalLevel(AdventureLevelConfig config) {
        if (config == null || config.isSpecial() || config.isBossDeferred()) {
            throw new IllegalArgumentException("NormalLevel requires a normal Adventure config.");
        }
        this.name = config.getName();
        this.selectionRules = config.getSelectionRules();
        this.waveConfig = config.getWaveConfig();
        this.adventureConfig = config;
    }

    @Override
    public model.sim.wave.WaveConfig getWaveConfig() {
        return waveConfig;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LevelSelectionRules getSelectionRules() {
        return selectionRules;
    }

    @Override
    public AdventureLevelConfig getAdventureConfig() {
        return adventureConfig;
    }
}
