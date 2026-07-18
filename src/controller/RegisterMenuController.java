package controller;

import model.Result;
import model.Store;
import model.enums.Gender;
import model.enums.MenuName;
import model.config.ChapterCatalog;
import model.enums.PlantType;
import model.miniGame.GreenHouse;
import model.user.Collection;
import model.user.Settings;
import model.user.User;
import service.PasswordService;
import service.SecurityQuestionCatalog;
import service.UserService;

import java.util.ArrayList;

/**
 * The registration flow: a validated {@code register} command produces a pending
 * account, which becomes a stored account only once its security question is
 * answered.
 */
public class RegisterMenuController {

    private final UserService userService;
    private final PasswordService passwordService;
    private final SecurityQuestionCatalog questionCatalog;

    public RegisterMenuController(
            UserService userService,
            PasswordService passwordService,
            SecurityQuestionCatalog questionCatalog
    ) {
        this.userService = userService;
        this.passwordService = passwordService;
        this.questionCatalog = questionCatalog;
    }

    /** Handles the {@code register} command. Nothing is stored yet. */
    public Result<String> register(
            String username,
            String password,
            String repeatPassword,
            String nickname,
            String email,
            String gender
    ) {
        Result<String> invalid =
                validate(username, password, repeatPassword, nickname, email, gender);

        if (invalid != null) {
            return invalid;
        }

        Result<String> result = new Result<>();

        if (questionCatalog.isEmpty()) {
            result.appendToMessage("no security questions are configured");
            return result;
        }

        Store.getAuthFlow().setPendingRegistration(
                buildPendingUser(username, password, nickname, email, gender)
        );

        result.setStatus(true);
        result.setData(username);
        result.appendToMessage("pick one of these security questions:" + System.lineSeparator());
        result.appendToMessage(questionCatalog.render());

        return result;
    }

    /** Handles {@code pick question -q <n> -a <answer> -c <answer_confirm>}. */
    public Result<String> pickQuestion(int questionNumber, String answer, String answerConfirm) {
        Result<String> result = new Result<>();
        User pending = Store.getAuthFlow().getPendingRegistration();

        if (pending == null) {
            result.appendToMessage("there is no registration waiting for a security question");
            return result;
        }

        if (!questionCatalog.isValidNumber(questionNumber)) {
            result.appendToMessage("choose one of the given questions");
            return result;
        }

        if (answer == null || answer.trim().isEmpty()) {
            result.appendToMessage("the answer should at least have 1 character");
            return result;
        }

        if (!answer.equals(answerConfirm)) {
            result.appendToMessage("the answer and its confirmation do not match");
            return result;
        }

        pending.setQuestion(questionCatalog.getQuestion(questionNumber));
        pending.setAnswerToQuestion(answer);
        ChapterCatalog.applyDefaultUnlocks(pending);

        Result<String> stored = userService.addUser(pending);

        if (!stored.getStatus()) {
            return stored;
        }

        Store.getAuthFlow().setPendingRegistration(null);
        Store.setCurrentMenu(MenuName.LOGIN);

        result.setStatus(true);
        result.setData(pending.getUsername());
        result.appendToMessage("your account has been created; you are now in the login menu");

        return result;
    }

    /** @return a failed result describing the broken rule, or null when everything is valid */
    private Result<String> validate(
            String username,
            String password,
            String repeatPassword,
            String nickname,
            String email,
            String gender
    ) {
        Result<String> result = new Result<>();

        if (!User.isUserNameValid(username)) {
            result.appendToMessage("incorrect username format");
            return result;
        }

        if (userService.findByUsername(username) != null) {
            result.appendToMessage("username already taken");
            return result;
        }

        Result<String> weakPassword = User.isPasswordValid(password);

        if (weakPassword != null) {
            return weakPassword;
        }

        if (!password.equals(repeatPassword)) {
            result.appendToMessage("the password and its confirmation do not match");
            return result;
        }

        if (!User.isNickNameValid(nickname)) {
            result.appendToMessage("the nickname must be between 3 and 30 characters");
            return result;
        }

        Result<String> invalidEmail = User.isEmailNameValid(email);

        if (invalidEmail != null) {
            return invalidEmail;
        }

        if (Gender.fromToken(gender) == null) {
            result.appendToMessage("gender must be male or female");
            return result;
        }

        return null;
    }

    private User buildPendingUser(
            String username,
            String password,
            String nickname,
            String email,
            String gender
    ) {
        Collection collection = new Collection();
        collection.purchasePlant(PlantType.SUNFLOWER);
        collection.purchasePlant(PlantType.PEASHOOTER);
        collection.purchasePlant(PlantType.WALL_NUT);
        return new User(
                username,
                passwordService.store(password),
                nickname,
                email,
                Gender.fromToken(gender).getToken(),
                0,
                0,
                collection,
                new ArrayList<PlantType>(),
                new Settings(),
                0,
                0,
                0,
                new GreenHouse(),
                null,
                null
        );
    }
}
