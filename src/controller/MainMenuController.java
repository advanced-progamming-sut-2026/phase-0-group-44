package controller;

import model.Result;
import model.Store;
import model.enums.MenuName;
import model.user.User;
import service.UserService;

/** The main menu's own command: logging out. */
public class MainMenuController {

    private final UserService userService;

    public MainMenuController(UserService userService) {
        this.userService = userService;
    }

    /** Handles {@code menu logout}: clears the session and returns to registration. */
    public Result<String> logout() {
        Result<String> result = new Result<>();
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.appendToMessage("no user is logged in");
            return result;
        }

        user.setStayLoggedIn(false);
        userService.updateUser(user);

        Store.setLoggedInUser(null);
        Store.setCurrentMenu(MenuName.REGISTER);

        result.setStatus(true);
        result.setData("register menu");
        result.appendToMessage("logged out successfully");

        return result;
    }
}
