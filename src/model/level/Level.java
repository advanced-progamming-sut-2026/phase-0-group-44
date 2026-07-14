package model.level;

/**
 * A playable level. Only the parts the plant-selection flow needs are defined
 * here; board layout, waves and win/loss belong to later sections.
 */
public interface Level {

    String getName();

    LevelSelectionRules getSelectionRules();
}
