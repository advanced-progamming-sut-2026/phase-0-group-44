package controller;

import model.Result;
import model.Store;
import model.enums.Command;
import model.enums.MenuName;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.UserService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The navigation rules, restated here independently of the production table so
 * that the two must agree.
 */
class MenuTransitionTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-14T09:30:00Z"), ZoneOffset.UTC);

    /** Expected routes for {@code menu enter}, taken from the requirements. */
    private static final Map<MenuName, Set<MenuName>> EXPECTED_ENTRIES = expectedEntries();

    /** Expected destination of {@code menu exit}; absent means "not allowed". */
    private static final Map<MenuName, MenuName> EXPECTED_EXITS = expectedExits();

    @TempDir
    Path tempDir;

    private MenuController controller;
    private UserService userService;

    @BeforeEach
    void setUp() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        Store.setCurrentMenu(MenuName.REGISTER);
        Store.setRunning(true);

        userService = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")),
                FIXED_CLOCK
        );
        controller = new MenuController(userService);
    }

    private static Map<MenuName, Set<MenuName>> expectedEntries() {
        Map<MenuName, Set<MenuName>> entries = new EnumMap<>(MenuName.class);

        entries.put(MenuName.REGISTER, EnumSet.of(MenuName.LOGIN));
        entries.put(MenuName.LOGIN, EnumSet.of(MenuName.MAIN));
        entries.put(MenuName.MAIN, EnumSet.of(
                MenuName.GAME, MenuName.SETTINGS, MenuName.NETWORK,
                MenuName.NEWS, MenuName.PROFILE
        ));
        entries.put(MenuName.GAME, EnumSet.of(
                MenuName.COLLECTION, MenuName.GREENHOUSE
        ));
        entries.put(MenuName.SETTINGS, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.NETWORK, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.NEWS, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.PROFILE, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.COLLECTION, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.GREENHOUSE, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.PLANT_SELECTION, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.GAMEPLAY, EnumSet.noneOf(MenuName.class));

        return entries;
    }

    private static Map<MenuName, MenuName> expectedExits() {
        Map<MenuName, MenuName> exits = new EnumMap<>(MenuName.class);

        exits.put(MenuName.LOGIN, MenuName.REGISTER);
        exits.put(MenuName.GAME, MenuName.MAIN);
        exits.put(MenuName.SETTINGS, MenuName.MAIN);
        exits.put(MenuName.NETWORK, MenuName.MAIN);
        exits.put(MenuName.NEWS, MenuName.MAIN);
        exits.put(MenuName.PROFILE, MenuName.MAIN);
        exits.put(MenuName.COLLECTION, MenuName.GAME);
        exits.put(MenuName.GREENHOUSE, MenuName.GAME);
        exits.put(MenuName.PLANT_SELECTION, MenuName.GAME);
        exits.put(MenuName.GAMEPLAY, MenuName.GAME);

        return exits;
    }

    private void authenticate() {
        User user = new User();
        user.setUsername("kasra");
        Store.getUsers().add(user);
        Store.setLoggedInUser(user);
    }

    // ---------- enter: the whole 9 x 9 table ----------

    @Test
    void everyRouteMatchesTheNavigationRules() {
        authenticate();

        for (MenuName source : MenuName.values()) {
            for (MenuName destination : MenuName.values()) {
                Store.setCurrentMenu(source);

                Result<String> result = controller.enterMenu(destination.getToken());
                boolean allowed = EXPECTED_ENTRIES.get(source).contains(destination);

                assertEquals(
                        allowed,
                        result.getStatus(),
                        "enter " + destination + " from " + source
                );
                assertEquals(
                        allowed ? destination : source,
                        Store.getCurrentMenu(),
                        "current menu after entering " + destination + " from " + source
                );
            }
        }
    }

    @Test
    void aRejectedRouteReportsWhyAndChangesNothing() {
        Store.setCurrentMenu(MenuName.REGISTER);

        Result<String> result = controller.enterMenu("game");

        assertFalse(result.getStatus());
        assertEquals("you cannot enter game menu from register menu", result.getMessage());
        assertEquals(MenuName.REGISTER, Store.getCurrentMenu());
    }

    @Test
    void loginCannotReachMainBeforeAuthentication() {
        Store.setCurrentMenu(MenuName.LOGIN);

        Result<String> rejected = controller.enterMenu("main");

        assertFalse(rejected.getStatus());
        assertEquals("you must log in first", rejected.getMessage());
        assertEquals(MenuName.LOGIN, Store.getCurrentMenu());

        authenticate();
        Result<String> accepted = controller.enterMenu("main");

        assertTrue(accepted.getStatus());
        assertEquals(MenuName.MAIN, Store.getCurrentMenu());
    }

    @Test
    void networkMenuIsNavigableFromMainAndReturnsToIt() {
        authenticate();
        Store.setCurrentMenu(MenuName.MAIN);

        assertTrue(controller.enterMenu("network").getStatus());
        assertEquals(MenuName.NETWORK, Store.getCurrentMenu());

        assertTrue(controller.exitMenu().getStatus());
        assertEquals(MenuName.MAIN, Store.getCurrentMenu());
    }

    // ---------- exit: every menu ----------

    @Test
    void exitFollowsTheNavigationRulesForEveryMenu() {
        for (MenuName source : MenuName.values()) {
            if (source == MenuName.REGISTER) {
                continue;
            }

            Store.setCurrentMenu(source);
            Result<String> result = controller.exitMenu();

            MenuName expected = EXPECTED_EXITS.get(source);

            if (expected == null) {
                assertFalse(result.getStatus(), "exit should be refused in " + source);
                assertEquals(source, Store.getCurrentMenu(), "no move out of " + source);
                continue;
            }

            assertTrue(result.getStatus(), "exit should be allowed in " + source);
            assertEquals(expected, Store.getCurrentMenu(), "exit destination of " + source);
        }
    }

    @Test
    void mainMenuRefusesExitAndPointsAtLogout() {
        authenticate();
        Store.setCurrentMenu(MenuName.MAIN);

        Result<String> result = controller.exitMenu();

        assertFalse(result.getStatus());
        assertEquals("use logout to leave the main menu", result.getMessage());
        assertEquals(MenuName.MAIN, Store.getCurrentMenu());
        assertTrue(Store.isRunning());
    }

    @Test
    void exitingRegistrationSavesProgressAndEndsTheProgram() {
        User user = new User();
        user.setUsername("kasra");
        userService.addUser(user);
        user.setCoins(4321);

        Store.setCurrentMenu(MenuName.REGISTER);
        Result<String> result = controller.exitMenu();

        assertTrue(result.getStatus());
        assertFalse(Store.isRunning());
        assertTrue(result.getMessage().contains("program finished"));

        UserService reloaded = new UserService(
                new JsonUserRepository(tempDir.resolve("users.json")),
                FIXED_CLOCK
        );
        reloaded.loadUsers();

        assertEquals(4321, reloaded.findByUsername("kasra").getCoins());
    }

    // ---------- names, parsing and malformed input ----------

    @Test
    void unknownMenuNamesAreRejectedWithoutMoving() {
        Store.setCurrentMenu(MenuName.REGISTER);

        Result<String> result = controller.enterMenu("lobby");

        assertFalse(result.getStatus());
        assertEquals("menu not found", result.getMessage());
        assertEquals(MenuName.REGISTER, Store.getCurrentMenu());
    }

    @Test
    void menuNamesTolerateCaseAndAnOptionalMenuWord() {
        Store.setCurrentMenu(MenuName.REGISTER);
        assertTrue(controller.enterMenu("Login Menu").getStatus());
        assertEquals(MenuName.LOGIN, Store.getCurrentMenu());
    }

    @Test
    void showCurrentReturnsTheMenuThroughTheResult() {
        Store.setCurrentMenu(MenuName.GAME);

        Result<String> result = controller.showCurrentMenu();

        assertTrue(result.getStatus());
        assertEquals("game menu", result.getData());
        assertEquals("current menu: game menu", result.getMessage());
    }

    @Test
    void malformedCommandsAreNotRecognizedByTheParser() {
        assertTrue(Command.MENU_ENTER.matches("menu enter login"));
        assertTrue(Command.MENU_ENTER.matches("menu   enter   login menu"));
        assertTrue(Command.MENU_SHOW_CURRENT.matches("menu show current"));
        assertTrue(Command.MENU_EXIT.matches("menu exit"));

        assertFalse(Command.MENU_ENTER.matches("menu enter"));
        assertFalse(Command.MENU_ENTER.matches("menuenter login"));
        assertFalse(Command.MENU_SHOW_CURRENT.matches("menu show  currents"));
        assertFalse(Command.MENU_EXIT.matches("menu exit now"));
        assertFalse(Command.MENU_EXIT.matches("exit"));
    }

    @Test
    void savingOnExitDoesNotLeaveATemporaryFile() {
        Store.setCurrentMenu(MenuName.REGISTER);
        controller.exitMenu();

        assertTrue(Files.exists(tempDir.resolve("users.json")));
        assertFalse(Files.exists(tempDir.resolve("users.json.tmp")));
    }
}
