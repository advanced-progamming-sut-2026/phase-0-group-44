package controller;

import model.inGame.plant.JsonPlantRepository;
import model.inGame.plant.PlantRepository;
import model.inGame.zombie.JsonZombieRepository;
import model.inGame.zombie.ZombieRepository;

public class App {
    private final PlantRepository plantRepository;
    private final ZombieRepository zombieRepository;
    private final CollectionMenuController collectionController;

    public App() {
        plantRepository = new JsonPlantRepository("data/plants.json");

        zombieRepository = new JsonZombieRepository("data/zombies.json");

        plantRepository.load();
        zombieRepository.load();

        collectionController = new CollectionMenuController(plantRepository, zombieRepository);
    }

        public void start() {
            // Start command loop
        }
}

