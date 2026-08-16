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
import controller.CollectionMenuController;
import model.Store;
import model.user.PlantCard;
import model.inGame.plant.PlantCollectionView;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantStats;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Full-screen plant detail view, following the almanac-style mockup: portrait +
 * seed-packet progress on the left, stat grid + plant food on the right, with
 * back navigation and prev/next arrows to cycle the unlocked roster.
 *
 * ASSUMPTIONS (flagged because the model doesn't carry this data today):
 *  - "Range" and "Special" have no backing field on PlantDefinition, so those two
 *    rows render their icon + label with a placeholder value ("--").
 *  - The yellow flavor/backstory paragraph in the mockup has no matching field
 *    (baseAbility has no public getter today), so it's omitted.
 *  - "Show Mint Stats" is implemented as a pure UI toggle: checked reveals the
 *    next-level ("current > next") preview on each stat row; unchecked shows the
 *    current level only. There's no dedicated "mint stats" concept in the model.
 *  - No family badge or tag icons are rendered at all: there are no image assets
 *    for plant families or tags, and no field on PlantDefinition/PlantCategory/
 *    PlantTag maps a regular plant to a specific mint family (e.g. Kernel-pult ->
 *    Arma-mint). If assets and a data field are added later, this can be wired in.
 */
public final class PlantDetailScreen implements Screen {

    private static final String ASSET_ROOT = "ui/collection/";
    private static final String PLANT_ICON_ROOT = "ui/collection/plants/";
    private static final String ADVENTURE_ROOT = "ui/adventure/";

    private final PvzGame game;
    private final App app;
    private final CollectionMenuController controller;
    private final List<PlantCollectionView> views;
    private final List<Texture> textures = new ArrayList<>();
    private Image background;

    private int index;
    private boolean showPreview = true;

    private Stage stage;
    private Skin skin;
    private ToastManager toast;
    private Table root;

    public PlantDetailScreen(PvzGame game, App app, List<PlantCollectionView> views, int index) {
        this.game = game;
        this.app = app;
        this.controller = app.getCollectionController();
        this.views = views;
        this.index = index;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        skin = PvzSkin.get();
        PvzSkinExtras.ensureDialogStyle(skin);
        toast = new ToastManager(stage, skin);
        Gdx.input.setInputProcessor(stage);
        background = new Image(loadTexture(ASSET_ROOT + "plantDetailBg.png"));
        background.setScaling(Scaling.fill);
        background.setFillParent(true);
        stage.addActor(background);
        buildScreen();
    }

    private PlantCollectionView current() {
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
        body.add(buildLeftColumn()).width(660f).top().padRight(24f);
        body.add(buildRightColumn()).width(760f).top().padTop(-80f);;

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
        PlantCollectionView view = current();
        PlantCard card = view.getCard();

        Table column = new Table();
        column.top();

        Table portrait = new Table();
        portrait.setBackground(
                new TextureRegionDrawable(
                        new TextureRegion(
                                loadTexture(ASSET_ROOT + "card_plant_bg_pirate.png")
                        )
                )
        );

        Image plantImage = new Image(loadTexture(PLANT_ICON_ROOT
                + view.getDefinition().getType().name().toLowerCase(Locale.ROOT) + ".png"));
        plantImage.setScaling(Scaling.fit);
        portrait.add(plantImage).size(120f).pad(20f);

        Stack portraitStack = new Stack();
        portraitStack.add(portrait);

        Table levelOverlay = new Table();
        levelOverlay.bottom().left().pad(0f, 130f, 10f, 0f);
        levelOverlay.add(new Label("Level " + card.getLevel(), skin, "medium_outline"));
        portraitStack.add(levelOverlay);

        column.add(portraitStack).width(320f).height(320f).row();

        column.add(buildSeedPacketBar(card)).width(320f).padTop(10f).row();

        TextButton findMoreButton = new TextButton("FIND MORE", skin, "purple");
        findMoreButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toast.showInfo("Seed packet store is not connected yet.");
            }
        });
        column.add(findMoreButton).width(320f).height(75f).padTop(14f);

        return column;
    }

    private Table buildSeedPacketBar(PlantCard card) {
        Table bar = new Table();

        Image levelIcon = new Image(loadTexture(ASSET_ROOT + "level_tab_gold.png"));
        levelIcon.setScaling(Scaling.fit);
        bar.add(levelIcon).size(48f).padRight(8f);

        ProgressBar progressBar = new ProgressBar(0f, Math.max(1, card.getUpgradeSeedPacketCost()), 4f, false, skin, "xp_yellow");
        progressBar.setValue(card.canUpgrade() ? Math.min(card.getSeedPackets(), card.getUpgradeSeedPacketCost()) : 1f);

        Stack stack = new Stack();
        stack.add(progressBar);

        String text = card.canUpgrade()
                ? card.getSeedPackets() + "/" + card.getUpgradeSeedPacketCost()
                : "MAX LEVEL";
        Label label = new Label(text, skin, "medium_outline");
        Table labelHolder = new Table();
        labelHolder.add(label);
        stack.add(labelHolder);

        bar.add(stack).growX().height(30f);
        return bar;
    }

    // ---------------------------------------------------------------- right column

    private Table buildRightColumn() {
        PlantCollectionView view = current();
        PlantDefinition definition = view.getDefinition();
        PlantCard card = view.getCard();

        Table column = new Table();
        column.top().left();
        column.pad(0f, 10f, 0f, 10f);

        column.add(new Label(definition.getName(), skin, "big_outline")).left().padBottom(18f).row();

        PlantStats currentStats = definition.statsAtLevel(card.getLevel());
        PlantStats nextStats = card.canUpgrade()
                ? definition.statsAtLevel(card.getLevel() + 1)
                : null;

        Table statGrid = new Table();
        statGrid.defaults().left().pad(12f, 0f, 12f, 60f);

        statGrid.add(buildStatRow(ASSET_ROOT + "suncost.png", "SUN COST",
                String.valueOf(currentStats.getCost()),
                nextStats == null ? null : String.valueOf(nextStats.getCost())));
        statGrid.add(buildStatRow(ASSET_ROOT + "recharge.png", "RECHARGE",
                String.valueOf(currentStats.getRecharge()), null));
        statGrid.row();

        statGrid.add(buildStatRow(ASSET_ROOT + "toughness.png", "TOUGHNESS",
                String.valueOf(currentStats.getHp()),
                nextStats == null ? null : String.valueOf(nextStats.getHp())));
        statGrid.add(buildStatRow(ASSET_ROOT + "damage.png", "DAMAGE",
                String.valueOf(currentStats.getDamage()),
                nextStats == null ? null : String.valueOf(nextStats.getDamage())));
        statGrid.row();

        statGrid.add(buildStatRow(ASSET_ROOT + "range.png", "RANGE", "--", null));
        statGrid.add(buildStatRow(ASSET_ROOT + "special.png", "SPECIAL", "--", null));

        column.add(statGrid).left().padBottom(16f).row();

        column.add(buildMintStatsRow()).left().padBottom(16f).row();

        column.add(buildPlantFoodRow(definition)).left().width(520f).padBottom(16f).row();

        // NOTE: the mockup's plain-description line (e.g. "Kernel-pults fling corn
        // kernels...") would read from PlantDefinition's baseAbility field, but that
        // field has no public getter today. Add one (getBaseAbility()) to wire this
        // row back in; omitted for now rather than guessing at an accessor name.

        return column;
    }

    private Table buildStatRow(String iconPath, String label, String currentValue, String nextValue) {
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

        textColumn.add(new Label(label, skin, "medium_outline"))
                .left()
                .row();

        if (showPreview && nextValue != null) {

            Table valueRow = new Table();

            // مقدار فعلی
            valueRow.add(new Label(currentValue, skin, "medium_outline"));

            // فلش
            valueRow.add(new Label(" > ", skin, "medium_outline"));

            // مقدار جدید - سبز
            Label nextLabel = new Label(nextValue, skin, "medium_outline");
            nextLabel.setColor(0f, 1f, 0f, 1f);

            valueRow.add(nextLabel);

            textColumn.add(valueRow).left();

        } else {

            textColumn.add(new Label(currentValue, skin, "medium_outline"))
                    .left();
        }

        row.add(textColumn).left();

        return row;
    }

    /**
     * No family badge / tag icons here: there are no image assets for plant
     * families or tags, and no field on the model maps a regular plant to a
     * specific mint family. This row is just the level-preview toggle.
     */
    private Table buildMintStatsRow() {
        Table row = new Table();

        CheckBox mintStatsBox = new CheckBox(" Show Mint Stats", skin);
        mintStatsBox.setChecked(showPreview);
        mintStatsBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showPreview = mintStatsBox.isChecked();
                buildScreen();
            }
        });
        row.add(mintStatsBox);
        return row;
    }


    private Table buildPlantFoodRow(PlantDefinition definition) {
        Table row = new Table();
        row.left().top();

        Image icon = new Image(loadTexture(ASSET_ROOT + "plantfood.png"));
        icon.setScaling(Scaling.fit);
        row.add(icon).size(58f).padRight(10f).top();

        String effect = definition.getPlantFoodEffect() == null ? "" : definition.getPlantFoodEffect();
        Label label = new Label("Plant Food: " + effect, skin, "medium_outline");
        label.setWrap(true);
        row.add(label).width(480f);
        return row;
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

        overlay.top().padTop(365f);

        overlay.add(previousButton)
                .size(54f, 64f)
                .left()
                .expandX()
                .padLeft(-40f);

        overlay.add(nextButton)
                .size(54f, 64f)
                .right()
                .expandX()
                .padRight(-40f);

        return overlay;
    }
    private ImageButton navArrowButton(String upPath, String downPath) {
        Texture up = loadTexture(upPath);
        Texture down = loadTexture(downPath);
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(up);
        style.imageDown = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(down);
        style.imageOver = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(down);
        return new ImageButton(style);
    }

    // ---------------------------------------------------------------- assets

    private Texture loadTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
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