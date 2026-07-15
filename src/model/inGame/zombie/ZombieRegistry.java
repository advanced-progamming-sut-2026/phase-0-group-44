package model.inGame.zombie;

import model.enums.ZombieType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ZombieRegistry implements ZombieRepository {
    public static final Path DEFAULT_CSV = Path.of("phase1", "assets", "Data", "zombies.csv");
    public static final Path DEFAULT_BONUS_LIST =
            Path.of("phase1", "assets", "Data", "bonus-zombies.txt");

    private static volatile ZombieRegistry defaultRegistry;

    private final ZombieRepository source;
    private final Map<ZombieType, ZombieDefinition> byType = new EnumMap<>(ZombieType.class);

    public ZombieRegistry(ZombieRepository source) {
        this.source = source;
    }

    public ZombieRegistry(Path csvPath, Path bonusListPath) {
        this(new CsvZombieRepository(csvPath, bonusListPath));
    }

    public static ZombieRegistry getDefault() {
        ZombieRegistry result = defaultRegistry;
        if (result == null) {
            synchronized (ZombieRegistry.class) {
                result = defaultRegistry;
                if (result == null) {
                    result = new ZombieRegistry(DEFAULT_CSV, DEFAULT_BONUS_LIST);
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
        for (ZombieDefinition definition : source.findAll()) {
            ZombieDefinition previous = byType.put(definition.getType(), definition);
            if (previous != null) {
                throw new IllegalStateException("Duplicate zombie type: " + definition.getType());
            }
        }
        if (byType.size() != 28) {
            throw new IllegalStateException("Canonical zombie registry must contain 28 classified rows; got "
                    + byType.size());
        }
    }

    @Override
    public ArrayList<ZombieDefinition> findAll() {
        return new ArrayList<>(byType.values());
    }

    public List<ZombieDefinition> findMandatory() {
        List<ZombieDefinition> result = new ArrayList<>();
        for (ZombieDefinition definition : byType.values()) {
            if (definition.isMandatory()) {
                result.add(definition);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<ZombieDefinition> findBonus() {
        List<ZombieDefinition> result = new ArrayList<>();
        for (ZombieDefinition definition : byType.values()) {
            if (definition.isBonus()) {
                result.add(definition);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public ZombieDefinition findByType(ZombieType type) {
        if (type == ZombieType.BASIC) {
            type = ZombieType.NORMAL;
        } else if (type == ZombieType.FOOTBALL) {
            type = ZombieType.ALL_STAR;
        } else if (type == ZombieType.NEWSPAPER) {
            type = ZombieType.NEWSPAPER_ZOMBIE;
        }
        return byType.get(type);
    }

    @Override
    public ZombieDefinition findByName(String name) {
        if (name == null) {
            return null;
        }
        ZombieType type = ZombieType.fromToken(name);
        ZombieDefinition byEnum = type == null ? null : findByType(type);
        if (byEnum != null) {
            return byEnum;
        }
        for (ZombieDefinition definition : byType.values()) {
            if (definition.getName().equalsIgnoreCase(name.trim())) {
                return definition;
            }
        }
        return null;
    }

    public ZombieDefinition require(ZombieType type) {
        ZombieDefinition definition = findByType(type);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown zombie type: " + type);
        }
        return definition;
    }

    public ZombieDefinition requireMandatory(ZombieType type) {
        ZombieDefinition definition = require(type);
        if (definition.isBonus()) {
            throw new UnsupportedOperationException(definition.getName() + " is a blue/bonus zombie.");
        }
        return definition;
    }
}
