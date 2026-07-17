package model.utility;

import java.util.LinkedHashMap;
import java.util.Map;

/** Persisted state for one active quest instance. */
public class QuestProgress {
    private String questName;
    private String variableValue;
    private int progress;
    private boolean completed;
    private boolean claimed;
    private boolean rewardApplied;
    private String recurrenceDate;
    private String completionDate;
    private Map<String, Integer> counters = new LinkedHashMap<>();
    private Map<String, String> facts = new LinkedHashMap<>();

    public QuestProgress() {
    }

    public QuestProgress(String questName, String variableValue, String recurrenceDate) {
        this.questName = questName;
        this.variableValue = variableValue;
        this.recurrenceDate = recurrenceDate;
    }

    public String getQuestName() {
        return questName;
    }

    public String getVariableValue() {
        return variableValue;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, progress);
    }

    public void addProgress(int amount) {
        if (amount > 0) {
            progress += amount;
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    public boolean isRewardApplied() {
        return rewardApplied;
    }

    public void setRewardApplied(boolean rewardApplied) {
        this.rewardApplied = rewardApplied;
    }

    public String getRecurrenceDate() {
        return recurrenceDate;
    }

    public void setRecurrenceDate(String recurrenceDate) {
        this.recurrenceDate = recurrenceDate;
    }

    public String getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(String completionDate) {
        this.completionDate = completionDate;
    }

    public Map<String, Integer> getCounters() {
        if (counters == null) {
            counters = new LinkedHashMap<>();
        }
        return counters;
    }

    public Map<String, String> getFacts() {
        if (facts == null) {
            facts = new LinkedHashMap<>();
        }
        return facts;
    }

    public int counter(String key) {
        return getCounters().getOrDefault(key, 0);
    }

    public int increment(String key, int amount) {
        int value = counter(key) + Math.max(0, amount);
        getCounters().put(key, value);
        return value;
    }

    public void resetForDate(String date) {
        progress = 0;
        completed = false;
        claimed = false;
        rewardApplied = false;
        recurrenceDate = date;
        completionDate = null;
        getCounters().clear();
        getFacts().clear();
    }
}
