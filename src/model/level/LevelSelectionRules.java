package model.level;

import model.enums.PlantCategory;
import model.enums.PlantType;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRegistry;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Data-driven plant-selection constraints for one level. */
public final class LevelSelectionRules {

    public static final int DEFAULT_CAPACITY = 8;

    private final int capacity;
    private final Set<PlantType> allowedPlants;
    private final Set<PlantType> forcedPlants;
    private final Set<PlantCategory> excludedCategories;
    private final boolean selectionBypassed;

    private LevelSelectionRules(
            int capacity,
            Set<PlantType> allowedPlants,
            Set<PlantType> forcedPlants,
            Set<PlantCategory> excludedCategories,
            boolean bypassed
    ) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative.");
        }
        this.capacity = capacity;
        this.allowedPlants = copyAllowedPlants(allowedPlants);
        this.forcedPlants = copyRequiredPlants(forcedPlants);
        this.excludedCategories = copyCategories(excludedCategories);
        this.selectionBypassed = bypassed;
        if (this.forcedPlants.size() > capacity && !bypassed) {
            throw new IllegalArgumentException("Forced plants exceed selection capacity.");
        }
    }

    public static LevelSelectionRules normal() {
        return new LevelSelectionRules(DEFAULT_CAPACITY, null, null, null, false);
    }

    public static LevelSelectionRules withCapacity(int capacity) {
        return new LevelSelectionRules(capacity, null, null, null, false);
    }

    public static LevelSelectionRules restrictedTo(int capacity, Set<PlantType> allowed) {
        return new LevelSelectionRules(capacity, allowed, null, null, false);
    }

    /** Locked-plants variant that excludes an entire canonical plant category. */
    public static LevelSelectionRules excludingCategories(
            int capacity,
            Set<PlantCategory> categories
    ) {
        return new LevelSelectionRules(capacity, null, null, categories, false);
    }

    /** Locked-plants variant that preselects plants the player cannot remove. */
    public static LevelSelectionRules withForcedPlants(
            int capacity,
            Set<PlantType> forced,
            Set<PlantCategory> excludedCategories
    ) {
        return new LevelSelectionRules(capacity, null, forced, excludedCategories, false);
    }

    public static LevelSelectionRules bypassed() {
        return new LevelSelectionRules(0, null, null, null, true);
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isSelectionBypassed() {
        return selectionBypassed;
    }

    public boolean allows(PlantType type) {
        if (type == null) {
            return false;
        }
        if (allowedPlants != null && !allowedPlants.contains(type)) {
            return false;
        }
        if (!excludedCategories.isEmpty()) {
            PlantDefinition definition = PlantRegistry.getDefault().findByType(type);
            if (definition != null && excludedCategories.contains(definition.getCategory())) {
                return false;
            }
        }
        return true;
    }

    public boolean isForced(PlantType type) {
        return forcedPlants.contains(type);
    }

    public Set<PlantType> getForcedPlants() {
        return forcedPlants;
    }

    public Set<PlantCategory> getExcludedCategories() {
        return excludedCategories;
    }

    public Set<PlantType> getAllowedPlants() {
        return allowedPlants == null ? Collections.emptySet() : allowedPlants;
    }

    private static Set<PlantType> copyAllowedPlants(Set<PlantType> source) {
        if (source == null) {
            return null;
        }
        return copyRequiredPlants(source);
    }

    private static Set<PlantType> copyRequiredPlants(Set<PlantType> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(source));
    }

    private static Set<PlantCategory> copyCategories(Set<PlantCategory> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(source));
    }
}
