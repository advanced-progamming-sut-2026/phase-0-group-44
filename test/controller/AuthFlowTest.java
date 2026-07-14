package controller;

import model.Result;
import model.Store;
import model.enums.Command;
import model.enums.MenuName;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.PasswordService;
import service.SecurityQuestionCatalog;
import service.Sha256PasswordService;
import service.UserService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Registration, security question, login, stay-logged-in and password recovery. */
class AuthFlowTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-14T09:30:00Z"), ZoneOffset.UTC);

    private static final String GOOD_PASSWORD = "Abcdef1!";
    private static final List<String> QUESTIONS = List.of(
            "What is my father's name?",
            "What was my first pet's name?",
            "What is my favourite food?"
    );

    @TempDir
    Path tempDir;

    private Path savePath;
    private PasswordService passwordService;
    private RegisterMenuController registerController;
    private LoginMenuController loginController;

    @BeforeEach
    void setUp() {
        savePath = tempDir.resolve("users.json");
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        Store.setCurrentMenu(MenuName.REGISTER);
        Store.setRunning(true);
        Store.resetAuthFlow();

        passwordService = new Sha256PasswordService();
        UserService userService = newUserService();
        registerController = new RegisterMenuController(
                userService, passwordService, new SecurityQuestionCatalog(QUESTIONS));
        loginController = new LoginMenuController(userService, passwordService);
    }

    private UserService newUserService() {
        return new UserService(new JsonUserRepository(savePath), FIXED_CLOCK);
    }

    /** Restarting the program: fresh services reading the same save file. */
    private UserService restart() {
        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);
        Store.resetAuthFlow();

        UserService userService = newUserService();
        userService.loadUsers();
        loginController = new LoginMenuController(userService, passwordService);
        registerController = new RegisterMenuController(
                userService, passwordService, new SecurityQuestionCatalog(QUESTIONS));

        return userService;
    }

    private Result<String> registerKasra() {
        return registerController.register(
                "kasra", GOOD_PASSWORD, GOOD_PASSWORD, "Kasra", "kasra@example.com", "male");
    }

    private void registerAndConfirm() {
        registerKasra();
        registerController.pickQuestion(2, "rex", "rex");
    }

    // ---------- registration validation ----------

    @Test
    void aValidRegistrationIsPendingUntilTheQuestionIsAnswered() {
        Result<String> result = registerKasra();

        assertTrue(result.getStatus());
        assertTrue(result.getMessage().contains("What was my first pet's name?"));
        assertTrue(Store.getUsers().isEmpty(), "no account before the question is answered");
        assertNotNull(Store.getAuthFlow().getPendingRegistration());
        assertFalse(Files.exists(savePath), "nothing persisted yet");
    }

    @Test
    void answeringTheQuestionCreatesPersistsAndMovesToLogin() {
        registerKasra();

        Result<String> result = registerController.pickQuestion(2, "rex", "rex");

        assertTrue(result.getStatus());
        assertEquals(1, Store.getUsers().size());
        assertNull(Store.getAuthFlow().getPendingRegistration());
        assertEquals(MenuName.LOGIN, Store.getCurrentMenu());

        User stored = Store.getUsers().get(0);
        assertEquals("What was my first pet's name?", stored.getQuestion());
        assertEquals("rex", stored.getAnswerToQuestion());
        assertTrue(Files.exists(savePath));
    }

    @Test
    void anInvalidUsernameCreatesNothing() {
        Result<String> result = registerController.register(
                "bad user!", GOOD_PASSWORD, GOOD_PASSWORD, "Kasra", "kasra@example.com", "male");

        assertFalse(result.getStatus());
        assertEquals("incorrect username format", result.getMessage());
        assertNull(Store.getAuthFlow().getPendingRegistration());
        assertTrue(Store.getUsers().isEmpty());
    }

    @Test
    void aDuplicateUsernameIsRejected() {
        registerAndConfirm();
        Store.setCurrentMenu(MenuName.REGISTER);

        Result<String> result = registerKasra();

        assertFalse(result.getStatus());
        assertEquals("username already taken", result.getMessage());
        assertEquals(1, Store.getUsers().size());
    }

    @Test
    void aWeakPasswordExplainsEveryMissingRequirement() {
        Result<String> result = registerController.register(
                "kasra", "abc", "abc", "Kasra", "kasra@example.com", "male");

        assertFalse(result.getStatus());
        assertTrue(result.getMessage().contains("Minimum length is 8 characters."));
        assertTrue(result.getMessage().contains("uppercase"));
        assertTrue(result.getMessage().contains("number"));
        assertTrue(result.getMessage().contains("special character"));
        assertNull(Store.getAuthFlow().getPendingRegistration());
    }

    @Test
    void aMismatchedPasswordConfirmationIsRejected() {
        Result<String> result = registerController.register(
                "kasra", GOOD_PASSWORD, "Abcdef2!", "Kasra", "kasra@example.com", "male");

        assertFalse(result.getStatus());
        assertEquals("the password and its confirmation do not match", result.getMessage());
        assertNull(Store.getAuthFlow().getPendingRegistration());
    }

    @Test
    void nicknameLengthBoundariesAreEnforced() {
        assertFalse(registerController.register(
                "kasra", GOOD_PASSWORD, GOOD_PASSWORD, "ab", "kasra@example.com", "male").getStatus());
        assertFalse(registerController.register(
                "kasra", GOOD_PASSWORD, GOOD_PASSWORD, "n".repeat(31),
                "kasra@example.com", "male").getStatus());

        assertTrue(registerController.register(
                "kasra", GOOD_PASSWORD, GOOD_PASSWORD, "abc",
                "kasra@example.com", "male").getStatus());
        assertTrue(registerController.register(
                "kasra2", GOOD_PASSWORD, GOOD_PASSWORD, "n".repeat(30),
                "kasra@example.com", "male").getStatus());
    }

    @Test
    void everyEmailRuleIsEnforced() {
        String[] invalid = {
            "no-at-sign.com",
            "two@@example.com",
            "a@b@example.com",
            ".leading@example.com",
            "trailing.@example.com",
            "double..dot@example.com",
            "user@nodot",
            "user@example.c",
            "user@-example.com",
            "user@exa_mple.com",
            "us!er@example.com",
        };

        for (String email : invalid) {
            Result<String> result = registerController.register(
                    "kasra", GOOD_PASSWORD, GOOD_PASSWORD, "Kasra", email, "male");

            assertFalse(result.getStatus(), "should reject " + email);
            assertNull(Store.getAuthFlow().getPendingRegistration());
        }

        assertTrue(registerController.register(
                "kasra", GOOD_PASSWORD, GOOD_PASSWORD, "Kasra",
                "a.b-c_1@sub-domain.example.ir", "male").getStatus());
        assertTrue(registerController.register(
                "kasra", GOOD_PASSWORD, GOOD_PASSWORD, "Kasra",
                "a@b.co", "male").getStatus(), "a single-character local part is legal");
    }

    @Test
    void onlyTheTwoDefinedGenderTokensAreAccepted() {
        assertFalse(registerController.register(
                "kasra", GOOD_PASSWORD, GOOD_PASSWORD, "Kasra",
                "kasra@example.com", "other").getStatus());
        assertNull(Store.getAuthFlow().getPendingRegistration());

        assertTrue(registerController.register(
                "kasra", GOOD_PASSWORD, GOOD_PASSWORD, "Kasra",
                "kasra@example.com", "FEMALE").getStatus());
        assertEquals("female",
                Store.getAuthFlow().getPendingRegistration().getGender());
    }

    // ---------- security question step ----------

    @Test
    void anOutOfRangeQuestionNumberIsRejected() {
        registerKasra();

        assertFalse(registerController.pickQuestion(0, "rex", "rex").getStatus());
        assertFalse(registerController.pickQuestion(4, "rex", "rex").getStatus());
        assertTrue(Store.getUsers().isEmpty());
        assertNotNull(Store.getAuthFlow().getPendingRegistration());
    }

    @Test
    void mismatchedOrEmptyAnswersAreRejected() {
        registerKasra();

        assertFalse(registerController.pickQuestion(1, "rex", "max").getStatus());
        assertFalse(registerController.pickQuestion(1, "  ", "  ").getStatus());
        assertTrue(Store.getUsers().isEmpty());
    }

    @Test
    void theQuestionStepDoesNothingWithoutAPendingRegistration() {
        Result<String> result = registerController.pickQuestion(1, "rex", "rex");

        assertFalse(result.getStatus());
        assertTrue(Store.getUsers().isEmpty());
    }

    // ---------- login ----------

    @Test
    void loginDistinguishesUnknownUserFromWrongPassword() {
        registerAndConfirm();

        Result<String> unknown = loginController.login("nobody", GOOD_PASSWORD, false);
        assertFalse(unknown.getStatus());
        assertEquals("username not found", unknown.getMessage());
        assertNull(Store.getLoggedInUser());

        Result<String> wrong = loginController.login("kasra", "Wrong1!x", false);
        assertFalse(wrong.getStatus());
        assertEquals("wrong password", wrong.getMessage());
        assertNull(Store.getLoggedInUser());
        assertEquals(MenuName.LOGIN, Store.getCurrentMenu());
    }

    @Test
    void aSuccessfulLoginEntersTheMainMenu() {
        registerAndConfirm();

        Result<String> result = loginController.login("kasra", GOOD_PASSWORD, false);

        assertTrue(result.getStatus());
        assertEquals("kasra", Store.getLoggedInUser().getUsername());
        assertEquals(MenuName.MAIN, Store.getCurrentMenu());
    }

    @Test
    void stayLoggedInSurvivesARestart() {
        registerAndConfirm();
        loginController.login("kasra", GOOD_PASSWORD, true);

        UserService afterRestart = restart();

        assertNotNull(afterRestart.getAutoLoginUser());
        assertEquals("kasra", afterRestart.getAutoLoginUser().getUsername());
        assertEquals("kasra", Store.getLoggedInUser().getUsername());
    }

    @Test
    void aSessionWithoutStayLoggedInDoesNotSurviveARestart() {
        registerAndConfirm();
        loginController.login("kasra", GOOD_PASSWORD, false);

        UserService afterRestart = restart();

        assertNull(afterRestart.getAutoLoginUser());
        assertNull(Store.getLoggedInUser());
    }

    @Test
    void anAccountCanLogInAgainAfterARestart() {
        registerAndConfirm();
        restart();

        assertTrue(loginController.login("kasra", GOOD_PASSWORD, false).getStatus());
    }

    // ---------- password recovery ----------

    @Test
    void recoveryChecksTheUsernameEmailPair() {
        registerAndConfirm();

        assertEquals("username not found",
                loginController.forgetPassword("nobody", "kasra@example.com").getMessage());
        assertEquals("this email does not belong to this username",
                loginController.forgetPassword("kasra", "other@example.com").getMessage());
    }

    @Test
    void recoveryAsksTheStoredQuestionAndAcceptsANewPassword() {
        registerAndConfirm();

        Result<String> asked = loginController.forgetPassword("kasra", "kasra@example.com");
        assertTrue(asked.getStatus());
        assertEquals("What was my first pet's name?", asked.getData());

        assertTrue(loginController.answer("rex").getStatus());

        Result<String> weak = loginController.newPassword("abc");
        assertFalse(weak.getStatus());
        assertTrue(loginController.login("kasra", GOOD_PASSWORD, false).getStatus(),
                "the old password still works after a rejected new one");
        Store.setLoggedInUser(null);
        Store.setCurrentMenu(MenuName.LOGIN);

        assertTrue(loginController.newPassword("Zxcvbn2#").getStatus());

        restart();
        assertFalse(loginController.login("kasra", GOOD_PASSWORD, false).getStatus());
        assertTrue(loginController.login("kasra", "Zxcvbn2#", false).getStatus());
    }

    @Test
    void aWrongAnswerChangesNothingAndReturnsToLogin() {
        registerAndConfirm();
        loginController.forgetPassword("kasra", "kasra@example.com");

        Result<String> result = loginController.answer("wrong");

        assertFalse(result.getStatus());
        assertEquals(MenuName.LOGIN, Store.getCurrentMenu());
        assertNull(Store.getAuthFlow().getRecoveringUser());

        assertFalse(loginController.newPassword("Zxcvbn2#").getStatus());
        assertTrue(loginController.login("kasra", GOOD_PASSWORD, false).getStatus());
    }

    @Test
    void aNewPasswordIsRefusedBeforeTheQuestionIsAnswered() {
        registerAndConfirm();
        loginController.forgetPassword("kasra", "kasra@example.com");

        Result<String> result = loginController.newPassword("Zxcvbn2#");

        assertFalse(result.getStatus());
        assertEquals("answer your security question first", result.getMessage());
    }

    // ---------- hashing (bonus, already present) ----------

    @Test
    void passwordsAreStoredAsSha256AndNeverInPlaintext() throws IOException {
        registerAndConfirm();

        User stored = Store.getUsers().get(0);
        assertEquals(64, stored.getHashOfPassword().length());
        assertEquals(User.hashPassword(GOOD_PASSWORD), stored.getHashOfPassword());
        assertTrue(passwordService.matches(GOOD_PASSWORD, stored.getHashOfPassword()));
        assertFalse(Files.readString(savePath).contains(GOOD_PASSWORD));
    }

    // ---------- command parsing ----------

    @Test
    void theAuthCommandsParseTheirArguments() {
        assertTrue(Command.REGISTER.matches(
                "register -u kasra -p Abcdef1! Abcdef1! -n Kas Ra -e k@example.com -g male"));
        assertTrue(Command.PICK_QUESTION.matches("pick question -q 2 -a rex -c rex"));
        assertTrue(Command.LOGIN.matches("login -u kasra -p Abcdef1!"));
        assertTrue(Command.LOGIN.matches("login -u kasra -p Abcdef1! -stay-logged-in"));
        assertTrue(Command.FORGET_PASSWORD.matches(
                "forget password -u kasra -e k@example.com"));
        assertTrue(Command.ANSWER.matches("answer -a rex"));

        assertFalse(Command.REGISTER.matches("register -u kasra -p Abcdef1!"));
        assertFalse(Command.LOGIN.matches("login -u kasra"));
        assertFalse(Command.PICK_QUESTION.matches("pick question -q two -a rex -c rex"));

        assertEquals("kasra",
                group(Command.LOGIN, "login -u kasra -p Abcdef1! -stay-logged-in", 1));
        assertNull(group(Command.LOGIN, "login -u kasra -p Abcdef1!", 3));
    }

    private String group(Command command, String input, int index) {
        java.util.regex.Matcher matcher = command.getMatcher(input);
        matcher.matches();

        return matcher.group(index);
    }
}
