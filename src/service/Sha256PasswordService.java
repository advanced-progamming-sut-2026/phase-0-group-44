package service;

import model.user.User;

/**
 * SHA-256 hashing (a bonus requirement the branch already implements).
 *
 * <p>Swapping this out for a different scheme requires no change to the
 * registration or login controllers.</p>
 */
public class Sha256PasswordService implements PasswordService {

    private static final java.util.regex.Pattern SHA_256_HEX =
            java.util.regex.Pattern.compile("^[0-9a-fA-F]{64}$");

    /** Whether a persisted value is already a SHA-256 hexadecimal digest. */
    public static boolean isStoredHash(String value) {
        return value != null && SHA_256_HEX.matcher(value).matches();
    }

    @Override
    public String store(String rawPassword) {
        return User.hashPassword(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        if (!isStoredHash(storedPassword)) {
            return false;
        }
        return storedPassword.equalsIgnoreCase(User.hashPassword(rawPassword));
    }
}
