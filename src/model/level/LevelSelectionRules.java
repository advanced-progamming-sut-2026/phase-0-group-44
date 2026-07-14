package model.level;

import model.enums.PlantType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Per-level rules for the plant-selection screen: how many slots there are,
 * which plants the level allows, and whether selection is skipped entirely.
 *
 * <p>Real level configurations are not part of this section, so a normal level
 * defaults to {@value #DEFAULT_CAPACITY} slots with every owned plant allowed.
 * Special levels that bypass or constrain selection will supply their own rules
 * later, through the level configuration, rather than through hardcoded command
 * exceptions.</p>
 */
public final class LevelSelectionRules {

    public static final int DEFAULT_CAPACITY = 8;

    private final int capacity;
    private final Set<PlantType> allowedPlants;
    private final boolean selectionBypassed;

    private LevelSelectionRules(int capacity, Set<PlantType> allowedPlants, boolean bypassed) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative.");
        }

        this.capacity = capacity;
        this.allowedPlants = allowedPlants;
        this.selectionBypassed = bypassed;
    }

    /** A normal level: {@value #DEFAULT_CAPACITY} slots, every owned plant allowed. */
    public static LevelSelectionRules normal() {
        return new LevelSelectionRules(DEFAULT_CAPACITY, null, false);
    }

    /** A level with a custom slot count and every owned plant allowed. */
    public static LevelSelectionRules withCapacity(int capacity) {
        return new LevelSelectionRules(capacity, null, false);
    }

    /** A level that restricts selection to a fixed set of plants. */
    public static LevelSelectionRules restrictedTo(int capacity, Set<PlantType> allowed) {
        return new LevelSelectionRules(capacity, EnumSet.copyOf(allowed), false);
    }

    /** A level whose selection screen is skipped (e.g. a conveyor-belt level). */
    public static LevelSelectionRules bypassed() {
        return new LevelSelectionRules(0, null, true);
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isSelectionBypassed() {
        return selectionBypassed;
    }

    /** Whether the level allows this plant to be selected at all (ownership aside). */
    public boolean allows(PlantType type) {
        return allowedPlants == null || allowedPlants.contains(type);
    }

    /** The explicit allow-list, or an empty set when the level allows all owned plants. */
    public Set<PlantType> getAllowedPlants() {
        return allowedPlants == null ? Collections.emptySet()
                : Collections.unmodifiableSet(allowedPlants);
    }
}
