package pvz.browser.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;

import pvz.browser.core.BrowserContext;

public class PvzAssetBrowser extends Game {
    private final Color bgColor = Color.valueOf("625834");

    @Override
    public void create() {
        BrowserContext.init();

        UiManager.init();
        Screens.init();
        UiManager.setCurrentScreen(Screens.animations);
    }

    @Override
    public void render() {
        if (BrowserContext.textures != null)
            BrowserContext.textures.update();
        ScreenUtils.clear(bgColor);
        UiManager.render(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void resize(int width, int height) {
        UiManager.resize(width, height);
    }
}
