package model.inGame;

import model.enums.PlantType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

/**
 * The mutable state of one plant-selection screen: the ordered, unique set of
 * chosen plants and which of them have been given the paid (diamond) boost.
 *
 * <p>Holds no rules of its own — capacity and allow-lists live in
 * {@link model.level.LevelSelectionRules}; the controller enforces them.</p>
 */
public class PlantSelection {

    private final Set<PlantType> chosen = new LinkedHashSet<>();
    private final Set<PlantType> diamondBoosted = new LinkedHashSet<>();

    public boolean contains(PlantType type) {
        return chosen.contains(type);
    }

    public boolean add(PlantType type) {
        return chosen.add(type);
    }

    public boolean remove(PlantType type) {
        diamondBoosted.remove(type);

        return chosen.remove(type);
    }

    public int size() {
        return chosen.size();
    }

    public boolean isEmpty() {
        return chosen.isEmpty();
    }

    public List<PlantType> getChosen() {
        return Collections.unmodifiableList(new ArrayList<>(chosen));
    }

    public boolean isDiamondBoosted(PlantType type) {
        return diamondBoosted.contains(type);
    }

    public void markDiamondBoosted(PlantType type) {
        diamondBoosted.add(type);
    }

    /** A detached copy, used when a session is started so later edits don't leak in. */
    public PlantSelection copy() {
        PlantSelection copy = new PlantSelection();
        copy.chosen.addAll(this.chosen);
        copy.diamondBoosted.addAll(this.diamondBoosted);

        return copy;
    }
}
