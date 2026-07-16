package model.miniGame.framework;

import java.util.List;

public final class MinigameDefinition {
    private final MinigameId id;
    private final List<MinigameLevelConfig> levels;
    public MinigameDefinition(MinigameId id, List<MinigameLevelConfig> levels) {
        if (id == null || levels == null || levels.size() != 3) {
            throw new IllegalArgumentException("A mandatory minigame requires exactly three levels.");
        }
        this.id = id;
        this.levels = List.copyOf(levels);
        for (int i = 0; i < 3; i++) {
            if (levels.get(i).getLevel() != i + 1) throw new IllegalArgumentException("Levels must be ordered 1..3.");
        }
    }
    public MinigameId getId() { return id; }
    public List<MinigameLevelConfig> getLevels() { return levels; }
    public MinigameLevelConfig level(int level) {
        if (level < 1 || level > 3) throw new IllegalArgumentException("Minigame level must be 1, 2, or 3.");
        return levels.get(level - 1);
    }
}
