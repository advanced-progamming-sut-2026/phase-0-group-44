package quest;

import model.Result;
import model.Store;
import model.events.DomainEventBus;
import model.events.DomainEventType;
import model.inGame.plant.PlantRegistry;
import model.user.User;
import model.utility.QuestInstance;
import model.utility.QuestProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.DomainEventPublisher;
import service.QuestCatalog;
import service.QuestRewardService;
import service.QuestService;
import service.UserService;
import util.RandomSource;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-17T09:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private UserService users;
    private QuestService quests;
    private DomainEventPublisher events;
    private User user;

    @BeforeEach
    void setUp() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        users = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")), CLOCK);
        user = new User();
        user.setUsername("quester");
        user.applyDefaults();
        users.addUser(user);

        DomainEventBus bus = new DomainEventBus();
        quests = new QuestService(
                QuestCatalog.fromFile(QuestCatalog.DEFAULT_PATH),
                new QuestRewardService(PlantRegistry.getDefault(), new ZeroRandom()),
                users);
        bus.subscribe(quests);
        events = new DomainEventPublisher(bus, users);
    }

    @Test
    void progressCompletionCountersAndRewardsAreIdempotent() {
        Result<QuestInstance> activation = quests.activate(
                user, "آفتاب گیر روزانه", "3000");
        assertTrue(activation.getStatus());

        events.publish(DomainEventType.SUN_PRODUCED, user, 1500, Map.of());
        events.publish(DomainEventType.SUN_PRODUCED, user, 1500, Map.of());
        QuestInstance instance = activation.getData();

        assertTrue(instance.getProgress().isCompleted());
        assertEquals(3000, instance.getProgress().getProgress());
        assertEquals(1, user.getCompletedDailyQuests());

        events.publish(DomainEventType.SUN_PRODUCED, user, 500, Map.of());
        assertEquals(1, user.getCompletedDailyQuests());

        assertTrue(quests.claim(user, instance).getStatus());
        assertEquals(30, user.getCoins());
        assertFalse(quests.claim(user, instance).getStatus());
        assertEquals(30, user.getCoins());
    }

    @Test
    void questStateSurvivesSaveAndReload() {
        QuestInstance instance = quests.activate(
                user, "آفتاب گیر روزانه", "4000").getData();
        events.publish(DomainEventType.SUN_PRODUCED, user, 1250, Map.of());
        users.saveUsers();

        UserService reloadedUsers = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")), CLOCK);
        assertTrue(reloadedUsers.loadUsers().getStatus());
        User restored = reloadedUsers.findByUsername("quester");
        assertNotNull(restored);

        QuestProgress restoredProgress = restored.getQuestProgress()
                .get(instance.getStorageKey());
        assertNotNull(restoredProgress);
        assertEquals("آفتاب گیر روزانه", restoredProgress.getQuestName());
        assertEquals("4000", restoredProgress.getVariableValue());
        assertEquals(1250, restoredProgress.getProgress());
        assertFalse(restoredProgress.isCompleted());
        assertFalse(restoredProgress.isClaimed());
        assertEquals("2026-07-17", restoredProgress.getRecurrenceDate());
    }

    private static final class ZeroRandom implements RandomSource {
        @Override
        public int nextInt(int bound) {
            return 0;
        }

        @Override
        public double nextDouble() {
            return 0;
        }
    }
}
