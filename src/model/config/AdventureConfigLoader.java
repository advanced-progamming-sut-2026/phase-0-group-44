package model.config;

import model.enums.PlantCategory;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.level.AdventureLevelConfig;
import model.level.Chapter;
import model.level.ChapterFactory;
import model.level.ChapterRules;
import model.level.FrozenZombiePlacement;
import model.level.LevelSelectionRules;
import model.level.ProtectedPlantPlacement;
import model.level.SpecialLevelType;
import model.level.TimedWarObjective;
import model.sim.wave.WaveConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Parses and validates the canonical Adventure CSV configuration files. */
final class AdventureConfigLoader {
    private final Path chapterPath;
    private final Path levelPath;

    AdventureConfigLoader(Path chapterPath, Path levelPath) {
        this.chapterPath = chapterPath;
        this.levelPath = levelPath;
    }

    Map<GameWorld, Chapter> load() {
        Map<GameWorld, ChapterRules> rules = loadChapterRules();
        Map<GameWorld, List<AdventureLevelConfig>> configs = loadLevelConfigs(rules);
        validate(configs);
        Map<GameWorld, Chapter> chapters = new EnumMap<>(GameWorld.class);
        ChapterFactory factory = new ChapterFactory();
        for (GameWorld world : GameWorld.values()) {
            chapters.put(world, factory.create(world, configs.get(world)));
        }
        return Collections.unmodifiableMap(chapters);
    }

    private Map<GameWorld, ChapterRules> loadChapterRules() {
        CsvTable table = read(chapterPath);
        Map<GameWorld, ChapterRules> result = new EnumMap<>(GameWorld.class);
        for (String[] row : table.rows) {
            GameWorld world = GameWorld.valueOf(table.value(row, "chapter"));
            ChapterRules.Builder builder = ChapterRules.builder(world);
            for (int[] coordinate : coordinates(table.value(row, "initial_graves"))) {
                builder.initialGrave(coordinate[0], coordinate[1]);
            }
            if (Boolean.parseBoolean(table.value(row, "final_wave_tornadoes"))) {
                builder.finalWaveTornadoes();
            }
            builder.icyWindRowsPerWave(integer(table.value(row, "icy_wind_rows"), 0));
            for (int[] coordinate : coordinates(table.value(row, "slippery_up"))) {
                builder.slipperyUp(coordinate[0], coordinate[1]);
            }
            for (int[] coordinate : coordinates(table.value(row, "slippery_down"))) {
                builder.slipperyDown(coordinate[0], coordinate[1]);
            }
            for (String token : tokens(table.value(row, "frozen_zombies"))) {
                String[] halves = token.split("@");
                String[] position = halves[1].split(":");
                builder.frozenZombie(new FrozenZombiePlacement(
                        ZombieType.valueOf(halves[0]),
                        Double.parseDouble(position[0]),
                        Integer.parseInt(position[1])));
            }
            int maximumWater = integer(table.value(row, "max_water_columns"), 0);
            builder.waterSchedule(maximumWater, integerArray(table.value(row, "water_schedule")));
            for (int[] coordinate : coordinates(table.value(row, "low_tide"))) {
                builder.lowTide(coordinate[0], coordinate[1]);
            }
            builder.darkGravesPerWave(integer(table.value(row, "dark_graves_per_wave"), 0));
            for (int[] coordinate : coordinates(table.value(row, "necromancy"))) {
                builder.necromancy(coordinate[0], coordinate[1]);
            }
            if (result.put(world, builder.build()) != null) {
                throw new IllegalStateException("Duplicate chapter row: " + world);
            }
        }
        if (result.size() != GameWorld.values().length) {
            throw new IllegalStateException("Adventure chapter CSV must define all four chapters.");
        }
        return result;
    }

    private Map<GameWorld, List<AdventureLevelConfig>> loadLevelConfigs(
            Map<GameWorld, ChapterRules> rules
    ) {
        CsvTable table = read(levelPath);
        Map<GameWorld, List<AdventureLevelConfig>> result = new EnumMap<>(GameWorld.class);
        for (String[] row : table.rows) {
            GameWorld world = GameWorld.valueOf(table.value(row, "chapter"));
            int number = Integer.parseInt(table.value(row, "level"));
            String kind = table.value(row, "kind");
            AdventureLevelConfig.Builder builder = AdventureLevelConfig.builder(
                            world, number, table.value(row, "name"))
                    .chapterRules(rules.get(world))
                    .skySunEnabled(Boolean.parseBoolean(table.value(row, "sky_sun")))
                    .startingSun(integer(table.value(row, "starting_sun"), 50))
                    .wavesInitiallyPaused(Boolean.parseBoolean(table.value(row, "waves_paused")))
                    .waveConfig(WaveConfig.of(
                            integer(table.value(row, "wave_count"), 3),
                            integer(table.value(row, "first_wave_cost"), 100)));

            if ("SPECIAL".equals(kind)) {
                builder.special(SpecialLevelType.valueOf(table.value(row, "special_type")));
            } else if ("BOSS".equals(kind)) {
                builder.bossDeferred();
            } else if (!"NORMAL".equals(kind)) {
                throw new IllegalStateException("Unknown level kind: " + kind);
            }

            Set<PlantType> forced = plantTypes(table.value(row, "forced_plants"));
            Set<PlantCategory> excluded = plantCategories(
                    table.value(row, "excluded_categories"));
            String selectionMode = table.value(row, "selection_mode");
            if ("BYPASSED".equals(selectionMode)) {
                builder.selectionRules(LevelSelectionRules.bypassed());
            } else if ("LOCKED".equals(selectionMode)) {
                builder.selectionRules(LevelSelectionRules.withForcedPlants(
                        LevelSelectionRules.DEFAULT_CAPACITY, forced, excluded));
            } else {
                builder.selectionRules(LevelSelectionRules.normal());
            }
            for (PlantCategory category : excluded) {
                builder.excludeCategory(category);
            }

            Set<PlantType> conveyor = plantTypes(table.value(row, "conveyor_candidates"));
            builder.conveyorCandidates(conveyor.toArray(PlantType[]::new));
            builder.protectedPlants(protectedPlants(table.value(row, "protected_plants")));

            String objective = table.value(row, "timed_objective");
            if (!objective.isBlank()) {
                builder.timedWar(
                        TimedWarObjective.valueOf(objective),
                        integer(table.value(row, "timed_target"), 1),
                        integer(table.value(row, "timed_seconds"), 1));
            }
            String deadLine = table.value(row, "dead_line");
            if (!deadLine.isBlank()) {
                builder.deadLineColumn(Integer.parseInt(deadLine));
            }
            String losses = table.value(row, "max_plant_losses");
            if (!losses.isBlank()) {
                builder.maximumPlantLosses(Integer.parseInt(losses));
            }

            result.computeIfAbsent(world, ignored -> new ArrayList<>()).add(builder.build());
        }
        for (List<AdventureLevelConfig> configs : result.values()) {
            configs.sort(java.util.Comparator.comparingInt(AdventureLevelConfig::getLevelNumber));
        }
        return result;
    }

    private void validate(Map<GameWorld, List<AdventureLevelConfig>> configs) {
        EnumSet<SpecialLevelType> seen = EnumSet.noneOf(SpecialLevelType.class);
        for (GameWorld world : GameWorld.values()) {
            List<AdventureLevelConfig> levels = configs.get(world);
            if (levels == null || levels.size() != 4) {
                throw new IllegalStateException(world + " must define exactly four levels.");
            }
            for (int index = 0; index < levels.size(); index++) {
                AdventureLevelConfig config = levels.get(index);
                if (config.getLevelNumber() != index + 1) {
                    throw new IllegalStateException(world + " level numbering is not contiguous.");
                }
                if (index == 0 && config.isSpecial()) {
                    throw new IllegalStateException("Level 1 must be normal in " + world);
                }
                if ((index == 1 || index == 2) && !config.isSpecial()) {
                    throw new IllegalStateException("Levels 2 and 3 must be special in " + world);
                }
                if (index == 3 && !config.isBossDeferred()) {
                    throw new IllegalStateException("Level 4 must be a deferred boss in " + world);
                }
                if (config.isSpecial() && !seen.add(config.getSpecialType())) {
                    throw new IllegalStateException("Duplicate special level type: "
                            + config.getSpecialType());
                }
            }
        }
        if (!seen.equals(EnumSet.allOf(SpecialLevelType.class))) {
            throw new IllegalStateException("Every special-level type must occur exactly once.");
        }
    }

    private CsvTable read(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            if (lines.size() < 2) {
                throw new IllegalStateException("Adventure CSV is empty: " + path);
            }
            return new CsvTable(lines);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read Adventure configuration: " + path, exception);
        }
    }

    private static List<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(";"));
    }

    private static List<int[]> coordinates(String value) {
        List<int[]> result = new ArrayList<>();
        for (String token : tokens(value)) {
            String[] parts = token.split(":");
            result.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }
        return result;
    }

    private static int[] integerArray(String value) {
        List<String> values = tokens(value);
        int[] result = new int[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = Integer.parseInt(values.get(index));
        }
        return result;
    }

    private static int integer(String value, int fallback) {
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
    }

    private static Set<PlantType> plantTypes(String value) {
        EnumSet<PlantType> result = EnumSet.noneOf(PlantType.class);
        for (String token : tokens(value)) {
            result.add(PlantType.valueOf(token));
        }
        return result;
    }

    private static Set<PlantCategory> plantCategories(String value) {
        EnumSet<PlantCategory> result = EnumSet.noneOf(PlantCategory.class);
        for (String token : tokens(value)) {
            result.add(PlantCategory.valueOf(token));
        }
        return result;
    }

    private static List<ProtectedPlantPlacement> protectedPlants(String value) {
        List<ProtectedPlantPlacement> result = new ArrayList<>();
        for (String token : tokens(value)) {
            String[] halves = token.split("@");
            String[] coordinate = halves[1].split(":");
            result.add(new ProtectedPlantPlacement(
                    PlantType.valueOf(halves[0]),
                    Integer.parseInt(coordinate[0]),
                    Integer.parseInt(coordinate[1])));
        }
        return result;
    }

    private static final class CsvTable {
        private final Map<String, Integer> columns = new HashMap<>();
        private final List<String[]> rows = new ArrayList<>();

        private CsvTable(List<String> lines) {
            String[] header = lines.get(0).split(",", -1);
            for (int index = 0; index < header.length; index++) {
                columns.put(header[index].trim(), index);
            }
            for (int index = 1; index < lines.size(); index++) {
                String line = lines.get(index).trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    rows.add(line.split(",", -1));
                }
            }
        }

        private String value(String[] row, String column) {
            Integer index = columns.get(column);
            if (index == null) {
                throw new IllegalStateException("Missing CSV column: " + column);
            }
            return index < row.length ? row[index].trim() : "";
        }
    }
}
