package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
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

    private Texture pathTexture;
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
        greenhouse.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Result<String> result = menuController.enterMenu("greenhouse");
                if (result.getStatus()) {
                    game.goToScreenForCurrentMenu();
                } else {
                    toast.showError(result.getMessage());
                }
            }
        });
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

    /**
     * Figure-3 inspired level map.  Instead of a second card/list screen, the
     * selected world stays visible as the visual anchor and the available
     * stages are placed around it like PVZ2 world-map nodes.
     */
    private Table buildLevelView() {
        Table content = new Table();
        content.top().padTop(103f).padLeft(45f).padRight(45f).padBottom(12f);

        User user = Store.getLoggedInUser();
        int completed = completedCoreLevels(user, selectedWorld);

        Label progress = new Label(
                completed + " / " + CORE_LEVEL_COUNT + " CORE LEVELS COMPLETE",
                skin
        );
        progress.setAlignment(Align.center);
        content.add(progress).padTop(4f).padBottom(2f).row();

        Label hint = new Label("Choose an unlocked stage", skin);
        hint.setColor(0.90f, 0.95f, 1f, 0.92f);
        hint.setAlignment(Align.center);
        content.add(hint).padBottom(5f).row();

        content.add(buildLevelMap()).width(1180f).height(505f);
        return content;
    }

    /**
     * A small zig-zag world map, closer to Figure 3 than the previous straight
     * row of level cards.  The layout is intentionally fixed because each
     * project chapter currently has four stages (three core stages + boss).
     */
    private Group buildLevelMap() {
        Group map = new Group();
        map.setSize(1180f, 505f);
        Image worldArt = new Image(loadTexture(ADVENTURE_ASSET_ROOT + worldAsset(selectedWorld)));
        worldArt.setScaling(Scaling.fit);
        worldArt.setColor(1f, 1f, 1f, 0.78f);
        worldArt.setBounds(388f, 55f, 414f, 414f);

        Chapter chapter = AdventureCatalog.chapter(selectedWorld);
        List<Level> levels = chapter.getLevels();

        // Positions imitate the floating-island route in Figure 3: stages
        // climb around the chapter landmark rather than appearing as cards.
        float[][] positions = {
                {78f, 252f},
                {300f, 132f},
                {690f, 252f},
                {932f, 132f}
        };

        int visibleCount = Math.min(levels.size(), positions.length);
        for (int index = 0; index < visibleCount - 1; index++) {
            addMapConnector(
                    map,
                    positions[index][0] + 80f,
                    positions[index][1] + 92f,
                    positions[index + 1][0] + 80f,
                    positions[index + 1][1] + 92f
            );
        }

        map.addActor(worldArt);

        for (int index = 0; index < visibleCount; index++) {
            Table node = buildLevelNode(levels.get(index));
            node.setBounds(positions[index][0], positions[index][1], 162f, 172f);
            map.addActor(node);
        }

        return map;
    }

    /** Adds a thin cyan route segment behind two stage nodes. */
    private void addMapConnector(Group map, float startX, float startY, float endX, float endY) {
        float dx = endX - startX;
        float dy = endY - startY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

        Image line = new Image(levelPathTexture());
        line.setBounds(startX, startY - 1.5f, length, 4f);
        line.setOrigin(0f, 2f);
        line.setRotation(angle);
        line.setColor(0.33f, 0.84f, 0.98f, 0.78f);
        map.addActor(line);
    }

    private Texture levelPathTexture() {
        if (pathTexture != null) {
            return pathTexture;
        }
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        pathTexture = new Texture(pixel);
        pixel.dispose();
        return pathTexture;
    }

    /**
     * Compact map node with a real world-map mini-platform behind the stage
     * badge, so the icons feel closer to Figure 3 than plain floating caps.
     */
    private Table buildLevelNode(Level level) {
        User user = Store.getLoggedInUser();
        boolean unlocked = ChapterCatalog.isLevelUnlocked(
                user,
                selectedWorld,
                level.getLevelNumber()
        );
        boolean completed = isLevelCompleted(user, selectedWorld, level.getLevelNumber());
        boolean boss = level.isBossDeferred();

        Table node = new Table();
        node.top();

        Stack iconStack = new Stack();

        Table shadowLayer = new Table();
        Image shadow = new Image(levelPathTexture());
        shadow.setColor(0f, 0f, 0f, unlocked ? 0.14f : 0.10f);
        shadowLayer.add(shadow).size(boss ? 92f : 84f, boss ? 9f : 8f).padTop(84f);
        iconStack.add(shadowLayer);

        Table platformLayer = new Table();
        Image platform = new Image(loadTexture(ADVENTURE_ASSET_ROOT + levelPlatformAsset(selectedWorld)));
        platform.setScaling(Scaling.fit);
        if (!unlocked) {
            platform.setColor(0.68f, 0.70f, 0.74f, 0.68f);
        } else if (boss) {
            platform.setColor(0.92f, 0.92f, 1f, 0.92f);
        } else if (completed) {
            platform.setColor(1f, 1f, 1f, 0.98f);
        } else {
            platform.setColor(1f, 1f, 1f, 0.90f);
        }
        platformLayer.add(platform).size(boss ? 138f : 126f, boss ? 98f : 88f).padTop(18f);
        iconStack.add(platformLayer);

        ImageButton button = levelButton(unlocked, completed, boss);
        Table buttonLayer = new Table();
        buttonLayer.add(button).size(boss ? 74f : 68f, boss ? 56f : 52f).padBottom(18f);
        iconStack.add(buttonLayer);

        Table numberOverlay = new Table();
        String numberText = boss ? "BOSS" : String.valueOf(level.getLevelNumber());
        Label number = new Label(numberText, skin, "medium_outline");
        if (!unlocked) {
            number.setColor(Color.LIGHT_GRAY);
        } else if (completed) {
            number.setColor(Color.GOLD);
        }
        numberOverlay.add(number).padBottom(17f);
        iconStack.add(numberOverlay);

        node.add(iconStack).width(158f).height(114f).padBottom(-4f).row();

        Label name = new Label(shortLevelName(level), skin);
        name.setWrap(true);
        name.setAlignment(Align.center);
        if (!unlocked) {
            name.setColor(Color.LIGHT_GRAY);
        }
        node.add(name).width(148f).height(28f).padTop(1f).row();

        String statusText = completed
                ? "COMPLETED"
                : (!unlocked ? "LOCKED" : compactLevelType(level));
        Label status = new Label(statusText, skin);
        status.setAlignment(Align.center);
        if (completed) {
            status.setColor(Color.GOLD);
        } else if (!unlocked) {
            status.setColor(Color.LIGHT_GRAY);
        } else if (boss) {
            status.setColor(Color.GOLD);
        } else {
            status.setColor(0.85f, 0.94f, 1f, 1f);
        }
        node.add(status).width(148f).height(20f).padTop(-1f);

        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                chooseLevel(level);
            }
        });
        return node;
    }

    private ImageButton levelButton(boolean unlocked, boolean completed, boolean boss) {
        String up;
        String over;
        if (!unlocked) {
            up = ADVENTURE_ASSET_ROOT + "level_gray.png";
            over = up;
        } else if (completed) {
            up = ADVENTURE_ASSET_ROOT + "level_blue.png";
            over = ADVENTURE_ASSET_ROOT + "level_green.png";
        } else if (boss) {
            up = ADVENTURE_ASSET_ROOT + "level_blue.png";
            over = ADVENTURE_ASSET_ROOT + "level_green.png";
        } else {
            up = ADVENTURE_ASSET_ROOT + "level_green.png";
            over = ADVENTURE_ASSET_ROOT + "level_blue.png";
        }

        ImageButton button = imageButton(up, over);
        button.getImageCell().size(boss ? 60f : 56f, boss ? 44f : 40f);
        return button;
    }

    private String shortLevelName(Level level) {
        if (level.isBossDeferred()) {
            return "Boss";
        }
        String name = levelName(level);
        // The number is already inside the map node, so avoid repeating it.
        name = name.replaceFirst("^\\d+\\s*[-:]?\\s*", "");
        return name.isBlank() ? "Level " + level.getLevelNumber() : name;
    }

    private String compactLevelType(Level level) {
        if (level.isBossDeferred()) {
            return "BONUS BOSS";
        }
        String type = levelType(level);
        return "NORMAL".equals(type) ? "OPEN" : type;
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

    private String levelPlatformAsset(GameWorld world) {
        return switch (world) {
            case ANCIENT_EGYPT -> "platforms/egypt_platform.png";
            case FROSTBITE_CAVES -> "platforms/ice_platform.png";
            case BIG_WAVE_BEACH -> "platforms/beach_platform.png";
            case DARK_AGES -> "platforms/dark_platform.png";
        };
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
        if (pathTexture != null) {
            pathTexture.dispose();
            pathTexture = null;
        }
    }
}
