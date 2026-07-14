package persistence;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes a text file safely: the content goes to a temporary file in the same
 * directory, is flushed to disk, and only then replaces the target.
 *
 * <p>A crash therefore leaves either the previous save or the new one intact,
 * never a half-written file.</p>
 */
public class AtomicFileWriter {

    private static final String TEMP_SUFFIX = ".tmp";

    public void write(Path target, String content) throws IOException {
        Path directory = target.toAbsolutePath().getParent();

        if (directory != null) {
            Files.createDirectories(directory);
        }

        Path temporary = resolveTemporary(target);

        try {
            Files.write(temporary, content.getBytes(StandardCharsets.UTF_8));
            forceToDisk(temporary);
            replace(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private Path resolveTemporary(Path target) {
        return target.resolveSibling(target.getFileName() + TEMP_SUFFIX);
    }

    private void forceToDisk(Path file) throws IOException {
        try (RandomAccessFile handle = new RandomAccessFile(file.toFile(), "rw")) {
            handle.getFD().sync();
        }
    }

    private void replace(Path temporary, Path target) throws IOException {
        try {
            Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
