package controller;

import model.Result;
import model.Store;
import model.user.Settings;
import model.user.User;

public class SettingsMenuController {
    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 5;

    public Result<Integer> changeDifficulty(int difficultyLevel) {
        Result<Integer> result = new Result<>();
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.setStatus(false);
            result.appendToMessage("no user is logged in");
            return result;
        }

        if (difficultyLevel < MIN_DIFFICULTY
                || difficultyLevel > MAX_DIFFICULTY) {
            result.setStatus(false);
            result.appendToMessage(
                    "difficulty must be between 1 and 5"
            );
            return result;
        }

        if (user.getSettings() == null) {
            user.setSettings(new Settings());
        }

        user.getSettings().setDifficultyLevel(difficultyLevel);

        result.setStatus(true);
        result.setData(difficultyLevel);
        result.appendToMessage(
                "difficulty changed to " + difficultyLevel
        );

        return result;
    }

    public Result<Integer> showDifficulty() {
        Result<Integer> result = new Result<>();
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.setStatus(false);
            result.appendToMessage("no user is logged in");
            return result;
        }

        if (user.getSettings() == null) {
            user.setSettings(new Settings());
        }

        int difficultyLevel =
                user.getSettings().getDifficultyLevel();

        result.setStatus(true);
        result.setData(difficultyLevel);
        result.appendToMessage(
                "current difficulty: " + difficultyLevel
        );

        return result;
    }
}
