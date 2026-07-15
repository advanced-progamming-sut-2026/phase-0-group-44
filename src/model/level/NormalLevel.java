package model.level;

/**
 * A standard adventure level. Its selection rules default to a normal level's
 * (8 slots, all owned plants) unless a specific configuration is supplied.
 */
public class NormalLevel implements Level {

    /** A placeholder default until level configs exist: 3 waves, first costs 100. */
    private static final model.sim.wave.WaveConfig DEFAULT_WAVES =
            model.sim.wave.WaveConfig.of(3, 100);

    private final String name;
    private final LevelSelectionRules selectionRules;
    private final model.sim.wave.WaveConfig waveConfig;

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
}
