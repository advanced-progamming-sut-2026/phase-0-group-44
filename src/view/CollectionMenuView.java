package view;

import controller.CollectionMenuController;
import model.Result;
import model.enums.Command;
import model.inGame.plant.Plant;
import model.user.User;

import java.util.List;
import java.util.regex.Matcher;

public class CollectionMenuView {
    private final CollectionMenuController controller = new CollectionMenuController();

    public void checkCommand(String input, User currentUser) {
        Matcher matcher;

        if (Command.MENU_COLLECTION_SHOW_PLANTS.matches(input)) {
            System.out.println(controller.showPlants(currentUser).getMessage());
            return;
        }

        if (Command.MENU_COLLECTION_SHOW_ALL_PLANTS.matches(input)) {
            System.out.println(controller.showAllPlants().getMessage());
            return;
        }

        if (Command.MENU_COLLECTION_SHOW_ZOMBIES.matches(input)) {
            System.out.println(controller.showZombies(currentUser).getMessage());
            return;
        }

        if (Command.MENU_COLLECTION_SHOW_ALL_ZOMBIES.matches(input)) {
            System.out.println(controller.showAllZombies().getMessage());
            return;
        }

        matcher = Command.MENU_COLLECTION_SHOW_PLANT.getMatcher(input);
        if (matcher.matches()) {
            System.out.println(controller.showPlant(currentUser, matcher.group(1)).getMessage());
            return;
        }

        matcher = Command.MENU_COLLECTION_SHOW_ZOMBIE.getMatcher(input);
        if (matcher.matches()) {
            System.out.println(controller.showZombie(currentUser, matcher.group(1)).getMessage());
            return;
        }

        matcher = Command.MENU_COLLECTION_UPGRADE_PLANT.getMatcher(input);
        if (matcher.matches()) {
            System.out.println(controller.upgradePlant(currentUser, matcher.group(1)).getMessage());
            return;
        }

        matcher = Command.MENU_COLLECTION_PURCHASE_PLANT.getMatcher(input);
        if (matcher.matches()) {
            System.out.println(controller.purchasePlant(currentUser, matcher.group(1)).getMessage());
            return;
        }

        System.out.println("Invalid command.");
    }
}