package model.utility;

/** Read-only pairing used by Travel Log and reward claiming. */
public final class QuestInstance {
    private final String storageKey;
    private final Quest quest;
    private final QuestProgress progress;

    public QuestInstance(String storageKey, Quest quest, QuestProgress progress) {
        this.storageKey = storageKey;
        this.quest = quest;
        this.progress = progress;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public Quest getQuest() {
        return quest;
    }

    public QuestProgress getProgress() {
        return progress;
    }

    public String getDisplayName() {
        if (progress.getVariableValue() == null || progress.getVariableValue().isBlank()) {
            return quest.getName();
        }
        return quest.getName() + " [" + progress.getVariableValue() + "]";
    }
}
