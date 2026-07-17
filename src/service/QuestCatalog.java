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
            case "آفتاب گیر روزانه" -> QuestConditionType.SUN_PRODUCED_IN_DAY;
            case "شکارچی chapter" -> QuestConditionType.ZOMBIES_KILLED_IN_CHAPTER;
            case "plant باز حرفه‌ای" -> QuestConditionType.ZOMBIES_KILLED_ONLY_BY_PLANT;
            case "only cactus" -> QuestConditionType.ZOMBIES_KILLED_ONLY_BY_CACTUS;
            case "گیاه خوار اقتصادی" -> QuestConditionType.WIN_WITH_MAX_PLANT_LOSSES;
            case "استاد دفاع" -> QuestConditionType.WIN_WITH_ZERO_SUN;
            case "سرعت عمل" -> QuestConditionType.KILLS_WITHIN_FIRST_WAVE_SECONDS;
            case "تخریب گر حرفه ای" -> QuestConditionType.EXPLOSIVE_PLANTS_USED;
            case "تقارن" -> QuestConditionType.WIN_WITH_SYMMETRIC_BOARD;
            case "کشتار خانوادگی" -> QuestConditionType.KILLS_ONLY_BY_PLANT_FAMILY;
            case "شکوفایی در محدودیت‌ها" -> QuestConditionType.WIN_WITHOUT_PLANT_FAMILY;
            case "شب یا صبح" -> QuestConditionType.WIN_DAY_LEVEL_WITH_NIGHT_PLANTS;
            case "برد پشت برد" -> QuestConditionType.MAX_DIFFICULTY_WIN_STREAK;
            case "تقریبا پیروز" -> QuestConditionType.FIRST_COLUMN_KILLS_WITHOUT_MOWER;
            case "OCD نَمَنَ" -> QuestConditionType.WIN_WITH_ASYMMETRIC_BOARD;
            case "روز ابری" -> QuestConditionType.WIN_WITH_ONLY_THREE_SUN_PRODUCERS;
            case "یه ستون کمتر" -> QuestConditionType.WIN_WITH_EMPTY_COLUMN;
            case "سطر بی دفاع" -> QuestConditionType.WIN_WITH_EMPTY_ROW;
            case "صلیب بی دفاع" -> QuestConditionType.WIN_WITH_EMPTY_ROW_AND_COLUMN;
            case "وقت چمن‌زنی" -> QuestConditionType.LAWNMOWER_KILLS;
            default -> throw new IllegalStateException(
                    "No machine condition mapping for canonical quest: " + name);
        };
    }

    private static Reward rewardFor(String name, String canonicalText) {
        return switch (name) {
            case "آفتاب گیر روزانه" -> new Reward(
                    RewardType.COINS, 0, null,
                    Reward.AmountRule.VARIABLE_DIVIDED_BY_100, canonicalText);
            case "شکارچی chapter" -> inventory(10, canonicalText);
            case "plant باز حرفه‌ای" -> new Reward(
                    RewardType.PLANT_UNLOCK, 1, "random-new-plant",
                    Reward.AmountRule.FIXED, canonicalText);
            case "only cactus" -> gems(20, canonicalText);
            case "گیاه خوار اقتصادی" -> new Reward(
                    RewardType.INVENTORY, 0, "seed-packets",
                    Reward.AmountRule.TWENTY_MINUS_VARIABLE, canonicalText);
            case "استاد دفاع" -> gems(200, canonicalText);
            case "سرعت عمل" -> coins(500, canonicalText);
            case "تخریب گر حرفه ای" -> coins(100, canonicalText);
            case "تقارن" -> coins(500, canonicalText);
            case "کشتار خانوادگی" -> coins(1000, canonicalText);
            case "شکوفایی در محدودیت‌ها" -> gems(100, canonicalText);
            case "شب یا صبح" -> gems(20, canonicalText);
            case "برد پشت برد" -> coins(5000, canonicalText);
            case "تقریبا پیروز" -> coins(300, canonicalText);
            case "OCD نَمَنَ" -> coins(800, canonicalText);
            case "روز ابری" -> gems(10, canonicalText);
            case "یه ستون کمتر" -> gems(10, canonicalText);
            case "سطر بی دفاع" -> gems(20, canonicalText);
            case "صلیب بی دفاع" -> gems(25, canonicalText);
            case "وقت چمن‌زنی" -> new Reward(
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
