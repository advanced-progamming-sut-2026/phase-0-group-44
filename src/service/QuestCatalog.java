package service;

import model.utility.Quest;
import model.utility.QuestCategory;
import model.utility.QuestConditionType;
import model.utility.QuestPriority;
import model.utility.Reward;
import model.utility.RewardType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads and validates the canonical quest workbook export. */
public final class QuestCatalog {
    public static final Path DEFAULT_PATH = Path.of(
            "phase1", "assets", "Data", "quests.csv");

    private final List<Quest> quests;
    private final Map<String, Quest> byName;

    public QuestCatalog(List<Quest> quests) {
        this.quests = Collections.unmodifiableList(new ArrayList<>(quests));
        Map<String, Quest> names = new LinkedHashMap<>();
        for (Quest quest : quests) {
            if (names.put(quest.getName(), quest) != null) {
                throw new IllegalArgumentException(
                        "Duplicate canonical quest name: " + quest.getName());
            }
        }
        this.byName = Collections.unmodifiableMap(names);
    }

    public static QuestCatalog fromFile(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalStateException("Canonical quest table not found: " + path);
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                throw new IllegalStateException("Canonical quest table is empty: " + path);
            }
            List<Quest> parsed = new ArrayList<>();
            for (int index = 1; index < lines.size(); index++) {
                if (lines.get(index).isBlank()) {
                    continue;
                }
                List<String> fields = parseCsvLine(lines.get(index));
                if (fields.size() != 6) {
                    throw new IllegalStateException(
                            "Quest row " + (index + 1) + " must contain 6 columns.");
                }
                parsed.add(parseQuest(fields, parsed.size()));
            }
            if (parsed.isEmpty()) {
                throw new IllegalStateException("Canonical quest table contains no quests.");
            }
            return new QuestCatalog(parsed);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read canonical quest table: " + path,
                    exception);
        }
    }

    public List<Quest> getQuests() {
        return quests;
    }

    public Quest findByName(String name) {
        return name == null ? null : byName.get(name);
    }

    public List<Quest> byCategory(QuestCategory category) {
        List<Quest> result = new ArrayList<>();
        for (Quest quest : quests) {
            if (quest.getCategory() == category) {
                result.add(quest);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static Quest parseQuest(List<String> row, int order) {
        String name = row.get(0).trim();
        QuestCategory category = QuestCategory.fromCanonicalLabel(row.get(1));
        QuestPriority priority = QuestPriority.fromCanonicalLabel(row.get(4));
        if (name.isBlank() || category == null || priority == null) {
            throw new IllegalStateException("Invalid canonical quest row: " + row);
        }
        return new Quest(
                name,
                category,
                row.get(2).trim(),
                rewardFor(name, row.get(3).trim()),
                priority,
                row.get(5).trim(),
                order,
                conditionFor(name)
        );
    }

    private static QuestConditionType conditionFor(String name) {
        return switch (name) {
            case "Daily Sun Collector" -> QuestConditionType.SUN_PRODUCED_IN_DAY;
            case "Chapter Hunter" -> QuestConditionType.ZOMBIES_KILLED_IN_CHAPTER;
            case "Plant Pro" -> QuestConditionType.ZOMBIES_KILLED_ONLY_BY_PLANT;
            case "Only Cactus" -> QuestConditionType.ZOMBIES_KILLED_ONLY_BY_CACTUS;
            case "Frugal Gardener" -> QuestConditionType.WIN_WITH_MAX_PLANT_LOSSES;
            case "Defense Master" -> QuestConditionType.WIN_WITH_ZERO_SUN;
            case "Quick Action" -> QuestConditionType.KILLS_WITHIN_FIRST_WAVE_SECONDS;
            case "Professional Demolisher" -> QuestConditionType.EXPLOSIVE_PLANTS_USED;
            case "Symmetry" -> QuestConditionType.WIN_WITH_SYMMETRIC_BOARD;
            case "Family Slaughter" -> QuestConditionType.KILLS_ONLY_BY_PLANT_FAMILY;
            case "Blooming Within Limits" -> QuestConditionType.WIN_WITHOUT_PLANT_FAMILY;
            case "Night or Day" -> QuestConditionType.WIN_DAY_LEVEL_WITH_NIGHT_PLANTS;
            case "Winning Streak" -> QuestConditionType.MAX_DIFFICULTY_WIN_STREAK;
            case "Almost Victorious" -> QuestConditionType.FIRST_COLUMN_KILLS_WITHOUT_MOWER;
            case "Asymmetric OCD" -> QuestConditionType.WIN_WITH_ASYMMETRIC_BOARD;
            case "Cloudy Day" -> QuestConditionType.WIN_WITH_ONLY_THREE_SUN_PRODUCERS;
            case "One Column Less" -> QuestConditionType.WIN_WITH_EMPTY_COLUMN;
            case "Undefended Row" -> QuestConditionType.WIN_WITH_EMPTY_ROW;
            case "Undefended Cross" -> QuestConditionType.WIN_WITH_EMPTY_ROW_AND_COLUMN;
            case "Lawnmower Time" -> QuestConditionType.LAWNMOWER_KILLS;
            default -> throw new IllegalStateException(
                    "No machine condition mapping for canonical quest: " + name);
        };
    }

    private static Reward rewardFor(String name, String canonicalText) {
        return switch (name) {
            case "Daily Sun Collector" -> new Reward(
                    RewardType.COINS, 0, null,
                    Reward.AmountRule.VARIABLE_DIVIDED_BY_100, canonicalText);
            case "Chapter Hunter" -> inventory(10, canonicalText);
            case "Plant Pro" -> new Reward(
                    RewardType.PLANT_UNLOCK, 1, "random-new-plant",
                    Reward.AmountRule.FIXED, canonicalText);
            case "Only Cactus" -> gems(20, canonicalText);
            case "Frugal Gardener" -> new Reward(
                    RewardType.INVENTORY, 0, "seed-packets",
                    Reward.AmountRule.TWENTY_MINUS_VARIABLE, canonicalText);
            case "Defense Master" -> gems(200, canonicalText);
            case "Quick Action" -> coins(500, canonicalText);
            case "Professional Demolisher" -> coins(100, canonicalText);
            case "Symmetry" -> coins(500, canonicalText);
            case "Family Slaughter" -> coins(1000, canonicalText);
            case "Blooming Within Limits" -> gems(100, canonicalText);
            case "Night or Day" -> gems(20, canonicalText);
            case "Winning Streak" -> coins(5000, canonicalText);
            case "Almost Victorious" -> coins(300, canonicalText);
            case "Asymmetric OCD" -> coins(800, canonicalText);
            case "Cloudy Day" -> gems(10, canonicalText);
            case "One Column Less" -> gems(10, canonicalText);
            case "Undefended Row" -> gems(20, canonicalText);
            case "Undefended Cross" -> gems(25, canonicalText);
            case "Lawnmower Time" -> new Reward(
                    RewardType.GEMS, 0, null, Reward.AmountRule.VARIABLE, canonicalText);
            default -> throw new IllegalStateException(
                    "No reward mapping for canonical quest: " + name);
        };
    }

    private static Reward coins(int amount, String text) {
        return new Reward(RewardType.COINS, amount, null, Reward.AmountRule.FIXED, text);
    }

    private static Reward gems(int amount, String text) {
        return new Reward(RewardType.GEMS, amount, null, Reward.AmountRule.FIXED, text);
    }

    private static Reward inventory(int amount, String text) {
        return new Reward(RewardType.INVENTORY, amount, "seed-packets",
                Reward.AmountRule.FIXED, text);
    }

    /** Minimal RFC-4180 line parser, sufficient for the checked-in six-column table. */
    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        values.add(current.toString());
        return values;
    }
}