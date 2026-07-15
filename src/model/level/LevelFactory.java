package model.level;

/** Constructs the concrete Level implementation matching a canonical config. */
public final class LevelFactory {
    public Level create(AdventureLevelConfig config) {
        if (config.isBossDeferred()) {
            return new BossLevel(config);
        }
        if (config.isSpecial()) {
            return new SpecialLevel(config);
        }
        return new NormalLevel(config);
    }
}
