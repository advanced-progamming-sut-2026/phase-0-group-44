package model.level;

/**
 * A standard adventure level. Its selection rules default to a normal level's
 * (8 slots, all owned plants) unless a specific configuration is supplied.
 */
public class NormalLevel implements Level {

    private final String name;
    private final LevelSelectionRules selectionRules;

    public NormalLevel(String name) {
        this(name, LevelSelectionRules.normal());
    }

    public NormalLevel(String name, LevelSelectionRules selectionRules) {
        this.name = name;
        this.selectionRules = selectionRules;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LevelSelectionRules getSelectionRules() {
        return selectionRules;
    }
}
