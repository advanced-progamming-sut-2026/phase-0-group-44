package view;

import controller.MenuController;
import controller.ScoredGameController;
import model.enums.Command;

import java.util.regex.Matcher;

/** Commands available while the daily scored game menu is open. */
public final class ScoredGameView extends MenuView {
    private final ScoredGameController controller;

    public ScoredGameView(MenuController menuController, ScoredGameController controller) {
        super(menuController);
        this.controller = controller;
    }

    public void checkCommand(String input) {
        if (Command.SCORED_GAME_START.matches(input)) {
            print(controller.start()); return;
        }
        if (Command.SCORED_GAME_STATUS.matches(input)) {
            print(controller.status()); return;
        }
        if (Command.SCORED_GAME_ADVANCE.matches(input)) {
            Matcher matcher = Command.SCORED_GAME_ADVANCE.getMatcher(input); matcher.matches();
            print(controller.advance(Integer.parseInt(matcher.group(1)))); return;
        }
        if (Command.SCORED_GAME_PLANT.matches(input)) {
            Matcher matcher = Command.SCORED_GAME_PLANT.getMatcher(input); matcher.matches();
            print(controller.plant(matcher.group(1), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)))); return;
        }
        if (Command.SCORED_GAME_COLLECT_SUN.matches(input)) {
            Matcher matcher = Command.SCORED_GAME_COLLECT_SUN.getMatcher(input); matcher.matches();
            print(controller.collectSun(Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)))); return;
        }
        if (Command.SCORED_GAME_FORFEIT.matches(input)) {
            print(controller.forfeit()); return;
        }
        if (!handleCommonCommand(input)) System.out.println("invalid command");
    }
}
