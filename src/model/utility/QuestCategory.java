package model.utility;

import java.util.Locale;

/** Categories named by the canonical quests workbook. */
public enum QuestCategory {
    MAIN("main", "Main"),
    EPIC("epic", "Epic (Challenge)"),
    DAILY("daily", "Daily");

    private final String pageToken;
    private final String canonicalLabel;

    QuestCategory(String pageToken, String canonicalLabel) {
        this.pageToken = pageToken;
        this.canonicalLabel = canonicalLabel;
    }

    public String getPageToken() {
        return pageToken;
    }

    public String getCanonicalLabel() {
        return canonicalLabel;
    }

    public static QuestCategory fromCanonicalLabel(String value) {
        if (value == null) {
            return null;
        }
        for (QuestCategory category : values()) {
            if (category.canonicalLabel.equals(value.trim())) {
                return category;
            }
        }
        return null;
    }

    public static QuestCategory fromPageToken(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace("_", "-").replace(" ", "-");
        for (QuestCategory category : values()) {
            if (category.pageToken.equals(normalized)
                    || category.canonicalLabel.toLowerCase(Locale.ROOT).equals(normalized)) {
                return category;
            }
        }
        if ("challenge".equals(normalized)) {
            return EPIC;
        }
        return null;
    }
}