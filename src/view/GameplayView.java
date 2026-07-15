package view;

import controller.GameplayController;
import controller.MenuController;
import model.enums.Command;

import java.util.regex.Matcher;

/** Reads gameplay commands and prints the results. */
public class GameplayView extends MenuView {

    private final GameplayController controller;

    public GameplayView(MenuController menuController, GameplayController controller) {
        super(menuController);
        this.controller = controller;
    }

    public void checkCommand(String input) {
        if (controller == null) {
            System.out.println("no game is running");
            return;
        }

        if (Command.ADVANCE_TIME.matches(input)) {
            Matcher matcher = Command.ADVANCE_TIME.getMatcher(input);
            matcher.matches();
            print(controller.advanceTime(Integer.parseInt(matcher.group(1))));

            return;
        }

        if (Command.COLLECT_SUN.matches(input)) {
            Matcher matcher = Command.COLLECT_SUN.getMatcher(input);
            matcher.matches();
            print(controller.collectSun(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))
            ));

            return;
        }

        if (Command.SHOW_SUN_AMOUNT.matches(input)) {
            print(controller.showSunAmount());
            return;
        }

        if (Command.CHEAT_ADD_SUNS.matches(input)) {
            Matcher matcher = Command.CHEAT_ADD_SUNS.getMatcher(input);
            matcher.matches();
            print(controller.cheatAddSuns(Integer.parseInt(matcher.group(1))));

            return;
        }

        if (handleCommonCommand(input)) {
            return;
        }

        System.out.println("invalid command");
    }
}
