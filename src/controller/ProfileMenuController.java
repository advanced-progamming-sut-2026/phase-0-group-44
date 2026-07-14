package controller;

import model.Result;
import model.Store;
import model.user.User;
import service.PasswordService;
import service.UserService;

/**
 * The profile menu: changing the account's own fields and showing its info.
 *
 * <p>Field validators are reused from {@link User} (the same rules registration
 * uses). Password checks go through {@link PasswordService}, so hashing stays out
 * of this controller.</p>
 */
public class ProfileMenuController {

    private final UserService userService;
    private final PasswordService passwordService;

    public ProfileMenuController(UserService userService, PasswordService passwordService) {
        this.userService = userService;
        this.passwordService = passwordService;
    }

    /** Handles {@code menu profile change-username -u <username>}. */
    public Result<String> changeUsername(String username) {
        Result<String> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        if (user.getUsername().equals(username)) {
            result.appendToMessage("this is already your username");
            return result;
        }

        if (!User.isUserNameValid(username)) {
            result.appendToMessage("incorrect username format");
            return result;
        }

        if (userService.findByUsername(username) != null) {
            result.appendToMessage("username already taken");
            return result;
        }

        user.setUsername(username);
        userService.updateUser(user);

        return success(result, username, "username changed");
    }

    /** Handles {@code menu profile change-nickname -u <nickname>}. */
    public Result<String> changeNickname(String nickname) {
        Result<String> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        if (user.getNickname().equals(nickname)) {
            result.appendToMessage("this is already your nickname");
            return result;
        }

        if (!User.isNickNameValid(nickname)) {
            result.appendToMessage("the nickname must be between 3 and 30 characters");
            return result;
        }

        user.setNickname(nickname);
        userService.updateUser(user);

        return success(result, nickname, "nickname changed");
    }

    /** Handles {@code menu profile change-email -e <email>}. */
    public Result<String> changeEmail(String email) {
        Result<String> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        if (user.getEmail() != null && user.getEmail().equals(email)) {
            result.appendToMessage("this is already your email");
            return result;
        }

        Result<String> invalidEmail = User.isEmailNameValid(email);

        if (invalidEmail != null) {
            return invalidEmail;
        }

        user.setEmail(email);
        userService.updateUser(user);

        return success(result, email, "email changed");
    }

    /** Handles {@code menu profile change-password -p <new> -o <old>}. */
    public Result<String> changePassword(String newPassword, String oldPassword) {
        Result<String> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        if (!passwordService.matches(oldPassword, user.getHashOfPassword())) {
            result.appendToMessage("the old password is incorrect");
            return result;
        }

        if (passwordService.matches(newPassword, user.getHashOfPassword())) {
            result.appendToMessage("the new password is the same as the current one");
            return result;
        }

        Result<String> weakPassword = User.isPasswordValid(newPassword);

        if (weakPassword != null) {
            return weakPassword;
        }

        user.setHashOfPassword(passwordService.store(newPassword));
        userService.updateUser(user);

        return success(result, user.getUsername(), "password changed");
    }

    /** Handles {@code menu profile show-info}. */
    public Result<String> showInfo() {
        Result<String> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        String info = "username: " + user.getUsername()
                + "\nnickname: " + user.getNickname()
                + "\nplayed games: " + user.getGamesPlayed()
                + "\ncoins: " + user.getCoins()
                + "\ndiamonds: " + user.getGems()
                + "\ncompleted levels: " + user.getCompletedLevelCount()
                + "\nhighest Mew Point: " + user.getHighestMewPoint();

        result.setStatus(true);
        result.setData(info);
        result.appendToMessage(info);

        return result;
    }

    private Result<String> success(Result<String> result, String data, String message) {
        result.setStatus(true);
        result.setData(data);
        result.appendToMessage(message);

        return result;
    }

    private User requireUser(Result<String> result) {
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.appendToMessage("no user is logged in");
        }

        return user;
    }
}
