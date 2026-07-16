package service.quest;

import model.News;
import model.enums.NewsType;
import model.enums.PlantType;
import model.quest.ActiveQuest;
import model.quest.QuestCatalog;
import model.quest.QuestCategory;
import model.quest.QuestDefinition;
import model.quest.QuestEvent;
import model.quest.QuestEventListener;
import model.quest.QuestEventType;
import model.quest.QuestProgress;
import model.quest.QuestReward;
import model.quest.RewardKind;
import model.user.User;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Coordinates quest state without coupling gameplay classes to quest definitions. */
public class QuestService implements QuestEventListener {
    private final User user;
    private final QuestCatalog catalog;
    private final Clock clock;

    public QuestService(User user, QuestCatalog catalog, Clock clock) {
        this.user = user; this.catalog = catalog; this.clock = clock;
        user.applyDefaults();
    }

    public void activate(String canonicalName, long target) {
        QuestDefinition definition = definition(canonicalName);
        user.getActiveQuests().putIfAbsent(canonicalName,
                new ActiveQuest(canonicalName, target, definition.getCanonicalOrder()));
        QuestProgress progress = user.getQuestProgress().computeIfAbsent(canonicalName, key -> new QuestProgress());
        if (definition.getCategory() == QuestCategory.DAILY && progress.getRecurrenceDate() == null) {
            progress.setRecurrenceDate(today());
        }
    }

    public List<QuestDefinition> travelLogPage(String pageName) {
        QuestCategory category = QuestCategory.fromPageName(pageName);
        List<QuestDefinition> result = new ArrayList<>();
        for (ActiveQuest active : user.getActiveQuests().values()) {
            QuestDefinition definition = definition(active.getCanonicalName());
            refreshDaily(definition, user.getQuestProgress().get(active.getCanonicalName()));
            if (definition.getCategory() == category) result.add(definition);
        }
        result.sort(Comparator.comparingInt((QuestDefinition q) -> effectivePriority(q).getOrder())
                .thenComparingInt(QuestDefinition::getCanonicalOrder));
        return result;
    }

    @Override
    public void onQuestEvent(QuestEvent event) {
        for (ActiveQuest active : user.getActiveQuests().values()) {
            QuestDefinition definition = definition(active.getCanonicalName());
            QuestProgress progress = user.getQuestProgress().computeIfAbsent(active.getCanonicalName(), key -> new QuestProgress());
            refreshDaily(definition, progress);
            if (progress.isCompleted()) continue;
            long delta = matchingProgress(event, definition);
            if (delta <= 0) continue;
            progress.add(delta);
            if (progress.getProgress() >= active.getTarget()) {
                progress.complete();
                if (definition.getCategory() == QuestCategory.DAILY) user.setCompletedDailyQuests(user.getCompletedDailyQuests() + 1);
                else user.setCompletedNonDailyQuests(user.getCompletedNonDailyQuests() + 1);
            }
        }
    }

    public void claim(String canonicalName) {
        QuestDefinition definition = definition(canonicalName);
        QuestProgress progress = user.getQuestProgress().get(canonicalName);
        if (progress == null || !progress.isCompleted()) throw new IllegalStateException("Quest is not complete: " + canonicalName);
        if (progress.isClaimed()) throw new IllegalStateException("Quest reward already claimed: " + canonicalName);
        applyReward(definition, user.getActiveQuests().get(canonicalName), canonicalName);
        progress.claim();
    }

    private long matchingProgress(QuestEvent event, QuestDefinition definition) {
        String explicitQuest = event.get("quest");
        if (explicitQuest != null) return explicitQuest.equals(definition.getCanonicalName()) ? event.getAmount() : 0;
        String condition = definition.getConditionText();
        if (event.getType() == QuestEventType.SUN_PRODUCED && condition.contains("خورشید")) return event.getAmount();
        if (event.getType() == QuestEventType.ZOMBIE_KILLED && condition.contains("زامبی")) return event.getAmount();
        if (event.getType() == QuestEventType.PLANT_PLANTED && condition.contains("گیاه")) return event.getAmount();
        if (event.getType() == QuestEventType.MINIGAME_COMPLETED && definition.getCategory() == QuestCategory.MINIGAME) return event.getAmount();
        if (event.getType() == QuestEventType.LEVEL_COMPLETED && (condition.contains("مرحله") || condition.contains("بازی"))) return event.getAmount();
        return 0;
    }

    private void applyReward(QuestDefinition definition, ActiveQuest active, String questName) {
        QuestReward reward = definition.getReward();
        if (reward == null) return;
        int amount = reward.getAmount();
        if (definition.getRewardText().contains("sun_amount /")) amount = (int) (active.getTarget() / 100);
        if (definition.getRewardText().contains("۲۰ - n")) amount = Math.max(0, 20 - (int) active.getTarget());
        switch (reward.getKind()) {
            case COINS -> user.setCoins(user.getCoins() + amount);
            case GEMS -> user.setGems(user.getGems() + amount);
            case INVENTORY -> user.getInventory().merge(reward.getItem(), amount, Integer::sum);
            case PLANT_UNLOCK -> unlockPlant(reward, questName);
            case LEVEL_UNLOCK -> unlockLevel(reward, questName);
        }
    }

    private void unlockPlant(QuestReward reward, String questName) {
        PlantType chosen = null;
        for (PlantType type : PlantType.values()) {
            if (!user.getCollection().hasPlant(type)) { chosen = type; break; }
        }
        if (chosen == null) return;
        user.getCollection().purchasePlant(chosen);
        user.getNewsList().add(new News("Plant unlocked", chosen + " unlocked by " + questName, NewsType.PLANT_UNLOCKED));
    }

    private void unlockLevel(QuestReward reward, String questName) {
        String item = reward.getItem();
        if (item == null || !item.matches("[0-9]+:[0-9]+")) throw new IllegalStateException("Level reward lacks canonical chapter:level data");
        String[] parts = item.split(":");
        int chapter = Integer.parseInt(parts[0]); int level = Integer.parseInt(parts[1]);
        user.getUnlockedLevels().computeIfAbsent(chapter, key -> new java.util.LinkedHashSet<>()).add(level);
        user.getNewsList().add(new News("Level unlocked", "Chapter " + chapter + ", level " + level + " unlocked by " + questName, NewsType.LEVEL_UNLOCKED));
    }

    private model.quest.QuestPriority effectivePriority(QuestDefinition definition) {
        if (definition.getCategory() == QuestCategory.STORY &&
                (definition.getReward().getKind() == RewardKind.PLANT_UNLOCK || definition.getReward().getKind() == RewardKind.LEVEL_UNLOCK)) {
            return model.quest.QuestPriority.CRITICAL;
        }
        if (definition.getCategory() == QuestCategory.EPIC && definition.getReward().getKind() == RewardKind.GEMS) {
            return model.quest.QuestPriority.HIGH;
        }
        return definition.getPriority();
    }

    private void refreshDaily(QuestDefinition definition, QuestProgress progress) {
        if (definition.getCategory() != QuestCategory.DAILY || progress == null) return;
        if (!today().equals(progress.getRecurrenceDate())) progress.reset(today());
    }

    private String today() { return LocalDate.now(clock).toString(); }
    private QuestDefinition definition(String canonicalName) {
        return catalog.getDefinitions().stream().filter(q -> q.getCanonicalName().equals(canonicalName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown canonical quest: " + canonicalName));
    }
}
