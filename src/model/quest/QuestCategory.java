package model.quest;

import java.util.Locale;

public enum QuestCategory {
    STORY("main"), EPIC("epic"), DAILY("daily"), MINIGAME("minigame");

    private final String pageName;
    QuestCategory(String pageName) { this.pageName = pageName; }
    public String getPageName() { return pageName; }

    public static QuestCategory fromCanonical(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("روزانه")) return DAILY;
        if (normalized.contains("چالش") || normalized.contains("epic")) return EPIC;
        if (normalized.contains("اصلی")) return STORY;
        throw new IllegalArgumentException("Unknown canonical quest category: " + value);
    }

    public static QuestCategory fromPageName(String value) {
        for (QuestCategory category : values()) {
            if (category.pageName.equalsIgnoreCase(value)) return category;
        }
        throw new IllegalArgumentException("Unknown Travel Log page: " + value);
    }
}
