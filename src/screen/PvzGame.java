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

    /**
     * هر Controller بعد از یک اکشن موفق، این متد را صدا می‌زند
     * تا صفحه عوض شود.
     */
    public void goToScreenForCurrentMenu() {
        switch (model.Store.getCurrentMenu()) {
            case REGISTER -> setScreen(new RegisterScreen(this, app));
            case LOGIN -> setScreen(new LoginScreen(this, app));
            case MAIN -> setScreen(new MainMenuScreen(this, app));
            case GAME -> setScreen(new AdventureMenuScreen(this, app));
            case GREENHOUSE -> setScreen(new GreenhouseScreen(this, app));
            case SHOP -> setScreen(new ShopScreen(this, app));
            case COLLECTION -> setScreen(new CollectionScreen(this, app));
            case GAMEPLAY -> setScreen(new GameplayScreen(this, app));
            case TRAVEL_LOG -> setScreen(new TravelLogScreen(this, app));
            case SETTINGS -> setScreen(new SettingsScreen(this,app));
            case PROFILE -> setScreen(new ProfileScreen(this,app));
            case LEADERBOARD ->
                    setScreen(
                            new LeaderboardScreen(
                                    this,
                                    app
                            )
                    );
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
