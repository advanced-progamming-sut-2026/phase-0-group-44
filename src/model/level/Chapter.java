package model.level;

import model.config.GameWorld;

import java.util.List;

/** Immutable four-level Adventure chapter. */
public final class Chapter {
    private final GameWorld world;
    private final List<Level> levels;

    public Chapter(GameWorld world, List<Level> levels) {
        if (world == null || levels == null || levels.size() != 4) {
            throw new IllegalArgumentException("A chapter must contain exactly four levels.");
        }
        this.world = world;
        this.levels = List.copyOf(levels);
        for (int index = 0; index < this.levels.size(); index++) {
            Level level = this.levels.get(index);
            if (level.getWorld() != world || level.getLevelNumber() != index + 1) {
                throw new IllegalArgumentException("Chapter level identity is inconsistent.");
            }
        }
    }

    public GameWorld getWorld() {
        return world;
    }

    public List<Level> getLevels() {
        return levels;
    }

    public Level getLevel(int levelNumber) {
        if (levelNumber < 1 || levelNumber > levels.size()) {
            return null;
        }
        return levels.get(levelNumber - 1);
    }
}
