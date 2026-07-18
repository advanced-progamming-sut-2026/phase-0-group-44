package view;

import controller.MainMenuController;
import controller.MenuController;
import controller.ScoredGameController;
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
    private final ScoredGameController scoredController;

    public CommonMenuView(MenuController menuController, MainMenuController mainController) {
        this(menuController, mainController, null);
    }

    public CommonMenuView(MenuController menuController, MainMenuController mainController,
                          ScoredGameController scoredController) {
        super(menuController);
        this.mainController = mainController;
        this.scoredController = scoredController;
    }

    public void checkCommand(String input) {
        if (Store.getCurrentMenu() == MenuName.MAIN
                && scoredController != null
                && Command.MENU_SCORED_GAME.matches(input)) {
            print(scoredController.open());
            return;
        }

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
