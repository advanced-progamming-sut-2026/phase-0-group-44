package persistence;

/** Thrown when the save file exists but cannot be read as valid save data. */
public class SaveDataException extends RuntimeException {
    public SaveDataException(String message) {
        super(message);
    }

    public SaveDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
