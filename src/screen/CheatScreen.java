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
import model.Store;
import model.enums.MenuName;
import model.user.User;
import pvz.skin.PvzSkin;

/**
 * Developer-only entry screen.
 *
 * <p>Jumping directly to Adventure (or another authenticated menu) used to
 * leave {@link Store#getLoggedInUser()} null.  That made every chapter look
 * locked even though the progression code was correct.  The cheat screen now
 * borrows the first loaded save as a preview user when a developer skips the
 * login flow.  It never creates or persists a fake account.</p>
 */
public final class CheatScreen implements Screen {

    private final PvzGame game;

    private Stage stage;
    private Skin skin;
    private Label statusLabel;

    public CheatScreen(PvzGame game, controller.App app) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        skin = PvzSkin.get();
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(40);
        stage.addActor(root);

        root.add(new Label("Cheat Menu", skin)).padBottom(20).row();
        root.add(new Label("Jump to any menu:", skin)).padBottom(8).row();

        statusLabel = new Label(
                "Authenticated previews use the first saved account if you skip Login.",
                skin
        );
        statusLabel.setWrap(true);
        root.add(statusLabel).width(520f).padBottom(10f).row();

        Table buttonList = new Table();
        buttonList.defaults().pad(6).fillX();

        for (MenuName menu : MenuName.values()) {
            TextButton button = new TextButton(menu.getDisplayName(), skin);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (!preparePreviewUser(menu)) {
                        return;
                    }
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

    private boolean preparePreviewUser(MenuName destination) {
        if (destination == MenuName.REGISTER || destination == MenuName.LOGIN) {
            Store.setLoggedInUser(null);
            return true;
        }

        if (Store.getLoggedInUser() != null) {
            Store.getLoggedInUser().applyDefaults();
            return true;
        }

        if (!Store.getUsers().isEmpty()) {
            User preview = Store.getUsers().get(0);
            preview.applyDefaults();
            Store.setLoggedInUser(preview);
            statusLabel.setText("Dev preview user: " + preview.getUsername());
            return true;
        }

        statusLabel.setText(
                "No saved account exists. Open Register/Login first, then return to this screen."
        );
        return false;
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

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }
    }
}
