package model.quest;

public class QuestDefinition {
    private String canonicalName;
    private QuestCategory category;
    private String conditionText;
    private String rewardText;
    private QuestPriority priority;
    private String variable;
    private int canonicalOrder;
    private QuestReward reward;

    public QuestDefinition() {}
    public QuestDefinition(String canonicalName, QuestCategory category, String conditionText,
                           String rewardText, QuestPriority priority, String variable,
                           int canonicalOrder, QuestReward reward) {
        this.canonicalName = canonicalName; this.category = category; this.conditionText = conditionText;
        this.rewardText = rewardText; this.priority = priority; this.variable = variable;
        this.canonicalOrder = canonicalOrder; this.reward = reward;
    }
    public String getCanonicalName() { return canonicalName; }
    public QuestCategory getCategory() { return category; }
    public String getConditionText() { return conditionText; }
    public String getRewardText() { return rewardText; }
    public QuestPriority getPriority() { return priority; }
    public String getVariable() { return variable; }
    public int getCanonicalOrder() { return canonicalOrder; }
    public QuestReward getReward() { return reward; }
}
