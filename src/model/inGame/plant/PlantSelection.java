package model.inGame.plant;

import model.enums.PlantType;
import model.user.Collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PlantSelection {
    private final PlantRegistry registry;
    private final int capacity;
    private final Set<PlantType> selected = new LinkedHashSet<>();

    public PlantSelection(PlantRegistry registry, int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Selection capacity must be positive.");
        }
        this.registry = registry;
        this.capacity = capacity;
    }

    public void select(PlantType type, Collection collection) {
        PlantDefinition definition = registry.require(type);
        if (collection == null || !collection.hasPlant(type)) {
            throw new IllegalStateException(definition.getName() + " is not unlocked.");
        }
        if (selected.contains(type)) {
            throw new IllegalStateException(definition.getName() + " is already selected.");
        }
        if (selected.size() >= capacity) {
            throw new IllegalStateException("Seed selection is full.");
        }
        selected.add(type);
    }

    public void remove(PlantType type) {
        selected.remove(type);
    }

    public Set<PlantType> getTypes() {
        return Collections.unmodifiableSet(selected);
    }
}
