package model.miniGame;

import java.util.Locale;

/** The three bowling plants explicitly described by the mandatory specification. */
public enum BowlingPlantType {
    BOWLING_WALL_NUT("bowling-wall-nut", "Bowling Wall-nut"),
    EXPLODE_O_NUT("explode-o-nut", "Explode-o-nut"),
    GIANT_WALL_NUT("giant-wall-nut", "Giant Wall-nut");

    private final String token;
    private final String displayName;

    BowlingPlantType(String token, String displayName) {
        this.token = token;
        this.displayName = displayName;
    }

    public String getToken() {
        return token;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BowlingPlantType fromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-');
        while (normalized.contains("--")) {
            normalized = normalized.replace("--", "-");
        }
        for (BowlingPlantType type : values()) {
            if (type.token.equals(normalized)
                    || type.name().toLowerCase(Locale.ROOT).replace('_', '-').equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
