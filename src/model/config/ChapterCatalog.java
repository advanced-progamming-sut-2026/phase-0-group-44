package model.config;

import model.user.User;

/**
 * Read-only knowledge about chapter ordering and unlocking.
 *
 * <p>Holds no gameplay: it only answers "which chapter is first", "what comes
 * next", and "is this chapter unlocked for this user". Completing a chapter's
 * content (done by gameplay, later) calls {@link #unlockNext} to open the
 * following world.</p>
 */
public final class ChapterCatalog {

    private ChapterCatalog() {
    }

    public static GameWorld first() {
        return GameWorld.ANCIENT_EGYPT;
    }

    /** The world after the given one, or {@code null} if it is the last. */
    public static GameWorld next(GameWorld world) {
        return GameWorld.fromChapterNumber(world.getChapterNumber() + 1);
    }

    /** Ensures a brand-new player can enter the first world and nothing else. */
    public static void applyDefaultUnlocks(User user) {
        if (user.getUnlockedChapters().isEmpty()) {
            user.getUnlockedChapters().add(first().getChapterNumber());
        }
    }

    public static boolean isUnlocked(User user, GameWorld world) {
        return user.getUnlockedChapters().contains(world.getChapterNumber());
    }

    /**
     * Opens the world that follows the given one. Returns the newly unlocked
     * world, or {@code null} when there is nothing further or it was already
     * open.
     */
    public static GameWorld unlockNext(User user, GameWorld completed) {
        GameWorld following = next(completed);

        if (following == null) {
            return null;
        }

        boolean added = user.getUnlockedChapters().add(following.getChapterNumber());

        return added ? following : null;
    }
}
