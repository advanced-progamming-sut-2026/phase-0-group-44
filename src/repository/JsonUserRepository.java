package repository;

import persistence.AtomicFileWriter;
import persistence.GsonSaveSerializer;
import persistence.SaveDataException;
import persistence.SaveFile;
import persistence.SaveSerializer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Stores users in a single JSON file next to the program.
 *
 * <p>The path, the serializer and the writer are injected so tests can run
 * against a temporary directory.</p>
 */
public class JsonUserRepository implements UserRepository {

    /** Default location, used when the caller does not supply one. */
    public static final Path DEFAULT_SAVE_PATH = Path.of("data", "users.json");

    private final Path saveFilePath;
    private final SaveSerializer serializer;
    private final AtomicFileWriter writer;

    public JsonUserRepository() {
        this(DEFAULT_SAVE_PATH, new GsonSaveSerializer(), new AtomicFileWriter());
    }

    public JsonUserRepository(Path saveFilePath) {
        this(saveFilePath, new GsonSaveSerializer(), new AtomicFileWriter());
    }

    public JsonUserRepository(
            Path saveFilePath,
            SaveSerializer serializer,
            AtomicFileWriter writer
    ) {
        this.saveFilePath = saveFilePath;
        this.serializer = serializer;
        this.writer = writer;
    }

    @Override
    public SaveFile load() {
        if (!Files.exists(saveFilePath)) {
            return new SaveFile();
        }

        String content = read();

        return serializer.deserialize(content);
    }

    @Override
    public void save(SaveFile saveFile) {
        String content = serializer.serialize(saveFile);

        try {
            writer.write(saveFilePath, content);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not write the save file: " + saveFilePath,
                    exception
            );
        }
    }

    @Override
    public Path quarantineCorruptSave(String stamp) {
        if (!Files.exists(saveFilePath)) {
            return null;
        }

        Path backup = saveFilePath.resolveSibling(
                saveFilePath.getFileName() + ".corrupt." + stamp
        );

        try {
            Files.move(saveFilePath, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not move the unreadable save file aside: " + saveFilePath,
                    exception
            );
        }

        return backup;
    }

    @Override
    public Path getSaveFilePath() {
        return saveFilePath;
    }

    private String read() {
        try {
            return Files.readString(saveFilePath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new SaveDataException(
                    "Could not read the save file: " + saveFilePath,
                    exception
            );
        }
    }
}
