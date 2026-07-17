package view;

import controller.GameMenuController;
import controller.MainMenuController;
import controller.MenuController;
import model.enums.Command;

import java.util.regex.Matcher;

/** Reads game-menu commands. Chapter entry is checked before the generic menu commands. */
public class GameMenuView extends MenuView {

    private final GameMenuController controller;
    private final MainMenuController mainController;

    public GameMenuView(
            MenuController menuController,
            GameMenuController controller,
            MainMenuController mainController
    ) {
        super(menuController);
        this.controller = controller;
        this.mainController = mainController;
    }

    public void checkCommand(String input) {
        if (Command.MENU_ENTER_CHAPTER.matches(input)) {
            Matcher matcher = Command.MENU_ENTER_CHAPTER.getMatcher(input);
            matcher.matches();
            print(controller.enterChapter(matcher.group(1)));

            return;
        }

        if (Command.MENU_GREENHOUSE.matches(input)) {
            print(controller.greenhouse());
            return;
        }

        if (Command.MENU_TRAVEL_LOG.matches(input)) {
            print(controller.travelLog());
            return;
        }

        if (Command.MENU_LEADERBOARD_SORT.matches(input)) {
            Matcher matcher = Command.MENU_LEADERBOARD_SORT.getMatcher(input);
            matcher.matches();
            print(controller.leaderboard(matcher.group(1), matcher.group(2)));
            return;
        }

        if (Command.MENU_LEADERBOARD.matches(input)) {
            print(controller.leaderboard());
            return;
        }

        if (Command.MENU_COIN_WALLET.matches(input)) {
            print(controller.showCoinWallet());
            return;
        }

        if (Command.MENU_GEM_WALLET.matches(input)) {
            print(controller.showGemWallet());
            return;
        }

        if (Command.MENU_CHEAT_ADD.matches(input)) {
            Matcher matcher = Command.MENU_CHEAT_ADD.getMatcher(input);
            matcher.matches();
            print(controller.cheatAdd(Integer.parseInt(matcher.group(1)), matcher.group(2)));

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
