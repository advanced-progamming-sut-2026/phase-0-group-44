package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import controller.App;
import controller.MainMenuController;
import controller.MenuController;
import model.Result;
import model.Store;
import model.user.User;
import model.utility.Leaderboard;
import model.utility.LeaderboardColumn;
import model.utility.LeaderboardEntry;
import model.utility.SortDirection;
import pvz.skin.PvzSkin;

import java.util.List;

public final class LeaderboardScreen implements Screen {

    private static final Color GOLD =
            new Color(1f, 0.78f, 0.08f, 1f);

    private static final Color SILVER =
            new Color(0.85f, 0.90f, 1f, 1f);

    private static final Color BRONZE =
            new Color(0.92f, 0.48f, 0.15f, 1f);

    private static final Color GREEN =
            new Color(0.58f, 1f, 0.22f, 1f);

    private static final Color CYAN =
            new Color(0.18f, 0.92f, 1f, 1f);

    private static final Color CREAM =
            new Color(1f, 0.96f, 0.72f, 1f);

    private final PvzGame game;
    private final MainMenuController mainController;
    private final MenuController menuController;

    private Stage stage;
    private Skin skin;

    private SpriteBatch batch;
    private Texture backgroundTexture;

    private Texture headerTexture;
    private Texture rowTexture;
    private Texture currentPlayerTexture;

    private SelectBox<String> sortBox;
    private SelectBox<String> directionBox;

    private Table leaderboardTable;

    private Label statusLabel;

    private boolean loading;

    public LeaderboardScreen(
            PvzGame game,
            App app
    ) {
        this.game = game;
        this.mainController = app.getMainController();
        this.menuController = app.getMenuController();
    }

    @Override
    public void show() {

        stage = new Stage(
                new ScreenViewport()
        );

        skin = PvzSkin.get();

        batch = new SpriteBatch();

        /*
         * Change this path if you make a
         * dedicated leaderboard background.
         */
        backgroundTexture = TextureQuality.load("ui/leaderboardBG.png");

        /*
         * Semi-transparent backgrounds.
         */

        headerTexture = createTexture(
                new Color(
                        0.10f,
                        0.20f,
                        0.07f,
                        0.88f
                )
        );

        rowTexture = createTexture(
                new Color(
                        0.04f,
                        0.08f,
                        0.03f,
                        0.72f
                )
        );

        currentPlayerTexture = createTexture(
                new Color(
                        0.28f,
                        0.38f,
                        0.08f,
                        0.88f
                )
        );

        Gdx.input.setInputProcessor(
                stage
        );

        buildScreen();
        refreshLeaderboard();
    }

    // =========================================================
    // SCREEN
    // =========================================================

    private void buildScreen() {

        Table root = new Table();

        root.setFillParent(true);
        root.pad(28f);

        stage.addActor(root);

        /*
         * Title
         */

        Label title = new Label(
                "LEADERBOARD",
                skin
        );

        title.setFontScale(1.9f);
        title.setColor(GOLD);

        root.add(title)
                .center()
                .padBottom(16f)
                .row();

        /*
         * Sort controls
         */

        root.add(
                buildSortControls()
        )
                .center()
                .padBottom(16f)
                .row();

        /*
         * Leaderboard
         */

        leaderboardTable =
                new Table();

        leaderboardTable.top();

        ScrollPane scrollPane =
                new ScrollPane(
                        leaderboardTable,
                        skin
                );

        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(
                true,
                false
        );

        root.add(scrollPane)
                .width(1000f)
                .height(530f)
                .padBottom(14f)
                .row();

        /*
         * Status
         */

        statusLabel = new Label(
                "",
                skin
        );

        statusLabel.setAlignment(
                Align.center
        );

        statusLabel.setFontScale(0.95f);

        root.add(statusLabel)
                .width(700f)
                .padBottom(10f)
                .row();

        /*
         * Back
         */

        TextButton backButton =
                new TextButton(
                        "BACK",
                        skin
                );

        backButton
                .getLabel()
                .setFontScale(1.25f);

        backButton
                .getLabel()
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
    // SORT CONTROLS
    // =========================================================

    private Table buildSortControls() {

        Table controls =
                new Table();

        Label sortLabel =
                new Label(
                        "SORT BY",
                        skin
                );

        sortLabel.setColor(CREAM);
        sortLabel.setFontScale(1.05f);

        controls.add(sortLabel)
                .padRight(10f);

        sortBox =
                new SelectBox<>(skin);

        sortBox.setItems(
                "Progress",
                "Username",
                "Minigames",
                "Daily Quests",
                "Non-Daily Quests",
                "Highest Score"
        );

        sortBox.addListener(
                sortListener()
        );

        controls.add(sortBox)
                .width(220f)
                .height(45f)
                .padRight(30f);

        Label directionLabel =
                new Label(
                        "ORDER",
                        skin
                );

        directionLabel.setColor(CREAM);
        directionLabel.setFontScale(1.05f);

        controls.add(directionLabel)
                .padRight(10f);

        directionBox =
                new SelectBox<>(skin);

        directionBox.setItems(
                "Descending",
                "Ascending"
        );

        directionBox.addListener(
                sortListener()
        );

        controls.add(directionBox)
                .width(190f)
                .height(45f);

        return controls;
    }

    // =========================================================
    // REFRESH
    // =========================================================

    private void refreshLeaderboard() {

        loading = true;

        LeaderboardColumn column =
                selectedColumn();

        SortDirection direction =
                selectedDirection();

        Result<Leaderboard> result =
                mainController.leaderboard(
                        column,
                        direction
                );

        leaderboardTable.clear();

        if (!result.getStatus()
                || result.getData() == null) {

            statusLabel.setText(
                    result.getMessage()
            );

            statusLabel.setColor(
                    Color.RED
            );

            loading = false;

            return;
        }

        Leaderboard leaderboard =
                result.getData();

        buildLeaderboardTable(
                leaderboard.getEntries()
        );

        statusLabel.setText(
                leaderboard.getEntries().size()
                        + " registered players"
        );

        statusLabel.setColor(GREEN);

        loading = false;
    }

    // =========================================================
    // TABLE
    // =========================================================

    private void buildLeaderboardTable(
            List<LeaderboardEntry> entries
    ) {

        leaderboardTable.clear();

        /*
         * Header
         */

        Table header = new Table();

        header.setBackground(
                drawable(headerTexture)
        );

        addHeaderCell(
                header,
                "#",
                70f
        );

        addHeaderCell(
                header,
                "PLAYER",
                230f
        );

        addHeaderCell(
                header,
                "PROGRESS",
                150f
        );

        addHeaderCell(
                header,
                "MINIGAMES",
                140f
        );

        addHeaderCell(
                header,
                "DAILY",
                120f
        );

        addHeaderCell(
                header,
                "QUESTS",
                120f
        );

        addHeaderCell(
                header,
                "SCORE",
                140f
        );

        leaderboardTable.add(header)
                .width(970f)
                .height(55f)
                .padBottom(5f)
                .row();

        /*
         * Rows
         */

        if (entries.isEmpty()) {

            Label empty =
                    new Label(
                            "No players found.",
                            skin
                    );

            empty.setFontScale(1.25f);
            empty.setColor(CREAM);

            leaderboardTable.add(empty)
                    .padTop(60f)
                    .row();

            return;
        }

        for (
                int i = 0;
                i < entries.size();
                i++
        ) {

            LeaderboardEntry entry =
                    entries.get(i);

            Table row =
                    buildRow(
                            i + 1,
                            entry
                    );

            leaderboardTable.add(row)
                    .width(970f)
                    .height(58f)
                    .padBottom(4f)
                    .row();
        }
    }

    private void addHeaderCell(
            Table table,
            String text,
            float width
    ) {

        Label label =
                new Label(
                        text,
                        skin
                );

        label.setFontScale(0.95f);
        label.setAlignment(Align.center);
        label.setColor(GOLD);

        table.add(label)
                .width(width)
                .center();
    }

    // =========================================================
    // ROW
    // =========================================================

    private Table buildRow(
            int rank,
            LeaderboardEntry entry
    ) {

        Table row =
                new Table();

        User loggedIn =
                Store.getLoggedInUser();

        boolean currentUser =
                loggedIn != null
                        && loggedIn.getUsername() != null
                        && loggedIn
                        .getUsername()
                        .equals(
                                entry.getUsername()
                        );

        row.setBackground(
                drawable(
                        currentUser
                                ? currentPlayerTexture
                                : rowTexture
                )
        );

        /*
         * Rank
         */

        Label rankLabel =
                new Label(
                        rankText(rank),
                        skin
                );

        rankLabel.setFontScale(
                rank <= 3
                        ? 1.2f
                        : 1f
        );

        rankLabel.setColor(
                rankColor(rank)
        );

        rankLabel.setAlignment(
                Align.center
        );

        row.add(rankLabel)
                .width(70f);

        /*
         * Username
         */

        Label username =
                new Label(
                        entry.getUsername(),
                        skin
                );

        username.setFontScale(1.05f);

        username.setColor(
                currentUser
                        ? GREEN
                        : CREAM
        );

        username.setAlignment(
                Align.left
        );

        row.add(username)
                .width(230f)
                .left()
                .padLeft(12f);

        /*
         * Progress
         */

        Label progress =
                valueLabel(
                        entry.getProgressLabel(),
                        CYAN
                );

        row.add(progress)
                .width(150f);

        /*
         * Minigames
         */

        Label minigames =
                valueLabel(
                        String.valueOf(
                                entry.getCompletedMinigames()
                        ),
                        GREEN
                );

        row.add(minigames)
                .width(140f);

        /*
         * Daily quests
         */

        Label daily =
                valueLabel(
                        String.valueOf(
                                entry.getCompletedDailyQuests()
                        ),
                        GOLD
                );

        row.add(daily)
                .width(120f);

        /*
         * Non-daily quests
         */

        Label nonDaily =
                valueLabel(
                        String.valueOf(
                                entry.getCompletedNonDailyQuests()
                        ),
                        BRONZE
                );

        row.add(nonDaily)
                .width(120f);

        /*
         * Highest score
         */

        Label score =
                valueLabel(
                        String.valueOf(
                                entry.getHighestScore()
                        ),
                        GOLD
                );

        score.setFontScale(1.1f);

        row.add(score)
                .width(140f);

        return row;
    }

    private Label valueLabel(
            String text,
            Color color
    ) {

        Label label =
                new Label(
                        text,
                        skin
                );

        label.setFontScale(1f);
        label.setColor(color);
        label.setAlignment(Align.center);

        return label;
    }

    // =========================================================
    // RANK
    // =========================================================

    private String rankText(
            int rank
    ) {

        return switch (rank) {

            case 1 -> "1";

            case 2 -> "2";

            case 3 -> "3";

            default ->
                    String.valueOf(rank);
        };
    }

    private Color rankColor(
            int rank
    ) {

        return switch (rank) {

            case 1 -> GOLD;

            case 2 -> SILVER;

            case 3 -> BRONZE;

            default -> CREAM;
        };
    }

    // =========================================================
    // SORT SELECTION
    // =========================================================

    private LeaderboardColumn selectedColumn() {

        String selected =
                sortBox.getSelected();

        return switch (selected) {

            case "Username" ->
                    LeaderboardColumn.USERNAME;

            case "Minigames" ->
                    LeaderboardColumn.MINIGAMES;

            case "Daily Quests" ->
                    LeaderboardColumn.DAILY_QUESTS;

            case "Non-Daily Quests" ->
                    LeaderboardColumn.NON_DAILY_QUESTS;

            case "Highest Score" ->
                    LeaderboardColumn.HIGHEST_SCORE;

            default ->
                    LeaderboardColumn.PROGRESS;
        };
    }

    private SortDirection selectedDirection() {

        if (
                "Ascending".equals(
                        directionBox.getSelected()
                )
        ) {

            return SortDirection.ASCENDING;
        }

        return SortDirection.DESCENDING;
    }

    private ChangeListener sortListener() {

        return new ChangeListener() {

            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {

                if (loading) {
                    return;
                }

                refreshLeaderboard();
            }
        };
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

                    statusLabel.setText(
                            result.getMessage()
                    );

                    statusLabel.setColor(
                            Color.RED
                    );
                }
            }
        };
    }

    // =========================================================
    // TEXTURE HELPERS
    // =========================================================

    private Texture createTexture(
            Color color
    ) {

        Pixmap pixmap =
                new Pixmap(
                        1,
                        1,
                        Pixmap.Format.RGBA8888
                );

        pixmap.setColor(color);
        pixmap.fill();

        Texture texture =
                new Texture(pixmap);

        pixmap.dispose();

        return texture;
    }

    private TextureRegionDrawable drawable(
            Texture texture
    ) {

        return new TextureRegionDrawable(
                new TextureRegion(texture)
        );
    }

    // =========================================================
    // RENDER
    // =========================================================

    @Override
    public void render(
            float delta
    ) {

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
                0f,
                0f,
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

        stage
                .getViewport()
                .update(
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

        if (batch != null) {
            batch.dispose();
        }

        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }

        if (headerTexture != null) {
            headerTexture.dispose();
        }

        if (rowTexture != null) {
            rowTexture.dispose();
        }

        if (currentPlayerTexture != null) {
            currentPlayerTexture.dispose();
        }
    }
}