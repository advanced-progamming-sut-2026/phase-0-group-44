package controller;

import model.Result;
import model.Store;
import model.enums.MenuName;
import model.menu.MenuExit;
import model.menu.MenuGraph;
import service.UserService;

/**
 * Handles the three commands that every menu shares: entering a menu, showing
 * the current one, and leaving one.
 *
 * <p>All routing decisions are read from {@link MenuGraph}; this controller only
 * applies them. A rejected command never changes the current menu.</p>
 */
public class MenuController {

    private final UserService userService;

    public MenuController(UserService userService) {
        if (userService == null) {
            throw new IllegalArgumentException("User service is required.");
        }

        this.userService = userService;
    }

    /** Handles {@code menu enter <menu_name>}. */
    public Result<String> enterMenu(String menuName) {
        Result<String> result = new Result<>();
        MenuName destination = MenuName.fromToken(menuName);

        if (destination == null) {
            result.appendToMessage("menu not found");
            return result;
        }

        MenuName current = Store.getCurrentMenu();

        if (!MenuGraph.canEnter(current, destination)) {
            result.appendToMessage(
                    "you cannot enter " + destination.getDisplayName()
                            + " from " + current.getDisplayName()
            );
            return result;
        }

        if (MenuGraph.requiresAuthentication(current, destination)
                && Store.getLoggedInUser() == null) {
            result.appendToMessage("you must log in first");
            return result;
        }

        Store.setCurrentMenu(destination);

        result.setStatus(true);
        result.setData(destination.getDisplayName());
        result.appendToMessage("entered " + destination.getDisplayName());

        return result;
    }

    /** Handles {@code menu show current}. */
    public Result<String> showCurrentMenu() {
        Result<String> result = new Result<>();
        MenuName current = Store.getCurrentMenu();

        result.setStatus(true);
        result.setData(current.getDisplayName());
        result.appendToMessage("current menu: " + current.getDisplayName());

        return result;
    }

    /** Handles {@code menu exit}. */
    public Result<String> exitMenu() {
        Result<String> result = new Result<>();
        MenuName current = Store.getCurrentMenu();
        MenuExit exit = MenuGraph.exitOf(current);

        switch (exit.getKind()) {
            case TERMINATE:
                return terminate(result);

            case RETURN_TO:
                return returnTo(result, exit.getDestination());

            default:
                return rejectExit(result, current);
        }
    }

    /** Leaving the registration menu ends the program, after progress is saved. */
    private Result<String> terminate(Result<String> result) {
        Result<String> saved = userService.shutdown();

        Store.setRunning(false);

        result.setStatus(true);
        result.setData("exit");
        result.appendToMessage(saved.getMessage() + "; program finished");

        return result;
    }

    private Result<String> returnTo(Result<String> result, MenuName destination) {
        Store.setCurrentMenu(destination);

        result.setStatus(true);
        result.setData(destination.getDisplayName());
        result.appendToMessage("returned to " + destination.getDisplayName());

        return result;
    }

    private Result<String> rejectExit(Result<String> result, MenuName current) {
        if (current == MenuName.MAIN) {
            result.appendToMessage("use logout to leave the main menu");
            return result;
        }

        result.appendToMessage("this menu cannot be exited");

        return result;
    }
}
