package view;

import controller.LoginMenuController;
import controller.MenuController;
import model.enums.Command;

import java.util.regex.Matcher;

/** Reads login and password-recovery commands and prints the results. */
public class LoginMenuView extends MenuView {

    private final LoginMenuController controller;

    public LoginMenuView(MenuController menuController, LoginMenuController controller) {
        super(menuController);
        this.controller = controller;
    }

    public void checkCommand(String input) {
        if (handleCommonCommand(input)) {
            return;
        }

        if (Command.LOGIN.matches(input)) {
            Matcher matcher = Command.LOGIN.getMatcher(input);
            matcher.matches();
            print(controller.login(
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3) != null
            ));

            return;
        }

        if (Command.FORGET_PASSWORD.matches(input)) {
            Matcher matcher = Command.FORGET_PASSWORD.getMatcher(input);
            matcher.matches();
            print(controller.forgetPassword(matcher.group(1), matcher.group(2)));

            return;
        }

        if (Command.ANSWER.matches(input)) {
            Matcher matcher = Command.ANSWER.getMatcher(input);
            matcher.matches();
            print(controller.answer(matcher.group(1)));

            return;
        }

        if (Command.NEW_PASSWORD.matches(input)) {
            Matcher matcher = Command.NEW_PASSWORD.getMatcher(input);
            matcher.matches();
            print(controller.newPassword(matcher.group(1)));

            return;
        }

        System.out.println("invalid command");
    }
}
