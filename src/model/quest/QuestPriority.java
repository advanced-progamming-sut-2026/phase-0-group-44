package model.quest;

public enum QuestPriority {
    CRITICAL(0), HIGH(1), MEDIUM(2), LOW(3);
    private final int order;
    QuestPriority(int order) { this.order = order; }
    public int getOrder() { return order; }

    public static QuestPriority fromCanonical(String value) {
        String v = value == null ? "" : value.trim();
        if (v.contains("بحرانی")) return CRITICAL;
        if (v.contains("بالا")) return HIGH;
        if (v.contains("متوسط")) return MEDIUM;
        if (v.contains("کم")) return LOW;
        throw new IllegalArgumentException("Unknown canonical quest priority: " + value);
    }
}
