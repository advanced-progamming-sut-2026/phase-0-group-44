package repository;

import persistence.SaveFile;

import java.nio.file.Path;

/**
 * Storage boundary for registered users and their progress.
 *
 * <p>Implementations own the storage medium only; they contain no game rules.</p>
 */
public interface UserRepository {

    /** Reads the stored save data, or empty save data when nothing is stored yet. */
    SaveFile load();

    /** Writes the given save data, replacing whatever was stored before. */
    void save(SaveFile saveFile);

    /** Moves unreadable save data aside so a fresh save can be written safely. */
    Path quarantineCorruptSave(String stamp);

    /** Location of the save file this repository reads and writes. */
    Path getSaveFilePath();
}
