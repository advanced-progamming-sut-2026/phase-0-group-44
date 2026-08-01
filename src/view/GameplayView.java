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

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public void checkCommand(String input) {
        if (controller == null) {
            System.out.println("no game is running");
            return;
        }

        if (Command.START_ZOMBIE_WAVES.matches(input)) {
            print(controller.startZombieWaves());
            return;
        }

        if (Command.ADVANCE_TIME.matches(input)) {
            Matcher matcher = Command.ADVANCE_TIME.getMatcher(input);
            matcher.matches();
            print(controller.advanceTime(Integer.parseInt(matcher.group(1))));

            return;
        }

        if (Command.CHEAT_COLLECT_ALL_SUNS.matches(input)) {
            print(controller.cheatCollectAllSuns());
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

        if (Command.RELEASE_NUKE.matches(input)) {
            print(controller.releaseNuke());
            return;
        }

        if (Command.ZOMBIES_INFO.matches(input)) {
            print(controller.zombiesInfo());
            return;
        }

        if (Command.CHEAT_SPAWN_ZOMBIE.matches(input)) {
            Matcher matcher = Command.CHEAT_SPAWN_ZOMBIE.getMatcher(input);
            matcher.matches();
            print(controller.spawnZombie(
                    matcher.group(1),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))));
            return;
        }

        if (handleBoardCommand(input)) {
            return;
        }

        if (handleCommonCommand(input)) {
            return;
        }

        System.out.println("invalid command");
    }

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    private boolean handleBoardCommand(String input) {
        controller.BoardController board = controller.getBoard();

        if (board == null) {
            return false;
        }

        if (Command.PLANT_PLANT.matches(input)) {
            Matcher matcher = Command.PLANT_PLANT.getMatcher(input);
            matcher.matches();
            print(board.plantPlant(
                    parsePlantType(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))));

            return true;
        }

        if (Command.PLUCK_PLANT.matches(input)) {
            Matcher matcher = Command.PLUCK_PLANT.getMatcher(input);
            matcher.matches();
            print(board.pluckPlant(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))));

            return true;
        }

        if (Command.CHEAT_REMOVE_COOLDOWN.matches(input)) {
            print(board.cheatRemoveCooldown());
            return true;
        }

        if (Command.FEED_PLANT.matches(input)) {
            Matcher matcher = Command.FEED_PLANT.getMatcher(input);
            matcher.matches();
            print(board.feedPlant(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))));

            return true;
        }

        if (Command.CHEAT_ADD_PLANT_FOOD.matches(input)) {
            print(board.cheatAddPlantFood());
            return true;
        }

        if (Command.SHOW_MAP.matches(input)) {
            print(board.showMap());
            return true;
        }

        if (Command.SHOW_CONVEYOR.matches(input)) {
            print(board.showConveyor());
            return true;
        }

        if (Command.SHOW_PLANTS_STATUS.matches(input)) {
            print(board.showPlantsStatus());
            return true;
        }

        if (Command.SHOW_TILE_STATUS.matches(input)) {
            Matcher matcher = Command.SHOW_TILE_STATUS.getMatcher(input);
            matcher.matches();
            print(board.showTileStatus(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2))));

            return true;
        }

        return false;
    }

    private model.enums.PlantType parsePlantType(String token) {
        String normalized = token.trim().toUpperCase().replace("-", "_").replace(" ", "_");

        for (model.enums.PlantType type : model.enums.PlantType.values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }

        return null;
    }
}
