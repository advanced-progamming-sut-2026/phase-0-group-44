package model.miniGame;

import java.util.Locale;

/** Canonical mandatory minigames present in the repository UML. */
public enum MiniGameId {
    VASE_BREAKER("vase-breaker", "Vase Breaker"),
    BOWLING_WALLNUT("bowling-wallnut", "Bowling Wall-nut");

    private final String token;
    private final String displayName;

    MiniGameId(String token, String displayName) {
        this.token = token;
        this.displayName = displayName;
    }

    public String getToken() {
        return token;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MiniGameId fromToken(String input) {
        if (input == null) {
            return null;
        }
        String normalized = normalize(input);
        for (MiniGameId id : values()) {
            if (normalize(id.token).equals(normalized)
                    || normalize(id.displayName).equals(normalized)
                    || normalize(id.name()).equals(normalized)) {
                return id;
            }
        }
        return null;
    }

    private static String normalize(String input) {
        return input.trim().toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");
    }
}
