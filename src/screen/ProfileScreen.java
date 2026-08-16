package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import controller.App;
import controller.MenuController;
import controller.ProfileMenuController;
import model.Result;
import model.Store;
import model.user.User;
import pvz.skin.PvzSkin;

public final class ProfileScreen implements Screen {

    // ---------------------------------------------------------
    // Colors
    // ---------------------------------------------------------

    private static final Color GOLD =
            new Color(1f, 0.72f, 0.08f, 1f);

    private static final Color ORANGE =
            new Color(1f, 0.40f, 0.08f, 1f);

    private static final Color LIME =
            new Color(0.55f, 1f, 0.12f, 1f);

    private static final Color CYAN =
            new Color(0.15f, 0.92f, 1f, 1f);

    private static final Color PINK =
            new Color(1f, 0.30f, 0.70f, 1f);

    private static final Color RED =
            new Color(1f, 0.20f, 0.14f, 1f);

    // ---------------------------------------------------------
    // Controllers
    // ---------------------------------------------------------

    private final PvzGame game;
    private final ProfileMenuController profileController;
    private final MenuController menuController;

    // ---------------------------------------------------------
    // LibGDX
    // ---------------------------------------------------------

    private Stage stage;
    private Skin skin;
    private SpriteBatch batch;

    private Texture morningBackground;
    private Texture nightBackground;

    private boolean nightMode = false;

    // ---------------------------------------------------------
    // Profile values
    // ---------------------------------------------------------

    private Label usernameValue;
    private Label nicknameValue;

    private Label gamesValue;
    private Label coinsValue;
    private Label gemsValue;
    private Label levelsValue;
    private Label mewPointValue;

    private Label statusLabel;

    // ---------------------------------------------------------
    // Text fields
    // ---------------------------------------------------------

    private TextField usernameField;
    private TextField nicknameField;
    private TextField emailField;

    private TextField oldPasswordField;
    private TextField newPasswordField;

    // ---------------------------------------------------------
    // Mode button
    // ---------------------------------------------------------

    private TextButton modeButton;

    public ProfileScreen(PvzGame game, App app) {
        this.game = game;
        this.profileController = app.getProfileController();
        this.menuController = app.getMenuController();
    }

    @Override
    public void show() {

        stage = new Stage(new ScreenViewport());
        skin = PvzSkin.get();

        batch = new SpriteBatch();

        /*
         * Morning:
         * assets/ui/profileBG.PNG
         *
         * Night:
         * assets/ui/profileBG1.PNG
         */

        morningBackground = new Texture(
                Gdx.files.internal("ui/profileBG1.PNG")
        );

        nightBackground = new Texture(
                Gdx.files.internal("ui/profileBG.PNG")
        );

        Gdx.input.setInputProcessor(stage);

        buildScreen();
        refreshProfile();
    }

    // =========================================================
    // SCREEN
    // =========================================================

    private void buildScreen() {

        Table root = new Table();

        root.setFillParent(true);
        root.pad(28f);

        stage.addActor(root);

        // -----------------------------------------------------
        // TOP
        // -----------------------------------------------------

        Table topBar = new Table();

        /*
         * Dummy space keeps title visually centered.
         */

        topBar.add()
                .width(210f);

        Label title = new Label(
                "PLAYER PROFILE",
                skin
        );

        title.setFontScale(1.9f);
        title.setColor(GOLD);

        topBar.add(title)
                .expandX()
                .center();

        modeButton = new TextButton(
                "NIGHT MODE",
                skin
        );

        modeButton.getLabel()
                .setFontScale(0.95f);

        modeButton.getLabel()
                .setColor(CYAN);

        modeButton.addListener(
                modeListener()
        );

        topBar.add(modeButton)
                .width(210f)
                .height(50f)
                .right();

        root.add(topBar)
                .width(1100f)
                .padBottom(20f)
                .row();

        // -----------------------------------------------------
        // BODY
        // -----------------------------------------------------

        Table body = new Table();

        Table editPanel =
                buildEditPanel();

        Table statsPanel =
                buildStatsPanel();

        body.add(editPanel)
                .width(510f)
                .top()
                .padRight(65f);

        body.add(statsPanel)
                .width(510f)
                .top();

        root.add(body)
                .width(1100f)
                .padBottom(10f)
                .row();

        // -----------------------------------------------------
        // STATUS
        // -----------------------------------------------------

        statusLabel = new Label(
                "",
                skin
        );

        statusLabel.setFontScale(1.08f);
        statusLabel.setAlignment(Align.center);
        statusLabel.setWrap(true);

        root.add(statusLabel)
                .width(750f)
                .padBottom(10f)
                .row();

        // -----------------------------------------------------
        // BACK
        // -----------------------------------------------------

        TextButton backButton = new TextButton(
                "BACK",
                skin
        );

        backButton.getLabel()
                .setFontScale(1.3f);

        backButton.getLabel()
                .setColor(GOLD);

        backButton.addListener(
                backListener()
        );

        root.add(backButton)
                .width(270f)
                .height(58f)
                .row();
    }

    // =========================================================
    // EDIT ACCOUNT
    // =========================================================

    private Table buildEditPanel() {

        Table panel = new Table();

        panel.pad(10f);

        // -----------------------------------------------------
        // Heading
        // -----------------------------------------------------

        Label heading = new Label(
                "EDIT ACCOUNT",
                skin
        );

        heading.setFontScale(1.5f);
        heading.setColor(ORANGE);

        panel.add(heading)
                .colspan(2)
                .center()
                .padBottom(22f)
                .row();

        // -----------------------------------------------------
        // Username
        // -----------------------------------------------------

        usernameField = new TextField(
                "",
                skin
        );

        usernameField.setMessageText(
                "New username"
        );

        addEditField(
                panel,
                "USERNAME",
                usernameField,
                usernameListener(),
                GOLD
        );

        // -----------------------------------------------------
        // Nickname
        // -----------------------------------------------------

        nicknameField = new TextField(
                "",
                skin
        );

        nicknameField.setMessageText(
                "New nickname"
        );

        addEditField(
                panel,
                "NICKNAME",
                nicknameField,
                nicknameListener(),
                LIME
        );

        // -----------------------------------------------------
        // Email
        // -----------------------------------------------------

        emailField = new TextField(
                "",
                skin
        );

        emailField.setMessageText(
                "New email"
        );

        addEditField(
                panel,
                "EMAIL",
                emailField,
                emailListener(),
                CYAN
        );

        // -----------------------------------------------------
        // Password title
        // -----------------------------------------------------

        Label passwordTitle = new Label(
                "PASSWORD",
                skin
        );

        passwordTitle.setFontScale(1.2f);
        passwordTitle.setColor(ORANGE);

        panel.add(passwordTitle)
                .colspan(2)
                .left()
                .padTop(16f)
                .padBottom(7f)
                .row();

        // -----------------------------------------------------
        // Current password
        // -----------------------------------------------------

        oldPasswordField = new TextField(
                "",
                skin
        );

        oldPasswordField.setMessageText(
                "Current password"
        );

        oldPasswordField.setPasswordMode(true);
        oldPasswordField.setPasswordCharacter('*');

        panel.add(oldPasswordField)
                .colspan(2)
                .width(420f)
                .height(48f)
                .padBottom(8f)
                .row();

        // -----------------------------------------------------
        // New password
        // -----------------------------------------------------

        newPasswordField = new TextField(
                "",
                skin
        );

        newPasswordField.setMessageText(
                "New password"
        );

        newPasswordField.setPasswordMode(true);
        newPasswordField.setPasswordCharacter('*');

        panel.add(newPasswordField)
                .colspan(2)
                .width(420f)
                .height(48f)
                .padBottom(11f)
                .row();

        // -----------------------------------------------------
        // Change password
        // -----------------------------------------------------

        TextButton passwordButton = new TextButton(
                "CHANGE PASSWORD",
                skin
        );

        passwordButton.getLabel()
                .setFontScale(1f);

        passwordButton.getLabel()
                .setColor(ORANGE);

        passwordButton.addListener(
                passwordListener()
        );

        panel.add(passwordButton)
                .colspan(2)
                .width(270f)
                .height(52f)
                .center()
                .row();

        return panel;
    }

    private void addEditField(
            Table panel,
            String labelText,
            TextField field,
            ChangeListener listener,
            Color color
    ) {

        Label label = new Label(
                labelText,
                skin
        );

        label.setFontScale(1.15f);
        label.setColor(color);

        panel.add(label)
                .colspan(2)
                .left()
                .padBottom(4f)
                .row();

        Table row = new Table();

        row.add(field)
                .width(300f)
                .height(48f)
                .padRight(9f);

        TextButton button = new TextButton(
                "CHANGE",
                skin
        );

        button.getLabel()
                .setFontScale(0.95f);

        button.getLabel()
                .setColor(color);

        button.addListener(listener);

        row.add(button)
                .width(118f)
                .height(48f);

        panel.add(row)
                .colspan(2)
                .left()
                .padBottom(12f)
                .row();
    }

    // =========================================================
    // PLAYER STATS
    // =========================================================

    private Table buildStatsPanel() {

        Table panel = new Table();

        panel.pad(10f);

        // -----------------------------------------------------
        // Heading
        // -----------------------------------------------------

        Label heading = new Label(
                "PLAYER STATS",
                skin
        );

        heading.setFontScale(1.5f);
        heading.setColor(CYAN);

        panel.add(heading)
                .colspan(2)
                .center()
                .padBottom(24f)
                .row();

        // -----------------------------------------------------
        // Identity
        // -----------------------------------------------------

        usernameValue =
                new Label("", skin);

        nicknameValue =
                new Label("", skin);

        Table identity = new Table();

        addStatLine(
                identity,
                "USERNAME",
                usernameValue,
                GOLD
        );

        addStatLine(
                identity,
                "NICKNAME",
                nicknameValue,
                LIME
        );

        panel.add(identity)
                .colspan(2)
                .width(440f)
                .padBottom(25f)
                .row();

        // -----------------------------------------------------
        // Values
        // -----------------------------------------------------

        gamesValue =
                new Label("0", skin);

        coinsValue =
                new Label("0", skin);

        gemsValue =
                new Label("0", skin);

        levelsValue =
                new Label("0", skin);

        mewPointValue =
                new Label("0", skin);

        Table statsGrid = new Table();

        statsGrid.defaults()
                .pad(8f);

        statsGrid.add(
                statCard(
                        "GAMES PLAYED",
                        gamesValue,
                        ORANGE
                )
        ).width(205f).height(90f);

        statsGrid.add(
                        statCard(
                                "COINS",
                                coinsValue,
                                GOLD
                        )
                )
                .width(205f)
                .height(90f)
                .row();

        statsGrid.add(
                statCard(
                        "GEMS",
                        gemsValue,
                        CYAN
                )
        ).width(205f).height(90f);

        statsGrid.add(
                        statCard(
                                "LEVELS",
                                levelsValue,
                                LIME
                        )
                )
                .width(205f)
                .height(90f)
                .row();

        statsGrid.add(
                        statCard(
                                "MEW POINT",
                                mewPointValue,
                                PINK
                        )
                )
                .colspan(2)
                .width(416f)
                .height(90f);

        panel.add(statsGrid)
                .colspan(2)
                .center()
                .row();

        return panel;
    }

    private Table statCard(
            String titleText,
            Label value,
            Color color
    ) {

        Table card = new Table();

        Label title = new Label(
                titleText,
                skin
        );

        title.setFontScale(1f);
        title.setAlignment(Align.center);
        title.setColor(color);

        value.setFontScale(1.6f);
        value.setAlignment(Align.center);

        /*
         * Use the same dominant accent color
         * for the value.
         */

        value.setColor(color);

        card.add(title)
                .center()
                .row();

        card.add(value)
                .center()
                .padTop(7f)
                .row();

        return card;
    }

    private void addStatLine(
            Table table,
            String name,
            Label value,
            Color color
    ) {

        Label label = new Label(
                name,
                skin
        );

        label.setFontScale(1.08f);
        label.setColor(color);

        value.setFontScale(1.2f);
        value.setColor(color);

        table.add(label)
                .left()
                .width(180f)
                .padBottom(10f);

        table.add(value)
                .right()
                .width(210f)
                .padBottom(10f)
                .row();
    }

    // =========================================================
    // MORNING / NIGHT MODE
    // =========================================================

    private ChangeListener modeListener() {

        return new ChangeListener() {

            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {

                nightMode = !nightMode;

                if (nightMode) {

                    modeButton.setText(
                            "MORNING MODE"
                    );

                    modeButton.getLabel()
                            .setColor(GOLD);

                    statusLabel.setText(
                            "morning mode enabled."
                    );

                    statusLabel.setColor(CYAN);

                } else {

                    modeButton.setText(
                            "NIGHT MODE"
                    );

                    modeButton.getLabel()
                            .setColor(CYAN);

                    statusLabel.setText(
                            "night mode enabled."
                    );

                    statusLabel.setColor(GOLD);
                }
            }
        };
    }

    // =========================================================
    // USERNAME
    // =========================================================

    private ChangeListener usernameListener() {

        return new ChangeListener() {

            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {

                String username =
                        usernameField
                                .getText()
                                .trim();

                Result<String> result =
                        profileController.changeUsername(
                                username
                        );

                showResult(result);

                if (result.getStatus()) {

                    usernameField.setText("");

                    refreshProfile();
                }
            }
        };
    }

    // =========================================================
    // NICKNAME
    // =========================================================

    private ChangeListener nicknameListener() {

        return new ChangeListener() {

            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {

                String nickname =
                        nicknameField
                                .getText()
                                .trim();

                Result<String> result =
                        profileController.changeNickname(
                                nickname
                        );

                showResult(result);

                if (result.getStatus()) {

                    nicknameField.setText("");

                    refreshProfile();
                }
            }
        };
    }

    // =========================================================
    // EMAIL
    // =========================================================

    private ChangeListener emailListener() {

        return new ChangeListener() {

            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {

                String email =
                        emailField
                                .getText()
                                .trim();

                Result<String> result =
                        profileController.changeEmail(
                                email
                        );

                showResult(result);

                if (result.getStatus()) {

                    emailField.setText("");
                }
            }
        };
    }

    // =========================================================
    // PASSWORD
    // =========================================================

    private ChangeListener passwordListener() {

        return new ChangeListener() {

            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {

                String oldPassword =
                        oldPasswordField.getText();

                String newPassword =
                        newPasswordField.getText();

                Result<String> result =
                        profileController.changePassword(
                                newPassword,
                                oldPassword
                        );

                showResult(result);

                if (result.getStatus()) {

                    oldPasswordField.setText("");

                    newPasswordField.setText("");
                }
            }
        };
    }

    // =========================================================
    // PROFILE DATA
    // =========================================================

    private void refreshProfile() {

        User user =
                Store.getLoggedInUser();

        if (user == null) {

            usernameValue.setText("-");
            nicknameValue.setText("-");

            gamesValue.setText("0");
            coinsValue.setText("0");
            gemsValue.setText("0");
            levelsValue.setText("0");
            mewPointValue.setText("0");

            return;
        }

        usernameValue.setText(
                safe(
                        user.getUsername()
                )
        );

        nicknameValue.setText(
                safe(
                        user.getNickname()
                )
        );

        gamesValue.setText(
                String.valueOf(
                        user.getGamesPlayed()
                )
        );

        coinsValue.setText(
                String.valueOf(
                        user.getCoins()
                )
        );

        gemsValue.setText(
                String.valueOf(
                        user.getGems()
                )
        );

        levelsValue.setText(
                String.valueOf(
                        user.getCompletedLevelCount()
                )
        );

        mewPointValue.setText(
                String.valueOf(
                        user.getHighestMewPoint()
                )
        );
    }

    private String safe(String value) {

        if (value == null || value.isBlank()) {
            return "-";
        }

        return value;
    }

    // =========================================================
    // RESULT
    // =========================================================

    private void showResult(
            Result<String> result
    ) {

        statusLabel.setText(
                result.getMessage()
        );

        if (result.getStatus()) {

            statusLabel.setColor(LIME);

        } else {

            statusLabel.setColor(RED);
        }
    }

    // =========================================================
    // BACK
    // =========================================================

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

                    showResult(result);
                }
            }
        };
    }

    // =========================================================
    // RENDER
    // =========================================================

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

        // -----------------------------------------------------
        // Choose background
        // -----------------------------------------------------

        Texture currentBackground =
                nightMode
                        ? nightBackground
                        : morningBackground;

        batch.begin();

        batch.draw(
                currentBackground,
                0,
                0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );

        batch.end();

        stage.act(delta);
        stage.draw();
    }

    // =========================================================
    // RESIZE
    // =========================================================

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

    // =========================================================
    // DISPOSE
    // =========================================================

    @Override
    public void dispose() {

        if (stage != null) {
            stage.dispose();
        }

        if (morningBackground != null) {
            morningBackground.dispose();
        }

        if (nightBackground != null) {
            nightBackground.dispose();
        }

        if (batch != null) {
            batch.dispose();
        }
    }
}