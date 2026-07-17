package model.miniGame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Immutable definition of one mandatory minigame and its three levels. */
public final class MiniGameDefinition {
    private final MiniGameId id;
    private final MiniGameId unlocksAfterCompletion;
    private final List<MiniGameLevelConfig> levels;

    public MiniGameDefinition(
            MiniGameId id,
            MiniGameId unlocksAfterCompletion,
            List<MiniGameLevelConfig> levels
    ) {
        if (id == null || levels == null || levels.size() != 3) {
            throw new IllegalArgumentException("A mandatory minigame must define exactly three levels.");
        }
        List<MiniGameLevelConfig> sorted = new ArrayList<>(levels);
        sorted.sort(Comparator.comparingInt(MiniGameLevelConfig::getLevelNumber));
        for (int index = 0; index < sorted.size(); index++) {
            MiniGameLevelConfig config = sorted.get(index);
            if (config.getMiniGameId() != id || config.getLevelNumber() != index + 1) {
                throw new IllegalArgumentException("Minigame levels must be numbered 1 through 3.");
            }
            if (index > 0 && !config.isHarderThan(sorted.get(index - 1))) {
                throw new IllegalArgumentException("Each minigame level must be explicitly harder.");
            }
        }
        this.id = id;
        this.unlocksAfterCompletion = unlocksAfterCompletion;
        this.levels = List.copyOf(sorted);
    }

    public MiniGameId getId() {
        return id;
    }

    public String getDisplayName() {
        return id.getDisplayName();
    }

    /** The next minigame unlocked after all three levels are first completed. */
    public MiniGameId getUnlocksAfterCompletion() {
        return unlocksAfterCompletion;
    }

    public List<MiniGameLevelConfig> getLevels() {
        return levels;
    }

    public MiniGameLevelConfig getLevel(int levelNumber) {
        if (levelNumber < 1 || levelNumber > levels.size()) {
            return null;
        }
        return levels.get(levelNumber - 1);
    }
}
