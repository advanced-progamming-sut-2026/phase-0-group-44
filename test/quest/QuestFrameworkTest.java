package quest;

import model.quest.QuestCatalog;
import model.quest.QuestDefinition;
import model.quest.QuestEvent;
import model.quest.QuestEventBus;
import model.quest.QuestEventType;
import model.user.User;
import persistence.GsonSaveSerializer;
import persistence.SaveFile;
import org.junit.jupiter.api.Test;
import service.quest.QuestService;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QuestFrameworkTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-16T00:00:00Z"), ZoneOffset.UTC);

    private QuestCatalog catalog() { return QuestCatalog.load(Path.of("src/assets/quests.csv")); }

    @Test
    void canonicalCsvLoadsAllNineteenRowsInStableOrder() {
        List<QuestDefinition> quests = catalog().getDefinitions();
        assertEquals(19, quests.size());
        assertEquals("آفتاب گیر روزانه", quests.get(0).getCanonicalName());
        assertEquals("صلیب بی دفاع", quests.get(18).getCanonicalName());
        assertEquals(0, quests.get(0).getCanonicalOrder());
        assertEquals(18, quests.get(18).getCanonicalOrder());
    }

    @Test
    void travelLogAcceptsFourKnownPagesAndRejectsUnknownOnes() {
        User user = new User();
        QuestService service = new QuestService(user, catalog(), CLOCK);
        service.activate("آفتاب گیر روزانه", 3000);
        assertEquals(1, service.travelLogPage("daily").size());
        assertTrue(service.travelLogPage("main").isEmpty());
        assertTrue(service.travelLogPage("epic").isEmpty());
        assertTrue(service.travelLogPage("minigame").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.travelLogPage("unknown"));
    }

    @Test
    void eventProgressCompletesAndRewardClaimIsIdempotent() {
        User user = new User();
        QuestService service = new QuestService(user, catalog(), CLOCK);
        QuestEventBus bus = new QuestEventBus();
        bus.subscribe(service);
        service.activate("only cactus", 10);

        bus.publish(new QuestEvent(QuestEventType.ZOMBIE_KILLED, 10, Map.of("quest", "only cactus")));
        assertTrue(user.getQuestProgress().get("only cactus").isCompleted());
        assertEquals(1, user.getCompletedDailyQuests());

        service.claim("only cactus");
        assertEquals(20, user.getGems());
        assertThrows(IllegalStateException.class, () -> service.claim("only cactus"));
        assertEquals(20, user.getGems());
    }

    @Test
    void canonicalFormulaRewardUsesActivatedVariableWithoutInventingAValue() {
        User user = new User();
        QuestService service = new QuestService(user, catalog(), CLOCK);
        service.activate("آفتاب گیر روزانه", 4000);
        service.onQuestEvent(QuestEvent.of(QuestEventType.SUN_PRODUCED, 4000));
        service.claim("آفتاب گیر روزانه");
        assertEquals(40, user.getCoins());
    }

    @Test
    void questStateSurvivesSerializerRoundTrip() {
        User user = new User();
        QuestService service = new QuestService(user, catalog(), CLOCK);
        service.activate("تخریب گر حرفه ای", 3);
        service.onQuestEvent(new QuestEvent(QuestEventType.QUEST_PROGRESS, 2, Map.of("quest", "تخریب گر حرفه ای")));

        SaveFile save = new SaveFile();
        save.setUsers(List.of(user));
        GsonSaveSerializer serializer = new GsonSaveSerializer();
        SaveFile restored = serializer.deserialize(serializer.serialize(save));
        User restoredUser = restored.getUsers().get(0);
        restoredUser.applyDefaults();

        assertEquals(3, restoredUser.getActiveQuests().get("تخریب گر حرفه ای").getTarget());
        assertEquals(2, restoredUser.getQuestProgress().get("تخریب گر حرفه ای").getProgress());
    }

    @Test
    void questStateLivesOnUserForNormalSavePersistence() {
        User user = new User();
        QuestService service = new QuestService(user, catalog(), CLOCK);
        service.activate("تخریب گر حرفه ای", 3);
        service.onQuestEvent(new QuestEvent(QuestEventType.QUEST_PROGRESS, 2, Map.of("quest", "تخریب گر حرفه ای")));
        assertEquals(2, user.getQuestProgress().get("تخریب گر حرفه ای").getProgress());
        assertEquals(3, user.getActiveQuests().get("تخریب گر حرفه ای").getTarget());
    }
}
