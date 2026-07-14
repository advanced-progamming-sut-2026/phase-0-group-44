package controller;

import model.Result;
import model.inGame.plant.JsonPlantRepository;
import model.inGame.plant.PlantRepository;
import model.inGame.zombie.JsonZombieRepository;
import model.inGame.zombie.ZombieRepository;
import repository.JsonUserRepository;
import repository.UserRepository;
import service.UserService;

import java.time.Clock;

public class App {
    private static final String PLANTS_PATH = "src/assets/plants.json";
    private static final String ZOMBIES_PATH = "src/assets/zombies.json";

    private final PlantRepository plantRepository;
    private final ZombieRepository zombieRepository;
    private final CollectionMenuController collectionController;
    private final UserService userService;

    public App() {
        this(new JsonUserRepository(), Clock.systemDefaultZone());
    }

    public App(UserRepository userRepository, Clock clock) {
        plantRepository = new JsonPlantRepository(PLANTS_PATH);
        zombieRepository = new JsonZombieRepository(ZOMBIES_PATH);

        plantRepository.load();
        zombieRepository.load();

        collectionController = new CollectionMenuController(plantRepository, zombieRepository);
        userService = new UserService(userRepository, clock);
    }

    /** Restores saved accounts and arranges for progress to be flushed on exit. */
    public Result<Integer> start() {
        Result<Integer> loaded = userService.loadUsers();

        Runtime.getRuntime().addShutdownHook(new Thread(userService::shutdown));

        return loaded;
    }

    public UserService getUserService() {
        return userService;
    }

    public CollectionMenuController getCollectionController() {
        return collectionController;
    }
}
