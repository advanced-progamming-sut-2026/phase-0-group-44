package service;

import model.GameEngine;
import model.Position;
import model.enums.PlantType;
import model.inGame.GameMap;
import model.inGame.plant.Plant;
import model.inGame.plant.PlantRegistry;

import java.util.Random;

public class GameService {
    private final PlantRegistry plantRegistry;
    private final GameEngine engine;

    public GameService() {
        this(PlantRegistry.getDefault());
    }

    public GameService(PlantRegistry plantRegistry) {
        this.plantRegistry = plantRegistry;
        this.engine = new GameEngine(plantRegistry, new GameMap(), new Random(0));
    }

    public Plant plant(PlantType type, int level, int row, int column) {
        return engine.plant(type, level, new Position(row, column));
    }

    public Plant plantImitater(PlantType copiedType, int imitaterLevel, int row, int column) {
        return engine.plantImitater(copiedType, imitaterLevel, new Position(row, column));
    }

    public void advanceTime(double seconds) {
        engine.tick(seconds);
    }

    public PlantRegistry getPlantRegistry() {
        return plantRegistry;
    }

    public GameEngine getEngine() {
        return engine;
    }
}
