package view;

import controller.MainMenuController;
import controller.MenuController;
import controller.NewsMenuController;
import model.enums.Command;

/** Reads news-menu commands. */
public class NewsMenuView extends MenuView {

    private final NewsMenuController controller;
    private final MainMenuController mainController;

    public NewsMenuView(
            MenuController menuController,
            NewsMenuController controller,
            MainMenuController mainController
    ) {
        super(menuController);
        this.controller = controller;
        this.mainController = mainController;
    }

    public void checkCommand(String input) {
        if (Command.MENU_NEWS_SHOW_UNREAD.matches(input)) {
            print(controller.showUnreadNews());
            return;
        }

        if (Command.MENU_NEWS_SHOW_ALL.matches(input)) {
            print(controller.showAllNews());
            return;
        }

        if (Command.MENU_LOGOUT.matches(input)) {
            print(mainController.logout());
            return;
        }

        if (handleCommonCommand(input)) {
            return;
        }

        System.out.println("invalid command");
    }
}
