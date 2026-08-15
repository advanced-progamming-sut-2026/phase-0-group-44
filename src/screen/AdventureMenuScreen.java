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
import com.badlogic.gdx.utils.Align;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Phase-2 Adventure screen.
 *
 * <p>The chapter page deliberately behaves like the PVZ2 world selector rather
 * than a generic settings grid: one world is the focus of a carousel while the
 * neighbouring worlds remain visible.  The persistent HUD exposes the
 * Adventure shortcuts (settings, collection, greenhouse, missions and shop)
 * with PVZ assets, and the wallet stays visible as required by the phase-2
 * specification.</p>
 */
public final class AdventureMenuScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;
    private static final String ADVENTURE_ASSET_ROOT = "ui/adventure/";
    private static final String HUD_ASSET_ROOT = ADVENTURE_ASSET_ROOT + "hud/";
    private static final String MAIN_MENU_ASSET_ROOT = "ui/mainmenu/";
    private static final int CORE_LEVEL_COUNT = 3;

    private final PvzGame game;
    private final GameMenuController gameController;
    private final MenuController menuController;
    private final Map<String, Texture> textures = new LinkedHashMap<>();

    private Stage stage;
    private Skin skin;
    private ToastManager toast;
    /** Null while browsing chapters; non-null while browsing that world's levels. */
    private GameWorld selectedWorld;
    private int chapterCursor;

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
        chapterCursor = furthestUnlockedChapterIndex(Store.getLoggedInUser());
        rebuild();
    }

    private void rebuild() {
        stage.clear();

        Stack root = new Stack();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(buildBackground());
        root.add(selectedWorld == null ? buildChapterView() : buildLevelView());
        root.add(buildTopHud());
    }

    private Image buildBackground() {
        Image background = new Image(loadTexture(MAIN_MENU_ASSET_ROOT + "background.png"));
        background.setScaling(Scaling.fill);
        return background;
    }

    /** The PVZ-like persistent Adventure HUD. */
    private Table buildTopHud() {
        Table hud = new Table();
        hud.top().pad(9f, 13f, 0f, 13f);

        Table left = new Table();
        left.defaults().padRight(5f);
        left.add(hudIcon(buildBackButton(), "BACK", 62f, 60f));

        ImageButton settings = imageButton(
                MAIN_MENU_ASSET_ROOT + "settings_normal.png",
                MAIN_MENU_ASSET_ROOT + "settings_selected.png"
        );
        addComingSoonListener(settings, "Settings");
        left.add(hudIcon(settings, "SETTINGS", 58f, 55f));

        ImageButton collection = imageButton(
                HUD_ASSET_ROOT + "collection_normal.png",
                HUD_ASSET_ROOT + "collection_selected.png"
        );
        addComingSoonListener(collection, "Collection / Almanac");
        left.add(hudIcon(collection, "COLLECTION", 58f, 55f));

        ImageButton greenhouse = imageButton(
                HUD_ASSET_ROOT + "greenhouse_normal.png",
                HUD_ASSET_ROOT + "greenhouse_selected.png"
        );
        addComingSoonListener(greenhouse, "Greenhouse");
        left.add(hudIcon(greenhouse, "GREENHOUSE", 64f, 55f));

        ImageButton missions = imageButton(
                HUD_ASSET_ROOT + "missions_normal.png",
                HUD_ASSET_ROOT + "missions_selected.png"
        );
        addComingSoonListener(missions, "Missions / Travel Log");
        left.add(hudIcon(missions, "MISSIONS", 58f, 55f));

        hud.add(left).left().top();

        String titleText = selectedWorld == null
                ? "ADVENTURE"
                : selectedWorld.getDisplayName().toUpperCase(Locale.ROOT);
        Label title = new Label(titleText, skin, "medium_outline");
        hud.add(title).expandX().top().padTop(9f);

        hud.add(buildWalletAndShop()).right().top();
        return hud;
    }

    private Table hudIcon(ImageButton button, String caption, float width, float height) {
        Table slot = new Table();
        slot.add(button).size(width, height).row();
        Label label = new Label(caption, skin);
        label.setAlignment(Align.center);
        slot.add(label).width(Math.max(width + 12f, 72f)).padTop(1f);
        return slot;
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

    private Table buildWalletAndShop() {
        Table right = new Table();
        User user = Store.getLoggedInUser();
        int gems = user == null ? 0 : user.getGems();
        int coins = user == null ? 0 : user.getCoins();

        right.add(resourceIcon(MAIN_MENU_ASSET_ROOT + "gem.png")).size(31f, 39f).padRight(4f);
        right.add(new Label(String.valueOf(gems), skin, "medium_outline")).padRight(13f);
        right.add(resourceIcon(MAIN_MENU_ASSET_ROOT + "coin.png")).size(31f, 31f).padRight(4f);
        right.add(new Label(String.valueOf(coins), skin, "medium_outline")).padRight(9f);

        ImageButton shop = imageButton(
                HUD_ASSET_ROOT + "shop_normal.png",
                HUD_ASSET_ROOT + "shop_selected.png"
        );
        addComingSoonListener(
                shop,
                "Shop (the Phase-1 route is through Greenhouse; the graphical shortcut is reserved here)"
        );
        right.add(hudIcon(shop, "SHOP", 69f, 69f));
        return right;
    }

    private Image resourceIcon(String path) {
        Image image = new Image(loadTexture(path));
        image.setScaling(Scaling.fit);
        return image;
    }

    /** Figure-2 style world selector. */
    private Table buildChapterView() {
        Table content = new Table();
        content.top().padTop(103f).padBottom(16f);

        List<Chapter> chapters = AdventureCatalog.chapters();
        Chapter current = chapters.get(chapterCursor);

        Table carousel = new Table();
        carousel.defaults().padLeft(8f).padRight(8f);

        carousel.add(buildCarouselArrow(-1)).size(48f, 48f);
        carousel.add(buildSideWorld(chapterCursor - 1)).width(205f).height(330f);
        carousel.add(buildCenterWorld(current)).width(390f).height(420f);
        carousel.add(buildSideWorld(chapterCursor + 1)).width(205f).height(330f);
        carousel.add(buildCarouselArrow(1)).size(48f, 48f);

        content.add(carousel).height(430f).row();
        content.add(buildChapterSummary(current)).padTop(1f);
        return content;
    }

    private Actor buildCarouselArrow(int direction) {
        boolean left = direction < 0;
        ImageButton button = imageButton(
                HUD_ASSET_ROOT + (left ? "carousel_left.png" : "carousel_right.png"),
                HUD_ASSET_ROOT + (left ? "carousel_left_down.png" : "carousel_right_down.png")
        );

        boolean enabled = direction < 0
                ? chapterCursor > 0
                : chapterCursor < AdventureCatalog.chapters().size() - 1;
        if (!enabled) {
            button.setTouchable(Touchable.disabled);
            button.setColor(1f, 1f, 1f, 0.22f);
            return button;
        }

        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                chapterCursor += direction;
                rebuild();
            }
        });
        return button;
    }

    private Actor buildSideWorld(int index) {
        Table holder = new Table();
        List<Chapter> chapters = AdventureCatalog.chapters();
        if (index < 0 || index >= chapters.size()) {
            return holder;
        }

        Chapter chapter = chapters.get(index);
        User user = Store.getLoggedInUser();
        boolean unlocked = ChapterCatalog.isUnlocked(user, chapter.getWorld());

        Stack stack = worldArtStack(chapter.getWorld(), unlocked, false);
        holder.add(stack).width(170f).height(250f).row();

        Label name = new Label(chapter.getWorld().getDisplayName(), skin);
        name.setWrap(true);
        name.setAlignment(Align.center);
        if (!unlocked) {
            name.setColor(Color.LIGHT_GRAY);
        }
        holder.add(name).width(185f).height(45f).padTop(1f);

        holder.setTouchable(Touchable.enabled);
        holder.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                chapterCursor = index;
                rebuild();
            }
        });
        return holder;
    }

    private Actor buildCenterWorld(Chapter chapter) {
        User user = Store.getLoggedInUser();
        boolean unlocked = ChapterCatalog.isUnlocked(user, chapter.getWorld());

        Table holder = new Table();
        Stack stack = worldArtStack(chapter.getWorld(), unlocked, true);
        holder.add(stack).width(330f).height(390f);
        holder.setTouchable(Touchable.enabled);
        holder.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                openChapter(chapter.getWorld());
            }
        });
        return holder;
    }

    private Stack worldArtStack(GameWorld world, boolean unlocked, boolean central) {
        Stack stack = new Stack();
        Image art = new Image(loadTexture(ADVENTURE_ASSET_ROOT + worldAsset(world)));
        art.setScaling(Scaling.fit);
        if (!unlocked) {
            art.setColor(0.34f, 0.34f, 0.34f, central ? 0.80f : 0.62f);
        } else if (!central) {
            art.setColor(0.78f, 0.78f, 0.78f, 0.84f);
        }
        stack.add(art);
        return stack;
    }

    private Table buildChapterSummary(Chapter chapter) {
        GameWorld world = chapter.getWorld();
        User user = Store.getLoggedInUser();
        boolean unlocked = ChapterCatalog.isUnlocked(user, world);
        int completed = completedCoreLevels(user, world);

        Table summary = new Table();
        Label name = new Label(world.getDisplayName().toUpperCase(Locale.ROOT), skin, "medium_outline");
        summary.add(name).row();

        Label progress = new Label(
                completed + " / " + CORE_LEVEL_COUNT + " CORE LEVELS COMPLETE",
                skin
        );
        summary.add(progress).padTop(2f).row();

        if (!unlocked) {
            Label locked = new Label("LOCKED", skin, "medium_outline");
            locked.setColor(Color.LIGHT_GRAY);
            summary.add(locked).padTop(4f);
            return summary;
        }

        TextButton enter = new TextButton("ENTER", skin, "purple");
        enter.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openChapter(world);
            }
        });
        summary.add(enter).width(165f).height(46f).padTop(5f);
        return summary;
    }

    private void openChapter(GameWorld world) {
        Result<String> result = gameController.enterChapter(world.getDisplayName());
        if (!result.getStatus()) {
            toast.showError(result.getMessage());
            return;
        }

        selectedWorld = world;
        chapterCursor = Math.max(0, selectedWorld.getChapterNumber() - 1);
        rebuild();
    }

    /** Figure-3 style level selector, without the old beige card grid. */
    private Table buildLevelView() {
        Table content = new Table();
        content.top().padTop(112f).padLeft(52f).padRight(52f).padBottom(20f);

        User user = Store.getLoggedInUser();
        int completed = completedCoreLevels(user, selectedWorld);

        Label heading = new Label("SELECT A LEVEL", skin, "medium_outline");
        content.add(heading).colspan(2).padBottom(2f).row();
        Label progress = new Label(
                completed + " / " + CORE_LEVEL_COUNT + " CORE LEVELS COMPLETE",
                skin
        );
        content.add(progress).colspan(2).padBottom(12f).row();

        Image worldArt = new Image(loadTexture(ADVENTURE_ASSET_ROOT + worldAsset(selectedWorld)));
        worldArt.setScaling(Scaling.fit);
        content.add(worldArt).width(255f).height(420f).padRight(28f);
        content.add(buildLevelPath()).width(865f).height(420f);
        return content;
    }

    private Table buildLevelPath() {
        Table path = new Table();
        Chapter chapter = AdventureCatalog.chapter(selectedWorld);
        List<Level> levels = chapter.getLevels();

        Table row = new Table();
        for (int index = 0; index < levels.size(); index++) {
            row.add(buildLevelNode(levels.get(index))).width(184f).height(305f);
            if (index < levels.size() - 1) {
                Label connector = new Label("- - -", skin, "medium_outline");
                connector.setColor(0.85f, 0.82f, 0.66f, 0.9f);
                row.add(connector).width(48f).padBottom(118f);
            }
        }
        path.add(row).expand().center();
        return path;
    }

    private Table buildLevelNode(Level level) {
        User user = Store.getLoggedInUser();
        boolean unlocked = ChapterCatalog.isLevelUnlocked(
                user,
                selectedWorld,
                level.getLevelNumber()
        );
        boolean completed = isLevelCompleted(user, selectedWorld, level.getLevelNumber());

        Table node = new Table();
        node.top();

        Stack buttonStack = new Stack();
        ImageButton button = levelButton(unlocked);
        buttonStack.add(button);

        Table numberOverlay = new Table();
        String numberText = level.isBossDeferred()
                ? "BOSS"
                : String.valueOf(level.getLevelNumber());
        Label number = new Label(numberText, skin, "medium_outline");
        if (!unlocked) {
            number.setColor(Color.LIGHT_GRAY);
        }
        numberOverlay.add(number).padBottom(8f);
        buttonStack.add(numberOverlay);

        node.add(buttonStack).width(132f).height(102f).padBottom(7f).row();

        Label name = new Label(levelName(level), skin);
        name.setWrap(true);
        name.setAlignment(Align.center);
        if (!unlocked) {
            name.setColor(Color.LIGHT_GRAY);
        }
        node.add(name).width(170f).height(61f).row();

        Label type = new Label(levelType(level), skin);
        type.setWrap(true);
        type.setAlignment(Align.center);
        type.setColor(level.isBossDeferred() ? Color.GOLD : Color.WHITE);
        node.add(type).width(170f).height(45f).row();

        String stateText = completed ? "COMPLETED" : (unlocked ? "OPEN" : "LOCKED");
        Label state = new Label(stateText, skin);
        state.setColor(completed || unlocked ? Color.GOLD : Color.LIGHT_GRAY);
        node.add(state).padTop(1f);

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

    private boolean isLevelCompleted(User user, GameWorld world, int levelNumber) {
        if (user == null || world == null) {
            return false;
        }
        if (user.getLatestCompletedChapter() > world.getChapterNumber()) {
            return true;
        }
        return user.getLatestCompletedChapter() == world.getChapterNumber()
                && user.getLatestCompletedLevel() >= levelNumber;
    }

    private int furthestUnlockedChapterIndex(User user) {
        if (user == null) {
            return 0;
        }
        List<Chapter> chapters = AdventureCatalog.chapters();
        int result = 0;
        for (int index = 0; index < chapters.size(); index++) {
            if (ChapterCatalog.isUnlocked(user, chapters.get(index).getWorld())) {
                result = index;
            }
        }
        return result;
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
