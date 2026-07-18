package model.miniGame;

import java.util.LinkedHashSet;
import java.util.Set;

/** Persisted unlock and first-completion state for one mandatory minigame. */
public class MiniGameProgress {
    private boolean unlocked;
    private Set<Integer> unlockedLevels = new LinkedHashSet<>();
    private Set<Integer> completedLevels = new LinkedHashSet<>();
    private boolean completionCounted;

    public MiniGameProgress() {
        // Required by Gson.
    }

    public static MiniGameProgress unlockedAtLevelOne() {
        MiniGameProgress progress = new MiniGameProgress();
        progress.unlockMiniGame();
        return progress;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public boolean unlockMiniGame() {
        boolean changed = !unlocked;
        unlocked = true;
        changed |= getUnlockedLevels().add(1);
        return changed;
    }

    public Set<Integer> getUnlockedLevels() {
        if (unlockedLevels == null) {
            unlockedLevels = new LinkedHashSet<>();
        }
        return unlockedLevels;
    }

    public Set<Integer> getCompletedLevels() {
        if (completedLevels == null) {
            completedLevels = new LinkedHashSet<>();
        }
        return completedLevels;
    }

    public boolean isLevelUnlocked(int level) {
        return unlocked && getUnlockedLevels().contains(level);
    }

    public boolean isLevelCompleted(int level) {
        return getCompletedLevels().contains(level);
    }

    public boolean unlockLevel(int level) {
        if (!unlocked || level < 1 || level > 3) {
            return false;
        }
        return getUnlockedLevels().add(level);
    }

    /** @return true only for the first completion of this level. */
    public boolean completeLevel(int level) {
        if (!isLevelUnlocked(level)) {
            return false;
        }
        return getCompletedLevels().add(level);
    }

    public boolean isFullyCompleted() {
        return getCompletedLevels().containsAll(Set.of(1, 2, 3));
    }

    public boolean isCompletionCounted() {
        return completionCounted;
    }

    /** @return true only when the completed-minigame statistic changes. */
    public boolean markCompletionCounted() {
        if (!isFullyCompleted() || completionCounted) {
            return false;
        }
        completionCounted = true;
        return true;
    }

    public void applyDefaults() {
        getUnlockedLevels();
        getCompletedLevels().removeIf(level -> level == null || level < 1 || level > 3);
        getUnlockedLevels().removeIf(level -> level == null || level < 1 || level > 3);
        if (unlocked) {
            getUnlockedLevels().add(1);
        }
        for (Integer completed : getCompletedLevels()) {
            getUnlockedLevels().add(completed);
        }
        if (!isFullyCompleted()) {
            completionCounted = false;
        }
    }
}
