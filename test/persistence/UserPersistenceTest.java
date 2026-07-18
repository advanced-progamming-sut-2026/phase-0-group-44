package persistence;

import model.News;
import model.Result;
import model.Store;
import model.enums.NewsType;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.miniGame.GreenhouseSlot;
import model.user.DailyShopState;
import model.user.PlantCard;
import model.user.Settings;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import repository.UserRepository;
import service.UserService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Persistence tests. Time is supplied by a fixed clock, so nothing here depends
 * on the real system time and no test waits.
 */
class UserPersistenceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-14T09:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private Path savePath;

    @BeforeEach
    void resetGlobalState() {
        savePath = tempDir.resolve("users.json");
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
    }

    private UserService newService() {
        return new UserService(new JsonUserRepository(savePath), FIXED_CLOCK);
    }

    private User sampleUser(String username) {
        User user = new User(
                username,
                User.hashPassword("Abcdef1!"),
                username + "-nick",
                username + "@example.com",
                "male",
                7,
                12,
                new model.user.Collection(),
                new ArrayList<PlantType>(),
                new Settings(),
                500,
                40,
                9001,
                new model.miniGame.GreenHouse(),
                "first pet?",
                "cactus"
        );
        user.applyDefaults();

        return user;
    }

    // ---------- successful round trip ----------

    @Test
    @SuppressWarnings("PMD.ExcessiveMethodLength")
    void savedUserIsReloadedWithEveryPersistentField() {
        UserService service = newService();

        User user = sampleUser("kasra");
        user.getSettings().setDifficultyLevel(5);
        user.setCompletedLevelCount(11);
        user.setLatestCompletedChapter(2);
        user.setLatestCompletedLevel(4);
        user.setCompletedDailyQuests(3);
        user.setCompletedNonDailyQuests(6);
        user.setCompletedMiniGames(2);
        user.setPlantFood(5);
        user.setStayLoggedIn(true);

        user.getCollection().purchasePlant(PlantType.PEASHOOTER);
        user.getCollection().upgradePlant(PlantType.PEASHOOTER);
        user.getCollection().getPlantCard(PlantType.PEASHOOTER).addSeedPackets(17);
        user.getCollection().markZombieAsSeen(ZombieType.CONEHEAD);

        user.getUnlockedChapters().add(1);
        user.getUnlockedLevels().put(1, new LinkedHashSet<>(Set.of(1, 2, 3)));
        user.getPlantBoosts().put(PlantType.SUNFLOWER, 2);
        user.getInventory().put("plant-food", 4);
        user.getNewsList().add(new News("Sunflower", "unlocked", NewsType.PLANT_UNLOCKED));

        GreenhouseSlot slot = user.getGreenHouse().getSlot(0);
        slot.plant(PlantType.SUNFLOWER, FIXED_INSTANT.getEpochSecond());

        DailyShopState shop = user.getDailyShop();
        shop.setOfferId("offer-1");
        shop.setOfferDate(LocalDate.now(FIXED_CLOCK).toString());
        shop.markPurchased("item-a");

        assertTrue(service.addUser(user).getStatus());

        // A second service instance stands in for restarting the program.
        UserService reloaded = newService();
        Result<Integer> loadResult = reloaded.loadUsers();

        assertTrue(loadResult.getStatus());
        assertEquals(1, loadResult.getData().intValue());

        User restored = reloaded.findByUsername("kasra");
        assertNotNull(restored);
        assertEquals(User.hashPassword("Abcdef1!"), restored.getHashOfPassword());
        assertEquals("kasra-nick", restored.getNickname());
        assertEquals("kasra@example.com", restored.getEmail());
        assertEquals("male", restored.getGender());
        assertEquals("first pet?", restored.getQuestion());
        assertEquals("cactus", restored.getAnswerToQuestion());
        assertEquals(5, restored.getSettings().getDifficultyLevel());
        assertEquals(500, restored.getCoins());
        assertEquals(40, restored.getGems());
        assertEquals(12, restored.getGamesPlayed());
        assertEquals(9001, restored.getHighestMewPoint());
        assertEquals(11, restored.getCompletedLevelCount());
        assertEquals(2, restored.getLatestCompletedChapter());
        assertEquals(4, restored.getLatestCompletedLevel());
        assertEquals(3, restored.getCompletedDailyQuests());
        assertEquals(6, restored.getCompletedNonDailyQuests());
        assertEquals(2, restored.getCompletedMiniGames());
        assertEquals(5, restored.getPlantFood());

        PlantCard card = restored.getCollection().getPlantCard(PlantType.PEASHOOTER);
        assertNotNull(card);
        assertTrue(card.isUnlocked());
        assertEquals(2, card.getLevel());
        assertEquals(17, card.getSeedPackets());
        assertTrue(restored.getCollection().getSeenZombies().contains(ZombieType.CONEHEAD));

        assertTrue(restored.getUnlockedChapters().contains(1));
        assertEquals(Set.of(1, 2, 3), restored.getUnlockedLevels().get(1));
        assertEquals(2, restored.getPlantBoosts().get(PlantType.SUNFLOWER).intValue());
        assertEquals(4, restored.getInventory().get("plant-food").intValue());
        assertEquals(1, restored.getNewsList().size());
        assertFalse(restored.getNewsList().get(0).isRead());

        GreenhouseSlot restoredSlot = restored.getGreenHouse().getSlot(0);
        assertNotNull(restoredSlot);
        assertTrue(restoredSlot.isOccupied());
        assertEquals(PlantType.SUNFLOWER, restoredSlot.getPlantType());
        assertEquals(FIXED_INSTANT.getEpochSecond(), restoredSlot.getPlantedAtEpochSecond());

        assertEquals("offer-1", restored.getDailyShop().getOfferId());
        assertEquals("2026-07-14", restored.getDailyShop().getOfferDate());
        assertTrue(restored.getDailyShop().isPurchased("item-a"));
    }

    @Test
    void readNewsStaysReadAfterReload() {
        UserService service = newService();
        User user = sampleUser("reader");
        News news = new News("Chapter 2", "unlocked", NewsType.LEVEL_UNLOCKED);
        news.markAsRead();
        user.getNewsList().add(news);
        user.getNewsList().add(new News("Chapter 3", "unlocked", NewsType.LEVEL_UNLOCKED));
        service.addUser(user);

        User restored = reloadedUser("reader");

        assertTrue(restored.getNewsList().get(0).isRead());
        assertFalse(restored.getNewsList().get(1).isRead());
    }

    // ---------- multiple users and session state ----------

    @Test
    void multipleUsersAndStayLoggedInSessionSurviveRestart() {
        UserService service = newService();

        User first = sampleUser("alice");
        User second = sampleUser("bob");
        second.setStayLoggedIn(true);
        second.setCoins(1234);

        service.addUser(first);
        service.addUser(second);
        Store.setLoggedInUser(second);
        assertTrue(service.saveUsers().getStatus());

        UserService reloaded = newService();
        assertEquals(2, reloaded.loadUsers().getData().intValue());

        assertNotNull(reloaded.findByUsername("alice"));
        assertEquals(1234, reloaded.findByUsername("bob").getCoins());
        assertNotNull(Store.getLoggedInUser());
        assertEquals("bob", Store.getLoggedInUser().getUsername());
        assertEquals("bob", reloaded.getAutoLoginUser().getUsername());
    }

    @Test
    void sessionIsNotRestoredWhenStayLoggedInIsFalse() {
        UserService service = newService();
        User user = sampleUser("carol");
        service.addUser(user);
        Store.setLoggedInUser(user);
        service.saveUsers();

        UserService reloaded = newService();
        reloaded.loadUsers();

        assertNull(Store.getLoggedInUser());
        assertNull(reloaded.getAutoLoginUser());
    }

    @Test
    void duplicateUsernameIsRejected() {
        UserService service = newService();
        assertTrue(service.addUser(sampleUser("dave")).getStatus());

        Result<String> second = service.addUser(sampleUser("dave"));

        assertFalse(second.getStatus());
        assertEquals("username already taken", second.getMessage());
        assertEquals(1, Store.getUsers().size());
    }

    // ---------- boundaries, defaults, migration ----------

    @Test
    void missingSaveFileLoadsAnEmptyRoster() {
        UserService service = newService();

        Result<Integer> result = service.loadUsers();

        assertTrue(result.getStatus());
        assertEquals(0, result.getData().intValue());
        assertTrue(Store.getUsers().isEmpty());
        assertFalse(Files.exists(savePath));
    }

    @Test
    void legacyBareArraySaveIsMigratedAndKeptCompatible() throws IOException {
        String legacy = "[{\"username\":\"legacy\",\"hashOfPassword\":\"h\",\"coins\":77,"
                + "\"stayLoggedIn\":true}]";
        Files.writeString(savePath, legacy, StandardCharsets.UTF_8);

        UserService service = newService();
        Result<Integer> result = service.loadUsers();

        assertTrue(result.getStatus());
        assertEquals(1, result.getData().intValue());

        User migrated = service.findByUsername("legacy");
        assertEquals(77, migrated.getCoins());
        assertNotNull(migrated.getCollection());
        assertNotNull(migrated.getGreenHouse());
        assertNotNull(migrated.getDailyShop());
        assertEquals(3, migrated.getSettings().getDifficultyLevel());
        assertEquals("legacy", Store.getLoggedInUser().getUsername());

        service.saveUsers();
        SaveFile rewritten = new JsonUserRepository(savePath).load();
        assertEquals(SaveFile.CURRENT_VERSION, rewritten.getVersion());
        assertEquals(1, rewritten.getUsers().size());
    }

    @Test
    void difficultyDefaultsToThreeWhenTheSaveDoesNotContainSettings() throws IOException {
        String save = "{\"version\":1,\"users\":[{\"username\":\"nodiff\"}]}";
        Files.writeString(savePath, save, StandardCharsets.UTF_8);

        UserService service = newService();
        service.loadUsers();

        assertEquals(3, service.findByUsername("nodiff").getSettings().getDifficultyLevel());
    }

    @Test
    void saveFileFromANewerVersionIsRejectedRatherThanTruncated() throws IOException {
        Files.writeString(savePath, "{\"version\":999,\"users\":[]}", StandardCharsets.UTF_8);

        assertThrows(
                SaveDataException.class,
                () -> new JsonUserRepository(savePath).load()
        );
    }

    @Test
    void unreadableSaveIsQuarantinedAndNotOverwritten() throws IOException {
        Files.writeString(savePath, "{ this is not json", StandardCharsets.UTF_8);

        UserService service = newService();
        Result<Integer> result = service.loadUsers();

        assertFalse(result.getStatus());
        assertEquals(0, result.getData().intValue());

        Path backup = savePath.resolveSibling(
                "users.json.corrupt." + FIXED_INSTANT.getEpochSecond()
        );
        assertTrue(Files.exists(backup));
        assertEquals("{ this is not json", Files.readString(backup));
        assertFalse(Files.exists(savePath));
    }

    // ---------- safe writes ----------

    @Test
    void writingLeavesNoTemporaryFileBehind() throws IOException {
        UserService service = newService();
        service.addUser(sampleUser("erin"));

        assertTrue(Files.exists(savePath));
        assertFalse(Files.exists(savePath.resolveSibling("users.json.tmp")));

        try (java.util.stream.Stream<Path> entries = Files.list(tempDir)) {
            assertEquals(1, entries.count());
        }
    }

    @Test
    void failedSerializationLeavesThePreviousSaveIntact() {
        UserRepository healthy = new JsonUserRepository(savePath);
        SaveFile good = new SaveFile();
        good.setUsers(new ArrayList<>(java.util.List.of(sampleUser("frank"))));
        healthy.save(good);

        SaveSerializer exploding = new SaveSerializer() {
            @Override
            public String serialize(SaveFile saveFile) {
                throw new IllegalStateException("boom");
            }

            @Override
            public SaveFile deserialize(String content) {
                return new SaveFile();
            }
        };

        UserRepository broken =
                new JsonUserRepository(savePath, exploding, new AtomicFileWriter());

        assertThrows(IllegalStateException.class, () -> broken.save(new SaveFile()));

        SaveFile stillThere = healthy.load();
        assertEquals(1, stillThere.getUsers().size());
        assertEquals("frank", stillThere.getUsers().get(0).getUsername());
    }

    // ---------- injected clock ----------

    @Test
    void serviceReadsTimeFromTheInjectedClock() {
        UserService service = newService();

        assertEquals(LocalDate.of(2026, 7, 14), service.today());
        assertEquals(FIXED_INSTANT, service.getClock().instant());
    }

    private User reloadedUser(String username) {
        UserService reloaded = newService();
        reloaded.loadUsers();

        return reloaded.findByUsername(username);
    }
}
