package controller;

import model.Result;
import model.Store;
import model.config.DifficultyScaling;
import model.user.Settings;
import model.user.User;
import service.UserService;

/** The settings menu: changing and reporting difficulty. */
public class SettingsMenuController {

    private final UserService userService;

    public SettingsMenuController(UserService userService) {
        this.userService = userService;
    }

    /** Handles {@code menu settings change-difficulty -l <level>}. */
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

        if (user.getSettings() == null) {
            user.setSettings(new Settings());
        }

        user.getSettings().setDifficultyLevel(difficultyLevel);
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

        if (user.getSettings() == null) {
            user.setSettings(new Settings());
        }

        int level = user.getSettings().getDifficultyLevel();

        result.setStatus(true);
        result.setData(level);
        result.appendToMessage("current difficulty: " + level);

        return result;
    }

    private User requireUser(Result<Integer> result) {
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.appendToMessage("no user is logged in");
        }

        return user;
    }
}
