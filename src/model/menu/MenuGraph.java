package model.menu;

import model.enums.MenuName;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The navigation rules of the program, expressed as one table instead of
 * conditionals spread across controllers.
 *
 * <p>The table is read-only: it answers questions, it never mutates state.</p>
 */
public final class MenuGraph {

    private static final Map<MenuName, Set<MenuName>> ENTRIES = buildEntries();
    private static final Map<MenuName, MenuExit> EXITS = buildExits();

    private MenuGraph() {
    }

    /** Menus reachable with {@code menu enter} from the given menu. */
    public static Set<MenuName> reachableFrom(MenuName source) {
        return ENTRIES.getOrDefault(source, Collections.emptySet());
    }

    public static boolean canEnter(MenuName source, MenuName destination) {
        return reachableFrom(source).contains(destination);
    }

    /**
     * Whether the route is guarded by successful authentication. Only the step
     * from the login menu into the main menu is.
     */
    public static boolean requiresAuthentication(MenuName source, MenuName destination) {
        return source == MenuName.LOGIN && destination == MenuName.MAIN;
    }

    /** What {@code menu exit} does in the given menu. */
    public static MenuExit exitOf(MenuName source) {
        return EXITS.getOrDefault(source, MenuExit.forbidden());
    }

    private static Map<MenuName, Set<MenuName>> buildEntries() {
        Map<MenuName, Set<MenuName>> entries = new EnumMap<>(MenuName.class);

        entries.put(MenuName.REGISTER, EnumSet.of(MenuName.LOGIN));
        entries.put(MenuName.LOGIN, EnumSet.of(MenuName.MAIN));
        entries.put(MenuName.MAIN, EnumSet.of(
                MenuName.GAME,
                MenuName.SETTINGS,
                MenuName.NETWORK,
                MenuName.NEWS,
                MenuName.PROFILE,
                MenuName.SCORED_GAME
        ));
        entries.put(MenuName.GAME, EnumSet.of(
                MenuName.COLLECTION, MenuName.GREENHOUSE, MenuName.TRAVEL_LOG,
                MenuName.SCORED_GAME
        ));
        entries.put(MenuName.SETTINGS, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.NETWORK, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.NEWS, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.PROFILE, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.COLLECTION, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.GREENHOUSE, EnumSet.of(MenuName.SHOP));
        entries.put(MenuName.TRAVEL_LOG, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.MINIGAME, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.SCORED_GAME, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.SHOP, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.PLANT_SELECTION, EnumSet.noneOf(MenuName.class));
        entries.put(MenuName.GAMEPLAY, EnumSet.noneOf(MenuName.class));

        return Collections.unmodifiableMap(entries);
    }

    private static Map<MenuName, MenuExit> buildExits() {
        Map<MenuName, MenuExit> exits = new EnumMap<>(MenuName.class);

        exits.put(MenuName.REGISTER, MenuExit.terminate());
        exits.put(MenuName.LOGIN, MenuExit.returnTo(MenuName.REGISTER));
        exits.put(MenuName.MAIN, MenuExit.forbidden());
        exits.put(MenuName.GAME, MenuExit.returnTo(MenuName.MAIN));
        exits.put(MenuName.SETTINGS, MenuExit.returnTo(MenuName.MAIN));
        exits.put(MenuName.NETWORK, MenuExit.returnTo(MenuName.MAIN));
        exits.put(MenuName.NEWS, MenuExit.returnTo(MenuName.MAIN));
        exits.put(MenuName.PROFILE, MenuExit.returnTo(MenuName.MAIN));
        exits.put(MenuName.COLLECTION, MenuExit.returnTo(MenuName.GAME));
        exits.put(MenuName.GREENHOUSE, MenuExit.returnTo(MenuName.GAME));
        exits.put(MenuName.TRAVEL_LOG, MenuExit.returnTo(MenuName.GAME));
        exits.put(MenuName.MINIGAME, MenuExit.returnTo(MenuName.TRAVEL_LOG));
        exits.put(MenuName.SCORED_GAME, MenuExit.returnTo(MenuName.MAIN));
        exits.put(MenuName.SHOP, MenuExit.returnTo(MenuName.GREENHOUSE));
        exits.put(MenuName.PLANT_SELECTION, MenuExit.returnTo(MenuName.GAME));
        exits.put(MenuName.GAMEPLAY, MenuExit.returnTo(MenuName.GAME));

        return Collections.unmodifiableMap(exits);
    }
}
