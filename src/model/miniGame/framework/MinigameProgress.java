package model.miniGame.framework;

import java.util.LinkedHashSet;
import java.util.Set;

public class MinigameProgress {
    private boolean unlocked;
    private int highestUnlockedLevel = 1;
    private Set<Integer> completedLevels = new LinkedHashSet<>();
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    public int getHighestUnlockedLevel() { return highestUnlockedLevel; }
    public void setHighestUnlockedLevel(int value) { highestUnlockedLevel = Math.max(1, Math.min(3, value)); }
    public Set<Integer> getCompletedLevels() {
        if (completedLevels == null) completedLevels = new LinkedHashSet<>();
        return completedLevels;
    }
    public boolean complete(int level) {
        boolean first = getCompletedLevels().add(level);
        if (first && level < 3) setHighestUnlockedLevel(level + 1);
        return first;
    }
}
