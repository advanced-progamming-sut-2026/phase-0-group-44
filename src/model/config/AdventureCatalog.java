package model.config;

import model.level.Chapter;
import model.level.Level;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Canonical Adventure campaign loaded from the repository CSV configuration. */
public final class AdventureCatalog {
    public static final Path DEFAULT_CHAPTERS = Path.of(
            "phase1", "assets", "Data", "adventure-chapters.csv");
    public static final Path DEFAULT_LEVELS = Path.of(
            "phase1", "assets", "Data", "adventure-levels.csv");

    private static final Map<GameWorld, Chapter> CHAPTERS =
            new AdventureConfigLoader(DEFAULT_CHAPTERS, DEFAULT_LEVELS).load();

    private AdventureCatalog() {
    }

    public static List<Chapter> chapters() {
        List<Chapter> result = new ArrayList<>();
        for (GameWorld world : GameWorld.values()) {
            result.add(CHAPTERS.get(world));
        }
        return List.copyOf(result);
    }

    public static Chapter chapter(GameWorld world) {
        return CHAPTERS.get(world);
    }

    public static Level level(GameWorld world, int levelNumber) {
        Chapter chapter = chapter(world);
        return chapter == null ? null : chapter.getLevel(levelNumber);
    }

    public static List<Level> allLevels() {
        List<Level> levels = new ArrayList<>();
        for (Chapter chapter : chapters()) {
            levels.addAll(chapter.getLevels());
        }
        return List.copyOf(levels);
    }
}
