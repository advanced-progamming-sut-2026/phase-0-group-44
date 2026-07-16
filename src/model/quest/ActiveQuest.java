package model.quest;

public class ActiveQuest {
    private String canonicalName;
    private long target;
    private int stableOrder;

    public ActiveQuest() {}
    public ActiveQuest(String canonicalName, long target, int stableOrder) {
        if (target <= 0) throw new IllegalArgumentException("Quest target must be positive.");
        this.canonicalName = canonicalName; this.target = target; this.stableOrder = stableOrder;
    }
    public String getCanonicalName() { return canonicalName; }
    public long getTarget() { return target; }
    public int getStableOrder() { return stableOrder; }
}
