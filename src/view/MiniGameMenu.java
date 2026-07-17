package view;

import controller.MenuController;
import controller.MiniGameController;
import model.Store;
import model.enums.Command;

import java.util.regex.Matcher;

/** Runtime commands shared by every mandatory minigame strategy. */
public class MiniGameMenu extends MenuView {
    private final MiniGameController controller;

    public MiniGameMenu(MenuController menuController, MiniGameController controller) {
        super(menuController);
        this.controller = controller;
    }

    public void checkCommand(String input) {
        if (Command.MENU_EXIT.matches(input)) {
            print(controller.forfeit(Store.getLoggedInUser()));
            return;
        }
        if (Command.MINIGAME_STATUS.matches(input)) {
            print(controller.status(Store.getLoggedInUser()));
            return;
        }
        if (Command.MINIGAME_ADVANCE.matches(input)) {
            Matcher matcher = Command.MINIGAME_ADVANCE.getMatcher(input);
            matcher.matches();
            print(controller.advance(Store.getLoggedInUser(), Integer.parseInt(matcher.group(1))));
            return;
        }
        if (Command.MINIGAME_COMMAND.matches(input)) {
            Matcher matcher = Command.MINIGAME_COMMAND.getMatcher(input);
            matcher.matches();
            print(controller.executeStrategyCommand(Store.getLoggedInUser(), matcher.group(1)));
            return;
        }
        if (Command.MINIGAME_FORFEIT.matches(input)) {
            print(controller.forfeit(Store.getLoggedInUser()));
            return;
        }
        if (handleCommonCommand(input)) {
            return;
        }
        System.out.println("invalid command");
    }
}
