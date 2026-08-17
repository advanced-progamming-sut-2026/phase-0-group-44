package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import controller.App;
import controller.MenuController;
import controller.TravelMenuController;
import model.Result;
import model.Store;
import model.miniGame.MiniGameProgress;
import model.miniGame.MiniGameStatus;
import model.user.User;
import model.utility.QuestInstance;
import model.utility.QuestPriority;
import model.utility.TravelLogPage;
import pvz.skin.PvzSkin;

import java.util.List;

public final class TravelLogScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;

    private static final String MAIN_MENU_ASSET_ROOT = "ui/mainmenu/";
    private static final String PANEL_DRAWABLE =
            "image_ui_mainmenu_mm_settings_tab_10";
    private static final String INNER_DRAWABLE =
            "image_ui_dialog_asset_inner_bkgd_10";
    private static final String BORDER_DRAWABLE =
            "image_ui_dialog_asset_dialogborder_10";

    private static final Color CYAN =
            new Color(.16f, .90f, 1f, 1f);
    private static final Color LIGHT_CYAN =
            new Color(.80f, .96f, 1f, 1f);
    private static final Color GREEN =
            new Color(.18f, 1f, .43f, 1f);
    private static final Color GOLD =
            new Color(1f, .76f, .06f, 1f);
    private static final Color PURPLE =
            new Color(.78f, .58f, 1f, 1f);
    private static final Color SOFT_WHITE =
            new Color(.92f, .96f, 1f, 1f);

    private final PvzGame game;
    private final MenuController menuController;
    private final TravelMenuController travelController;

    private Stage stage;
    private Skin skin;
    private ToastManager toast;
    private Texture backgroundTexture;

    private TravelLogPage selectedPage = TravelLogPage.MAIN;

    public TravelLogScreen(PvzGame game, App app) {
        this.game = game;
        this.menuController = app.getMenuController();
        this.travelController = app.getTravelController();
    }

    @Override
    public void show() {
        stage = new Stage(
                new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT)
        );

        skin = PvzSkin.get();
        toast = new ToastManager(stage, skin);

        backgroundTexture = new Texture(
                Gdx.files.internal(
                        MAIN_MENU_ASSET_ROOT + "background.png"
                )
        );

        Gdx.input.setInputProcessor(stage);
        rebuild();
    }

    private void rebuild() {
        stage.clear();

        travelController.page(selectedPage.getToken());

        Stack root = new Stack();
        root.setFillParent(true);
        stage.addActor(root);

        Image background = new Image(backgroundTexture);
        background.setScaling(Scaling.fill);
        root.add(background);

        Table content = new Table();
        content.setFillParent(true);
        content.top();
        content.pad(18f, 42f, 25f, 42f);

        root.add(content);

        content.add(buildHeader())
                .growX()
                .height(76f)
                .row();

        content.add(buildTabs())
                .growX()
                .height(54f)
                .padTop(7f)
                .row();

        content.add(buildContentBoard())
                .grow()
                .padTop(13f);
    }

    private Table buildHeader() {
        Table header = new Table();

        header.setBackground(
                skin.newDrawable(
                        INNER_DRAWABLE,
                        new Color(.91f, .86f, .68f, 1f)
                )
        );

        header.pad(9f, 14f, 9f, 14f);

        TextButton back = new TextButton(
                "BACK",
                skin,
                "brown"
        );

        back.addListener(new ChangeListener() {
            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {
                Result<String> result =
                        menuController.exitMenu();

                if (result.getStatus()) {
                    game.goToScreenForCurrentMenu();
                }
            }
        });

        header.add(back)
                .width(115f)
                .height(43f)
                .left();

        Table heading = new Table();

        Label title = new Label(
                "TRAVEL LOG",
                skin,
                "big_outline"
        );

        title.setAlignment(Align.center);
        heading.add(title).row();

        Label subtitle = new Label(
                "MISSIONS • REWARDS • MINI-GAMES",
                skin,
                "medium_outline"
        );

        subtitle.setColor(GOLD);
        subtitle.setAlignment(Align.center);

        heading.add(subtitle)
                .padTop(-5f);

        header.add(heading)
                .expandX();

        User user = Store.getLoggedInUser();

        Table totals = new Table();

        Label questText = new Label(
                "QUESTS",
                skin
        );
        questText.setColor(SOFT_WHITE);

        totals.add(questText).row();

        Label questCount = new Label(
                String.valueOf(totalCompleted(user)),
                skin,
                "medium_outline"
        );
        questCount.setColor(CYAN);

        totals.add(questCount);

        header.add(totals)
                .width(115f)
                .right();

        return header;
    }

    private int totalCompleted(User user) {
        if (user == null) {
            return 0;
        }

        return user.getCompletedDailyQuests()
                + user.getCompletedNonDailyQuests();
    }

    private Table buildTabs() {
        Table tabs = new Table();
        tabs.defaults().padRight(9f);

        addTab(
                tabs,
                TravelLogPage.MAIN,
                "MAIN QUESTS"
        );

        addTab(
                tabs,
                TravelLogPage.EPIC,
                "EPIC QUESTS"
        );

        addTab(
                tabs,
                TravelLogPage.DAILY,
                "DAILY QUESTS"
        );

        addTab(
                tabs,
                TravelLogPage.MINIGAME,
                "MINI GAMES"
        );

        return tabs;
    }

    private void addTab(
            Table tabs,
            TravelLogPage page,
            String caption
    ) {
        TextButton tab = new TextButton(
                caption,
                skin,
                selectedPage == page
                        ? "purple"
                        : "brown"
        );

        tab.addListener(new ChangeListener() {
            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {
                if (selectedPage != page) {
                    selectedPage = page;
                    rebuild();
                }
            }
        });

        tabs.add(tab)
                .width(205f)
                .height(47f);
    }

    private Table buildContentBoard() {
        Table board = new Table();

        board.setBackground(
                skin.newDrawable(
                        BORDER_DRAWABLE,
                        new Color(.07f, .27f, .40f, .98f)
                )
        );

        board.pad(22f, 29f, 20f, 29f);

        if (selectedPage == TravelLogPage.MINIGAME) {
            board.add(buildMiniGameBoard()).grow();
        } else {
            board.add(buildQuestBoard()).grow();
        }

        return board;
    }

    // =========================================================
    // QUESTS
    // =========================================================

    private Table buildQuestBoard() {
        List<QuestInstance> quests =
                travelController.getActiveQuests(
                        selectedPage
                );

        Table list = new Table();
        list.top();
        list.defaults().growX();

        if (quests.isEmpty()) {
            list.add(buildEmptyQuestState())
                    .padTop(110f);
        } else {
            for (int index = 0;
                 index < quests.size();
                 index++) {

                list.add(
                                buildQuestCard(
                                        index + 1,
                                        quests.get(index)
                                )
                        )
                        .height(121f)
                        .padBottom(11f)
                        .row();
            }
        }

        ScrollPane scroll =
                new ScrollPane(list, skin);

        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        Table content = new Table();
        content.add(scroll).grow();

        return content;
    }

    private Table buildEmptyQuestState() {
        Table empty = new Table();

        Label title = new Label(
                "NO ACTIVE MISSIONS",
                skin,
                "medium_outline"
        );

        title.setColor(GOLD);
        empty.add(title).row();

        Label message = new Label(
                "New missions will appear here "
                        + "when they are activated.",
                skin
        );

        message.setColor(LIGHT_CYAN);
        message.setAlignment(Align.center);

        empty.add(message)
                .padTop(8f);

        return empty;
    }

    private Table buildQuestCard(
            int position,
            QuestInstance instance
    ) {
        Table card = new Table();

        QuestPriority priority =
                instance.getQuest().getPriority();

        card.setBackground(
                skin.newDrawable(
                        PANEL_DRAWABLE,
                        questCardColor(priority)
                )
        );

        card.pad(10f, 15f, 10f, 15f);

        Label number = new Label(
                String.format("%02d", position),
                skin,
                "big_outline"
        );

        number.setColor(priorityColor(priority));

        card.add(number)
                .width(58f)
                .center();

        Table description = new Table();

        Label name = new Label(
                instance.getDisplayName().toUpperCase(),
                skin,
                "medium_outline"
        );

        name.setColor(priorityColor(priority));

        description.add(name)
                .left()
                .row();

        Label objective = new Label(
                instance.getQuest().getConditionText(),
                skin
        );

        objective.setColor(SOFT_WHITE);
        objective.setWrap(true);
        objective.setAlignment(Align.left);

        description.add(objective)
                .width(505f)
                .left()
                .padTop(3f)
                .row();

        Label reward = new Label(
                "REWARD: "
                        + instance.getQuest()
                        .getReward()
                        .getCanonicalText(),
                skin
        );

        reward.setColor(GOLD);

        description.add(reward)
                .left()
                .padTop(3f);

        card.add(description)
                .expandX()
                .fillX()
                .padLeft(9f);

        Table status = new Table();

        boolean complete =
                instance.getProgress().isCompleted();

        Label state = new Label(
                complete
                        ? "COMPLETE!"
                        : "IN PROGRESS",
                skin,
                "medium_outline"
        );

        state.setColor(
                complete
                        ? GREEN
                        : CYAN
        );

        status.add(state)
                .center()
                .row();

        if (complete
                && !instance.getProgress().isClaimed()) {

            TextButton claim = new TextButton(
                    "CLAIM",
                    skin,
                    "green"
            );

            claim.addListener(new ChangeListener() {
                @Override
                public void changed(
                        ChangeEvent event,
                        Actor actor
                ) {
                    Result<String> result =
                            travelController.claim(position);

                    if (!result.getStatus()) {
                        toast.showError(
                                result.getMessage()
                        );
                        return;
                    }

                    toast.showInfo(
                            result.getMessage()
                    );

                    rebuild();
                }
            });

            status.add(claim)
                    .width(120f)
                    .height(37f)
                    .padTop(5f);
        } else {
            Label progress = new Label(
                    "Progress: "
                            + instance.getProgress()
                            .getProgress(),
                    skin
            );

            progress.setColor(LIGHT_CYAN);
            progress.setAlignment(Align.center);

            status.add(progress)
                    .padTop(5f);
        }

        card.add(status)
                .width(150f)
                .right();

        return card;
    }

    // =========================================================
    // MINI GAMES
    // =========================================================

    private Table buildMiniGameBoard() {
        Table content = new Table();
        content.top();

        Table heading = new Table();

        Label title = new Label(
                "MINI-GAME VAULT",
                skin,
                "big_outline"
        );

        title.setColor(CYAN);

        heading.add(title)
                .left()
                .row();

        Label subtitle = new Label(
                "Choose an unlocked level "
                        + "to preview its game mode.",
                skin
        );

        subtitle.setColor(LIGHT_CYAN);

        heading.add(subtitle)
                .left()
                .padTop(-4f);

        content.add(heading)
                .growX()
                .left()
                .padBottom(9f)
                .row();

        Table legend = buildMiniGameLegend();

        content.add(legend)
                .growX()
                .left()
                .padBottom(13f)
                .row();

        Table list = new Table();
        list.top();
        list.defaults().growX();

        List<MiniGameStatus> miniGames =
                travelController.getMiniGameStatuses();

        if (miniGames.isEmpty()) {
            list.add(buildEmptyMiniGameState())
                    .padTop(110f);
        } else {
            for (MiniGameStatus miniGame : miniGames) {
                list.add(buildMiniGameCard(miniGame))
                        .height(136f)
                        .padBottom(11f)
                        .row();
            }
        }

        ScrollPane scroll =
                new ScrollPane(list, skin);

        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        content.add(scroll).grow();

        return content;
    }

    private Table buildMiniGameLegend() {
        Table legend = new Table();

        legend.setBackground(
                skin.newDrawable(
                        INNER_DRAWABLE,
                        new Color(
                                .04f,
                                .18f,
                                .31f,
                                .98f
                        )
                )
        );

        legend.pad(6f, 12f, 6f, 12f);

        addLegendItem(
                legend,
                "CLEARED",
                GREEN
        );

        addLegendItem(
                legend,
                "AVAILABLE",
                GOLD
        );

        addLegendItem(
                legend,
                "LOCKED",
                PURPLE
        );

        return legend;
    }

    private void addLegendItem(
            Table legend,
            String text,
            Color color
    ) {
        Label item = new Label(
                "●  " + text,
                skin
        );

        item.setColor(color);

        legend.add(item)
                .padRight(22f);
    }

    private Table buildEmptyMiniGameState() {
        Table empty = new Table();

        Label title = new Label(
                "NO MINI-GAMES AVAILABLE",
                skin,
                "medium_outline"
        );

        title.setColor(GOLD);
        empty.add(title).row();

        Label message = new Label(
                "Complete Adventure progress "
                        + "to unlock mini-games.",
                skin
        );

        message.setColor(LIGHT_CYAN);

        empty.add(message)
                .padTop(8f);

        return empty;
    }

    private Table buildMiniGameCard(
            MiniGameStatus miniGame
    ) {
        Table card = new Table();

        MiniGameProgress progress =
                miniGame.progress();

        card.setBackground(
                skin.newDrawable(
                        PANEL_DRAWABLE,
                        miniGameCardColor(
                                progress.isUnlocked()
                        )
                )
        );

        card.pad(13f, 18f, 13f, 18f);

        Table details = new Table();

        Label title = new Label(
                miniGame.definition()
                        .getDisplayName()
                        .toUpperCase(),
                skin,
                "medium_outline"
        );

        title.setColor(
                progress.isUnlocked()
                        ? CYAN
                        : PURPLE
        );

        details.add(title)
                .left()
                .row();

        Label availability = new Label(
                progress.isUnlocked()
                        ? "ARCADE READY"
                        : "SEALED",
                skin
        );

        availability.setColor(
                progress.isUnlocked()
                        ? GREEN
                        : PURPLE
        );

        details.add(availability)
                .left()
                .padTop(1f)
                .row();

        Label description = new Label(
                progress.isUnlocked()
                        ? "Three levels • choose PLAY "
                        + "to preview an available level"
                        : "Clear earlier mini-games "
                        + "to unlock this challenge",
                skin
        );

        description.setColor(LIGHT_CYAN);

        details.add(description)
                .left()
                .padTop(4f);

        card.add(details)
                .expandX()
                .left();

        Table levels = new Table();

        for (int level = 1;
             level <= 3;
             level++) {

            levels.add(
                            buildLevelBadge(
                                    miniGame,
                                    level
                            )
                    )
                    .width(106f)
                    .padLeft(8f);
        }

        card.add(levels)
                .right();

        return card;
    }

    private Table buildLevelBadge(
            MiniGameStatus miniGame,
            int level
    ) {
        Table badge = new Table();

        MiniGameProgress progress =
                miniGame.progress();

        boolean completed =
                progress.isLevelCompleted(level);

        boolean available =
                progress.isLevelUnlocked(level);

        badge.setBackground(
                skin.newDrawable(
                        INNER_DRAWABLE,
                        levelBackgroundColor(
                                completed,
                                available
                        )
                )
        );

        badge.pad(6f, 5f, 6f, 5f);

        Label levelName = new Label(
                "LEVEL " + level,
                skin,
                "medium_outline"
        );

        levelName.setColor(SOFT_WHITE);
        levelName.setAlignment(Align.center);

        badge.add(levelName)
                .center()
                .row();

        String stateText;

        if (completed) {
            stateText = "CLEARED";
        } else if (available) {
            stateText = "AVAILABLE";
        } else {
            stateText = "LOCKED";
        }

        Label state = new Label(
                stateText,
                skin
        );

        state.setAlignment(Align.center);

        if (completed) {
            state.setColor(GREEN);
        } else if (available) {
            state.setColor(GOLD);
        } else {
            state.setColor(PURPLE);
        }

        badge.add(state)
                .center()
                .padTop(2f)
                .row();

        if (available) {
            TextButton play = new TextButton(
                    "PLAY",
                    skin,
                    completed
                            ? "purple"
                            : "green"
            );

            play.addListener(new ChangeListener() {
                @Override
                public void changed(
                        ChangeEvent event,
                        Actor actor
                ) {
                    toast.showInfo(
                            "The screen for "
                                    + miniGame.definition()
                                    .getDisplayName()
                                    + " level "
                                    + level
                                    + " has not been "
                                    + "implemented yet."
                    );
                }
            });

            badge.add(play)
                    .width(84f)
                    .height(28f)
                    .padTop(5f);
        }

        return badge;
    }

    // =========================================================
    // COLORS
    // =========================================================

    private Color miniGameCardColor(
            boolean unlocked
    ) {
        if (unlocked) {
            return new Color(
                    .06f,
                    .32f,
                    .52f,
                    .98f
            );
        }

        return new Color(
                .20f,
                .10f,
                .33f,
                .95f
        );
    }

    private Color levelBackgroundColor(
            boolean completed,
            boolean available
    ) {
        if (completed) {
            return new Color(
                    .04f,
                    .38f,
                    .24f,
                    .98f
            );
        }

        if (available) {
            return new Color(
                    .43f,
                    .27f,
                    .03f,
                    .98f
            );
        }

        return new Color(
                .20f,
                .10f,
                .32f,
                .96f
        );
    }

    private Color priorityColor(
            QuestPriority priority
    ) {
        if (priority == QuestPriority.CRITICAL) {
            return new Color(
                    1f,
                    .25f,
                    .30f,
                    1f
            );
        }

        if (priority == QuestPriority.HIGH) {
            return new Color(
                    1f,
                    .65f,
                    .08f,
                    1f
            );
        }

        if (priority == QuestPriority.MEDIUM) {
            return CYAN;
        }

        return new Color(
                .35f,
                1f,
                .55f,
                1f
        );
    }

    private Color questCardColor(
            QuestPriority priority
    ) {
        if (priority == QuestPriority.CRITICAL) {
            return new Color(
                    .40f,
                    .07f,
                    .18f,
                    .97f
            );
        }

        if (priority == QuestPriority.HIGH) {
            return new Color(
                    .38f,
                    .20f,
                    .03f,
                    .97f
            );
        }

        if (priority == QuestPriority.MEDIUM) {
            return new Color(
                    .05f,
                    .29f,
                    .49f,
                    .97f
            );
        }

        return new Color(
                .08f,
                .32f,
                .19f,
                .97f
        );
    }

    // =========================================================
    // SCREEN
    // =========================================================

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(
                .03f,
                .06f,
                .10f,
                1f
        );

        Gdx.gl.glClear(
                GL20.GL_COLOR_BUFFER_BIT
        );

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
    }
}