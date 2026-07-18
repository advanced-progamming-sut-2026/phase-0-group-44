package model.miniGame;

import java.util.Locale;

/** Phase-1 minigames exposed through Travel Log, with bonus scope explicit. */
public enum MiniGameId {
    VASE_BREAKER("vase-breaker", "Vase Breaker", false),
    BOWLING_WALLNUT("bowling-wallnut", "Bowling Wall-nut", false),
    I_ZOMBIE("i-zombie", "I, Zombie", false),
    BEGHOULED("beghouled", "Beghouled", true),
    ZOMBOTANY("zombotany", "Zombotany", true);

    private final String token;
    private final String displayName;
    private final boolean bonus;

    MiniGameId(String token, String displayName, boolean bonus) {
        this.token = token;
        this.displayName = displayName;
        this.bonus = bonus;
    }

    public String getToken() {
        return token;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isBonus() {
        return bonus;
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
                .replaceAll("[^a-z0-9]", "");
    }
}
