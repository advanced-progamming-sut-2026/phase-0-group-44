package service;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The fixed list of security questions offered during registration.
 *
 * <p>The list is data, not code: it is read from a JSON file so that replacing
 * it with the list the specification prescribes needs no code change.</p>
 */
public class SecurityQuestionCatalog {

    /** Location used by the running program. */
    public static final Path DEFAULT_PATH = Path.of("src", "assets", "security-questions.json");

    private final List<String> questions;

    /** Catalog with an explicit list; used by tests and by callers with their own source. */
    public SecurityQuestionCatalog(List<String> questions) {
        this.questions = questions == null ? new ArrayList<>() : new ArrayList<>(questions);
    }

    /** Reads the catalog from a file; an unreadable or missing file yields an empty catalog. */
    public static SecurityQuestionCatalog fromFile(Path path) {
        if (!Files.exists(path)) {
            return new SecurityQuestionCatalog(new ArrayList<>());
        }

        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            CatalogFile parsed = new Gson().fromJson(content, CatalogFile.class);

            if (parsed == null || parsed.questions == null) {
                return new SecurityQuestionCatalog(new ArrayList<>());
            }

            return new SecurityQuestionCatalog(parsed.questions);
        } catch (IOException | JsonParseException exception) {
            return new SecurityQuestionCatalog(new ArrayList<>());
        }
    }

    public List<String> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public boolean isEmpty() {
        return questions.isEmpty();
    }

    public int size() {
        return questions.size();
    }

    /** @param number 1-based index as shown to the player */
    public boolean isValidNumber(int number) {
        return number >= 1 && number <= questions.size();
    }

    /** @param number 1-based index as shown to the player */
    public String getQuestion(int number) {
        if (!isValidNumber(number)) {
            return null;
        }

        return questions.get(number - 1);
    }

    /** The numbered list shown after a valid registration command. */
    public String render() {
        StringBuilder builder = new StringBuilder();

        for (int number = 1; number <= questions.size(); number++) {
            builder.append(number)
                    .append(") ")
                    .append(questions.get(number - 1))
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    /** Layout of the JSON file. */
    private static final class CatalogFile {
        private List<String> questions;
    }
}
