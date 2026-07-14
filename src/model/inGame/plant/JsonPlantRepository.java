package model.inGame.plant;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.enums.PlantType;

public class JsonPlantRepository implements PlantRepository {
    private final String filePath;
    private final ArrayList<PlantDefinition> plants;

    public JsonPlantRepository(String filePath) {
        this.filePath = filePath;
        this.plants = new ArrayList<PlantDefinition>();
    }

    @Override
    public void load() {
        plants.clear();

        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();

            Type listType = new TypeToken<ArrayList<PlantDefinition>>() {}.getType();

            ArrayList<PlantDefinition> loadedPlants = gson.fromJson(reader, listType);

            if (loadedPlants != null) {
                plants.addAll(loadedPlants);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load plant definitions from: " + filePath,
                    exception
            );
        }
    }

    @Override
    public ArrayList<PlantDefinition> findAll() {
        return new ArrayList<PlantDefinition>(plants);
    }

    @Override
    public PlantDefinition findByType(PlantType type) {
        if (type == null) {
            return null;
        }

        for (PlantDefinition plant : plants) {
            if (plant.getType() == type) {
                return plant;
            }
        }

        return null;
    }

    @Override
    public PlantDefinition findByName(String name) {
        if (name == null) {
            return null;
        }

        String normalizedName = name.trim();

        for (PlantDefinition plant : plants) {
            if (plant.getName().equalsIgnoreCase(normalizedName)
                    || plant.getType().name().equalsIgnoreCase(
                    normalizeEnumName(normalizedName)
            )) {
                return plant;
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

