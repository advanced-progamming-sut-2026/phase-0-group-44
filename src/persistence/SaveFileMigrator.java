package persistence;

import model.user.User;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Upgrades save data produced by older versions of the program to the current
 * version, and fills in defaults for fields that an older save did not contain.
 *
 * <p>Old, still-compatible save data is never discarded: it is migrated.</p>
 */
public class SaveFileMigrator {

    /**
     * Migrates the given save file in place and returns it.
     *
     * @param saveFile freshly deserialized data, possibly from an older version
     * @return the same instance, upgraded to {@link SaveFile#CURRENT_VERSION}
     */
    public SaveFile migrate(SaveFile saveFile) {
        if (saveFile == null) {
            return new SaveFile();
        }

        if (saveFile.getVersion() > SaveFile.CURRENT_VERSION) {
            throw new SaveDataException(
                    "Save file version " + saveFile.getVersion()
                            + " is newer than the supported version "
                            + SaveFile.CURRENT_VERSION + "."
            );
        }

        if (saveFile.getVersion() < SaveFile.CURRENT_VERSION) {
            upgradeToCurrent(saveFile);
        }

        normalizeUsers(saveFile.getUsers());
        clearUnknownSession(saveFile);

        saveFile.setVersion(SaveFile.CURRENT_VERSION);

        return saveFile;
    }

    /**
     * Version 0 (a bare JSON array of users) carried no session state and no
     * progress counters. Nothing has to be rewritten: the defaults declared by
     * the model already describe the missing fields, so the upgrade is limited
     * to stamping the new version.
     */
    private void upgradeToCurrent(SaveFile saveFile) {
        if (saveFile.getLastLoggedInUsername() == null) {
            saveFile.setLastLoggedInUsername(findStayLoggedInUsername(saveFile.getUsers()));
        }
    }

    private String findStayLoggedInUsername(List<User> users) {
        for (User user : users) {
            if (user != null && user.isStayLoggedIn()) {
                return user.getUsername();
            }
        }

        return null;
    }

    /**
     * Drops unusable rows and materializes the lazily-created defaults of every
     * user, so callers never see a null collection coming out of the save file.
     */
    private void normalizeUsers(List<User> users) {
        Iterator<User> iterator = users.iterator();

        while (iterator.hasNext()) {
            User user = iterator.next();

            if (user == null || user.getUsername() == null) {
                iterator.remove();
                continue;
            }

            user.applyDefaults();
        }

        removeDuplicateUsernames(users);
    }

    private void removeDuplicateUsernames(List<User> users) {
        List<String> seen = new ArrayList<>();
        Iterator<User> iterator = users.iterator();

        while (iterator.hasNext()) {
            String username = iterator.next().getUsername();

            if (seen.contains(username)) {
                iterator.remove();
                continue;
            }

            seen.add(username);
        }
    }

    /** A remembered session is only valid while the referenced user still exists. */
    private void clearUnknownSession(SaveFile saveFile) {
        String username = saveFile.getLastLoggedInUsername();

        if (username == null) {
            return;
        }

        for (User user : saveFile.getUsers()) {
            if (username.equals(user.getUsername())) {
                return;
            }
        }

        saveFile.setLastLoggedInUsername(null);
    }
}
