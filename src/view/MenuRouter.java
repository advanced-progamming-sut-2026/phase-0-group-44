package view;

import controller.App;
import model.Store;
import model.enums.MenuName;

/** Sends one line of input to the view of the menu the player is currently in. */
public class MenuRouter {

    private final RegisterMenuView registerMenuView;
    private final LoginMenuView loginMenuView;
    private final GameMenuView gameMenuView;
    private final SettingsMenuView settingsMenuView;
    private final NewsMenuView newsMenuView;
    private final ProfileMenuView profileMenuView;
    private final CollectionMenuView collectionMenuView;
    private final PlantSelectionView plantSelectionView;
    private final CommonMenuView commonMenuView;

    public MenuRouter(App app) {
        this.registerMenuView = new RegisterMenuView(
                app.getMenuController(), app.getRegisterController());
        this.loginMenuView = new LoginMenuView(
                app.getMenuController(), app.getLoginController());
        this.gameMenuView = new GameMenuView(
                app.getMenuController(), app.getGameController(), app.getMainController());
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
            default:
                commonMenuView.checkCommand(input);
        }
    }
}
