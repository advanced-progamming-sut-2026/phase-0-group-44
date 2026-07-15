package view;

import controller.MainMenuController;
import controller.MenuController;
import controller.SettingsMenuController;
import model.enums.Command;

import java.util.regex.Matcher;

/** Reads settings-menu commands. */
public class SettingsMenuView extends MenuView {

    private final SettingsMenuController controller;
    private final MainMenuController mainController;

    public SettingsMenuView(
            MenuController menuController,
            SettingsMenuController controller,
            MainMenuController mainController
    ) {
        super(menuController);
        this.controller = controller;
        this.mainController = mainController;
    }

    public void checkCommand(String input) {
        if (Command.MENU_SETTINGS_CHANGE_DIFFICULTY.matches(input)) {
            Matcher matcher = Command.MENU_SETTINGS_CHANGE_DIFFICULTY.getMatcher(input);
            matcher.matches();
            print(controller.changeDifficulty(Integer.parseInt(matcher.group(1))));

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
