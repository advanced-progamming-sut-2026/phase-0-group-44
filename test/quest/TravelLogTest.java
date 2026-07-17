package quest;

import controller.TravelMenuController;
import model.Result;
import model.Store;
import model.events.DomainEventBus;
import model.inGame.plant.PlantRegistry;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelLogTest {
    @TempDir
    Path tempDir;

    private TravelMenuController controller;

    @BeforeEach
    void setUp() {
        Store.setUsers(new ArrayList<>());
        UserService users = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")),
                Clock.fixed(Instant.parse("2026-07-17T09:00:00Z"), ZoneOffset.UTC));
        User user = new User();
        user.setUsername("traveler");
        user.applyDefaults();
        users.addUser(user);
        Store.setLoggedInUser(user);

        QuestService quests = new QuestService(
                QuestCatalog.fromFile(QuestCatalog.DEFAULT_PATH),
                new QuestRewardService(PlantRegistry.getDefault(), new ZeroRandom()),
                users);
        new DomainEventBus().subscribe(quests);
        controller = new TravelMenuController(quests);
    }

    @Test
    void unknownPagesAreRejectedAndMinigamePageExists() {
        assertFalse(controller.page("made-up").getStatus());
        assertTrue(controller.page("minigame").getStatus());
        assertTrue(controller.page("minigame").getMessage()
                .contains("no minigame quest rows exist"));
    }

    @Test
    void epicPageUsesPriorityThenCanonicalOrder() {
        Result<String> page = controller.page("epic");
        assertTrue(page.getStatus());
        String output = page.getMessage();
        int critical = output.indexOf("استاد دفاع");
        int high = output.indexOf("شب یا صبح");
        assertTrue(critical >= 0);
        assertTrue(high > critical);
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
