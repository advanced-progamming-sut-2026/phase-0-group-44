package model.utility;

/** Priority values copied from the canonical quest table. */
public enum QuestPriority {
    CRITICAL(0, "بحرانی"),
    HIGH(1, "بالا"),
    MEDIUM(2, "متوسط"),
    LOW(3, "کم");

    private final int sortRank;
    private final String canonicalLabel;

    QuestPriority(int sortRank, String canonicalLabel) {
        this.sortRank = sortRank;
        this.canonicalLabel = canonicalLabel;
    }

    public int getSortRank() {
        return sortRank;
    }

    public String getCanonicalLabel() {
        return canonicalLabel;
    }

    public static QuestPriority fromCanonicalLabel(String value) {
        if (value == null) {
            return null;
        }
        for (QuestPriority priority : values()) {
            if (priority.canonicalLabel.equals(value.trim())) {
                return priority;
            }
        }
        return null;
    }
}
