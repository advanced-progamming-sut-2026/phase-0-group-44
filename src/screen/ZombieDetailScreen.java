package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import controller.App;
import model.Store;
import model.inGame.zombie.ZombieDefinition;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Full-screen zombie detail view, mirroring PlantDetailScreen's structure (top
 * bar, centered portrait + stats panel, prev/next arrows). Unlike plants, a
 * zombie has no card/level/seed-packet concept, so the left column holds the
 * portrait plus a single-column stat list instead of a progress bar, and the
 * right column is just the zombie's name.
 *
 * ASSUMPTIONS (flagged because some data/assets don't exist yet):
 *  - ARMOR shows definition.getArmor() (a total int); individual armor-part
 *    names/breakability aren't shown because ZombieArmorPart's own fields
 *    haven't been reviewed.
 *  - BEHAVIOR has no icon asset, so that row has a blank spacer instead.
 *  - New icon assets required: speed.png, wavecost.png, armor.png (under
 *    ASSET_ROOT). HEALTH reuses toughness.png and EAT DPS reuses damage.png,
 *    same as the plant detail screen's equivalents.
 *  - Background reuses the same "plantDetailBg.png" as PlantDetailScreen for
 *    visual consistency; swap this if a zombie-specific background exists.
 */
public final class ZombieDetailScreen implements Screen {

    private static final String ASSET_ROOT = "ui/collection/";
    private static final String ZOMBIE_ICON_ROOT = "ui/collection/zombies/";
    private static final String ADVENTURE_ROOT = "ui/adventure/";

    private final PvzGame game;
    private final App app;
    private final List<ZombieDefinition> views;
    private final List<Texture> textures = new ArrayList<>();

    private int index;

    private Stage stage;
    private Skin skin;
    private Table root;
    private Image background;

    public ZombieDetailScreen(PvzGame game, App app, List<ZombieDefinition> views, int index) {
        this.game = game;
        this.app = app;
        this.views = views;
        this.index = index;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        skin = PvzSkin.get();
        PvzSkinExtras.ensureDialogStyle(skin);
        Gdx.input.setInputProcessor(stage);

        background = new Image(loadTexture(ASSET_ROOT + "plantDetailBg.png"));
        background.setScaling(Scaling.fill);
        background.setFillParent(true);
        stage.addActor(background);

        buildScreen();
    }

    private ZombieDefinition current() {
        return views.get(index);
    }

    // ---------------------------------------------------------------- layout

    private void buildScreen() {
        if (root != null) {
            root.remove();
        }
        root = new Table();
        root.setFillParent(true);
        root.top();
        stage.addActor(root);

        root.add(buildTopBar()).growX().colspan(2).row();

        Table body = new Table();
        body.top();
        body.add(buildLeftColumn()).width(400f).top().padRight(24f);
        body.add(buildRightColumn()).width(360f).top();

        Stack withArrows = new Stack();
        withArrows.add(body);
        withArrows.add(buildArrowOverlay());

        root.add(withArrows).expand().center().pad(10f, 24f, 20f, 24f);
    }

    private Table buildTopBar() {
        Table topBar = new Table();
        topBar.pad(16f, 20f, 0f, 20f);

        ImageButton backButton = navArrowButton(ADVENTURE_ROOT + "back_normal.png", ADVENTURE_ROOT + "back_selected.png");
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new CollectionScreen(game, app));
            }
        });
        topBar.add(backButton).size(72f, 66f).left();

        topBar.add().expandX();

        Table resources = new Table();
        model.user.User user = Store.getLoggedInUser();
        int gems = user == null ? 0 : user.getGems();
        int coins = user == null ? 0 : user.getCoins();
        resources.add(new Label(String.valueOf(gems), skin, "medium_outline")).padRight(18f);
        resources.add(new Label(String.valueOf(coins), skin, "medium_outline"));
        topBar.add(resources).right();
        return topBar;
    }

    // ---------------------------------------------------------------- left column

    private Table buildLeftColumn() {
        ZombieDefinition definition = current();

        Table column = new Table();
        column.top();

        Table portrait = new Table();
        portrait.setBackground(
                new TextureRegionDrawable(
                        new TextureRegion(
                                loadTexture(ASSET_ROOT + "zombieDetailBg.png")
                        )
                )
        );

        Image zombieImage = new Image(loadTexture(ZOMBIE_ICON_ROOT
                + definition.getType().name().toLowerCase(Locale.ROOT) + ".png"));
        zombieImage.setScaling(Scaling.fit);
        portrait.add(zombieImage).size(120f).pad(20f);

        Stack portraitStack = new Stack();
        portraitStack.add(portrait);

        Table chapterOverlay = new Table();
        chapterOverlay.bottom().left().pad(0f, 10f, 10f, 0f);
        chapterOverlay.add(new Label(chapterLabel(definition), skin, "medium_outline"));
        portraitStack.add(chapterOverlay);

        column.add(portraitStack).width(320f).height(280f).row();

        column.add(buildZombieStats(definition)).width(320f).padTop(14f);

        return column;
    }

    private String chapterLabel(ZombieDefinition definition) {
        return definition.getChapter() == null
                ? ""
                : definition.getChapter().name().replace('_', ' ');
    }

    private Table buildZombieStats(ZombieDefinition definition) {
        Table stats = new Table();
        stats.top().left();
        stats.defaults().left().padBottom(10f);

        stats.add(buildStatRow(ASSET_ROOT + "toughness.png", "HEALTH",
                String.valueOf(definition.getHealth()))).row();
        stats.add(buildStatRow(ASSET_ROOT + "speed.png", "SPEED",
                String.valueOf(definition.getSpeedTilesPerSecond()))).row();
        stats.add(buildStatRow(ASSET_ROOT + "damage.png", "EAT DPS",
                String.valueOf(definition.getEatDamagePerSecond()))).row();
        stats.add(buildStatRow(ASSET_ROOT + "wavecost.png", "WAVE COST",
                String.valueOf(definition.getWaveCost()))).row();
        stats.add(buildStatRow(ASSET_ROOT + "armor.png", "ARMOR",
                String.valueOf(definition.getArmor()))).row();
        stats.add(buildStatRow(null, "BEHAVIOR",
                definition.getBehaviorKind() == null ? "" : definition.getBehaviorKind().name()));

        return stats;
    }

    private Table buildStatRow(String iconPath, String label, String value) {
        Table row = new Table();

        if (iconPath != null) {
            Image icon = new Image(loadTexture(iconPath));
            icon.setScaling(Scaling.fit);
            row.add(icon).size(60f).padRight(10f);
        } else {
            Table spacer = new Table();
            row.add(spacer).size(60f).padRight(10f);
        }

        Table textColumn = new Table();
        textColumn.left();
        textColumn.add(new Label(label, skin, "medium_outline")).left().row();
        textColumn.add(new Label(value, skin, "medium_outline")).left();

        row.add(textColumn).left();
        return row;
    }

    // ---------------------------------------------------------------- right column

    private Table buildRightColumn() {
        ZombieDefinition definition = current();

        Table column = new Table();
        column.top().left();
        column.pad(4f, 10f, 0f, 10f);

        Label nameLabel = new Label(definition.getName(), skin, "big_outline");
        nameLabel.setWrap(true);
        column.add(nameLabel).left().width(340f);

        return column;
    }

    // ---------------------------------------------------------------- nav arrows

    private Table buildArrowOverlay() {
        Table overlay = new Table();
        overlay.setFillParent(true);

        ImageButton previousButton = navArrowButton(
                ASSET_ROOT + "stats_screen_nav_arrow_previous.png",
                ASSET_ROOT + "stats_screen_nav_arrow_previous_down.png");

        previousButton.setVisible(views.size() > 1);
        previousButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                index = (index - 1 + views.size()) % views.size();
                buildScreen();
            }
        });

        ImageButton nextButton = navArrowButton(
                ASSET_ROOT + "stats_screen_nav_arrow_next.png",
                ASSET_ROOT + "stats_screen_nav_arrow_next_down.png");

        nextButton.setVisible(views.size() > 1);
        nextButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                index = (index + 1) % views.size();
                buildScreen();
            }
        });

        overlay.top().padTop(400f);

        overlay.add(previousButton).size(54f, 64f).left().expandX().padLeft(-40f);
        overlay.add(nextButton).size(54f, 64f).right().expandX().padRight(-40f);

        return overlay;
    }

    private ImageButton navArrowButton(String upPath, String downPath) {
        Texture up = loadTexture(upPath);
        Texture down = loadTexture(downPath);
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(up);
        style.imageDown = new TextureRegionDrawable(down);
        style.imageOver = new TextureRegionDrawable(down);
        return new ImageButton(style);
    }

    // ---------------------------------------------------------------- assets

    private Texture loadTexture(String path) {
        Texture texture = TextureQuality.load(path);
        textures.add(texture);
        return texture;
    }

    // ---------------------------------------------------------------- Screen

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.04f, 0.09f, 1f);
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
        for (Texture texture : textures) {
            texture.dispose();
        }
    }
}