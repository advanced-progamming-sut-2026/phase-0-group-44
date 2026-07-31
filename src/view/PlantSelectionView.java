package view;

import controller.MenuController;
import controller.PlantSelectionController;
import model.enums.Command;

import java.util.regex.Matcher;

/** Reads plant-selection commands. */
public class PlantSelectionView extends MenuView {

    private final PlantSelectionController controller;

    public PlantSelectionView(
            MenuController menuController,
            PlantSelectionController controller
    ) {
        super(menuController);
        this.controller = controller;
    }

    public void checkCommand(String input) {
        if (Command.SHOW_ALL_PLANTS.matches(input)) {
            print(controller.showAllPlants());
            return;
        }

        if (Command.SHOW_AVAILABLE_PLANTS.matches(input)) {
            print(controller.showAvailablePlants());
            return;
        }

        if (Command.CHEAT_SELECT_ALL_PLANTS.matches(input)) {
            print(controller.cheatSelectAllPlants());
            return;
        }
        if (Command.ADD_PLANT.matches(input)) {
            print(controller.addPlant(group(Command.ADD_PLANT, input)));
            return;
        }

        if (Command.REMOVE_PLANT.matches(input)) {
            print(controller.removePlant(group(Command.REMOVE_PLANT, input)));
            return;
        }

        if (Command.BOOST_PLANT.matches(input)) {
            print(controller.boostPlant(group(Command.BOOST_PLANT, input)));
            return;
        }

        if (Command.START_GAME.matches(input)) {
            print(controller.startGame());
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
