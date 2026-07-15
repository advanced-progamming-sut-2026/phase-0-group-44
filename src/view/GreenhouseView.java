package view;

import controller.GreenhouseController;
import controller.MenuController;
import model.enums.Command;

import java.util.regex.Matcher;

/** Reads greenhouse commands. */
public class GreenhouseView extends MenuView {
    private final GreenhouseController controller;

    public GreenhouseView(MenuController menuController, GreenhouseController controller) {
        super(menuController);
        this.controller = controller;
    }

    public void checkCommand(String input) {
        if (Command.SHOW_GREENHOUSE.matches(input)) {
            print(controller.showGreenhouse());
            return;
        }

        if (Command.PLANT_POT.matches(input)) {
            Matcher matcher = Command.PLANT_POT.getMatcher(input);
            matcher.matches();
            print(controller.plantPot(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))
            ));
            return;
        }

        if (Command.COLLECT_GREENHOUSE.matches(input)) {
            Matcher matcher = Command.COLLECT_GREENHOUSE.getMatcher(input);
            matcher.matches();
            print(controller.collect(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))
            ));
            return;
        }

        if (Command.GROW_GREENHOUSE.matches(input)) {
            Matcher matcher = Command.GROW_GREENHOUSE.getMatcher(input);
            matcher.matches();
            print(controller.grow(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))
            ));
            return;
        }

        if (Command.ENTER_SHOP.matches(input)) {
            print(getMenuController().enterMenu("shop"));
            return;
        }

        if (handleCommonCommand(input)) {
            return;
        }

        System.out.println("invalid command");
    }
}
