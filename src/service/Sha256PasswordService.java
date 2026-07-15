package service;

import model.user.User;

/**
 * SHA-256 hashing (a bonus requirement the branch already implements).
 *
 * <p>Swapping this out for a different scheme requires no change to the
 * registration or login controllers.</p>
 */
public class Sha256PasswordService implements PasswordService {

    @Override
    public String store(String rawPassword) {
        return User.hashPassword(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        return storedPassword.equals(User.hashPassword(rawPassword));
    }
}
