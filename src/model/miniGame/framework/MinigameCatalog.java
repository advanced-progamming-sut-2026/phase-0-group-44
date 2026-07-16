package model.miniGame.framework;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MinigameCatalog {
    private final Map<MinigameId, MinigameDefinition> definitions = new EnumMap<>(MinigameId.class);
    public MinigameCatalog() {
        register(MinigameId.BOWLING_WALLNUT, 4, 18, 1.00, 0);
        register(MinigameId.CONVEYOR_BELT, 5, 16, 1.05, 0);
        register(MinigameId.VASEBREAKER, 4, 20, 1.00, 50);
    }
    private void register(MinigameId id, int baseWaves, int baseInterval, double baseHealth, int baseSun) {
        definitions.put(id, new MinigameDefinition(id, List.of(
                new MinigameLevelConfig(1, baseWaves, baseInterval, baseHealth, baseSun),
                new MinigameLevelConfig(2, baseWaves + 2, Math.max(1, baseInterval - 3), baseHealth + .20, Math.max(0, baseSun - 25)),
                new MinigameLevelConfig(3, baseWaves + 4, Math.max(1, baseInterval - 6), baseHealth + .45, Math.max(0, baseSun - 50))
        )));
    }
    public MinigameDefinition get(MinigameId id) {
        MinigameDefinition value = definitions.get(id);
        if (value == null) throw new IllegalArgumentException("Unknown mandatory minigame.");
        return value;
    }
    public List<MinigameDefinition> all() { return List.copyOf(definitions.values()); }
}
