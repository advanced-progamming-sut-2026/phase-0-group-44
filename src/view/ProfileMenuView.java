package view;

import controller.MainMenuController;
import controller.MenuController;
import controller.ProfileMenuController;
import model.enums.Command;

import java.util.regex.Matcher;

/** Reads profile-menu commands. */
public class ProfileMenuView extends MenuView {

    private final ProfileMenuController controller;
    private final MainMenuController mainController;

    public ProfileMenuView(
            MenuController menuController,
            ProfileMenuController controller,
            MainMenuController mainController
    ) {
        super(menuController);
        this.controller = controller;
        this.mainController = mainController;
    }

    public void checkCommand(String input) {
        if (Command.MENU_PROFILE_CHANGE_USERNAME.matches(input)) {
            print(controller.changeUsername(group(Command.MENU_PROFILE_CHANGE_USERNAME, input)));
            return;
        }

        if (Command.MENU_PROFILE_CHANGE_NICKNAME.matches(input)) {
            print(controller.changeNickname(group(Command.MENU_PROFILE_CHANGE_NICKNAME, input)));
            return;
        }

        if (Command.MENU_PROFILE_CHANGE_EMAIL.matches(input)) {
            print(controller.changeEmail(group(Command.MENU_PROFILE_CHANGE_EMAIL, input)));
            return;
        }

        if (Command.MENU_PROFILE_CHANGE_PASSWORD.matches(input)) {
            Matcher matcher = Command.MENU_PROFILE_CHANGE_PASSWORD.getMatcher(input);
            matcher.matches();
            print(controller.changePassword(matcher.group(1), matcher.group(2)));

            return;
        }

        if (Command.MENU_PROFILE_SHOW_INFO.matches(input)) {
            print(controller.showInfo());
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

    private String group(Command command, String input) {
        Matcher matcher = command.getMatcher(input);
        matcher.matches();

        return matcher.group(1);
    }
}
