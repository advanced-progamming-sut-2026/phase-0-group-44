package model.level;

import model.config.GameWorld;

import java.util.ArrayList;
import java.util.List;

/** Builds one chapter from its four data-driven level configurations. */
public final class ChapterFactory {
    private final LevelFactory levelFactory;

    public ChapterFactory() {
        this(new LevelFactory());
    }

    public ChapterFactory(LevelFactory levelFactory) {
        this.levelFactory = levelFactory;
    }

    public Chapter create(GameWorld world, List<AdventureLevelConfig> configs) {
        List<Level> levels = new ArrayList<>();
        for (AdventureLevelConfig config : configs) {
            levels.add(levelFactory.create(config));
        }
        return new Chapter(world, levels);
    }
}
