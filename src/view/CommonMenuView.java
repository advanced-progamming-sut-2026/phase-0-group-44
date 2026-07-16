package view;

import controller.MainMenuController;
import controller.MenuController;
import model.enums.Command;

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
        if (Command.MENU_LOGOUT.matches(input)) {
            print(mainController.logout());
            return;
        }

        if (!handleCommonCommand(input)) {
            System.out.println("invalid command");
        }
    }
}
