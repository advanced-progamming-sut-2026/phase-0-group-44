package controller;

import model.Result;
import model.Store;
import model.config.DifficultyScaling;
import model.user.Settings;
import model.user.User;
import service.UserService;

/** The settings menu: changing and reporting user gameplay settings. */
public class SettingsMenuController {

    private final UserService userService;

    public SettingsMenuController(UserService userService) {
        this.userService = userService;
    }

    /** Handles difficulty level from 1 through 5. */
    public Result<Integer> changeDifficulty(int difficultyLevel) {
        Result<Integer> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        if (difficultyLevel < DifficultyScaling.MIN_DIFFICULTY
                || difficultyLevel > DifficultyScaling.MAX_DIFFICULTY) {
            result.appendToMessage("difficulty must be an integer from 1 through 5");
            return result;
        }

        Settings settings = requireSettings(user);
        settings.setDifficultyLevel(difficultyLevel);
        userService.updateUser(user);

        result.setStatus(true);
        result.setData(difficultyLevel);
        result.appendToMessage("difficulty changed to " + difficultyLevel);

        return result;
    }

    public Result<Integer> showDifficulty() {
        Result<Integer> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        int level = requireSettings(user).getDifficultyLevel();

        result.setStatus(true);
        result.setData(level);
        result.appendToMessage("current difficulty: " + level);

        return result;
    }

    /** Handles game progress speed from 1 through 3. */
    public Result<Integer> changeGameSpeed(int gameSpeed) {
        Result<Integer> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        if (gameSpeed < 1 || gameSpeed > 3) {
            result.appendToMessage("game speed must be an integer from 1 through 3");
            return result;
        }

        Settings settings = requireSettings(user);
        settings.setGameSpeed(gameSpeed);
        userService.updateUser(user);

        result.setStatus(true);
        result.setData(gameSpeed);
        result.appendToMessage("game speed changed to " + gameSpeed);

        return result;
    }

    public Result<Integer> showGameSpeed() {
        Result<Integer> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        int gameSpeed = requireSettings(user).getGameSpeed();

        result.setStatus(true);
        result.setData(gameSpeed);
        result.appendToMessage("current game speed: " + gameSpeed);

        return result;
    }

    /** Handles red grid visibility during the game. */
    public Result<Boolean> changeGridVisible(boolean gridVisible) {
        Result<Boolean> result = new Result<>();
        User user = requireUserBoolean(result);

        if (user == null) {
            return result;
        }

        Settings settings = requireSettings(user);
        settings.setGridVisible(gridVisible);
        userService.updateUser(user);

        result.setStatus(true);
        result.setData(gridVisible);
        result.appendToMessage("grid visibility changed to " + gridVisible);

        return result;
    }

    public Result<Boolean> showGridVisible() {
        Result<Boolean> result = new Result<>();
        User user = requireUserBoolean(result);

        if (user == null) {
            return result;
        }

        boolean gridVisible = requireSettings(user).isGridVisible();

        result.setStatus(true);
        result.setData(gridVisible);
        result.appendToMessage("grid visible: " + gridVisible);

        return result;
    }

    /** Handles debug mode for showing coin/gem/sun/plant-food controls. */
    public Result<Boolean> changeDebugMode(boolean debugMode) {
        Result<Boolean> result = new Result<>();
        User user = requireUserBoolean(result);

        if (user == null) {
            return result;
        }

        Settings settings = requireSettings(user);
        settings.setDebugMode(debugMode);
        userService.updateUser(user);

        result.setStatus(true);
        result.setData(debugMode);
        result.appendToMessage("debug mode changed to " + debugMode);

        return result;
    }

    public Result<Boolean> showDebugMode() {
        Result<Boolean> result = new Result<>();
        User user = requireUserBoolean(result);

        if (user == null) {
            return result;
        }

        boolean debugMode = requireSettings(user).isDebugMode();

        result.setStatus(true);
        result.setData(debugMode);
        result.appendToMessage("debug mode: " + debugMode);

        return result;
    }

    private Settings requireSettings(User user) {
        if (user.getSettings() == null) {
            user.setSettings(new Settings());
        }

        return user.getSettings();
    }

    private User requireUser(Result<Integer> result) {
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.appendToMessage("no user is logged in");
        }

        return user;
    }

    private User requireUserBoolean(Result<Boolean> result) {
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.appendToMessage("no user is logged in");
        }

        return user;
    }
}