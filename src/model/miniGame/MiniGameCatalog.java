package model.miniGame;

import model.sim.SimulationWorld;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Mandatory minigame catalog. Beghouled and Zombotany are intentionally not
 * registered because they are bonus modes outside this implementation.
 */
public final class MiniGameCatalog {
    private final Map<MiniGameId, MiniGameDefinition> definitions;

    public MiniGameCatalog(List<MiniGameDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("At least one mandatory minigame is required.");
        }
        Map<MiniGameId, MiniGameDefinition> indexed = new EnumMap<>(MiniGameId.class);
        for (MiniGameDefinition definition : definitions) {
            if (definition == null || indexed.put(definition.getId(), definition) != null) {
                throw new IllegalArgumentException("Duplicate or missing minigame definition.");
            }
        }
        this.definitions = Map.copyOf(indexed);
    }

    public static MiniGameCatalog mandatoryDefaults() {
        int rows = SimulationWorld.DEFAULT_ROWS;
        int columns = SimulationWorld.DEFAULT_COLUMNS;

        MiniGameDefinition vaseBreaker = new MiniGameDefinition(
                MiniGameId.VASE_BREAKER,
                MiniGameId.BOWLING_WALLNUT,
                List.of(
                        config(MiniGameId.VASE_BREAKER, 1, 8, 2, 0, 2400, rows, columns),
                        config(MiniGameId.VASE_BREAKER, 2, 12, 9, 0, 2100, rows, columns),
                        config(MiniGameId.VASE_BREAKER, 3, 16, 16, 0, 1800, rows, columns)
                )
        );
        MiniGameDefinition bowling = new MiniGameDefinition(
                MiniGameId.BOWLING_WALLNUT,
                MiniGameId.I_ZOMBIE,
                List.of(
                        bowlingConfig(1, rows, columns),
                        bowlingConfig(2, rows, columns),
                        bowlingConfig(3, rows, columns)
                )
        );
        MiniGameDefinition iZombie = new MiniGameDefinition(
                MiniGameId.I_ZOMBIE,
                null,
                List.of(
                        iZombieConfig(1, rows, columns),
                        iZombieConfig(2, rows, columns),
                        iZombieConfig(3, rows, columns)
                )
        );
        return new MiniGameCatalog(List.of(vaseBreaker, bowling, iZombie));
    }

    private static MiniGameLevelConfig iZombieConfig(int level, int rows, int columns) {
        IZombieLevelRules rules = IZombie.rulesFor(level);
        return new MiniGameLevelConfig(
                MiniGameId.I_ZOMBIE,
                level,
                level,
                rules.getPreplacedPlantCount(),
                rules.getPlantPressure(),
                150,
                rules.getTimeLimitTicks(),
                rows,
                columns,
                false
        );
    }

    private static MiniGameLevelConfig bowlingConfig(int level, int rows, int columns) {
        WallNutBowlingLevelRules rules = BowlingWallnut.rulesFor(level);
        return new MiniGameLevelConfig(
                MiniGameId.BOWLING_WALLNUT,
                level,
                level,
                rules.getZombieCount(),
                rules.getDangerScore(),
                0,
                rules.getTimeLimitTicks(),
                rows,
                columns,
                false
        );
    }

    private static MiniGameLevelConfig config(
            MiniGameId id,
            int level,
            int objective,
            int threat,
            int resources,
            int ticks,
            int rows,
            int columns
    ) {
        return new MiniGameLevelConfig(
                id, level, level, objective, threat, resources,
                ticks, rows, columns, false
        );
    }

    public List<MiniGameDefinition> getDefinitions() {
        return definitions.values().stream()
                .sorted((left, right) -> Integer.compare(left.getId().ordinal(), right.getId().ordinal()))
                .toList();
    }

    public MiniGameDefinition find(MiniGameId id) {
        return id == null ? null : definitions.get(id);
    }

    public MiniGameDefinition find(String token) {
        return find(MiniGameId.fromToken(token));
    }
}
