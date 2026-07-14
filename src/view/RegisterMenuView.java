package view;

import controller.MenuController;
import controller.RegisterMenuController;
import model.enums.Command;

import java.util.regex.Matcher;

/** Reads registration commands and prints the results. */
public class RegisterMenuView extends MenuView {

    private final RegisterMenuController controller;

    public RegisterMenuView(MenuController menuController, RegisterMenuController controller) {
        super(menuController);
        this.controller = controller;
    }

    public void checkCommand(String input) {
        if (handleCommonCommand(input)) {
            return;
        }

        if (Command.REGISTER.matches(input)) {
            Matcher matcher = Command.REGISTER.getMatcher(input);
            matcher.matches();
            print(controller.register(
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4),
                    matcher.group(5),
                    matcher.group(6)
            ));

            return;
        }

        if (Command.PICK_QUESTION.matches(input)) {
            Matcher matcher = Command.PICK_QUESTION.getMatcher(input);
            matcher.matches();
            print(controller.pickQuestion(
                    Integer.parseInt(matcher.group(1)),
                    matcher.group(2),
                    matcher.group(3)
            ));

            return;
        }

        System.out.println("invalid command");
    }
}
