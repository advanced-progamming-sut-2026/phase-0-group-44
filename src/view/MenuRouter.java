package view;

import controller.App;
import model.Store;
import model.enums.MenuName;

/** Sends one line of input to the view of the menu the player is currently in. */
public class MenuRouter {

    private final RegisterMenuView registerMenuView;
    private final LoginMenuView loginMenuView;
    private final CommonMenuView commonMenuView;

    public MenuRouter(App app) {
        this.registerMenuView = new RegisterMenuView(
                app.getMenuController(), app.getRegisterController());
        this.loginMenuView = new LoginMenuView(
                app.getMenuController(), app.getLoginController());
        this.commonMenuView = new CommonMenuView(app.getMenuController());
    }

    public void route(String input) {
        MenuName current = Store.getCurrentMenu();

        if (current == MenuName.REGISTER) {
            registerMenuView.checkCommand(input);
            return;
        }

        if (current == MenuName.LOGIN) {
            loginMenuView.checkCommand(input);
            return;
        }

        commonMenuView.checkCommand(input);
    }
}
