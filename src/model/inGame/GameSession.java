package model.inGame;

import model.enums.PlantType;
import model.level.Level;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A single play of a level, created fresh by {@code start game}.
 *
 * <p>The session owns its own copy of the finalized selection, the difficulty
 * snapshot taken at start, the plants whose plant-food effect should fire on
 * planting (from the diamond boost), and the plants that still have a stored
 * greenhouse boost to consume on first use. Two sessions never share mutable
 * state.</p>
 */
public class GameSession {

    private final Level level;
    private final PlantSelection selection;
    private final int difficulty;
    private final Set<PlantType> pendingGreenhouseBoosts;
    private final Set<PlantType> usedPlants = new LinkedHashSet<>();

    public GameSession(
            Level level,
            PlantSelection selection,
            int difficulty,
            Set<PlantType> pendingGreenhouseBoosts
    ) {
        this.level = level;
        this.selection = selection.copy();
        this.difficulty = difficulty;
        this.pendingGreenhouseBoosts = new LinkedHashSet<>(pendingGreenhouseBoosts);
    }

    public Level getLevel() {
        return level;
    }

    public PlantSelection getSelection() {
        return selection;
    }

    public int getDifficulty() {
        return difficulty;
    }

    /** Whether this plant's plant-food effect should fire immediately when planted. */
    public boolean hasImmediatePlantFood(PlantType type) {
        return selection.isDiamondBoosted(type);
    }

    /** Whether a stored greenhouse boost is still waiting to be consumed for this plant. */
    public boolean hasPendingGreenhouseBoost(PlantType type) {
        return pendingGreenhouseBoosts.contains(type);
    }

    public Set<PlantType> getPendingGreenhouseBoosts() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(pendingGreenhouseBoosts));
    }

    /**
     * Records that a plant was used and reports whether a stored greenhouse boost
     * was consumed by this first use. The pending flag is cleared so a later use
     * in the same session does not consume again.
     */
    public boolean useAndConsumeGreenhouseBoost(PlantType type) {
        boolean firstUse = usedPlants.add(type);

        if (firstUse && pendingGreenhouseBoosts.remove(type)) {
            return true;
        }

        return false;
    }
}
