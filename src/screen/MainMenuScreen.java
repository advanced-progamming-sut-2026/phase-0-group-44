package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import controller.App;
import controller.MainMenuController;
import controller.MenuController;
import controller.NewsMenuController;
import model.Result;
import model.Store;
import model.user.User;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;

public final class MainMenuScreen implements Screen {

    private static final String ASSET_ROOT =
            "ui/mainmenu/";

    private final PvzGame game;

    private final MainMenuController mainController;
    private final MenuController menuController;
    private final NewsMenuController newsController;

    private final List<Texture> textures =
            new ArrayList<>();

    private Stage stage;
    private Skin skin;

    private ToastManager toast;

    private Label unreadBadge;

    private NewsDialog newsDialog;

    public MainMenuScreen(
            PvzGame game,
            App app
    ) {

        this.game = game;

        this.mainController =
                app.getMainController();

        this.menuController =
                app.getMenuController();

        this.newsController =
                app.getNewsController();
    }

    // =========================================================
    // SHOW
    // =========================================================

    @Override
    public void show() {

        stage =
                new Stage(
                        new ScreenViewport()
                );

        skin =
                PvzSkin.get();

        PvzSkinExtras.ensureDialogStyle(
                skin
        );

        toast =
                new ToastManager(
                        stage,
                        skin
                );

        Gdx.input.setInputProcessor(
                stage
        );

        buildScreen();

        refreshUnreadBadge();
    }

    // =========================================================
    // BUILD SCREEN
    // =========================================================

    private void buildScreen() {

        Stack root =
                new Stack();

        root.setFillParent(true);

        stage.addActor(root);

        /*
         * Background
         */

        root.add(
                buildBackground()
        );

        /*
         * Center content
         */

        root.add(
                buildCenterContent()
        );

        /*
         * Top bar
         */

        root.add(
                buildTopBar()
        );

        /*
         * Bottom navigation
         */

        root.add(
                buildBottomBar()
        );
    }

    // =========================================================
    // BACKGROUND
    // =========================================================

    private Image buildBackground() {

        Image background =
                new Image(
                        loadTexture(
                                "background.png"
                        )
                );

        background.setScaling(
                Scaling.fill
        );

        return background;
    }

    // =========================================================
    // CENTER CONTENT
    // =========================================================

    private Table buildCenterContent() {

        Table content =
                new Table();

        content
                .center()
                .padTop(30f)
                .padBottom(105f);

        /*
         * Logo
         */

        Image logo =
                new Image(
                        loadTexture(
                                "logo.png"
                        )
                );

        logo.setScaling(
                Scaling.fit
        );

        content.add(logo)
                .width(520f)
                .height(88f)
                .padBottom(24f)
                .row();

        /*
         * Banner
         */

        Image banner =
                new Image(
                        loadTexture(
                                "banner_offline.png"
                        )
                );

        banner.setScaling(
                Scaling.fit
        );

        content.add(banner)
                .width(620f)
                .height(215f);

        return content;
    }

    // =========================================================
    // TOP BAR
    // =========================================================

    private Table buildTopBar() {

        Table topBar =
                new Table();

        topBar
                .top()
                .pad(
                        18f,
                        22f,
                        0f,
                        22f
                );

        /*
         * Profile / Logout
         */

        topBar.add(
                        buildProfileArea()
                )
                .expandX()
                .left();

        /*
         * Gems / Coins
         */

        topBar.add(
                        buildResourceArea()
                )
                .right();

        return topBar;
    }

    // =========================================================
    // PROFILE AREA
    // =========================================================

    private Table buildProfileArea() {

        Table profileArea =
                new Table();

        /*
         * PROFILE BUTTON
         */

        TextButton profileButton =
                new TextButton(
                        profileCaption(),
                        skin,
                        "brown"
                );

        profileButton.addListener(
                profileListener()
        );

        profileArea.add(
                        profileButton
                )
                .height(48f)
                .padRight(8f);

        /*
         * LOG OUT
         */

        TextButton logoutButton =
                new TextButton(
                        "LOG OUT",
                        skin,
                        "brown"
                );

        logoutButton.addListener(
                logoutListener()
        );

        profileArea.add(
                        logoutButton
                )
                .height(48f);

        return profileArea;
    }

    // =========================================================
    // RESOURCE AREA
    // =========================================================

    private Table buildResourceArea() {

        Table resources =
                new Table();

        User user =
                Store.getLoggedInUser();

        int gems =
                user == null
                        ? 0
                        : user.getGems();

        int coins =
                user == null
                        ? 0
                        : user.getCoins();

        /*
         * Gems
         */

        resources.add(
                resourceIcon(
                        "gem.png",
                        30f,
                        39f
                )
        ).padRight(5f);

        resources.add(
                new Label(
                        String.valueOf(gems),
                        skin,
                        "medium_outline"
                )
        ).padRight(18f);

        /*
         * Coins
         */

        resources.add(
                resourceIcon(
                        "coin.png",
                        30f,
                        30f
                )
        ).padRight(5f);

        resources.add(
                new Label(
                        String.valueOf(coins),
                        skin,
                        "medium_outline"
                )
        );

        return resources;
    }

    private Image resourceIcon(
            String fileName,
            float width,
            float height
    ) {

        Image image =
                new Image(
                        loadTexture(
                                fileName
                        )
                );

        image.setScaling(
                Scaling.fit
        );

        image.setSize(
                width,
                height
        );

        return image;
    }

    // =========================================================
    // BOTTOM BAR
    // =========================================================

    private Table buildBottomBar() {

        Table bottomBar =
                new Table();

        bottomBar
                .bottom()
                .pad(
                        0f,
                        28f,
                        22f,
                        28f
                );

        /*
         * Left side
         */

        bottomBar.add(
                        buildLeftNavigation()
                )
                .expandX()
                .left();

        /*
         * Play
         */

        bottomBar.add(
                        buildPlayButton()
                )
                .width(220f)
                .height(64f)
                .padBottom(2f);

        /*
         * Right side
         */

        bottomBar.add(
                        buildRightNavigation()
                )
                .expandX()
                .right();

        return bottomBar;
    }

    // =========================================================
    // LEFT NAVIGATION
    // =========================================================

    private Table buildLeftNavigation() {

        Table left =
                new Table();

        left.defaults()
                .padRight(12f);

        /*
         * NETWORK
         */

        ImageButton networkButton =
                singleStateButton(
                        "network_button.png"
                );

        addComingSoonListener(
                networkButton,
                "Network"
        );

        left.add(
                iconItem(
                        "NETWORK",
                        networkButton
                )
        );

        /*
         * NEWS
         */

        ImageButton newsButton =
                imageButton(
                        "news_normal.png",
                        "news_selected.png"
                );

        newsButton.addListener(
                newsListener()
        );

        left.add(
                iconItem(
                        "NEWS",
                        newsButton
                )
        );

        return left;
    }

    // =========================================================
    // RIGHT NAVIGATION
    // =========================================================

    private Table buildRightNavigation() {

        Table right =
                new Table();

        right.defaults()
                .padLeft(12f);

        /*
         * SETTINGS
         */

        ImageButton settingsButton =
                imageButton(
                        "settings_normal.png",
                        "settings_selected.png"
                );

        settingsButton.addListener(
                settingsListener()
        );

        right.add(
                iconItem(
                        "SETTINGS",
                        settingsButton
                )
        );

        /*
         * LEADERBOARD
         */

        ImageButton leaderboardButton =
                singleStateButton(
                        "iconLeaderboard.png"
                );

        leaderboardButton.addListener(
                leaderboardListener()
        );

        right.add(
                iconItem(
                        "SCORES",
                        leaderboardButton
                )
        );

        return right;
    }

    // =========================================================
    // PLAY
    // =========================================================

    private TextButton buildPlayButton() {

        TextButton playButton =
                new TextButton(
                        "PLAY",
                        skin,
                        "purple"
                );

        playButton.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        Result<String> result =
                                menuController.enterMenu(
                                        "game"
                                );

                        if (result.getStatus()) {

                            game.goToScreenForCurrentMenu();

                        } else {

                            toast.showError(
                                    result.getMessage()
                            );
                        }
                    }
                }
        );

        return playButton;
    }

    // =========================================================
    // ICON ITEM
    // =========================================================

    private Table iconItem(
            String caption,
            ImageButton button
    ) {

        Stack buttonStack =
                new Stack();

        buttonStack.add(
                button
        );

        /*
         * Add unread badge on NEWS only
         */

        if (
                "NEWS".equals(
                        caption
                )
        ) {

            buttonStack.add(
                    buildUnreadBadgeOverlay()
            );
        }

        Table item =
                new Table();

        item.add(
                        buttonStack
                )
                .width(82f)
                .height(78f)
                .row();

        Label captionLabel =
                new Label(
                        caption,
                        skin,
                        "medium_outline"
                );

        item.add(
                        captionLabel
                )
                .padTop(2f);

        return item;
    }

    // =========================================================
    // NEWS BADGE
    // =========================================================

    private Table buildUnreadBadgeOverlay() {

        unreadBadge =
                new Label(
                        "",
                        skin,
                        "medium_outline"
                );

        unreadBadge.setColor(
                com.badlogic.gdx.graphics.Color.RED
        );

        Table overlay =
                new Table();

        overlay
                .top()
                .right();

        overlay.add(
                        unreadBadge
                )
                .padTop(-2f)
                .padRight(-3f);

        return overlay;
    }

    // =========================================================
    // IMAGE BUTTON HELPERS
    // =========================================================

    private ImageButton singleStateButton(
            String fileName
    ) {

        Texture texture =
                loadTexture(
                        fileName
                );

        return makeImageButton(
                texture,
                texture
        );
    }

    private ImageButton imageButton(
            String normalFile,
            String selectedFile
    ) {

        return makeImageButton(
                loadTexture(
                        normalFile
                ),
                loadTexture(
                        selectedFile
                )
        );
    }

    private ImageButton makeImageButton(
            Texture normal,
            Texture selected
    ) {

        ImageButton.ImageButtonStyle style =
                new ImageButton.ImageButtonStyle();

        style.imageUp =
                new TextureRegionDrawable(
                        normal
                );

        style.imageOver =
                new TextureRegionDrawable(
                        selected
                );

        style.imageDown =
                new TextureRegionDrawable(
                        selected
                );

        return new ImageButton(
                style
        );
    }

    // =========================================================
    // TEXTURE
    // =========================================================

    private Texture loadTexture(
            String fileName
    ) {

        Texture texture =
                TextureQuality.load(ASSET_ROOT + fileName);

        textures.add(
                texture
        );

        return texture;
    }

    // =========================================================
    // PROFILE
    // =========================================================

    private ChangeListener profileListener() {

        return new ChangeListener() {

            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {

                Result<String> result =
                        menuController.enterMenu(
                                "profile"
                        );

                if (result.getStatus()) {

                    game.goToScreenForCurrentMenu();

                } else {

                    toast.showError(
                            result.getMessage()
                    );
                }
            }
        };
    }

    // =========================================================
    // SETTINGS
    // =========================================================

    private ChangeListener settingsListener() {

        return new ChangeListener() {

            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {

                Result<String> result =
                        menuController.enterMenu(
                                "settings"
                        );

                if (result.getStatus()) {

                    game.goToScreenForCurrentMenu();

                } else {

                    toast.showError(
                            result.getMessage()
                    );
                }
            }
        };
    }

    // =========================================================
    // LEADERBOARD
    // =========================================================

    private ChangeListener leaderboardListener() {

        return new ChangeListener() {

            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {

                Result<String> result =
                        menuController.enterMenu(
                                "leaderboard"
                        );

                if (result.getStatus()) {

                    game.goToScreenForCurrentMenu();

                } else {

                    toast.showError(
                            result.getMessage()
                    );
                }
            }
        };
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private ChangeListener logoutListener() {

        return new ChangeListener() {

            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {

                Result<String> result =
                        mainController.logout();

                if (result.getStatus()) {

                    game.goToScreenForCurrentMenu();

                } else {

                    toast.showError(
                            result.getMessage()
                    );
                }
            }
        };
    }

    // =========================================================
    // NEWS
    // =========================================================

    private ChangeListener newsListener() {

        return new ChangeListener() {

            @Override
            public void changed(
                    ChangeEvent event,
                    Actor actor
            ) {

                openNewsDialog();
            }
        };
    }

    private void openNewsDialog() {

        if (
                newsDialog != null
                        && newsDialog.hasParent()
        ) {

            return;
        }

        newsDialog =
                new NewsDialog(
                        skin,
                        newsController,
                        this::refreshUnreadBadge
                );

        newsDialog.showCentered(
                stage
        );
    }

    private void refreshUnreadBadge() {

        if (unreadBadge == null) {
            return;
        }

        Result<Integer> result =
                newsController.getUnreadNewsCount();

        int count =
                result.getStatus()
                        && result.getData() != null
                        ? result.getData()
                        : 0;

        unreadBadge.setText(
                count > 0
                        ? String.valueOf(count)
                        : ""
        );

        unreadBadge.setVisible(
                count > 0
        );
    }

    // =========================================================
    // COMING SOON
    // =========================================================

    private void addComingSoonListener(
            Actor actor,
            String screenName
    ) {

        actor.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor source
                    ) {

                        toast.showInfo(
                                screenName
                                        + " screen is not connected yet."
                        );
                    }
                }
        );
    }

    // =========================================================
    // PROFILE CAPTION
    // =========================================================

    private String profileCaption() {

        User user =
                Store.getLoggedInUser();

        if (
                user == null
                        || user.getUsername() == null
                        || user.getUsername().isBlank()
        ) {

            return "PROFILE";
        }

        return user.getUsername();
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

        stage.act(
                delta
        );

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

        stage.getViewport()
                .update(
                        width,
                        height,
                        true
                );

        if (
                newsDialog != null
                        && newsDialog.hasParent()
        ) {

            newsDialog.centerOn(
                    stage
            );
        }
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

        for (
                Texture texture
                : textures
        ) {

            texture.dispose();
        }

        textures.clear();
    }
}