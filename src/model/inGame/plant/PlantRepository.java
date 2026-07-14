package model.inGame.plant;

import model.enums.PlantType;

import java.util.ArrayList;

public interface PlantRepository {
    void load();

    ArrayList<PlantDefinition> findAll();

    PlantDefinition findByType(PlantType type);

    PlantDefinition findByName(String name);
}

