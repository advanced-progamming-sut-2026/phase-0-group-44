package view;

import controller.MainMenuController;
import controller.MenuController;
import controller.GameMenuController;

import java.util.regex.Matcher;
import model.enums.Command;

/**
 * The view for menus that have only the shared commands plus logout — currently
 * the main menu. Sub-menu-specific views extend {@link MenuView} directly.
 */
public class CommonMenuView extends MenuView {

    private final MainMenuController mainController;
    private final GameMenuController gameController;

    public CommonMenuView(MenuController menuController, MainMenuController mainController, GameMenuController gameController) {
        super(menuController);
        this.mainController = mainController;
        this.gameController = gameController;
    }

    public void checkCommand(String input) {
        // Compatibility route: the document places leaderboard in both Main and Game.
        if (Command.MENU_LEADERBOARD.matches(input)) {
            Matcher matcher = Command.MENU_LEADERBOARD.getMatcher(input);
            matcher.matches();
            print(gameController.leaderboard(matcher.group(1), matcher.group(2)));
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
