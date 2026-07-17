package view;

import controller.MenuController;
import controller.TravelMenuController;
import model.enums.Command;

import java.util.regex.Matcher;

/** Commands available after entering Travel Log from the game menu. */
public class TravelMenuView extends MenuView {
    private final TravelMenuController controller;

    public TravelMenuView(MenuController menuController, TravelMenuController controller) {
        super(menuController);
        this.controller = controller;
    }

    public void checkCommand(String input) {
        if (Command.TRAVEL_LOG_PAGE.matches(input)) {
            Matcher matcher = Command.TRAVEL_LOG_PAGE.getMatcher(input);
            matcher.matches();
            print(controller.page(matcher.group(1)));
            return;
        }
        if (Command.TRAVEL_LOG_CLAIM.matches(input)) {
            Matcher matcher = Command.TRAVEL_LOG_CLAIM.getMatcher(input);
            matcher.matches();
            print(controller.claim(Integer.parseInt(matcher.group(1))));
            return;
        }
        if (Command.MINIGAME_SELECT.matches(input)) {
            Matcher matcher = Command.MINIGAME_SELECT.getMatcher(input);
            matcher.matches();
            print(controller.selectMiniGame(matcher.group(1), Integer.parseInt(matcher.group(2))));
            return;
        }
        if (Command.MINIGAME_START.matches(input)) {
            print(controller.startMiniGame());
            return;
        }
        if (handleCommonCommand(input)) {
            return;
        }
        System.out.println("invalid command");
    }
}
