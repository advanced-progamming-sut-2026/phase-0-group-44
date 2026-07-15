package controller;

import model.Result;
import model.Store;
import model.config.ChapterCatalog;
import model.config.GameWorld;
import model.user.User;
import service.UserService;

/**
 * The game menu: navigation into worlds and the sub-screens, wallet queries and
 * the cheat command. No chapter gameplay lives here; entering a world is a
 * guarded navigation that ends at a stable placeholder.
 */
public class GameMenuController {

    private final UserService userService;

    public GameMenuController(UserService userService) {
        this.userService = userService;
    }

    /** Handles {@code menu enter chapter -c <chaptername>}. */
    public Result<String> enterChapter(String chapterName) {
        Result<String> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        GameWorld world = GameWorld.fromName(chapterName);

        if (world == null) {
            result.appendToMessage("no chapter named \"" + chapterName + "\"");
            return result;
        }

        if (!ChapterCatalog.isUnlocked(user, world)) {
            result.appendToMessage(world.getDisplayName() + " is locked");
            return result;
        }

        result.setStatus(true);
        result.setData(world.getDisplayName());
        result.appendToMessage("entered chapter " + world.getDisplayName());

        return result;
    }

    public Result<String> greenhouse() {
        return placeholder("greenhouse");
    }

    public Result<String> travelLog() {
        return placeholder("travel log");
    }

    public Result<String> leaderboard() {
        return placeholder("leaderboard");
    }

    public Result<Integer> showCoinWallet() {
        Result<Integer> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        result.setStatus(true);
        result.setData(user.getCoins());
        result.appendToMessage("coins: " + user.getCoins());

        return result;
    }

    public Result<Integer> showGemWallet() {
        Result<Integer> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        result.setStatus(true);
        result.setData(user.getGems());
        result.appendToMessage("gems: " + user.getGems());

        return result;
    }

    /** Handles {@code menu cheat add <n> <coin/diamond>}. */
    public Result<Integer> cheatAdd(int amount, String currency) {
        Result<Integer> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        if (amount <= 0) {
            result.appendToMessage("amount must be a positive number");
            return result;
        }

        if (isCoin(currency)) {
            user.setCoins(user.getCoins() + amount);
            userService.updateUser(user);

            result.setStatus(true);
            result.setData(user.getCoins());
            result.appendToMessage("coins: " + user.getCoins());

            return result;
        }

        if (isDiamond(currency)) {
            user.setGems(user.getGems() + amount);
            userService.updateUser(user);

            result.setStatus(true);
            result.setData(user.getGems());
            result.appendToMessage("gems: " + user.getGems());

            return result;
        }

        result.appendToMessage("currency must be coin or diamond");

        return result;
    }

    private boolean isCoin(String currency) {
        return "coin".equalsIgnoreCase(currency) || "coins".equalsIgnoreCase(currency);
    }

    private boolean isDiamond(String currency) {
        return "diamond".equalsIgnoreCase(currency)
                || "diamonds".equalsIgnoreCase(currency)
                || "gem".equalsIgnoreCase(currency)
                || "gems".equalsIgnoreCase(currency);
    }

    private Result<String> placeholder(String screenName) {
        Result<String> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        result.setStatus(true);
        result.setData(screenName);
        result.appendToMessage("opened " + screenName);

        return result;
    }

    private <T> User requireUser(Result<T> result) {
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.appendToMessage("no user is logged in");
        }

        return user;
    }
}
