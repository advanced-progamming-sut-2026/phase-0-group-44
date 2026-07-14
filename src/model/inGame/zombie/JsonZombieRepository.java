package model.inGame.zombie;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.enums.ZombieType;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class JsonZombieRepository implements ZombieRepository {

    private final String filePath;
    private final ArrayList<ZombieDefinition> zombies;

    public JsonZombieRepository(String filePath) {
        this.filePath = filePath;
        this.zombies = new ArrayList<>();
    }

    public void load() {
        zombies.clear();

        try (FileReader reader = new FileReader(filePath)) {

            Gson gson = new Gson();

            Type listType =
                    new TypeToken<ArrayList<ZombieDefinition>>() {
                    }.getType();

            ArrayList<ZombieDefinition> loaded =
                    gson.fromJson(reader, listType);

            if (loaded != null) {
                zombies.addAll(loaded);
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not load zombies from: " + filePath,
                    e
            );
        }
    }

    @Override
    public ArrayList<ZombieDefinition> findAll() {
        return new ArrayList<>(zombies);
    }

    @Override
    public ZombieDefinition findByType(ZombieType type) {
        if (type == null) return null;

        for (ZombieDefinition zombie : zombies) {
            if (zombie.getType() == type) {
                return zombie;
            }
        }

        return null;
    }

    @Override
    public ZombieDefinition findByName(String name) {
        if (name == null) return null;

        String normalized = name.trim();

        for (ZombieDefinition zombie : zombies) {
            if (zombie.getName().equalsIgnoreCase(normalized)
                    || zombie.getType().name().equalsIgnoreCase(
                    normalizeEnumName(normalized))) {
                return zombie;
            }
        }

        return null;
    }

    private String normalizeEnumName(String value) {
        return value.trim()
                .toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');
    }
}