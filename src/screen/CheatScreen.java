package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import controller.App;
import model.Store;
import model.enums.MenuName;

/**
 * Developer-only entry screen. Lists every {@link MenuName} as a button so
 * each teammate can jump straight into the menu they are working on without
 * needing the rest of the flow (login, previous menus, etc.) to be wired up
 * yet. This screen does not touch any controller logic - it only flips
 * {@link Store#setCurrentMenu(MenuName)} and asks {@link PvzGame} to render
 * whatever screen is currently mapped for that menu.
 */
public final class CheatScreen implements Screen {

    private final PvzGame game;

    private Stage stage;
    private Skin skin;

    public CheatScreen(PvzGame game, App app) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(40);
        stage.addActor(root);

        root.add(new Label("Cheat Menu", skin)).padBottom(20).row();
        root.add(new Label("Jump to any menu:", skin)).padBottom(10).row();

        Table buttonList = new Table();
        buttonList.defaults().pad(6).fillX();

        for (MenuName menu : MenuName.values()) {
            TextButton button = new TextButton(menu.getDisplayName(), skin);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Store.setCurrentMenu(menu);
                    game.goToScreenForCurrentMenu();
                }
            });
            buttonList.add(button).width(320).row();
        }

        ScrollPane scrollPane = new ScrollPane(buttonList, skin);
        scrollPane.setFadeScrollBars(false);
        root.add(scrollPane).expand().fill().row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
