package controller;

import model.Result;
import model.Store;
import model.user.User;

public class GameMenuController {

    public Result<Integer> showCoinWallet() {
        Result<Integer> result = new Result<>();
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.setStatus(false);
            result.appendToMessage("no user is logged in");
            return result;
        }

        result.setStatus(true);
        result.setData(user.getCoins());
        result.appendToMessage(
                "coins: " + user.getCoins()
        );

        return result;
    }

    public Result<Integer> showGemWallet() {
        Result<Integer> result = new Result<>();
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.setStatus(false);
            result.appendToMessage("no user is logged in");
            return result;
        }

        result.setStatus(true);
        result.setData(user.getGems());
        result.appendToMessage(
                "gems: " + user.getGems()
        );

        return result;
    }

    public Result<Integer> cheatAdd(
            int amount,
            String currency
    ) {
        Result<Integer> result = new Result<>();
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.setStatus(false);
            result.appendToMessage("no user is logged in");
            return result;
        }

        if (amount <= 0) {
            result.setStatus(false);
            result.appendToMessage(
                    "amount must be positive"
            );
            return result;
        }

        if (currency == null) {
            result.setStatus(false);
            result.appendToMessage(
                    "currency must be coin or diamond"
            );
            return result;
        }

        if (currency.equalsIgnoreCase("coin")) {
            int newBalance = user.getCoins() + amount;
            user.setCoins(newBalance);

            result.setStatus(true);
            result.setData(newBalance);
            result.appendToMessage(
                    "coins: " + newBalance
            );
            return result;
        }

        if (currency.equalsIgnoreCase("diamond")
                || currency.equalsIgnoreCase("gem")) {
            int newBalance = user.getGems() + amount;
            user.setGems(newBalance);

            result.setStatus(true);
            result.setData(newBalance);
            result.appendToMessage(
                    "gems: " + newBalance
            );
            return result;
        }

        result.setStatus(false);
        result.appendToMessage(
                "currency must be coin or diamond"
        );

        return result;
    }
}
