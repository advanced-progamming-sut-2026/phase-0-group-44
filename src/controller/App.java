package controller;

import model.Result;
import model.Store;
import model.enums.MenuName;
import model.inGame.plant.JsonPlantRepository;
import model.inGame.plant.PlantRepository;
import model.inGame.zombie.JsonZombieRepository;
import model.inGame.zombie.ZombieRepository;
import repository.JsonUserRepository;
import repository.UserRepository;
import service.PasswordService;
import service.SecurityQuestionCatalog;
import service.Sha256PasswordService;
import service.UserService;

import java.time.Clock;

public class App {
    private static final String PLANTS_PATH = "src/assets/plants.json";
    private static final String ZOMBIES_PATH = "src/assets/zombies.json";

    private final PlantRepository plantRepository;
    private final ZombieRepository zombieRepository;
    private final CollectionMenuController collectionController;
    private final UserService userService;
    private final MenuController menuController;
    private final PasswordService passwordService;
    private final SecurityQuestionCatalog questionCatalog;
    private final RegisterMenuController registerController;
    private final LoginMenuController loginController;

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
        menuController = new MenuController(userService);
        passwordService = new Sha256PasswordService();
        questionCatalog = SecurityQuestionCatalog.fromFile(SecurityQuestionCatalog.DEFAULT_PATH);
        registerController =
                new RegisterMenuController(userService, passwordService, questionCatalog);
        loginController = new LoginMenuController(userService, passwordService);
    }

    /** Restores saved accounts and arranges for progress to be flushed on exit. */
    public Result<Integer> start() {
        Result<Integer> loaded = userService.loadUsers();

        if (Store.getLoggedInUser() != null) {
            Store.setCurrentMenu(MenuName.MAIN);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(userService::shutdown));

        return loaded;
    }

    public UserService getUserService() {
        return userService;
    }

    public MenuController getMenuController() {
        return menuController;
    }

    public RegisterMenuController getRegisterController() {
        return registerController;
    }

    public LoginMenuController getLoginController() {
        return loginController;
    }

    public CollectionMenuController getCollectionController() {
        return collectionController;
    }
}
