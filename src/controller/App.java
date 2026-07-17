package controller;

import model.Result;
import model.events.DomainEventBus;
import model.Store;
import model.enums.MenuName;
import model.inGame.plant.PlantRepository;
import model.inGame.zombie.ZombieRepository;
import model.inGame.zombie.ZombieRegistry;
import repository.JsonUserRepository;
import repository.UserRepository;
import service.PasswordService;
import service.QuestCatalog;
import service.QuestRewardService;
import service.QuestService;
import model.sim.board.DefaultPlantSpecSource;
import model.sim.zombie.DefaultZombieSpecSource;
import model.sim.zombie.ZombieSpecSource;
import service.DomainEventPublisher;
import service.GameConclusionService;
import service.RewardService;
import util.SeededRandomSource;
import model.sim.board.PlantSpecSource;
import service.GreenhouseBoostService;
import service.NewsService;
import service.SecurityQuestionCatalog;
import service.Sha256PasswordService;
import service.UserService;
import model.inGame.plant.PlantRegistry;
import java.time.Clock;

public class App {
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
    private final GreenhouseController greenhouseController;
    private final ShopController shopController;
    private final SettingsMenuController settingsController;
    private final NewsMenuController newsController;
    private final ProfileMenuController profileController;
    private final NewsService newsService;
    private final GreenhouseBoostService greenhouseBoostService;
    private final PlantSelectionController plantSelectionController;
    private final PlantSpecSource plantSpecSource;
    private final ZombieSpecSource zombieSpecSource;
    private final RewardService rewardService;
    private final GameConclusionService conclusionService;
    private final DomainEventBus domainEventBus;
    private final DomainEventPublisher domainEvents;
    private final QuestCatalog questCatalog;
    private final QuestService questService;
    private final TravelMenuController travelController;
    private final MiniGameController miniGameController;

    public App() {
        this(new JsonUserRepository(), Clock.systemDefaultZone());
    }

    public App(UserRepository userRepository, Clock clock) {
        plantRepository = PlantRegistry.getDefault();
        zombieRepository = ZombieRegistry.getDefault();

        loadQuietly(plantRepository::load, "plant");
        loadQuietly(zombieRepository::load, "zombie");

        userService = new UserService(userRepository, clock);
        domainEventBus = new DomainEventBus();
        domainEvents = new DomainEventPublisher(domainEventBus, userService);
        questCatalog = QuestCatalog.fromFile(QuestCatalog.DEFAULT_PATH);
        questService = new QuestService(
                questCatalog,
                new QuestRewardService(plantRepository, new SeededRandomSource()),
                userService
        );
        domainEventBus.subscribe(questService);
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
        travelController = new TravelMenuController(questService);
        miniGameController = new MiniGameController(domainEvents, userService);
        greenhouseController = new GreenhouseController(
                plantRepository, userService, new SeededRandomSource()
        );
        shopController = new ShopController(
                plantRepository, userService, new SeededRandomSource(), domainEvents
        );
        settingsController = new SettingsMenuController(userService);
        newsController = new NewsMenuController(userService);
        profileController = new ProfileMenuController(userService, passwordService);
        newsService = new NewsService(userService);
        greenhouseBoostService = new GreenhouseBoostService(userService);
        plantSpecSource = new DefaultPlantSpecSource(plantRepository);
        zombieSpecSource = new DefaultZombieSpecSource(zombieRepository);
        rewardService = new RewardService(userService, new SeededRandomSource());
        conclusionService = new GameConclusionService(userService, newsService);
        plantSelectionController = new PlantSelectionController(
                plantRepository, userService, new SeededRandomSource(), domainEvents);
        plantSelectionController.setZombieSpecSource(zombieSpecSource);
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

    public TravelMenuController getTravelController() {
        return travelController;
    }

    public MiniGameController getMiniGameController() {
        return miniGameController;
    }

    public ShopController getShopController() {
        return shopController;
    }

    public GreenhouseController getGreenhouseController() {
        return greenhouseController;
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

    public PlantSpecSource getPlantSpecSource() {
        return plantSpecSource;
    }

    public ZombieSpecSource getZombieSpecSource() {
        return zombieSpecSource;
    }

    public RewardService getRewardService() {
        return rewardService;
    }

    public GameConclusionService getConclusionService() {
        return conclusionService;
    }

    public DomainEventBus getDomainEventBus() {
        return domainEventBus;
    }

    public DomainEventPublisher getDomainEvents() {
        return domainEvents;
    }

    public QuestService getQuestService() {
        return questService;
    }
}
