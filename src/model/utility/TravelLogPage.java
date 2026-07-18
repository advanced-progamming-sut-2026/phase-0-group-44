package model.utility;

import java.util.Locale;

/** Travel Log pages: one per canonical category plus the required minigame page. */
public enum TravelLogPage {
    MAIN(QuestCategory.MAIN),
    EPIC(QuestCategory.EPIC),
    DAILY(QuestCategory.DAILY),
    MINIGAME(null);

    private final QuestCategory category;

    TravelLogPage(QuestCategory category) {
        this.category = category;
    }

    public QuestCategory getCategory() {
        return category;
    }

    public String getToken() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static TravelLogPage fromToken(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace("_", "-").replace(" ", "-");
        if ("minigame".equals(normalized) || "mini-game".equals(normalized)) {
            return MINIGAME;
        }
        QuestCategory category = QuestCategory.fromPageToken(normalized);
        if (category == null) {
            return null;
        }
        return switch (category) {
            case MAIN -> MAIN;
            case EPIC -> EPIC;
            case DAILY -> DAILY;
        };
    }
}
