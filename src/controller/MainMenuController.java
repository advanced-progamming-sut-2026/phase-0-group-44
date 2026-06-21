package controller;

import model.Result;
import model.Store;
import model.enums.MenuName;
import model.user.User;

public class MainMenuController {

    public Result<String> logout() {
        Result<String> result = new Result<>();
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.setStatus(false);
            result.appendToMessage("no user is logged in");
            return result;
        }

        user.setStayLoggedIn(false);
        Store.setLoggedInUser(null);
        Store.setCurrentMenu(MenuName.REGISTER);

        result.setStatus(true);
        result.setData("register menu");
        result.appendToMessage("logged out successfully");

        return result;
    }
}
