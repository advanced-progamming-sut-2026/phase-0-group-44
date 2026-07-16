package model.quest;

public class QuestReward {
    private RewardKind kind;
    private int amount;
    private String item;
    private boolean random;

    public QuestReward() {}
    public QuestReward(RewardKind kind, int amount, String item, boolean random) {
        this.kind = kind; this.amount = amount; this.item = item; this.random = random;
    }
    public RewardKind getKind() { return kind; }
    public int getAmount() { return amount; }
    public String getItem() { return item; }
    public boolean isRandom() { return random; }
}
