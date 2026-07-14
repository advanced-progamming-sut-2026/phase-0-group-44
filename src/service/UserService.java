package service;

import model.Result;
import model.Store;
import model.user.User;
import persistence.SaveDataException;
import persistence.SaveFile;
import repository.UserRepository;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Owns the lifecycle of persisted accounts: loading them at start-up, keeping
 * {@link Store} in sync, and writing progress back to disk.
 *
 * <p>The clock is injected rather than read from {@code System}, so the daily
 * shop and the greenhouse can later depend on a deterministic "today".</p>
 */
public class UserService {

    private final UserRepository repository;
    private final Clock clock;

    public UserService(UserRepository repository, Clock clock) {
        if (repository == null || clock == null) {
            throw new IllegalArgumentException("Repository and clock are required.");
        }

        this.repository = repository;
        this.clock = clock;
    }

    public Clock getClock() {
        return clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * Reads the save file into {@link Store} and restores the remembered
     * session. Unreadable save data is moved aside instead of being overwritten.
     */
    public Result<Integer> loadUsers() {
        Result<Integer> result = new Result<>();
        SaveFile saveFile;

        try {
            saveFile = repository.load();
        } catch (SaveDataException exception) {
            return handleUnreadableSave(result, exception);
        }

        Store.setUsers(new ArrayList<>(saveFile.getUsers()));
        Store.setLoggedInUser(resolveSession(saveFile.getLastLoggedInUsername()));

        result.setStatus(true);
        result.setData(Store.getUsers().size());
        result.appendToMessage("loaded " + Store.getUsers().size() + " user(s)");

        return result;
    }

    /** Writes every registered user and the current session to the save file. */
    public Result<String> saveUsers() {
        Result<String> result = new Result<>();

        SaveFile saveFile = new SaveFile();
        saveFile.setUsers(Store.getUsers());
        saveFile.setLastLoggedInUsername(sessionUsername());

        repository.save(saveFile);

        result.setStatus(true);
        result.setData(repository.getSaveFilePath().toString());
        result.appendToMessage("progress saved");

        return result;
    }

    /** Registers a new account and persists it immediately. */
    public Result<String> addUser(User user) {
        Result<String> result = new Result<>();

        if (user == null || user.getUsername() == null) {
            result.appendToMessage("user is missing a username");
            return result;
        }

        if (Store.findUser(user.getUsername()) != null) {
            result.appendToMessage("username already taken");
            return result;
        }

        user.applyDefaults();
        Store.getUsers().add(user);
        saveUsers();

        result.setStatus(true);
        result.setData(user.getUsername());
        result.appendToMessage("user saved");

        return result;
    }

    /** Persists a change made to an already registered user. */
    public Result<String> updateUser(User user) {
        Result<String> result = new Result<>();

        if (user == null || Store.findUser(user.getUsername()) == null) {
            result.appendToMessage("user is not registered");
            return result;
        }

        saveUsers();

        result.setStatus(true);
        result.setData(user.getUsername());
        result.appendToMessage("progress saved");

        return result;
    }

    public User findByUsername(String username) {
        return Store.findUser(username);
    }

    /** The user whose session survives a restart, or {@code null}. */
    public User getAutoLoginUser() {
        for (User user : Store.getUsers()) {
            if (user.isStayLoggedIn()) {
                return user;
            }
        }

        return null;
    }

    /** Controlled shutdown: flush progress. */
    public Result<String> shutdown() {
        return saveUsers();
    }

    private String sessionUsername() {
        User loggedIn = Store.getLoggedInUser();

        if (loggedIn == null || !loggedIn.isStayLoggedIn()) {
            return null;
        }

        return loggedIn.getUsername();
    }

    private User resolveSession(String username) {
        User user = Store.findUser(username);

        if (user == null || !user.isStayLoggedIn()) {
            return null;
        }

        return user;
    }

    private Result<Integer> handleUnreadableSave(
            Result<Integer> result,
            SaveDataException exception
    ) {
        String stamp = String.valueOf(clock.instant().getEpochSecond());
        Path backup = repository.quarantineCorruptSave(stamp);

        Store.setUsers(new ArrayList<>());
        Store.setLoggedInUser(null);

        result.setStatus(false);
        result.setData(0);
        result.appendToMessage(
                "save file could not be read (" + exception.getMessage() + ")"
        );

        if (backup != null) {
            result.appendToMessage("; kept a copy at " + backup);
        }

        return result;
    }
}
