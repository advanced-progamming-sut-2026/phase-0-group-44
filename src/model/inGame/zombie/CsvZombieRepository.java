package model.inGame.zombie;

import model.enums.ZombieType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CsvZombieRepository implements ZombieRepository {
    private static final int EXPECTED_COLUMNS = 9;

    private final Path csvPath;
    private final Path bonusListPath;
    private final ArrayList<ZombieDefinition> definitions = new ArrayList<>();

    public CsvZombieRepository(Path csvPath, Path bonusListPath) {
        this.csvPath = csvPath;
        this.bonusListPath = bonusListPath;
    }

    @Override
    public void load() {
        definitions.clear();
        Set<String> bonusNames = readBonusNames();
        try {
            List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                throw new IllegalStateException("Zombie CSV is empty: " + csvPath);
            }
            for (int index = 1; index < lines.size(); index++) {
                if (lines.get(index).isBlank()) {
                    continue;
                }
                List<String> fields = parseCsvLine(lines.get(index));
                if (fields.size() != EXPECTED_COLUMNS) {
                    throw new IllegalStateException("Invalid zombie CSV row " + (index + 1)
                            + ": expected " + EXPECTED_COLUMNS + " columns but got " + fields.size());
                }
                String name = fields.get(1).trim();
                definitions.add(new ZombieDefinition(
                        requireType(fields.get(0), index),
                        name,
                        ZombieChapter.valueOf(fields.get(2).trim()),
                        parseInt(fields.get(3), "base health", index),
                        parseDouble(fields.get(4), "speed", index),
                        parseInt(fields.get(5), "eat DPS", index),
                        parseInt(fields.get(6), "wave cost", index),
                        parseArmor(fields.get(7), index),
                        ZombieBehaviorKind.valueOf(fields.get(8).trim()),
                        bonusNames.contains(normalize(name))
                ));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load canonical zombie data: " + csvPath, exception);
        }
        validate(bonusNames);
    }

    private Set<String> readBonusNames() {
        try {
            Set<String> result = new HashSet<>();
            for (String line : Files.readAllLines(bonusListPath, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isBlank() && !trimmed.startsWith("#")) {
                    result.add(normalize(trimmed));
                }
            }
            if (result.isEmpty()) {
                throw new IllegalStateException("Zombie bonus classification is empty: " + bonusListPath);
            }
            return result;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read zombie bonus classification: "
                    + bonusListPath, exception);
        }
    }

    private void validate(Set<String> bonusNames) {
        Set<ZombieType> types = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (ZombieDefinition definition : definitions) {
            if (!types.add(definition.getType())) {
                throw new IllegalStateException("Duplicate zombie type: " + definition.getType());
            }
            if (!names.add(normalize(definition.getName()))) {
                throw new IllegalStateException("Duplicate zombie name: " + definition.getName());
            }
        }
        Set<String> unresolved = new HashSet<>(bonusNames);
        unresolved.removeAll(names);
        if (!unresolved.isEmpty()) {
            throw new IllegalStateException("Bonus zombie names absent from CSV: " + unresolved);
        }
    }

    private static ZombieType requireType(String value, int index) {
        ZombieType type = ZombieType.fromToken(value);
        if (type == null) {
            throw new IllegalStateException("Unknown zombie type on CSV row " + (index + 1));
        }
        return type;
    }

    private static List<ZombieArmorPart> parseArmor(String value, int index) {
        List<ZombieArmorPart> result = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return result;
        }
        for (String token : value.split("\\|")) {
            String[] pieces = token.trim().split(":");
            if (pieces.length != 3) {
                throw new IllegalStateException("Invalid armor on zombie CSV row " + (index + 1));
            }
            result.add(new ZombieArmorPart(
                    pieces[0].trim(),
                    parseInt(pieces[1], "armor health", index),
                    Boolean.parseBoolean(pieces[2].trim())
            ));
        }
        return result;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString());
        return result;
    }

    private static int parseInt(String value, String field, int index) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid " + field + " on zombie CSV row "
                    + (index + 1), exception);
        }
    }

    private static double parseDouble(String value, String field, int index) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid " + field + " on zombie CSV row "
                    + (index + 1), exception);
        }
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public ArrayList<ZombieDefinition> findAll() {
        return new ArrayList<>(definitions);
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
        for (ZombieDefinition definition : definitions) {
            if (definition.getType() == type) {
                return definition;
            }
        }
        return null;
    }

    @Override
    public ZombieDefinition findByName(String name) {
        if (name == null) {
            return null;
        }
        ZombieType type = ZombieType.fromToken(name);
        if (type != null) {
            ZombieDefinition byType = findByType(type);
            if (byType != null) {
                return byType;
            }
        }
        String normalized = normalize(name);
        for (ZombieDefinition definition : definitions) {
            if (normalize(definition.getName()).equals(normalized)) {
                return definition;
            }
        }
        return null;
    }
}
