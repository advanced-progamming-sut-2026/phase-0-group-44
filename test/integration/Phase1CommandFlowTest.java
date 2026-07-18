package integration;

import controller.App;
import model.Store;
import model.enums.MenuName;
import model.enums.PlantType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import view.MenuRouter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end command routing for the mandatory Phase-1 entry flow. */
class Phase1CommandFlowTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-18T10:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private App app;
    private MenuRouter router;

    @BeforeEach
    void setUp() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        Store.setCurrentMenu(MenuName.REGISTER);
        Store.setActiveSession(null);
        Store.setActiveSimulation(null);
        Store.setRunning(true);

        app = new App(new JsonUserRepository(tempDir.resolve("users.json")), CLOCK);
        app.start();
        router = new MenuRouter(app);
    }

    @Test
    void documentedCommandsReachAPlayableAdventureSession() {
        route("register -u audit-user -p Abcdef1! Abcdef1! "
                + "-n Audit User -e audit@example.com -g male");
        assertEquals(MenuName.REGISTER, Store.getCurrentMenu());

        route("pick question -q 1 -a answer -c answer");
        assertEquals(MenuName.LOGIN, Store.getCurrentMenu());

        route("login -u audit-user -p Abcdef1!");
        assertEquals(MenuName.MAIN, Store.getCurrentMenu());

        route("menu enter game");
        assertEquals(MenuName.GAME, Store.getCurrentMenu());

        String output = route("menu enter chapter -c Ancient Egypt");

        assertEquals(MenuName.GAMEPLAY, Store.getCurrentMenu());
        assertNotNull(Store.getActiveSession());
        assertNotNull(Store.getActiveSimulation());
        assertTrue(output.contains("game started for"));
        assertEquals(3, Store.getActiveSession().getSelection().size());
        assertTrue(Store.getActiveSession().getSelection().contains(PlantType.SUNFLOWER));
        assertTrue(Store.getActiveSession().getSelection().contains(PlantType.PEASHOOTER));
        assertTrue(Store.getActiveSession().getSelection().contains(PlantType.WALL_NUT));
    }

    @Test
    void rejectedGameplayCommandsDoNotMutateTheSession() {
        documentedCommandsReachAPlayableAdventureSession();
        int sunBefore = Store.getActiveSimulation().getWorld().getSunBalance();
        int plantCountBefore = Store.getActiveSimulation().getWorld().getPlants().size();
        long tickBefore = Store.getActiveSimulation().getCurrentTick();

        String malformed = route("plant plant sunflower at nowhere");
        String invalidTile = route("plant plant -t sunflower -l (99, 99)");
        String invalidTime = route("advance time -t 0 ticks");

        assertTrue(malformed.contains("invalid command"));
        assertTrue(invalidTile.contains("invalid tile"));
        assertTrue(invalidTime.contains("positive integer"));
        assertEquals(sunBefore, Store.getActiveSimulation().getWorld().getSunBalance());
        assertEquals(plantCountBefore, Store.getActiveSimulation().getWorld().getPlants().size());
        assertEquals(tickBefore, Store.getActiveSimulation().getCurrentTick());
    }

    private String route(String command) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            router.route(command);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
