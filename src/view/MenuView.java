package view;

import controller.MenuController;
import model.Result;
import model.enums.Command;

import java.util.regex.Matcher;

/**
 * Base of every menu view. Holds the commands that all menus share, so each
 * concrete menu only has to add its own.
 */
public abstract class MenuView {

    private final MenuController menuController;

    protected MenuView(MenuController menuController) {
        this.menuController = menuController;
    }

    protected MenuController getMenuController() {
        return menuController;
    }

    /**
     * Handles {@code menu enter}, {@code menu show current} and {@code menu exit}.
     *
     * @return true when the input was one of the shared commands
     */
    public boolean handleCommonCommand(String input) {
        if (input == null) {
            return false;
        }

        if (Command.MENU_ENTER.matches(input)) {
            Matcher matcher = Command.MENU_ENTER.getMatcher(input);
            matcher.matches();
            print(menuController.enterMenu(matcher.group(1)));

            return true;
        }

        if (Command.MENU_SHOW_CURRENT.matches(input)) {
            print(menuController.showCurrentMenu());

            return true;
        }

        if (Command.MENU_EXIT.matches(input)) {
            print(menuController.exitMenu());

            return true;
        }

        return false;
    }

    protected void print(Result<?> result) {
        System.out.println(result.getMessage());
    }
}
