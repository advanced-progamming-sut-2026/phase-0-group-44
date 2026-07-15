package model.inGame.plant;

import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.enums.PlantType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CsvPlantRepository implements PlantRepository {
    private static final int EXPECTED_COLUMN_COUNT = 14;

    private final Path csvPath;
    private final Path bonusListPath;
    private final ArrayList<PlantDefinition> plants = new ArrayList<>();

    public CsvPlantRepository(String csvPath, String bonusListPath) {
        this(Path.of(csvPath), Path.of(bonusListPath));
    }

    public CsvPlantRepository(Path csvPath, Path bonusListPath) {
        this.csvPath = csvPath;
        this.bonusListPath = bonusListPath;
    }

    @Override
    public void load() {
        plants.clear();
        Set<String> bonusNames = readBonusNames();
        try {
            List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                throw new IllegalStateException("Plant CSV is empty: " + csvPath);
            }
            for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
                String line = lines.get(lineIndex);
                if (line.isBlank()) {
                    continue;
                }
                List<String> columns = CsvLineParser.parse(line);
                if (columns.size() != EXPECTED_COLUMN_COUNT) {
                    throw new IllegalStateException("Invalid plant CSV row " + (lineIndex + 1)
                            + ": expected " + EXPECTED_COLUMN_COUNT + " columns but got " + columns.size());
                }
                String name = columns.get(1).trim();
                List<PlantUpgrade> upgrades = List.of(
                        PlantUpgrade.parse(2, columns.get(9)),
                        PlantUpgrade.parse(3, columns.get(10)),
                        PlantUpgrade.parse(4, columns.get(11))
                );
                plants.add(new PlantDefinition(
                        parseInt(columns.get(0), "ID", lineIndex),
                        name,
                        PlantCategory.fromCsv(columns.get(2)),
                        PlantTag.parseCsv(columns.get(3)),
                        parseInt(columns.get(4), "Cost", lineIndex),
                        parseInt(columns.get(5), "Base HP", lineIndex),
                        DamageProfile.parse(columns.get(6)),
                        columns.get(7).trim(),
                        columns.get(8).trim(),
                        upgrades,
                        parseSeconds(columns.get(12)),
                        parseSeconds(columns.get(13)),
                        bonusNames.contains(normalize(name))
                ));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load canonical plant data from: " + csvPath, exception);
        }
        validateBonusClassification(bonusNames);
    }

    private Set<String> readBonusNames() {
        try {
            Set<String> result = new HashSet<>();
            for (String line : Files.readAllLines(bonusListPath, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    result.add(normalize(trimmed));
                }
            }
            if (result.isEmpty()) {
                throw new IllegalStateException("Bonus classification is empty: " + bonusListPath);
            }
            return result;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read bonus plant classification: " + bonusListPath, exception);
        }
    }

    private void validateBonusClassification(Set<String> bonusNames) {
        Set<String> loadedNames = new HashSet<>();
        Set<Integer> ids = new HashSet<>();
        for (PlantDefinition definition : plants) {
            if (!ids.add(definition.getId())) {
                throw new IllegalStateException("Duplicate plant ID: " + definition.getId());
            }
            if (!loadedNames.add(normalize(definition.getName()))) {
                throw new IllegalStateException("Duplicate plant name: " + definition.getName());
            }
        }
        Set<String> unknownBonusNames = new HashSet<>(bonusNames);
        unknownBonusNames.removeAll(loadedNames);
        if (!unknownBonusNames.isEmpty()) {
            throw new IllegalStateException("Bonus list contains names absent from CSV: " + unknownBonusNames);
        }
    }

    private static int parseInt(String value, String field, int lineIndex) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid " + field + " on plant CSV row " + (lineIndex + 1), exception);
        }
    }

    private static double parseSeconds(String value) {
        String trimmed = value.trim();
        if (trimmed.isBlank() || trimmed.equals("-")) {
            return 0.0;
        }
        return Double.parseDouble(trimmed);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public ArrayList<PlantDefinition> findAll() {
        return new ArrayList<>(plants);
    }

    @Override
    public PlantDefinition findByType(PlantType type) {
        if (type == null) {
            return null;
        }
        for (PlantDefinition plant : plants) {
            if (plant.getType() == type) {
                return plant;
            }
        }
        return null;
    }

    @Override
    public PlantDefinition findByName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = normalize(name);
        for (PlantDefinition plant : plants) {
            if (normalize(plant.getName()).equals(normalized)) {
                return plant;
            }
        }
        try {
            return findByType(PlantType.fromCanonicalName(name));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
