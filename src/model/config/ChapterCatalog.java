package model.config;

import model.level.Level;
import model.user.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Read-only chapter ordering plus persistent level/chapter progression rules. */
public final class ChapterCatalog {

    private ChapterCatalog() {
    }

    public static GameWorld first() {
        return GameWorld.ANCIENT_EGYPT;
    }

    public static GameWorld next(GameWorld world) {
        return GameWorld.fromChapterNumber(world.getChapterNumber() + 1);
    }

    /** New users start with Ancient Egypt level 1 only. */
    public static void applyDefaultUnlocks(User user) {
        if (user == null) {
            return;
        }
        user.getUnlockedChapters().add(first().getChapterNumber());
        unlockLevel(user, first(), 1);
    }

    public static boolean isUnlocked(User user, GameWorld world) {
        return user != null && world != null
                && user.getUnlockedChapters().contains(world.getChapterNumber());
    }

    public static boolean isLevelUnlocked(User user, GameWorld world, int levelNumber) {
        if (!isUnlocked(user, world)) {
            return false;
        }
        Set<Integer> levels = user.getUnlockedLevels().get(world.getChapterNumber());
        return levels != null && levels.contains(levelNumber);
    }

    public static boolean unlockLevel(User user, GameWorld world, int levelNumber) {
        if (user == null || world == null || levelNumber < 1 || levelNumber > 4) {
            return false;
        }
        user.getUnlockedChapters().add(world.getChapterNumber());
        return user.getUnlockedLevels()
                .computeIfAbsent(world.getChapterNumber(), ignored -> new java.util.LinkedHashSet<>())
                .add(levelNumber);
    }

    /** Existing compatibility operation: opens the next chapter and its level 1. */
    public static GameWorld unlockNext(User user, GameWorld completed) {
        GameWorld following = next(completed);
        if (following == null) {
            return null;
        }
        boolean added = user.getUnlockedChapters().add(following.getChapterNumber());
        unlockLevel(user, following, 1);
        return added ? following : null;
    }

    /**
     * Records the Phase-1 prerequisite chain. Completing levels 1 and 2 unlocks
     * the next level. Completing level 3 exposes the deferred boss placeholder
     * and opens the next chapter because boss gameplay is reserved for Phase 2.
     */
    public static CompletionResult completeLevel(User user, Level completed) {
        if (user == null || completed == null || completed.getWorld() == null
                || completed.isBossDeferred()) {
            return CompletionResult.empty();
        }
        GameWorld world = completed.getWorld();
        int number = completed.getLevelNumber();
        List<String> unlocked = new ArrayList<>();
        if (number < 3) {
            int nextLevel = number + 1;
            if (unlockLevel(user, world, nextLevel)) {
                unlocked.add(world.getDisplayName() + " level " + nextLevel);
            }
        } else if (number == 3) {
            if (unlockLevel(user, world, 4)) {
                unlocked.add(world.getDisplayName() + " boss (deferred)");
            }
            GameWorld nextWorld = unlockNext(user, world);
            if (nextWorld != null) {
                unlocked.add(nextWorld.getDisplayName() + " level 1");
            }
        }
        return new CompletionResult(unlocked);
    }

    public static final class CompletionResult {
        private final List<String> unlockedLabels;

        private CompletionResult(List<String> unlockedLabels) {
            this.unlockedLabels = Collections.unmodifiableList(new ArrayList<>(unlockedLabels));
        }

        public static CompletionResult empty() {
            return new CompletionResult(List.of());
        }

        public List<String> getUnlockedLabels() {
            return unlockedLabels;
        }
    }
    public static void unlockAll(User user) {
        if (user == null) {
            return;
        }
        for (GameWorld world : GameWorld.values()) {
            user.getUnlockedChapters().add(world.getChapterNumber());
            for (int level = 1; level <= 4; level++) {
                unlockLevel(user, world, level);
            }
        }
    }
}
