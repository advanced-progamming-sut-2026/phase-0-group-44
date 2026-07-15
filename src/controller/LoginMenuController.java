package controller;

import model.Result;
import model.Store;
import model.enums.MenuName;
import model.user.AuthFlowState;
import model.user.User;
import service.PasswordService;
import service.UserService;

/**
 * The login flow and the password-recovery flow.
 *
 * <p>A wrong recovery answer changes no password and puts the player back at the
 * start of the login menu.</p>
 */
public class LoginMenuController {

    private final UserService userService;
    private final PasswordService passwordService;

    public LoginMenuController(UserService userService, PasswordService passwordService) {
        this.userService = userService;
        this.passwordService = passwordService;
    }

    /** Handles {@code login -u <username> -p <password> [-stay-logged-in]}. */
    public Result<String> login(String username, String password, boolean stayLoggedIn) {
        Result<String> result = new Result<>();
        User user = userService.findByUsername(username);

        if (user == null) {
            result.appendToMessage("username not found");
            return result;
        }

        if (!passwordService.matches(password, user.getHashOfPassword())) {
            result.appendToMessage("wrong password");
            return result;
        }

        user.setStayLoggedIn(stayLoggedIn);
        Store.setLoggedInUser(user);
        Store.setCurrentMenu(MenuName.MAIN);
        userService.updateUser(user);

        result.setStatus(true);
        result.setData(user.getUsername());
        result.appendToMessage("welcome " + user.getNickname());

        return result;
    }

    /** Handles {@code forget password -u <username> -e <email>}. */
    public Result<String> forgetPassword(String username, String email) {
        Result<String> result = new Result<>();
        User user = userService.findByUsername(username);

        if (user == null) {
            result.appendToMessage("username not found");
            return result;
        }

        if (user.getEmail() == null || !user.getEmail().equals(email)) {
            result.appendToMessage("this email does not belong to this username");
            return result;
        }

        if (user.getQuestion() == null) {
            result.appendToMessage("this account has no security question");
            return result;
        }

        Store.getAuthFlow().startRecovery(user);

        result.setStatus(true);
        result.setData(user.getQuestion());
        result.appendToMessage("answer this question: " + user.getQuestion());

        return result;
    }

    /** Handles {@code answer -a <answer>}. */
    public Result<String> answer(String answer) {
        Result<String> result = new Result<>();
        AuthFlowState flow = Store.getAuthFlow();
        User user = flow.getRecoveringUser();

        if (user == null) {
            result.appendToMessage("no password recovery is in progress");
            return result;
        }

        if (user.getAnswerToQuestion() == null || !user.getAnswerToQuestion().equals(answer)) {
            flow.clearRecovery();
            Store.setCurrentMenu(MenuName.LOGIN);

            result.appendToMessage("answer is wrong; back to the login menu");
            return result;
        }

        flow.acceptRecoveryAnswer();

        result.setStatus(true);
        result.setData(user.getUsername());
        result.appendToMessage("give the new password");

        return result;
    }

    /** Handles the controlled new-password step that follows a correct answer. */
    public Result<String> newPassword(String password) {
        Result<String> result = new Result<>();
        AuthFlowState flow = Store.getAuthFlow();
        User user = flow.getRecoveringUser();

        if (user == null || !flow.isRecoveryAnswerAccepted()) {
            result.appendToMessage("answer your security question first");
            return result;
        }

        Result<String> weakPassword = User.isPasswordValid(password);

        if (weakPassword != null) {
            return weakPassword;
        }

        user.setHashOfPassword(passwordService.store(password));
        userService.updateUser(user);
        flow.clearRecovery();

        result.setStatus(true);
        result.setData(user.getUsername());
        result.appendToMessage("password changed");

        return result;
    }
}
