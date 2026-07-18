package model.utility;

/** One row from the canonical quests workbook. The canonical name is its source identity. */
public class Quest {
    private String name;
    private QuestCategory category;
    private String conditionText;
    private Reward reward;
    private QuestPriority priority;
    private String variableSpec;
    private int canonicalOrder;
    private QuestConditionType conditionType;
    private QuestRecurrence recurrence;

    public Quest() {
    }

    public Quest(String name, QuestCategory category, String conditionText,
                 Reward reward, QuestPriority priority, String variableSpec,
                 int canonicalOrder, QuestConditionType conditionType) {
        this.name = name;
        this.category = category;
        this.conditionText = conditionText;
        this.reward = reward;
        this.priority = priority;
        this.variableSpec = variableSpec == null ? "" : variableSpec;
        this.canonicalOrder = canonicalOrder;
        this.conditionType = conditionType;
        this.recurrence = category == QuestCategory.DAILY
                ? QuestRecurrence.DAILY : QuestRecurrence.UNSPECIFIED;
    }

    public String getName() {
        return name;
    }

    public QuestCategory getCategory() {
        return category;
    }

    public String getConditionText() {
        return conditionText;
    }

    public Reward getReward() {
        return reward;
    }

    public QuestPriority getPriority() {
        return priority;
    }

    public String getVariableSpec() {
        return variableSpec;
    }

    public int getCanonicalOrder() {
        return canonicalOrder;
    }

    public QuestConditionType getConditionType() {
        return conditionType;
    }

    public QuestRecurrence getRecurrence() {
        return recurrence;
    }

    public boolean requiresBinding() {
        return !variableSpec.isBlank();
    }
}
