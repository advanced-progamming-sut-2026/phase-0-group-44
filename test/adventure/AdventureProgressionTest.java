package adventure;

import model.Store;
import model.config.AdventureCatalog;
import model.config.ChapterCatalog;
import model.config.GameWorld;
import model.inGame.GameSession;
import model.inGame.PlantSelection;
import model.level.Level;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.GameConclusionService;
import service.NewsService;
import service.UserService;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureProgressionTest {
    @TempDir
    Path tempDir;

    private User user;

    @BeforeEach
    void setUp() {
        Store.setUsers(new ArrayList<>());
        user = new User();
        user.setUsername("adventurer");
        user.applyDefaults();
    }

    @Test
    void prerequisitesUnlockLevelsThenDeferredBossAndNextChapter() {
        assertTrue(ChapterCatalog.isLevelUnlocked(user, GameWorld.ANCIENT_EGYPT, 1));
        assertFalse(ChapterCatalog.isLevelUnlocked(user, GameWorld.ANCIENT_EGYPT, 2));
        assertFalse(ChapterCatalog.isUnlocked(user, GameWorld.FROSTBITE_CAVES));

        ChapterCatalog.completeLevel(user,
                AdventureCatalog.level(GameWorld.ANCIENT_EGYPT, 1));
        assertTrue(ChapterCatalog.isLevelUnlocked(user, GameWorld.ANCIENT_EGYPT, 2));

        ChapterCatalog.completeLevel(user,
                AdventureCatalog.level(GameWorld.ANCIENT_EGYPT, 2));
        assertTrue(ChapterCatalog.isLevelUnlocked(user, GameWorld.ANCIENT_EGYPT, 3));

        ChapterCatalog.completeLevel(user,
                AdventureCatalog.level(GameWorld.ANCIENT_EGYPT, 3));
        assertTrue(ChapterCatalog.isLevelUnlocked(user, GameWorld.ANCIENT_EGYPT, 4));
        assertTrue(AdventureCatalog.level(GameWorld.ANCIENT_EGYPT, 4).isBossDeferred());
        assertTrue(ChapterCatalog.isLevelUnlocked(user, GameWorld.FROSTBITE_CAVES, 1));
    }

    @Test
    void completingAConfiguredLevelUpdatesProfileNewsAndPersistence() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC);
        UserService users = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")), clock);
        users.addUser(user);
        NewsService news = new NewsService(users);
        GameConclusionService conclusions = new GameConclusionService(users, news);
        Level level = AdventureCatalog.level(GameWorld.ANCIENT_EGYPT, 1);
        GameSession session = new GameSession(
                level, new PlantSelection(), 3, Set.of());

        conclusions.onWin(user, session);

        assertEquals(1, user.getCompletedLevelCount());
        assertEquals(1, user.getGamesPlayed());
        assertEquals(1, user.getLatestCompletedChapter());
        assertEquals(1, user.getLatestCompletedLevel());
        assertTrue(ChapterCatalog.isLevelUnlocked(user, GameWorld.ANCIENT_EGYPT, 2));
        assertFalse(user.getNewsList().isEmpty());
    }
}
