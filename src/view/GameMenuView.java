package view;

import controller.GameMenuController;
import controller.MainMenuController;
import controller.MenuController;
import controller.PlantSelectionController;
import controller.ScoredGameController;
import model.Result;
import model.enums.Command;
import model.inGame.GameSession;
import model.level.Level;

import java.util.regex.Matcher;

/** Reads game-menu commands. Chapter entry is checked before the generic menu commands. */
public class GameMenuView extends MenuView {

    private final GameMenuController controller;
    private final MainMenuController mainController;
    private final ScoredGameController scoredController;
    private final PlantSelectionController plantSelectionController;

    public GameMenuView(
            MenuController menuController,
            GameMenuController controller,
            MainMenuController mainController,
            ScoredGameController scoredController,
            PlantSelectionController plantSelectionController
    ) {
        super(menuController);
        this.controller = controller;
        this.mainController = mainController;
        this.scoredController = scoredController;
        this.plantSelectionController = plantSelectionController;
    }

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public void checkCommand(String input) {
        if (scoredController != null && Command.MENU_SCORED_GAME.matches(input)) {
            print(scoredController.open());
            return;
        }

        if (Command.MENU_ENTER_CHAPTER.matches(input)) {
            Matcher matcher = Command.MENU_ENTER_CHAPTER.getMatcher(input);
            matcher.matches();
            openChapter(matcher.group(1));

            return;
        }

        if (Command.MENU_GREENHOUSE.matches(input)) {
            print(controller.greenhouse());
            return;
        }

        if (Command.MENU_TRAVEL_LOG.matches(input)) {
            print(controller.travelLog());
            return;
        }

        if (Command.MENU_LEADERBOARD_SORT.matches(input)) {
            Matcher matcher = Command.MENU_LEADERBOARD_SORT.getMatcher(input);
            matcher.matches();
            print(controller.leaderboard(matcher.group(1), matcher.group(2)));
            return;
        }

        if (Command.MENU_LEADERBOARD.matches(input)) {
            print(controller.leaderboard());
            return;
        }

        if (Command.MENU_COIN_WALLET.matches(input)) {
            print(controller.showCoinWallet());
            return;
        }

        if (Command.MENU_GEM_WALLET.matches(input)) {
            print(controller.showGemWallet());
            return;
        }

        if (Command.MENU_CHEAT_ADD.matches(input)) {
            Matcher matcher = Command.MENU_CHEAT_ADD.getMatcher(input);
            matcher.matches();
            print(controller.cheatAdd(Integer.parseInt(matcher.group(1)), matcher.group(2)));

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

    private void openChapter(String chapterName) {
        if (plantSelectionController == null) {
            print(controller.enterChapter(chapterName));
            return;
        }

        Result<Level> levelResult = controller.enterLatestPlayableLevel(chapterName);
        if (!levelResult.getStatus()) {
            print(levelResult);
            return;
        }

        Result<GameSession> startResult =
                plantSelectionController.beginForPlayer(levelResult.getData());
        print(startResult);
    }
}
