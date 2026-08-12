package screen;

import com.badlogic.gdx.Game;
import controller.App;

public final class PvzGame extends Game {

    private App app;

    @Override
    public void create() {
        app = new App();
        System.out.println(app.start().getMessage());

        // Cheat menu: dev-only landing screen with a button to every menu,
        // so each teammate can jump straight to the menu they're building.
        setScreen(new CheatScreen(this, app));
    }

    /** هر Controller بعد از یک اکشن موفق، این متد را صدا می‌زند تا صفحه عوض شود. */
    public void goToScreenForCurrentMenu() {
        switch (model.Store.getCurrentMenu()) {
            case REGISTER -> setScreen(new RegisterScreen(this, app));
            case LOGIN -> setScreen(new LoginScreen(this, app));
        //    case MAIN -> setScreen(new MainMenuScreen(this, app));
        //    case GAME -> setScreen(new ChapterSelectScreen(this, app));
        //    case SCORED_GAME -> setScreen(new ScoredGameScreen(this, app));
            // ... بقیه‌ی MenuNameها به مرور اضافه می‌شن
            default -> setScreen(new RegisterScreen(this, app));
        }
    }

    public App getApp() {
        return app;
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        app.getUserService().shutdown();
        super.dispose();
    }
}