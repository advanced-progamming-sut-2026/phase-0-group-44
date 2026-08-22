package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import controller.App;
import controller.MenuController;
import controller.SettingsMenuController;
import model.Result;
import pvz.skin.PvzSkin;

public final class SettingsScreen implements Screen {

    private final PvzGame game;
    private final SettingsMenuController settingsController;
    private final MenuController menuController;

    private Stage stage;
    private Skin skin;

    private SelectBox<String> difficultyBox;
    private SelectBox<String> gameSpeedBox;
    private CheckBox gridVisibleCheckBox;
    private CheckBox debugModeCheckBox;
    private Label statusLabel;

    private SpriteBatch batch;
    private Texture backgroundTexture;

    public SettingsScreen(PvzGame game, App app) {
        this.game = game;
        this.settingsController = app.getSettingsController();
        this.menuController = app.getMenuController();
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        skin = PvzSkin.get();

        batch = new SpriteBatch();
        backgroundTexture = TextureQuality.load("ui/settingsBG.PNG");

        Gdx.input.setInputProcessor(stage);

        buildScreen();
        loadCurrentSettings();
    }

    private void buildScreen() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(40f);

        stage.addActor(root);

        /*
         * Title
         */

        Label title = new Label("SETTINGS", skin);
        title.setFontScale(2.2f);

        root.add(title)
                .padBottom(30f)
                .row();

        Table panel = new Table();
        panel.defaults().pad(10f);

        /*
         * Difficulty
         */

        Label difficultyLabel = new Label(
                "Difficulty",
                skin
        );

        difficultyLabel.setFontScale(1.6f);

        panel.add(difficultyLabel)
                .left();

        difficultyBox = new SelectBox<>(skin);

        difficultyBox.setItems(
                "1",
                "2",
                "3",
                "4",
                "5"
        );

        panel.add(difficultyBox)
                .width(180f)
                .row();

        /*
         * Game Speed
         */

        Label gameSpeedLabel = new Label(
                "Game Speed",
                skin
        );

        gameSpeedLabel.setFontScale(1.60f);

        panel.add(gameSpeedLabel)
                .left();

        gameSpeedBox = new SelectBox<>(skin);

        gameSpeedBox.setItems(
                "1",
                "2",
                "3"
        );

        panel.add(gameSpeedBox)
                .width(180f)
                .row();

        /*
         * Grid option
         */

        gridVisibleCheckBox = new CheckBox(
                " Show red grid during game",
                skin
        );

        gridVisibleCheckBox
                .getLabel()
                .setFontScale(1.3f);

        panel.add(gridVisibleCheckBox)
                .colspan(2)
                .left()
                .row();

        /*
         * Debug mode
         */

        debugModeCheckBox = new CheckBox(
                " Enable debug mode",
                skin
        );

        debugModeCheckBox
                .getLabel()
                .setFontScale(1.3f);

        panel.add(debugModeCheckBox)
                .colspan(2)
                .left()
                .row();

        /*
         * Apply button
         */

        TextButton applyButton = new TextButton(
                "APPLY",
                skin
        );

        applyButton
                .getLabel()
                .setFontScale(1.30f);

        applyButton.addListener(
                applySettingsListener()
        );

        panel.add(applyButton)
                .colspan(2)
                .width(240f)
                .height(55f)
                .padTop(20f)
                .row();

        /*
         * Back button
         */

        TextButton backButton = new TextButton(
                "BACK",
                skin
        );

        backButton
                .getLabel()
                .setFontScale(1.30f);

        backButton.addListener(
                backListener()
        );

        panel.add(backButton)
                .colspan(2)
                .width(240f)
                .height(55f)
                .row();

        /*
         * Status
         */

        statusLabel = new Label(
                "",
                skin
        );

        statusLabel.setFontScale(1.15f);
        statusLabel.setWrap(true);

        panel.add(statusLabel)
                .colspan(2)
                .width(520f)
                .padTop(15f)
                .row();

        root.add(panel);
    }

    private void loadCurrentSettings() {
        Result<Integer> difficultyResult =
                settingsController.showDifficulty();

        if (difficultyResult.getStatus()
                && difficultyResult.getData() != null) {

            difficultyBox.setSelected(
                    String.valueOf(
                            difficultyResult.getData()
                    )
            );
        }

        Result<Integer> speedResult =
                settingsController.showGameSpeed();

        if (speedResult.getStatus()
                && speedResult.getData() != null) {

            gameSpeedBox.setSelected(
                    String.valueOf(
                            speedResult.getData()
                    )
            );
        }

        Result<Boolean> gridResult =
                settingsController.showGridVisible();

        if (gridResult.getStatus()
                && gridResult.getData() != null) {

            gridVisibleCheckBox.setChecked(
                    gridResult.getData()
            );
        }

        Result<Boolean> debugResult =
                settingsController.showDebugMode();

        if (debugResult.getStatus()
                && debugResult.getData() != null) {

            debugModeCheckBox.setChecked(
                    debugResult.getData()
            );
        }

        statusLabel.setText(
                "Settings loaded."
        );
    }

    private ChangeListener applySettingsListener() {
        return new ChangeListener() {

            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {

                int difficulty =
                        Integer.parseInt(
                                difficultyBox.getSelected()
                        );

                int gameSpeed =
                        Integer.parseInt(
                                gameSpeedBox.getSelected()
                        );

                boolean gridVisible =
                        gridVisibleCheckBox.isChecked();

                boolean debugMode =
                        debugModeCheckBox.isChecked();

                Result<Integer> difficultyResult =
                        settingsController.changeDifficulty(
                                difficulty
                        );

                Result<Integer> speedResult =
                        settingsController.changeGameSpeed(
                                gameSpeed
                        );

                Result<Boolean> gridResult =
                        settingsController.changeGridVisible(
                                gridVisible
                        );

                Result<Boolean> debugResult =
                        settingsController.changeDebugMode(
                                debugMode
                        );

                if (difficultyResult.getStatus()
                        && speedResult.getStatus()
                        && gridResult.getStatus()
                        && debugResult.getStatus()) {

                    statusLabel.setText(
                            "Settings saved successfully."
                    );

                } else {

                    statusLabel.setText(
                            difficultyResult.getMessage()
                                    + "\n"
                                    + speedResult.getMessage()
                                    + "\n"
                                    + gridResult.getMessage()
                                    + "\n"
                                    + debugResult.getMessage()
                    );
                }
            }
        };
    }

    private ChangeListener backListener() {
        return new ChangeListener() {

            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {

                Result<String> result =
                        menuController.exitMenu();

                if (result.getStatus()) {

                    game.goToScreenForCurrentMenu();

                } else {

                    statusLabel.setText(
                            result.getMessage()
                    );
                }
            }
        };
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(
                0f,
                0f,
                0f,
                1f
        );

        Gdx.gl.glClear(
                GL20.GL_COLOR_BUFFER_BIT
        );

        batch.begin();

        batch.draw(
                backgroundTexture,
                0,
                0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );

        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(
            int width,
            int height
    ) {

        stage.getViewport().update(
                width,
                height,
                true
        );
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
        if (stage != null) {
            stage.dispose();
        }

        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }

        if (batch != null) {
            batch.dispose();
        }
    }
}