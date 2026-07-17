package view;

import controller.MainMenuController;
import controller.MenuController;
import model.Store;
import model.enums.Command;
import model.enums.MenuName;

import java.util.regex.Matcher;

/**
 * The view for menus that have only the shared commands plus logout — currently
 * the main menu. Sub-menu-specific views extend {@link MenuView} directly.
 */
public class CommonMenuView extends MenuView {

    private final MainMenuController mainController;

    public CommonMenuView(MenuController menuController, MainMenuController mainController) {
        super(menuController);
        this.mainController = mainController;
    }

    public void checkCommand(String input) {
        if (Store.getCurrentMenu() == MenuName.MAIN
                && Command.MENU_LEADERBOARD_SORT.matches(input)) {
            Matcher matcher = Command.MENU_LEADERBOARD_SORT.getMatcher(input);
            matcher.matches();
            print(mainController.leaderboard(matcher.group(1), matcher.group(2)));
            return;
        }

        if (Store.getCurrentMenu() == MenuName.MAIN
                && Command.MENU_LEADERBOARD.matches(input)) {
            print(mainController.leaderboard());
            return;
        }

        if (Command.MENU_LOGOUT.matches(input)) {
            print(mainController.logout());
            return;
        }

        if (!handleCommonCommand(input)) {
            System.out.println("invalid command");
        }
    }
}
