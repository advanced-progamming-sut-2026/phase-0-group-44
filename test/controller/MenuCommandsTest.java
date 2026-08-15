package controller;

import model.News;
import model.Result;
import model.Store;
import model.config.ChapterCatalog;
import model.config.DifficultyScaling;
import model.config.GameWorld;
import model.enums.MenuName;
import model.enums.NewsType;
import model.user.Settings;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.NewsService;
import service.PasswordService;
import service.Sha256PasswordService;
import service.UserService;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Main, Game, Settings, News and Profile menu commands. */
class MenuCommandsTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-14T09:30:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private Path savePath;
    private UserService userService;
    private PasswordService passwordService;
    private User user;

    @BeforeEach
    void setUp() {
        savePath = tempDir.resolve("users.json");
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        Store.setCurrentMenu(MenuName.MAIN);
        Store.setRunning(true);

        passwordService = new Sha256PasswordService();
        userService = newService();

        user = new User();
        user.setUsername("kasra");
        user.setNickname("Kasra");
        user.setEmail("kasra@example.com");
        user.setHashOfPassword(passwordService.store("Abcdef1!"));
        user.applyDefaults();
        ChapterCatalog.applyDefaultUnlocks(user);
        userService.addUser(user);
        Store.setLoggedInUser(user);
    }

    private UserService newService() {
        return new UserService(new JsonUserRepository(savePath), CLOCK);
    }

    private User reloadUser() {
        UserService reloaded = newService();
        reloaded.loadUsers();

        return reloaded.findByUsername("kasra");
    }

    // ---------- logout ----------

    @Test
    void logoutClearsSessionPersistsAndReturnsToRegistration() {
        user.setStayLoggedIn(true);
        userService.updateUser(user);

        Result<String> result = new MainMenuController(userService).logout();

        assertTrue(result.getStatus());
        assertNull(Store.getLoggedInUser());
        assertEquals(MenuName.REGISTER, Store.getCurrentMenu());
        assertFalse(reloadUser().isStayLoggedIn());
    }

    // ---------- game: chapters ----------

    @Test
    void anUnlockedChapterCanBeEnteredAndALockedOneCannot() {
        GameMenuController game = new GameMenuController(userService);

        Result<String> egypt = game.enterChapter("Ancient Egypt");
        assertTrue(egypt.getStatus());
        assertEquals("Ancient Egypt", egypt.getData());

        Result<String> locked = game.enterChapter("Dark Ages");
        assertFalse(locked.getStatus());
        assertTrue(locked.getMessage().contains("locked"));

        Result<String> unknown = game.enterChapter("Atlantis");
        assertFalse(unknown.getStatus());
    }

    @Test
    void completingAChapterUnlocksTheNextOne() {
        assertFalse(ChapterCatalog.isUnlocked(user, GameWorld.FROSTBITE_CAVES));

        GameWorld unlocked = ChapterCatalog.unlockNext(user, GameWorld.ANCIENT_EGYPT);

        assertEquals(GameWorld.FROSTBITE_CAVES, unlocked);
        assertTrue(ChapterCatalog.isUnlocked(user, GameWorld.FROSTBITE_CAVES));
        assertTrue(new GameMenuController(userService).enterChapter("Frostbite Caves").getStatus());
    }

    @Test
    void chapterOnlyCommandResolvesTheLatestUnlockedNonBossLevel() {
        GameMenuController game = new GameMenuController(userService);
        assertEquals(1, game.enterLatestPlayableLevel("Ancient Egypt")
                .getData().getLevelNumber());

        ChapterCatalog.unlockLevel(user, GameWorld.ANCIENT_EGYPT, 2);
        ChapterCatalog.unlockLevel(user, GameWorld.ANCIENT_EGYPT, 3);
        ChapterCatalog.unlockLevel(user, GameWorld.ANCIENT_EGYPT, 4);

        assertEquals(3, game.enterLatestPlayableLevel("Ancient Egypt")
                .getData().getLevelNumber());
    }

    // ---------- game: wallets and cheat ----------

    @Test
    void walletsReportCurrentBalances() {
        user.setCoins(150);
        user.setGems(7);
        GameMenuController game = new GameMenuController(userService);

        assertEquals(150, game.showCoinWallet().getData());
        assertEquals(7, game.showGemWallet().getData());
    }

    @Test
    void cheatAddsToTheChosenWalletAndPersists() {
        GameMenuController game = new GameMenuController(userService);

        assertTrue(game.cheatAdd(100, "coin").getStatus());
        assertTrue(game.cheatAdd(5, "diamond").getStatus());

        assertEquals(100, user.getCoins());
        assertEquals(5, user.getGems());
        assertEquals(100, reloadUser().getCoins());
        assertEquals(5, reloadUser().getGems());
    }

    @Test
    void cheatRejectsNonPositiveAmountsAndUnknownCurrencies() {
        GameMenuController game = new GameMenuController(userService);

        assertFalse(game.cheatAdd(0, "coin").getStatus());
        assertFalse(game.cheatAdd(-5, "coin").getStatus());
        assertFalse(game.cheatAdd(10, "rubies").getStatus());
        assertEquals(0, user.getCoins());
        assertEquals(0, user.getGems());
    }

    // ---------- settings ----------

    @Test
    void difficultyAcceptsOneThroughFiveAndPersists() {
        SettingsMenuController settings = new SettingsMenuController(userService);

        assertTrue(settings.changeDifficulty(1).getStatus());
        assertTrue(settings.changeDifficulty(5).getStatus());
        assertEquals(5, reloadUser().getSettings().getDifficultyLevel());
    }

    @Test
    void difficultyRejectsOutOfRangeValues() {
        SettingsMenuController settings = new SettingsMenuController(userService);

        assertFalse(settings.changeDifficulty(0).getStatus());
        assertFalse(settings.changeDifficulty(6).getStatus());
        assertEquals(3, user.getSettings().getDifficultyLevel());
    }

    @Test
    void newAccountsDefaultToDifficultyThree() {
        assertEquals(3, new Settings().getDifficultyLevel());
        assertEquals(3, reloadUser().getSettings().getDifficultyLevel());
    }

    @Test
    void difficultyThreeIsTheNeutralScalingPoint() {
        assertEquals(1.0, DifficultyScaling.zombieHealthMultiplier(3), 1e-9);
        assertEquals(1.0, DifficultyScaling.zombieDamageMultiplier(3), 1e-9);
        assertEquals(1.0, DifficultyScaling.gameSpeedMultiplier(3), 1e-9);
        assertEquals(1.0, DifficultyScaling.waveCostMultiplier(3), 1e-9);
        assertEquals(1.0, DifficultyScaling.skySunRateMultiplier(3), 1e-9);
    }

    @Test
    void scalingMovesEachAxisInTheDirectionTheSpecRequires() {
        assertTrue(DifficultyScaling.zombieHealthMultiplier(5)
                > DifficultyScaling.zombieHealthMultiplier(3));
        assertTrue(DifficultyScaling.zombieDamageMultiplier(5)
                > DifficultyScaling.zombieDamageMultiplier(3));
        assertTrue(DifficultyScaling.gameSpeedMultiplier(5)
                > DifficultyScaling.gameSpeedMultiplier(3));
        assertTrue(DifficultyScaling.waveCostMultiplier(5)
                < DifficultyScaling.waveCostMultiplier(3));
        assertTrue(DifficultyScaling.skySunRateMultiplier(5)
                < DifficultyScaling.skySunRateMultiplier(3));

        assertTrue(DifficultyScaling.zombieHealthMultiplier(1)
                < DifficultyScaling.zombieHealthMultiplier(3));
        assertTrue(DifficultyScaling.waveCostMultiplier(1)
                > DifficultyScaling.waveCostMultiplier(3));
    }

    // ---------- news ----------

    @Test
    void showUnreadMarksEntriesReadAndPersistsSoTheyAreNotReturnedAgain() {
        NewsService newsService = new NewsService(userService);
        newsService.plantUnlocked(user, "Sunflower");
        newsService.zombieEncountered(user, "Conehead");

        NewsMenuController news = new NewsMenuController(userService);

        Result<ArrayList<News>> first = news.showUnreadNews();
        assertTrue(first.getStatus());
        assertEquals(2, first.getData().size());

        Result<ArrayList<News>> second = news.showUnreadNews();
        assertTrue(second.getData().isEmpty());

        for (News entry : reloadUser().getNewsList()) {
            assertTrue(entry.isRead(), "read state persisted");
        }
    }

    @Test
    void showAllReturnsEveryEntryWithoutChangingReadState() {
        user.getNewsList().add(new News("a", "b", NewsType.OTHER));
        userService.updateUser(user);

        Result<ArrayList<News>> all = new NewsMenuController(userService).showAllNews();

        assertTrue(all.getStatus());
        assertEquals(1, all.getData().size());
        assertFalse(reloadUser().getNewsList().get(0).isRead());
    }

    @Test
    void unreadCountDoesNotChangeReadState() {
        user.getNewsList().add(new News("a", "b", NewsType.OTHER));
        userService.updateUser(user);

        NewsMenuController news = new NewsMenuController(userService);
        Result<Integer> count = news.getUnreadNewsCount();

        assertTrue(count.getStatus());
        assertEquals(1, count.getData());
        assertFalse(reloadUser().getNewsList().get(0).isRead());
    }

    @Test
    void markAllAsReadReturnsHowManyEntriesChangedAndPersistsThem() {
        user.getNewsList().add(new News("a", "b", NewsType.OTHER));
        user.getNewsList().add(new News("c", "d", NewsType.OTHER));
        user.getNewsList().get(1).markAsRead();
        userService.updateUser(user);

        NewsMenuController news = new NewsMenuController(userService);
        Result<Integer> marked = news.markAllAsRead();

        assertTrue(marked.getStatus());
        assertEquals(1, marked.getData());
        assertTrue(reloadUser().getNewsList().stream().allMatch(News::isRead));
    }

    @Test
    void newsServiceCreatesAnEntryForEachKindOfUnlock() {
        NewsService newsService = new NewsService(userService);

        newsService.plantUnlocked(user, "Peashooter");
        newsService.zombieEncountered(user, "Buckethead");
        newsService.levelUnlocked(user, "Egypt 1-1");
        newsService.miniGameUnlocked(user, "Vasebreaker");

        assertEquals(4, user.getNewsList().size());
        assertEquals(NewsType.PLANT_UNLOCKED, user.getNewsList().get(0).getType());
        assertEquals(NewsType.ZOMBIE_UNLOCKED, user.getNewsList().get(1).getType());
        assertEquals(NewsType.LEVEL_UNLOCKED, user.getNewsList().get(2).getType());
        assertEquals(NewsType.MINIGAME_UNLOCKED, user.getNewsList().get(3).getType());
        assertEquals(4, reloadUser().getNewsList().size());
    }

    // ---------- profile ----------

    private ProfileMenuController profile() {
        return new ProfileMenuController(userService, passwordService);
    }

    @Test
    void changingAFieldToItsCurrentValueGivesADistinctError() {
        assertEquals("this is already your username",
                profile().changeUsername("kasra").getMessage());
        assertEquals("this is already your nickname",
                profile().changeNickname("Kasra").getMessage());
        assertEquals("this is already your email",
                profile().changeEmail("kasra@example.com").getMessage());
    }

    @Test
    void changingFieldsReusesRegistrationValidatorsAndPersists() {
        assertFalse(profile().changeUsername("bad name!").getStatus());
        assertFalse(profile().changeNickname("ab").getStatus());
        assertFalse(profile().changeEmail("nope").getStatus());

        assertTrue(profile().changeUsername("kasra2").getStatus());
        assertTrue(profile().changeNickname("Kasra The Second").getStatus());
        assertTrue(profile().changeEmail("kasra2@example.com").getStatus());

        assertEquals("kasra2", reloadByName("kasra2").getUsername());
        assertEquals("Kasra The Second", reloadByName("kasra2").getNickname());
        assertEquals("kasra2@example.com", reloadByName("kasra2").getEmail());
    }

    @Test
    void changingUsernameToAnotherUsersNameIsRejected() {
        User other = new User();
        other.setUsername("taken");
        other.applyDefaults();
        userService.addUser(other);

        assertEquals("username already taken", profile().changeUsername("taken").getMessage());
    }

    @Test
    void passwordChangeDistinguishesWrongOldFromNewEqualsCurrent() {
        Result<String> wrongOld = profile().changePassword("Newpass1!", "WrongOld1!");
        assertFalse(wrongOld.getStatus());
        assertEquals("the old password is incorrect", wrongOld.getMessage());

        Result<String> sameAsCurrent = profile().changePassword("Abcdef1!", "Abcdef1!");
        assertFalse(sameAsCurrent.getStatus());
        assertEquals("the new password is the same as the current one", sameAsCurrent.getMessage());

        Result<String> weak = profile().changePassword("abc", "Abcdef1!");
        assertFalse(weak.getStatus());

        assertTrue(profile().changePassword("Newpass1!", "Abcdef1!").getStatus());
        assertTrue(passwordService.matches("Newpass1!", reloadUser().getHashOfPassword()));
    }

    @Test
    void showInfoReportsTheRequiredFields() {
        user.setGamesPlayed(9);
        user.setCoins(320);
        user.setGems(11);
        user.setCompletedLevelCount(6);
        user.setHighestMewPoint(4200);

        String info = profile().showInfo().getData();

        assertTrue(info.contains("username: kasra"));
        assertTrue(info.contains("nickname: Kasra"));
        assertTrue(info.contains("played games: 9"));
        assertTrue(info.contains("coins: 320"));
        assertTrue(info.contains("diamonds: 11"));
        assertTrue(info.contains("completed levels: 6"));
        assertTrue(info.contains("highest Mew Point: 4200"));
    }

    private User reloadByName(String username) {
        UserService reloaded = newService();
        reloaded.loadUsers();

        return reloaded.findByUsername(username);
    }
}
