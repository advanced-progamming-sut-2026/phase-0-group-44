package model.miniGame.framework;

public enum MinigameId {
    BOWLING_WALLNUT("bowling-wallnut", "Bowling Wall-nut"),
    CONVEYOR_BELT("conveyor-belt", "Conveyor Belt"),
    VASEBREAKER("vasebreaker", "Vasebreaker");

    private final String token;
    private final String displayName;
    MinigameId(String token, String displayName) { this.token = token; this.displayName = displayName; }
    public String getToken() { return token; }
    public String getDisplayName() { return displayName; }
    public static MinigameId fromToken(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase().replace('_', '-').replace(' ', '-');
        for (MinigameId id : values()) if (id.token.equals(normalized)) return id;
        return null;
    }
}
