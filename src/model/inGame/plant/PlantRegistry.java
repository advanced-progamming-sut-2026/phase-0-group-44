package model.inGame.plant;

import model.enums.PlantCategory;
import model.enums.PlantType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PlantRegistry implements PlantRepository {
    public static final Path DEFAULT_CSV = Path.of("phase1", "assets", "Data", "plants.csv");
    public static final Path DEFAULT_BONUS_LIST = Path.of("phase1", "assets", "Data", "bonus-plants.txt");

    private static volatile PlantRegistry defaultRegistry;

    private final PlantRepository source;
    private final Map<PlantType, PlantDefinition> byType = new EnumMap<>(PlantType.class);

    public PlantRegistry(PlantRepository source) {
        this.source = source;
    }

    public PlantRegistry(Path csvPath, Path bonusListPath) {
        this(new CsvPlantRepository(csvPath, bonusListPath));
    }

    public static PlantRegistry getDefault() {
        PlantRegistry result = defaultRegistry;
        if (result == null) {
            synchronized (PlantRegistry.class) {
                result = defaultRegistry;
                if (result == null) {
                    result = new PlantRegistry(DEFAULT_CSV, DEFAULT_BONUS_LIST);
                    result.load();
                    defaultRegistry = result;
                }
            }
        }
        return result;
    }

    public static void resetDefaultForTests() {
        defaultRegistry = null;
    }

    @Override
    public void load() {
        source.load();
        byType.clear();
        for (PlantDefinition definition : source.findAll()) {
            PlantDefinition previous = byType.put(definition.getType(), definition);
            if (previous != null) {
                throw new IllegalStateException("Duplicate plant type: " + definition.getType());
            }
        }
        if (byType.size() != PlantType.values().length) {
            throw new IllegalStateException("Canonical registry is incomplete: expected "
                    + PlantType.values().length + " rows, got " + byType.size());
        }
    }

    @Override
    public ArrayList<PlantDefinition> findAll() {
        return new ArrayList<>(byType.values());
    }

    public List<PlantDefinition> findMandatory() {
        List<PlantDefinition> result = new ArrayList<>();
        for (PlantDefinition definition : byType.values()) {
            if (definition.isMandatory()) {
                result.add(definition);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<PlantDefinition> findBonus() {
        List<PlantDefinition> result = new ArrayList<>();
        for (PlantDefinition definition : byType.values()) {
            if (definition.isBonus()) {
                result.add(definition);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<PlantDefinition> findByCategory(PlantCategory category, boolean mandatoryOnly) {
        List<PlantDefinition> result = new ArrayList<>();
        for (PlantDefinition definition : byType.values()) {
            if (definition.getCategory() == category && (!mandatoryOnly || definition.isMandatory())) {
                result.add(definition);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public PlantDefinition findByType(PlantType type) {
        return byType.get(type);
    }

    @Override
    public PlantDefinition findByName(String name) {
        if (name == null) {
            return null;
        }
        try {
            return findByType(PlantType.fromCanonicalName(name));
        } catch (IllegalArgumentException ignored) {
            for (PlantDefinition definition : byType.values()) {
                if (definition.getName().equalsIgnoreCase(name.trim())) {
                    return definition;
                }
            }
            return null;
        }
    }

    public PlantDefinition require(PlantType type) {
        PlantDefinition definition = findByType(type);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown plant type: " + type);
        }
        return definition;
    }

    public PlantDefinition requireMandatory(PlantType type) {
        PlantDefinition definition = require(type);
        if (definition.isBonus()) {
            throw new UnsupportedOperationException(definition.getName() + " is a blue/bonus plant.");
        }
        return definition;
    }
}
