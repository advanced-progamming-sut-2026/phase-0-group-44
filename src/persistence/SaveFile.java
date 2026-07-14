package persistence;

import model.user.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Versioned envelope written to the save file.
 *
 * <p>This type carries no domain behaviour: it only groups the persisted
 * state so that serialization stays isolated from the model.</p>
 */
public class SaveFile {
    /** Version written by the current code. */
    public static final int CURRENT_VERSION = 1;

    /** Version used by the pre-envelope format (a bare JSON array of users). */
    public static final int LEGACY_VERSION = 0;

    private int version = CURRENT_VERSION;
    private List<User> users = new ArrayList<>();
    private String lastLoggedInUsername;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<User> getUsers() {
        if (users == null) {
            users = new ArrayList<>();
        }

        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users == null ? new ArrayList<>() : new ArrayList<>(users);
    }

    public String getLastLoggedInUsername() {
        return lastLoggedInUsername;
    }

    public void setLastLoggedInUsername(String lastLoggedInUsername) {
        this.lastLoggedInUsername = lastLoggedInUsername;
    }
}
