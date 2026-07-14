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
import service.GreenhouseBoostService;
import service.NewsService;
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
    private final MainMenuController mainController;
    private final GameMenuController gameController;
    private final SettingsMenuController settingsController;
    private final NewsMenuController newsController;
    private final ProfileMenuController profileController;
    private final NewsService newsService;
    private final GreenhouseBoostService greenhouseBoostService;
    private final PlantSelectionController plantSelectionController;

    public App() {
        this(new JsonUserRepository(), Clock.systemDefaultZone());
    }

    public App(UserRepository userRepository, Clock clock) {
        plantRepository = new JsonPlantRepository(PLANTS_PATH);
        zombieRepository = new JsonZombieRepository(ZOMBIES_PATH);

        loadQuietly(plantRepository::load, "plant");
        loadQuietly(zombieRepository::load, "zombie");

        userService = new UserService(userRepository, clock);
        collectionController =
                new CollectionMenuController(plantRepository, zombieRepository, userService);
        menuController = new MenuController(userService);
        passwordService = new Sha256PasswordService();
        questionCatalog = SecurityQuestionCatalog.fromFile(SecurityQuestionCatalog.DEFAULT_PATH);
        registerController =
                new RegisterMenuController(userService, passwordService, questionCatalog);
        loginController = new LoginMenuController(userService, passwordService);
        mainController = new MainMenuController(userService);
        gameController = new GameMenuController(userService);
        settingsController = new SettingsMenuController(userService);
        newsController = new NewsMenuController(userService);
        profileController = new ProfileMenuController(userService, passwordService);
        newsService = new NewsService(userService);
        greenhouseBoostService = new GreenhouseBoostService(userService);
        plantSelectionController = new PlantSelectionController(plantRepository, userService);
    }

    /**
     * Loads a data source without letting a malformed file abort start-up. A
     * failure leaves the registry empty and the menus usable; the collection
     * detail commands then report no data until the source is corrected.
     */
    private void loadQuietly(Runnable load, String label) {
        try {
            load.run();
        } catch (RuntimeException exception) {
            System.out.println("warning: could not load " + label
                    + " data (" + exception.getMessage() + ")");
        }
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

    public MainMenuController getMainController() {
        return mainController;
    }

    public GameMenuController getGameController() {
        return gameController;
    }

    public SettingsMenuController getSettingsController() {
        return settingsController;
    }

    public NewsMenuController getNewsController() {
        return newsController;
    }

    public ProfileMenuController getProfileController() {
        return profileController;
    }

    public NewsService getNewsService() {
        return newsService;
    }

    public GreenhouseBoostService getGreenhouseBoostService() {
        return greenhouseBoostService;
    }

    public PlantSelectionController getPlantSelectionController() {
        return plantSelectionController;
    }
}
