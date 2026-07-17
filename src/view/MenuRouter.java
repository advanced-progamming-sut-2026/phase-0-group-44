package view;

import controller.App;
import model.Store;
import controller.BoardController;
import controller.GameplayController;
import model.user.User;
import model.sim.SimulationWorld;
import model.sim.board.PlantSpecSource;
import model.Store;
import model.enums.MenuName;

/** Sends one line of input to the view of the menu the player is currently in. */
public class MenuRouter {

    private final controller.MenuController menuController;
    private final RegisterMenuView registerMenuView;
    private final LoginMenuView loginMenuView;
    private final GameMenuView gameMenuView;
    private final GreenhouseView greenhouseView;
    private final TravelMenuView travelMenuView;
    private final MiniGameMenu miniGameMenu;
    private final ShopView shopView;
    private final SettingsMenuView settingsMenuView;
    private final NewsMenuView newsMenuView;
    private final ProfileMenuView profileMenuView;
    private final CollectionMenuView collectionMenuView;
    private final PlantSelectionView plantSelectionView;
    private final CommonMenuView commonMenuView;
    private final PlantSpecSource plantSpecSource;
    private final controller.App app;

    public MenuRouter(App app) {
        this.menuController = app.getMenuController();
        this.registerMenuView = new RegisterMenuView(
                app.getMenuController(), app.getRegisterController());
        this.loginMenuView = new LoginMenuView(
                app.getMenuController(), app.getLoginController());
        this.gameMenuView = new GameMenuView(
                app.getMenuController(), app.getGameController(), app.getMainController());
        this.greenhouseView = new GreenhouseView(
                app.getMenuController(), app.getGreenhouseController());
        this.travelMenuView = new TravelMenuView(
                app.getMenuController(), app.getTravelController());
        this.miniGameMenu = new MiniGameMenu(
                app.getMenuController(), app.getMiniGameController());
        this.shopView = new ShopView(
                app.getMenuController(), app.getShopController());
        this.settingsMenuView = new SettingsMenuView(
                app.getMenuController(), app.getSettingsController(), app.getMainController());
        this.newsMenuView = new NewsMenuView(
                app.getMenuController(), app.getNewsController(), app.getMainController());
        this.profileMenuView = new ProfileMenuView(
                app.getMenuController(), app.getProfileController(), app.getMainController());
        this.collectionMenuView = new CollectionMenuView(
                app.getMenuController(), app.getCollectionController(), app.getMainController());
        this.plantSelectionView = new PlantSelectionView(
                app.getMenuController(), app.getPlantSelectionController());
        this.commonMenuView = new CommonMenuView(app.getMenuController(), app.getMainController());
        this.plantSpecSource = app.getPlantSpecSource();
        this.app = app;
    }

    private GameplayView gameplayView() {
        if (Store.getActiveSimulation() == null) {
            return new GameplayView(menuController, null);
        }

        SimulationWorld world = Store.getActiveSimulation().getWorld();
        BoardController board = new BoardController(
                world,
                Store.getActiveSession() == null ? null : Store.getActiveSession().getSelection(),
                plantSpecSource,
                app.getDomainEvents(),
                Store.getLoggedInUser(),
                Store.getActiveSession());
        User user = Store.getLoggedInUser();
        GameplayController controller = new GameplayController(
                Store.getActiveSimulation(),
                board,
                app.getRewardService(),
                app.getConclusionService(),
                Store.getActiveSession(),
                user,
                app.getDomainEvents());

        return new GameplayView(menuController, controller);
    }

    public void route(String input) {
        MenuName current = Store.getCurrentMenu();

        switch (current) {
            case REGISTER:
                registerMenuView.checkCommand(input);
                return;
            case LOGIN:
                loginMenuView.checkCommand(input);
                return;
            case GAME:
                gameMenuView.checkCommand(input);
                return;
            case GREENHOUSE:
                greenhouseView.checkCommand(input);
                return;
            case TRAVEL_LOG:
                travelMenuView.checkCommand(input);
                return;
            case MINIGAME:
                miniGameMenu.checkCommand(input);
                return;
            case SHOP:
                shopView.checkCommand(input);
                return;
            case SETTINGS:
                settingsMenuView.checkCommand(input);
                return;
            case NEWS:
                newsMenuView.checkCommand(input);
                return;
            case PROFILE:
                profileMenuView.checkCommand(input);
                return;
            case COLLECTION:
                collectionMenuView.checkCommand(input);
                return;
            case PLANT_SELECTION:
                plantSelectionView.checkCommand(input);
                return;
            case GAMEPLAY:
                gameplayView().checkCommand(input);
                return;
            default:
                commonMenuView.checkCommand(input);
        }
    }
}
