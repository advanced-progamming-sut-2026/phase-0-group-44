package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import controller.App;
import controller.GameMenuController;
import controller.MenuController;
import model.Result;
import model.Store;
import model.config.AdventureCatalog;
import model.config.ChapterCatalog;
import model.config.GameWorld;
import model.level.Chapter;
import model.level.Level;
import model.user.User;
import pvz.skin.PvzSkin;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Graphical Adventure menu for Phase 2.
 *
 * <p>The chapter view follows the intent of figure 2: chapter artwork, title,
 * progress and lock state are visible together. Selecting an unlocked chapter
 * switches to the level view, which follows figure 3 with clearly selectable
 * level nodes and locked-state feedback.</p>
 */
public final class AdventureMenuScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;
    private static final String ADVENTURE_ASSET_ROOT = "ui/adventure/";
    private static final String MAIN_MENU_ASSET_ROOT = "ui/mainmenu/";
    private static final int CORE_LEVEL_COUNT = 3;

    private final PvzGame game;
    private final GameMenuController gameController;
    private final MenuController menuController;
    private final Map<String, Texture> textures = new LinkedHashMap<>();

    private Stage stage;
    private Skin skin;
    private ToastManager toast;
    private GameWorld selectedWorld;

    public AdventureMenuScreen(PvzGame game, App app) {
        this.game = game;
        this.gameController = app.getGameController();
        this.menuController = app.getMenuController();
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        skin = PvzSkin.get();
        toast = new ToastManager(stage, skin);
        Gdx.input.setInputProcessor(stage);
        rebuild();
    }

    private void rebuild() {
        stage.clear();

        Stack root = new Stack();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(buildBackground());
        root.add(selectedWorld == null ? buildChapterView() : buildLevelView());
        root.add(buildTopBar());
        root.add(buildShortcutBar());
    }

    private Image buildBackground() {
        Image background = new Image(loadTexture(MAIN_MENU_ASSET_ROOT + "background.png"));
        background.setScaling(Scaling.fill);
        return background;
    }

    private Table buildTopBar() {
        Table top = new Table();
        top.top().pad(14f, 20f, 0f, 20f);

        top.add(buildBackButton()).size(68f, 64f).left();

        String title = selectedWorld == null
                ? "ADVENTURE"
                : selectedWorld.getDisplayName().toUpperCase(Locale.ROOT);
        Label titleLabel = new Label(title, skin, "medium_outline");
        top.add(titleLabel).expandX().center();

        top.add(buildResourceArea()).right();
        return top;
    }

    private ImageButton buildBackButton() {
        ImageButton button = imageButton(
                ADVENTURE_ASSET_ROOT + "back_normal.png",
                ADVENTURE_ASSET_ROOT + "back_selected.png"
        );
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (selectedWorld != null) {
                    selectedWorld = null;
                    rebuild();
                    return;
                }

                Result<String> result = menuController.exitMenu();
                if (result.getStatus()) {
                    game.goToScreenForCurrentMenu();
                } else {
                    toast.showError(result.getMessage());
                }
            }
        });
        return button;
    }

    private Table buildResourceArea() {
        Table resources = new Table();
        User user = Store.getLoggedInUser();
        int gems = user == null ? 0 : user.getGems();
        int coins = user == null ? 0 : user.getCoins();

        resources.add(resourceIcon(MAIN_MENU_ASSET_ROOT + "gem.png", 29f, 38f)).padRight(5f);
        resources.add(new Label(String.valueOf(gems), skin, "medium_outline")).padRight(16f);
        resources.add(resourceIcon(MAIN_MENU_ASSET_ROOT + "coin.png", 29f, 29f)).padRight(5f);
        resources.add(new Label(String.valueOf(coins), skin, "medium_outline"));
        return resources;
    }

    private Image resourceIcon(String path, float width, float height) {
        Image image = new Image(loadTexture(path));
        image.setScaling(Scaling.fit);
        image.setSize(width, height);
        return image;
    }

    private Table buildChapterView() {
        Table content = new Table();
        content.top().padTop(92f).padBottom(82f);

        Label heading = new Label("CHOOSE A CHAPTER", skin, "medium_outline");
        content.add(heading).padBottom(9f).row();

        Label hint = new Label(
                "Choose an unlocked world. Progress shows completed core levels.",
                skin,
                "medium_outline"
        );
        content.add(hint).padBottom(16f).row();

        Table chapters = new Table();
        chapters.defaults().pad(0f, 8f, 0f, 8f);
        for (Chapter chapter : AdventureCatalog.chapters()) {
            chapters.add(buildChapterCard(chapter)).width(258f).height(474f);
        }

        content.add(chapters);
        return content;
    }

    private Table buildChapterCard(Chapter chapter) {
        GameWorld world = chapter.getWorld();
        User user = Store.getLoggedInUser();
        boolean unlocked = ChapterCatalog.isUnlocked(user, world);

        Table card = new Table();
        card.top().pad(10f);
        card.setTouchable(Touchable.enabled);
        card.setBackground(skin.getDrawable("image_ui_mainmenu_mm_settings_tab_10"));

        Stack artStack = new Stack();
        Image art = new Image(loadTexture(ADVENTURE_ASSET_ROOT + worldAsset(world)));
        art.setScaling(Scaling.fit);
        if (!unlocked) {
            art.setColor(0.42f, 0.42f, 0.42f, 0.72f);
        }
        artStack.add(art);

        if (!unlocked) {
            Table lockOverlay = new Table();
            lockOverlay.bottom().padBottom(18f);
            Label locked = new Label("LOCKED", skin, "medium_outline");
            locked.setColor(Color.LIGHT_GRAY);
            lockOverlay.add(locked);
            artStack.add(lockOverlay);
        }

        card.add(artStack).width(210f).height(320f).padBottom(6f).row();

        Label worldName = new Label(
                world.getDisplayName().toUpperCase(Locale.ROOT),
                skin,
                "medium_outline"
        );
        worldName.setWrap(true);
        worldName.setAlignment(com.badlogic.gdx.utils.Align.center);
        card.add(worldName).width(220f).height(50f).row();

        int completed = completedCoreLevels(user, world);
        Label progress = new Label(
                completed + " / " + CORE_LEVEL_COUNT + " CORE LEVELS",
                skin,
                "medium_outline"
        );
        if (!unlocked) {
            progress.setColor(Color.LIGHT_GRAY);
        }
        card.add(progress).padTop(3f).row();

        Label state = new Label(unlocked ? "OPEN" : "LOCKED", skin, "medium_outline");
        state.setColor(unlocked ? Color.GOLD : Color.LIGHT_GRAY);
        card.add(state).padTop(4f);

        card.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                openChapter(world);
            }
        });
        return card;
    }

    private void openChapter(GameWorld world) {
        Result<String> result = gameController.enterChapter(world.getDisplayName());
        if (!result.getStatus()) {
            toast.showError(result.getMessage());
            return;
        }

        selectedWorld = world;
        rebuild();
    }

    private Table buildLevelView() {
        Table content = new Table();
        content.top().padTop(100f).padBottom(84f).padLeft(54f).padRight(54f);

        Table worldPanel = new Table();
        worldPanel.setBackground(skin.getDrawable("image_ui_mainmenu_mm_settings_tab_10"));
        worldPanel.pad(12f);

        Image worldArt = new Image(loadTexture(ADVENTURE_ASSET_ROOT + worldAsset(selectedWorld)));
        worldArt.setScaling(Scaling.fit);
        worldPanel.add(worldArt).width(245f).height(430f).row();
        worldPanel.add(new Label(selectedWorld.getDisplayName(), skin, "medium_outline"))
                .padTop(4f);

        content.add(worldPanel).width(285f).height(515f).padRight(24f);
        content.add(buildLevelPanel()).width(850f).height(515f);
        return content;
    }

    private Table buildLevelPanel() {
        Table panel = new Table();
        panel.top().pad(22f, 24f, 20f, 24f);
        panel.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));

        User user = Store.getLoggedInUser();
        int completed = completedCoreLevels(user, selectedWorld);

        Label title = new Label("SELECT A LEVEL", skin, "medium_outline");
        panel.add(title).padBottom(6f).row();

        Label progress = new Label(
                selectedWorld.getDisplayName() + "   "
                        + completed + " / " + CORE_LEVEL_COUNT + " CORE LEVELS COMPLETE",
                skin,
                "medium_outline"
        );
        panel.add(progress).padBottom(28f).row();

        Table nodes = new Table();
        nodes.defaults().padLeft(6f).padRight(6f);
        Chapter chapter = AdventureCatalog.chapter(selectedWorld);
        for (Level level : chapter.getLevels()) {
            nodes.add(buildLevelNode(level)).width(180f).height(320f);
        }

        panel.add(nodes).growX();
        return panel;
    }

    private Table buildLevelNode(Level level) {
        User user = Store.getLoggedInUser();
        boolean unlocked = ChapterCatalog.isLevelUnlocked(
                user,
                selectedWorld,
                level.getLevelNumber()
        );

        Table node = new Table();
        node.top();

        Stack buttonStack = new Stack();
        ImageButton button = levelButton(unlocked);
        buttonStack.add(button);

        Table numberOverlay = new Table();
        numberOverlay.center();
        String numberText = level.isBossDeferred()
                ? "BOSS"
                : String.valueOf(level.getLevelNumber());
        Label number = new Label(numberText, skin, "medium_outline");
        if (!unlocked) {
            number.setColor(Color.LIGHT_GRAY);
        }
        numberOverlay.add(number).padBottom(9f);
        buttonStack.add(numberOverlay);

        node.add(buttonStack).width(132f).height(102f).padBottom(8f).row();

        Label name = new Label(levelName(level), skin, "medium_outline");
        name.setWrap(true);
        name.setAlignment(com.badlogic.gdx.utils.Align.center);
        if (!unlocked) {
            name.setColor(Color.LIGHT_GRAY);
        }
        node.add(name).width(168f).height(78f).row();

        Label type = new Label(levelType(level), skin, "medium_outline");
        type.setWrap(true);
        type.setAlignment(com.badlogic.gdx.utils.Align.center);
        type.setColor(level.isBossDeferred() ? Color.GOLD : Color.WHITE);
        node.add(type).width(168f).height(56f).row();

        Label state = new Label(unlocked ? "UNLOCKED" : "LOCKED", skin, "medium_outline");
        state.setColor(unlocked ? Color.GOLD : Color.LIGHT_GRAY);
        node.add(state).padTop(2f);

        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                chooseLevel(level);
            }
        });
        return node;
    }

    private ImageButton levelButton(boolean unlocked) {
        String up = ADVENTURE_ASSET_ROOT + (unlocked ? "level_green.png" : "level_gray.png");
        String over = ADVENTURE_ASSET_ROOT + (unlocked ? "level_blue.png" : "level_gray.png");
        ImageButton button = imageButton(up, over);
        button.getImage().setScaling(Scaling.fit);
        button.getImageCell().size(118f, 88f);
        return button;
    }

    private void chooseLevel(Level level) {
        Result<Level> result = gameController.enterLevel(
                selectedWorld.getDisplayName(),
                level.getLevelNumber()
        );

        if (!result.getStatus()) {
            if (level.isBossDeferred()) {
                toast.showInfo(result.getMessage());
            } else {
                toast.showError(result.getMessage());
            }
            return;
        }

        toast.showInfo(
                result.getData().getName()
                        + " is ready. Plant selection will open here once its Phase-2 screen is connected."
        );
    }

    private Table buildShortcutBar() {
        Table shortcuts = new Table();
        shortcuts.bottom().padBottom(14f);

        TextButton collection = new TextButton("COLLECTION", skin, "brown");
        addComingSoonListener(collection, "Collection");
        shortcuts.add(collection).width(170f).height(46f).padRight(8f);

        TextButton greenhouse = new TextButton("GREENHOUSE", skin, "brown");
        addComingSoonListener(greenhouse, "Greenhouse");
        shortcuts.add(greenhouse).width(170f).height(46f).padRight(8f);

        TextButton missions = new TextButton("MISSIONS", skin, "brown");
        addComingSoonListener(missions, "Missions / Travel Log");
        shortcuts.add(missions).width(190f).height(46f);
        return shortcuts;
    }

    private void addComingSoonListener(Actor actor, String name) {
        actor.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor source) {
                toast.showInfo(name + " screen is not connected yet.");
            }
        });
    }

    private int completedCoreLevels(User user, GameWorld world) {
        if (user == null || world == null) {
            return 0;
        }

        int latestChapter = user.getLatestCompletedChapter();
        int latestLevel = user.getLatestCompletedLevel();
        if (latestChapter > world.getChapterNumber()) {
            return CORE_LEVEL_COUNT;
        }
        if (latestChapter == world.getChapterNumber()) {
            return Math.min(CORE_LEVEL_COUNT, Math.max(0, latestLevel));
        }
        return 0;
    }

    private String worldAsset(GameWorld world) {
        return switch (world) {
            case ANCIENT_EGYPT -> "egypt.png";
            case FROSTBITE_CAVES -> "iceage.png";
            case BIG_WAVE_BEACH -> "beach.png";
            case DARK_AGES -> "dark.png";
        };
    }

    private String levelName(Level level) {
        String worldPrefix = selectedWorld.getDisplayName() + " ";
        String name = level.getName();
        if (name.startsWith(worldPrefix)) {
            name = name.substring(worldPrefix.length());
        }
        return name;
    }

    private String levelType(Level level) {
        if (level.isBossDeferred()) {
            return "BONUS BOSS";
        }
        if (level.getAdventureConfig() == null
                || level.getAdventureConfig().getSpecialType() == null) {
            return "NORMAL";
        }
        return level.getAdventureConfig().getSpecialType().name().replace('_', ' ');
    }

    private ImageButton imageButton(String normalPath, String selectedPath) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(loadTexture(normalPath));
        style.imageOver = new TextureRegionDrawable(loadTexture(selectedPath));
        style.imageDown = new TextureRegionDrawable(loadTexture(selectedPath));
        ImageButton button = new ImageButton(style);
        button.getImage().setScaling(Scaling.fit);
        return button;
    }

    private Texture loadTexture(String path) {
        Texture cached = textures.get(path);
        if (cached != null) {
            return cached;
        }
        Texture texture = new Texture(Gdx.files.internal(path));
        textures.put(path, texture);
        return texture;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
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
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
    }
}
