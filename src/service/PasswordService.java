package service;

/**
 * The single place that knows how a password is turned into what gets stored.
 *
 * <p>Menu logic only calls {@link #store(String)} and {@link #matches}, so the
 * storage scheme can change without touching any controller.</p>
 */
public interface PasswordService {

    /** Converts a raw password into the representation that is persisted. */
    String store(String rawPassword);

    /** Checks a raw password against a stored representation. */
    boolean matches(String rawPassword, String storedPassword);
}
