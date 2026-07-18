package service;

import model.Result;
import model.events.DomainEvent;
import model.events.DomainEventListener;
import model.events.DomainEventType;
import model.user.User;
import model.utility.Quest;
import model.utility.QuestCategory;
import model.utility.QuestInstance;
import model.utility.QuestProgress;
import model.utility.QuestRecurrence;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Event-driven quest progress, completion, recurrence and idempotent claiming. */
public final class QuestService implements DomainEventListener {
    private static final String KEY_SEPARATOR = "\u001f";

    private final QuestCatalog catalog;
    private final QuestRewardService rewards;
    private final UserService users;

    public QuestService(QuestCatalog catalog, QuestRewardService rewards, UserService users) {
        if (catalog == null || rewards == null || users == null) {
            throw new IllegalArgumentException("Quest catalog, rewards and user service are required.");
        }
        this.catalog = catalog;
        this.rewards = rewards;
        this.users = users;
    }

    public QuestCatalog getCatalog() {
        return catalog;
    }

    /** Activates a canonical row. Parameterized rows require a source-backed binding. */
    public Result<QuestInstance> activate(User user, String questName, String variableValue) {
        Result<QuestInstance> result = new Result<>();
        if (user == null) {
            result.appendToMessage("no user is logged in");
            return result;
        }
        Quest quest = catalog.findByName(questName);
        if (quest == null) {
            result.appendToMessage("unknown canonical quest");
            return result;
        }
        String binding = normalizeBinding(variableValue);
        if (quest.requiresBinding() && binding == null) {
            result.appendToMessage("this quest requires a canonical variable binding");
            return result;
        }
        if (!quest.requiresBinding() && binding != null) {
            result.appendToMessage("this quest has no canonical variable");
            return result;
        }
        if (!isAllowedBinding(quest, binding)) {
            result.appendToMessage("binding is not listed by the canonical quest row");
            return result;
        }
        String key = storageKey(quest.getName(), binding);
        QuestProgress progress = user.getQuestProgress().get(key);
        if (progress == null) {
            progress = new QuestProgress(
                    quest.getName(), binding,
                    quest.getRecurrence() == QuestRecurrence.DAILY
                            ? users.today().toString() : null);
            user.getQuestProgress().put(key, progress);
            users.updateUser(user);
        }
        result.setStatus(true);
        result.setData(new QuestInstance(key, quest, progress));
        result.appendToMessage("quest activated");
        return result;
    }

    /** Fixed rows can be active without inventing a variable-selection policy. */
    private void ensureFixedRows(User user) {
        for (Quest quest : catalog.getQuests()) {
            if (!quest.requiresBinding()) {
                String key = storageKey(quest.getName(), null);
                user.getQuestProgress().computeIfAbsent(key, ignored -> new QuestProgress(
                        quest.getName(), null,
                        quest.getRecurrence() == QuestRecurrence.DAILY
                                ? users.today().toString() : null));
            }
        }
    }

    public List<QuestInstance> getActiveQuests(User user, QuestCategory category) {
        if (user == null || category == null) {
            return List.of();
        }
        ensureFixedRows(user);
        resetDailyRows(user);
        List<QuestInstance> active = new ArrayList<>();
        for (Map.Entry<String, QuestProgress> entry : user.getQuestProgress().entrySet()) {
            QuestProgress progress = entry.getValue();
            Quest quest = progress == null ? null : catalog.findByName(progress.getQuestName());
            if (quest != null && quest.getCategory() == category && !progress.isClaimed()) {
                active.add(new QuestInstance(entry.getKey(), quest, progress));
            }
        }
        active.sort(Comparator
                .comparingInt((QuestInstance item) -> item.getQuest().getPriority().getSortRank())
                .thenComparingInt(item -> item.getQuest().getCanonicalOrder())
                .thenComparing(item -> nullToEmpty(item.getProgress().getVariableValue())));
        return List.copyOf(active);
    }

    public Result<String> claim(User user, QuestInstance instance) {
        Result<String> result = new Result<>();
        if (user == null || instance == null) {
            result.appendToMessage("quest claim is missing a user or quest");
            return result;
        }
        QuestProgress stored = user.getQuestProgress().get(instance.getStorageKey());
        if (stored == null || stored != instance.getProgress()) {
            result.appendToMessage("quest is not active for this user");
            return result;
        }
        if (!stored.isCompleted()) {
            result.appendToMessage("quest is not complete");
            return result;
        }
        if (stored.isClaimed() || stored.isRewardApplied()) {
            result.appendToMessage("quest reward was already claimed");
            return result;
        }

        stored.setRewardApplied(true);
        try {
            String message = rewards.apply(user, instance.getQuest(), stored);
            stored.setClaimed(true);
            users.updateUser(user);
            result.setStatus(true);
            result.setData(message);
            result.appendToMessage(message);
        } catch (RuntimeException exception) {
            stored.setRewardApplied(false);
            result.appendToMessage("could not apply quest reward: " + exception.getMessage());
        }
        return result;
    }

    @Override
    public void onDomainEvent(DomainEvent event) {
        if (event == null || event.getUser() == null) {
            return;
        }
        User user = event.getUser();
        ensureFixedRows(user);
        boolean changed = resetDailyRows(user);
        for (Map.Entry<String, QuestProgress> entry : user.getQuestProgress().entrySet()) {
            QuestProgress progress = entry.getValue();
            Quest quest = progress == null ? null : catalog.findByName(progress.getQuestName());
            if (quest == null || progress.isCompleted()) {
                continue;
            }
            if (evaluate(quest, progress, event)) {
                complete(user, quest, progress, event);
                changed = true;
            } else if (eventTouches(quest, event)) {
                changed = true;
            }
        }
        if (changed) {
            users.updateUser(user);
        }
    }

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    private boolean evaluate(Quest quest, QuestProgress progress, DomainEvent event) {
        if (event.getType() == DomainEventType.LEVEL_STARTED) {
            resetLevelCounters(progress);
            return false;
        }
        String binding = progress.getVariableValue();
        return switch (quest.getConditionType()) {
            case SUN_PRODUCED_IN_DAY -> amountTarget(
                    progress, event, DomainEventType.SUN_PRODUCED, numeric(binding));
            case ZOMBIES_KILLED_IN_CHAPTER -> event.getType() == DomainEventType.ZOMBIE_KILLED
                    && equalsNormalized(binding, event.attribute("chapter"))
                    && incrementTo(progress, "chapterKills", event.getAmount(), 50);
            case ZOMBIES_KILLED_ONLY_BY_PLANT -> exclusiveKillBy(
                    progress, event, "killerPlant", binding, 10);
            case ZOMBIES_KILLED_ONLY_BY_CACTUS -> exclusiveKillBy(
                    progress, event, "killerPlant", "cactus", 10);
            case WIN_WITH_MAX_PLANT_LOSSES -> won(event)
                    && event.intAttribute("plantLossCount", Integer.MAX_VALUE) <= numeric(binding);
            case WIN_WITH_ZERO_SUN -> won(event) && event.intAttribute("sunBalance", -1) == 0;
            case KILLS_WITHIN_FIRST_WAVE_SECONDS -> event.getType() == DomainEventType.ZOMBIE_KILLED
                    && event.intAttribute("secondsSinceFirstWave", Integer.MAX_VALUE) < 30
                    && incrementTo(progress, "fastKills", event.getAmount(), 10);
            case EXPLOSIVE_PLANTS_USED -> event.getType() == DomainEventType.PLANT_PLANTED
                    && event.booleanAttribute("explosive")
                    && incrementTo(progress, "level.explosivePlants", event.getAmount(), 3);
            case WIN_WITH_SYMMETRIC_BOARD -> won(event) && event.booleanAttribute("boardSymmetric");
            case KILLS_ONLY_BY_PLANT_FAMILY -> exclusiveKillBy(
                    progress, event, "killerFamily", binding, 1);
            case WIN_WITHOUT_PLANT_FAMILY -> won(event)
                    && !containsToken(event.attribute("usedPlantFamilies"), binding);
            case WIN_DAY_LEVEL_WITH_NIGHT_PLANTS -> won(event)
                    && event.booleanAttribute("dayLevel")
                    && event.booleanAttribute("allUsedPlantsNight");
            case MAX_DIFFICULTY_WIN_STREAK -> winStreak(progress, event);
            case FIRST_COLUMN_KILLS_WITHOUT_MOWER -> event.getType() == DomainEventType.ZOMBIE_KILLED
                    && event.intAttribute("tileX", -1) == 0
                    && event.booleanAttribute("mowerReady")
                    && incrementTo(progress, "level.firstColumnKills", event.getAmount(), 10);
            case WIN_WITH_ASYMMETRIC_BOARD -> won(event)
                    && event.getAttributes().containsKey("boardSymmetric")
                    && !event.booleanAttribute("boardSymmetric");
            case WIN_WITH_ONLY_THREE_SUN_PRODUCERS -> won(event)
                    && event.intAttribute("plantsUsedCount", -1) == 3
                    && event.booleanAttribute("allUsedPlantsSunProducers");
            case WIN_WITH_EMPTY_COLUMN -> won(event)
                    && containsToken(event.attribute("emptyColumns"), binding);
            case WIN_WITH_EMPTY_ROW -> won(event)
                    && containsToken(event.attribute("emptyRows"), binding);
            case WIN_WITH_EMPTY_ROW_AND_COLUMN -> won(event)
                    && containsToken(event.attribute("emptyRows"), binding)
                    && containsToken(event.attribute("emptyColumns"), binding);
            case LAWNMOWER_KILLS -> event.getType() == DomainEventType.ZOMBIE_KILLED
                    && "lawnmower".equalsIgnoreCase(nullToEmpty(event.attribute("cause")))
                    && incrementTo(progress, "lawnmowerKills", event.getAmount(), numeric(binding));
        };
    }

    private boolean eventTouches(Quest quest, DomainEvent event) {
        return switch (quest.getConditionType()) {
            case SUN_PRODUCED_IN_DAY -> event.getType() == DomainEventType.SUN_PRODUCED;
            case ZOMBIES_KILLED_IN_CHAPTER, ZOMBIES_KILLED_ONLY_BY_PLANT,
                    ZOMBIES_KILLED_ONLY_BY_CACTUS, KILLS_WITHIN_FIRST_WAVE_SECONDS,
                    KILLS_ONLY_BY_PLANT_FAMILY, FIRST_COLUMN_KILLS_WITHOUT_MOWER,
                    LAWNMOWER_KILLS -> event.getType() == DomainEventType.ZOMBIE_KILLED;
            case EXPLOSIVE_PLANTS_USED -> event.getType() == DomainEventType.PLANT_PLANTED;
            default -> event.getType() == DomainEventType.LEVEL_COMPLETED
                    || event.getType() == DomainEventType.LEVEL_STARTED;
        };
    }

    private void complete(User user, Quest quest, QuestProgress progress, DomainEvent event) {
        if (progress.isCompleted()) {
            return;
        }
        progress.setCompleted(true);
        progress.setCompletionDate(event.getOccurredAt().toLocalDate().toString());
        if (quest.getCategory() == QuestCategory.DAILY) {
            user.setCompletedDailyQuests(user.getCompletedDailyQuests() + 1);
        } else {
            user.setCompletedNonDailyQuests(user.getCompletedNonDailyQuests() + 1);
        }
    }

    private boolean resetDailyRows(User user) {
        boolean changed = false;
        String today = users.today().toString();
        for (QuestProgress progress : user.getQuestProgress().values()) {
            Quest quest = progress == null ? null : catalog.findByName(progress.getQuestName());
            if (quest != null && quest.getRecurrence() == QuestRecurrence.DAILY
                    && !today.equals(progress.getRecurrenceDate())) {
                progress.resetForDate(today);
                changed = true;
            }
        }
        return changed;
    }

    private void resetLevelCounters(QuestProgress progress) {
        progress.getCounters().keySet().removeIf(key -> key.startsWith("level."));
        progress.getFacts().keySet().removeIf(key -> key.startsWith("level."));
    }

    private boolean exclusiveKillBy(QuestProgress progress, DomainEvent event,
                                    String attribute, String expected, int target) {
        if (event.getType() != DomainEventType.ZOMBIE_KILLED) {
            return false;
        }
        String actual = event.attribute(attribute);
        if (!equalsNormalized(expected, actual)) {
            progress.increment("level.disqualifiedKills", event.getAmount());
            return false;
        }
        if (progress.counter("level.disqualifiedKills") > 0) {
            return false;
        }
        return incrementTo(progress, "level.exclusiveKills", event.getAmount(), target);
    }

    private boolean winStreak(QuestProgress progress, DomainEvent event) {
        if (event.getType() != DomainEventType.LEVEL_COMPLETED) {
            return false;
        }
        if (won(event) && event.intAttribute("difficulty", -1) == 5) {
            return incrementTo(progress, "winStreak", 1, 5);
        }
        progress.getCounters().put("winStreak", 0);
        progress.setProgress(0);
        return false;
    }

    private boolean amountTarget(QuestProgress progress, DomainEvent event,
                                 DomainEventType type, int target) {
        return event.getType() == type
                && incrementTo(progress, "amount", event.getAmount(), target);
    }

    private boolean incrementTo(QuestProgress progress, String key, int amount, int target) {
        int value = progress.increment(key, amount);
        progress.setProgress(Math.min(value, target));
        return value >= target;
    }

    private boolean won(DomainEvent event) {
        return event.getType() == DomainEventType.LEVEL_COMPLETED
                && event.booleanAttribute("won");
    }

    private boolean isAllowedBinding(Quest quest, String binding) {
        if (!quest.requiresBinding()) {
            return binding == null;
        }
        String spec = quest.getVariableSpec();
        if (spec.matches("[0-9]+(?:-[0-9]+)+")) {
            for (String value : spec.split("-")) {
                if (value.equals(binding)) {
                    return true;
                }
            }
            return false;
        }
        return binding != null && !binding.isBlank();
    }

    private int numeric(String value) {
        if (value == null) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private boolean containsToken(String csv, String expected) {
        if (csv == null || expected == null) {
            return false;
        }
        for (String token : csv.split(",")) {
            if (equalsNormalized(token, expected)) {
                return true;
            }
        }
        return false;
    }

    private boolean equalsNormalized(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private String normalize(String value) {
        return nullToEmpty(value).trim().toLowerCase(Locale.ROOT)
                .replace("_", "").replace("-", "").replace(" ", "");
    }

    private String normalizeBinding(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String storageKey(String questName, String variableValue) {
        return questName + (variableValue == null ? "" : KEY_SEPARATOR + variableValue);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
