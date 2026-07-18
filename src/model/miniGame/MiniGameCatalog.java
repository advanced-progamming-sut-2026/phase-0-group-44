package model.miniGame;

import model.sim.SimulationWorld;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Catalog of all implemented Phase-1 three-level minigames. */
public final class MiniGameCatalog {
    private final Map<MiniGameId, MiniGameDefinition> definitions;

    public MiniGameCatalog(List<MiniGameDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("At least one minigame is required.");
        }
        Map<MiniGameId, MiniGameDefinition> indexed = new EnumMap<>(MiniGameId.class);
        for (MiniGameDefinition definition : definitions) {
            if (definition == null || indexed.put(definition.getId(), definition) != null) {
                throw new IllegalArgumentException("Duplicate or missing minigame definition.");
            }
        }
        this.definitions = Map.copyOf(indexed);
    }

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public static MiniGameCatalog phaseOneDefaults() {
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
                MiniGameId.BEGHOULED,
                List.of(
                        iZombieConfig(1, rows, columns),
                        iZombieConfig(2, rows, columns),
                        iZombieConfig(3, rows, columns)
                )
        );
        MiniGameDefinition beghouled = new MiniGameDefinition(
                MiniGameId.BEGHOULED,
                MiniGameId.ZOMBOTANY,
                List.of(
                        beghouledConfig(1, rows, columns),
                        beghouledConfig(2, rows, columns),
                        beghouledConfig(3, rows, columns)
                )
        );
        MiniGameDefinition zombotany = new MiniGameDefinition(
                MiniGameId.ZOMBOTANY,
                null,
                List.of(
                        zombotanyConfig(1, rows, columns),
                        zombotanyConfig(2, rows, columns),
                        zombotanyConfig(3, rows, columns)
                )
        );
        return new MiniGameCatalog(List.of(
                vaseBreaker, bowling, iZombie, beghouled, zombotany));
    }

    /** Compatibility alias retained for callers created before bonus scope was reconciled. */
    @Deprecated
    public static MiniGameCatalog mandatoryDefaults() {
        return phaseOneDefaults();
    }



    private static MiniGameLevelConfig zombotanyConfig(int level, int rows, int columns) {
        ZombotanyLevelRules rules = Zombotany.rulesFor(level);
        return new MiniGameLevelConfig(
                MiniGameId.ZOMBOTANY,
                level,
                level,
                rules.getZombieCount(),
                rules.getDangerScore(),
                50,
                rules.getTimeLimitTicks(),
                rows,
                columns,
                true
        );
    }

    private static MiniGameLevelConfig beghouledConfig(int level, int rows, int columns) {
        BeghouledLevelRules rules = Beghouled.rulesFor(level);
        return new MiniGameLevelConfig(
                MiniGameId.BEGHOULED,
                level,
                level,
                rules.getTargetMatches(),
                rules.getDangerScore(),
                0,
                rules.getTimeLimitTicks(),
                rows,
                columns,
                false
        );
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
