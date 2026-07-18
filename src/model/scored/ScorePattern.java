package model.scored;

/** Five independent scoring patterns used by the scored game. */
public enum ScorePattern {
    QUICK_KILL("quick kills"),
    MULTI_KILL_PROJECTILE("multi-kill projectiles"),
    SIMULTANEOUS_KILLS("simultaneous kills"),
    SUN_EFFICIENCY("remaining sun"),
    PERFECT_DEFENSE("perfect defense");

    private final String label;

    ScorePattern(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
