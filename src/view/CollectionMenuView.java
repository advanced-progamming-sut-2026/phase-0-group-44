package view;

import controller.CollectionMenuController;
import controller.MainMenuController;
import controller.MenuController;
import model.Store;
import model.enums.Command;
import model.user.User;

import java.util.regex.Matcher;

/** Reads collection-menu commands. */
public class CollectionMenuView extends MenuView {

    private final CollectionMenuController controller;
    private final MainMenuController mainController;

    public CollectionMenuView(
            MenuController menuController,
            CollectionMenuController controller,
            MainMenuController mainController
    ) {
        super(menuController);
        this.controller = controller;
        this.mainController = mainController;
    }

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public void checkCommand(String input) {
        User user = Store.getLoggedInUser();

        if (Command.MENU_COLLECTION_SHOW_PLANTS.matches(input)) {
            print(controller.showPlants(user));
            return;
        }

        if (Command.MENU_COLLECTION_SHOW_ALL_PLANTS.matches(input)) {
            print(controller.showAllPlants());
            return;
        }

        if (Command.MENU_COLLECTION_SHOW_ZOMBIES.matches(input)) {
            print(controller.showZombies(user));
            return;
        }

        if (Command.MENU_COLLECTION_SHOW_ALL_ZOMBIES.matches(input)) {
            print(controller.showAllZombies());
            return;
        }

        if (Command.MENU_COLLECTION_SHOW_PLANT.matches(input)) {
            print(controller.showPlant(user, group(Command.MENU_COLLECTION_SHOW_PLANT, input)));
            return;
        }

        if (Command.MENU_COLLECTION_SHOW_ZOMBIE.matches(input)) {
            print(controller.showZombie(user, group(Command.MENU_COLLECTION_SHOW_ZOMBIE, input)));
            return;
        }

        if (Command.MENU_COLLECTION_UPGRADE_PLANT.matches(input)) {
            print(controller.upgradePlant(user, group(Command.MENU_COLLECTION_UPGRADE_PLANT, input)));
            return;
        }

        if (Command.CHEAT_BUY_ALL_PLANTS.matches(input)) {
            print(controller.cheatBuyAllPlants(user));
            return;
        }

        if (Command.MENU_COLLECTION_PURCHASE_PLANT.matches(input)) {
            print(controller.purchasePlant(
                    user, group(Command.MENU_COLLECTION_PURCHASE_PLANT, input)));
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
