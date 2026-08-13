package pvz.browser.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import pvz.skin.BorderedTable;

public abstract class AbstractScreen {
    protected Skin skin;
    protected Stage uiStage;
    protected static SpriteBatch uiBatch;
    protected Table rootTable;

    public AbstractScreen() {
        skin = UiManager.skin;
        ScreenViewport screenViewport = new ScreenViewport();
        // screenViewport.setUnitsPerPixel(0.8f);
        uiStage = new Stage(screenViewport);

        rootTable = new Table();
        rootTable.setFillParent(true);

        uiStage.addActor(rootTable);

        if (uiBatch == null) {
            uiBatch = new SpriteBatch();
        }

        Gdx.input.setInputProcessor(uiStage);

        // uiStage.setDebugAll(true);
    }

    public void render(float delta) {
        uiStage.draw();
    }

    public void act(float delta) {
        uiStage.act(delta);
    }

    public void onEnter() {

    }

    public void onExit() {

    }

    public void drawOffset(float fractionX, float fractionY) {
        Camera camera = uiStage.getViewport().getCamera();
        float worldWidth = uiStage.getViewport().getWorldWidth();
        float worldHeight = uiStage.getViewport().getWorldHeight();
        camera.position.set(worldWidth / 2f - fractionX * worldWidth,
                worldHeight / 2f - fractionY * worldHeight, 0f);
        camera.update();
        uiStage.draw();
    }

    public Stage getStage() {
        return uiStage;
    }

    public void resize(int width, int height) {
        uiStage.getViewport().update(width, height, true);
    }
}
