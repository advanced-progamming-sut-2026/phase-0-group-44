package model.config;

/**
 * The four Adventure worlds, in play order.
 *
 * <p>Names and order come from the project's zombie chapter guide
 * ({@code Zombies.md}): Ancient Egypt, Frostbite Caves, Big Wave Beach, Dark
 * Ages. Chapter numbers match {@code User.getUnlockedChapters()} (1-based).
 * Per-world level layouts and gameplay are defined elsewhere and are not
 * modelled here.</p>
 */
public enum GameWorld {
    ANCIENT_EGYPT(1, "Ancient Egypt"),
    FROSTBITE_CAVES(2, "Frostbite Caves"),
    BIG_WAVE_BEACH(3, "Big Wave Beach"),
    DARK_AGES(4, "Dark Ages");

    private final int chapterNumber;
    private final String displayName;

    GameWorld(int chapterNumber, String displayName) {
        this.chapterNumber = chapterNumber;
        this.displayName = displayName;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Resolves a typed chapter name, tolerating case, spaces, hyphens and underscores. */
    public static GameWorld fromName(String input) {
        if (input == null) {
            return null;
        }

        String normalized = normalize(input);

        for (GameWorld world : values()) {
            if (normalize(world.displayName).equals(normalized)
                    || normalize(world.name()).equals(normalized)) {
                return world;
            }
        }

        return null;
    }

    public static GameWorld fromChapterNumber(int chapterNumber) {
        for (GameWorld world : values()) {
            if (world.chapterNumber == chapterNumber) {
                return world;
            }
        }

        return null;
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase().replace("-", "").replace("_", "").replace(" ", "");
    }
}
