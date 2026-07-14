package persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import model.user.User;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * JSON implementation of {@link SaveSerializer}, built on the Gson library that
 * the project already ships in {@code lib/}.
 *
 * <p>Reading accepts two shapes:</p>
 * <ul>
 *   <li>the current versioned envelope, a JSON object;</li>
 *   <li>the legacy shape, a bare JSON array of users, which is migrated.</li>
 * </ul>
 */
public class GsonSaveSerializer implements SaveSerializer {

    private static final Type USER_LIST_TYPE =
            new TypeToken<ArrayList<User>>() { }.getType();

    private final Gson gson;
    private final SaveFileMigrator migrator;

    public GsonSaveSerializer() {
        this(new SaveFileMigrator());
    }

    public GsonSaveSerializer(SaveFileMigrator migrator) {
        this.migrator = migrator;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    @Override
    public String serialize(SaveFile saveFile) {
        SaveFile target = saveFile == null ? new SaveFile() : saveFile;
        target.setVersion(SaveFile.CURRENT_VERSION);

        return gson.toJson(target);
    }

    @Override
    public SaveFile deserialize(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new SaveFile();
        }

        JsonElement root = parse(content);

        if (root.isJsonArray()) {
            return migrator.migrate(readLegacyArray(root));
        }

        if (!root.isJsonObject()) {
            throw new SaveDataException("Save file must contain a JSON object or array.");
        }

        return migrator.migrate(readEnvelope(root));
    }

    private JsonElement parse(String content) {
        try {
            return JsonParser.parseString(content);
        } catch (JsonParseException exception) {
            throw new SaveDataException("Save file is not valid JSON.", exception);
        }
    }

    private SaveFile readLegacyArray(JsonElement root) {
        SaveFile saveFile = new SaveFile();
        saveFile.setVersion(SaveFile.LEGACY_VERSION);
        saveFile.setUsers(gson.fromJson(root, USER_LIST_TYPE));

        return saveFile;
    }

    private SaveFile readEnvelope(JsonElement root) {
        try {
            SaveFile saveFile = gson.fromJson(root, SaveFile.class);

            return saveFile == null ? new SaveFile() : saveFile;
        } catch (JsonParseException exception) {
            throw new SaveDataException("Save file does not match the expected layout.", exception);
        }
    }
}
