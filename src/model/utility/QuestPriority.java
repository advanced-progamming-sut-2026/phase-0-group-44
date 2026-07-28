package model.utility;

/** Priority values copied from the canonical quest table. */
public enum QuestPriority {
    CRITICAL(0, "Critical"),
    HIGH(1, "High"),
    MEDIUM(2, "Medium"),
    LOW(3, "Low");

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
