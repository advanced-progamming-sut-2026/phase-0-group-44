package persistence;

/**
 * Converts a {@link SaveFile} to and from its textual representation.
 *
 * <p>Kept as an interface so the storage format can change without touching
 * the repository, the services, or the domain model.</p>
 */
public interface SaveSerializer {
    String serialize(SaveFile saveFile);

    SaveFile deserialize(String content);
}
