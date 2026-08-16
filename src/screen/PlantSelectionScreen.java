package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import controller.App;
import controller.CollectionMenuController;
import controller.PlantSelectionController;
import model.Result;
import model.Store;
import model.enums.MenuName;
import model.enums.PlantType;
import model.inGame.GameSession;
import model.inGame.PlantSelection;
import model.inGame.plant.PlantCollectionView;
import model.inGame.plant.PlantDefinition;
import model.user.User;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Full-screen plant-selection UI shown before a level starts (skipped
 * entirely for levels flagged selection-bypassed, per the spec). Reuses
 * PlantCardWidget for both the browse grid and the sidebar of chosen
 * plants, matching the reuse requirement shared with CollectionScreen.
 *
 * ASSUMPTIONS (flagged because some of this isn't confirmed from source):
 *  - Assumes App exposes getPlantSelectionController(), mirroring the other
 *    App.get*Controller() accessors already used elsewhere.
 *  - PlantSelectionController has no getCapacity()/getLevel() getter, so the
 *    sidebar's slot count defaults to 8 (per the phase-1 spec text) rather
 *    than reading the level's real LevelSelectionRules.getCapacity().
 *  - This screen does NOT call PlantSelectionController.begin()/
 *    beginForPlayer() itself -- it has no Level reference. It assumes
 *    whatever screen navigates here (a level-select screen, not seen in
 *    what's been shared) already called one of those first. If
 *    controller.getSelection() is null, this screen shows an error state.
 *  - "normalBg.png" / "goldBg.png" under ASSET_ROOT are assumed filenames
 *    (the message that supplied them had what looked like a typo).
 */
public final class PlantSelectionScreen implements Screen {

    private static final String ASSET_ROOT = "ui/collection/";
    private static final String PLANT_ICON_ROOT = "ui/collection/plants/";
    private static final String MAIN_MENU_ASSET_ROOT = "ui/mainmenu/";
    private static final float GRID_CARD_SIZE = 78f;
    private static final float SIDEBAR_CARD_SIZE = 64f;
    private static final int DEFAULT_CAPACITY = 8;
    private static final int GRID_COLUMNS = 8;

    private final PvzGame game;
    private final App app;
    private final PlantSelectionController controller;
    private final CollectionMenuController collectionController;
    private final List<Texture> textures = new ArrayList<>();
    private final Map<String, Texture> iconCache = new LinkedHashMap<>();

    private Stage stage;
    private Skin skin;
    private ToastManager toast;
    private Table root;
    private Table sidebar;
    private Table grid;
    private Table detailPanel;

    private PlantDefinition focusedDefinition;

    public PlantSelectionScreen(PvzGame game, App app) {
        this.game = game;
        this.app = app;
        this.controller = app.getPlantSelectionController();
        this.collectionController = app.getCollectionController();
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        skin = PvzSkin.get();
        PvzSkinExtras.ensureDialogStyle(skin);
        toast = new ToastManager(stage, skin);
        Gdx.input.setInputProcessor(stage);
        buildScreen();
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

        PlantSelection selection = controller.getSelection();
        if (selection == null) {
            root.add(new Label("Plant selection has not been started.", skin, "medium_outline")).expand().center();
            return;
        }

        root.add(buildTopBar()).growX().colspan(2).row();

        sidebar = new Table();
        sidebar.top();
        root.add(sidebar).width(SIDEBAR_CARD_SIZE + 24f).top().pad(10f, 12f, 10f, 6f);

        Table right = new Table();
        right.top();

        detailPanel = new Table();
        right.add(detailPanel).growX().padBottom(10f).row();

        ScrollPane gridScroll = buildGridScroll();
        right.add(gridScroll).grow().row();

        right.add(buildBottomBar()).growX().padTop(8f);

        root.add(right).grow().pad(10f, 6f, 10f, 12f).row();

        refreshSidebar();
        refreshDetailPanel();
    }

    private Table buildTopBar() {
        Table topBar = new Table();
        topBar.pad(16f, 20f, 0f, 20f);

        TextButton backButton = new TextButton("BACK", skin, "brown");
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Store.setCurrentMenu(MenuName.MAIN);
                game.goToScreenForCurrentMenu();
            }
        });
        topBar.add(backButton).height(46f).left();

        topBar.add(new Label("SELECT YOUR PLANTS", skin, "big_outline")).expandX();

        Table resources = new Table();
        User user = Store.getLoggedInUser();
        int gems = user == null ? 0 : user.getGems();
        int coins = user == null ? 0 : user.getCoins();
        resources.add(icon(loadTexture(MAIN_MENU_ASSET_ROOT + "gem.png"), 30f, 39f)).padRight(5f);
        resources.add(new Label(String.valueOf(gems), skin, "medium_outline")).padRight(18f);
        resources.add(icon(loadTexture(MAIN_MENU_ASSET_ROOT + "coin.png"), 30f, 30f)).padRight(5f);
        resources.add(new Label(String.valueOf(coins), skin, "medium_outline"));
        topBar.add(resources).right();
        return topBar;
    }

    private Table buildBottomBar() {
        Table bottomBar = new Table();
        bottomBar.pad(4f, 0f, 4f, 0f);

        TextButton letsRockButton = new TextButton("LET'S ROCK!", skin, "purple");
        letsRockButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleStartGame();
            }
        });
        bottomBar.add(letsRockButton).height(60f).width(260f).right().expandX();
        return bottomBar;
    }

    // ---------------------------------------------------------------- sidebar

    private void refreshSidebar() {
        sidebar.clear();
        PlantSelection selection = controller.getSelection();

        List<PlantType> chosen = new ArrayList<>();
        for (PlantType type : selection.getChosen()) {
            chosen.add(type);
        }

        for (PlantType type : chosen) {
            sidebar.add(buildSidebarCard(type)).size(SIDEBAR_CARD_SIZE, SIDEBAR_CARD_SIZE + 24f).padBottom(8f).row();
        }
        for (int i = chosen.size(); i < DEFAULT_CAPACITY; i++) {
            sidebar.add(buildEmptySlot()).size(SIDEBAR_CARD_SIZE, SIDEBAR_CARD_SIZE + 24f).padBottom(8f).row();
        }
    }

    private PlantCardWidget buildSidebarCard(PlantType type) {
        PlantDefinition definition = findDefinition(type);
        PlantCardWidget widget = new PlantCardWidget(skin, SIDEBAR_CARD_SIZE - 12f, cardNormalBg(), cardGoldBg());
        if (definition != null) {
            widget.setIcon(loadPlantIcon(type));
            widget.setBottomLabel(String.valueOf(definition.getCost()));
        }
        widget.setBoosted(isBoosted(type));
        widget.setSelected(true);

        widget.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                Result<String> result = controller.removePlant(type.name());
                if (result.getStatus()) {
                    refreshSidebar();
                    refreshGrid();
                    refreshDetailPanel();
                } else {
                    toast.showError(result.getMessage());
                }
            }
        });
        return widget;
    }

    private Table buildEmptySlot() {
        Table slot = new Table();
        slot.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(cardNormalBg())));
        slot.setColor(1f, 1f, 1f, 0.35f);
        return slot;
    }

    // ---------------------------------------------------------------- grid

    private ScrollPane buildGridScroll() {
        grid = new Table();
        grid.top().left();
        grid.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        grid.pad(16f);

        refreshGrid();

        ScrollPane scrollPane = new ScrollPane(grid, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        return scrollPane;
    }

    private void refreshGrid() {
        grid.clear();

        List<PlantDefinition> definitionsResult = collectionController.showAllPlants().getData();
        List<PlantDefinition> allDefinitions = new ArrayList<>(
                definitionsResult == null ? List.<PlantDefinition>of() : definitionsResult);

        Set<PlantType> availableTypes = new HashSet<>();
        Result<List<PlantDefinition>> availableResult = controller.showAvailablePlants();
        if (availableResult.getStatus() && availableResult.getData() != null) {
            for (PlantDefinition definition : availableResult.getData()) {
                availableTypes.add(definition.getType());
            }
        }

        Map<PlantType, String> lockReasons = new HashMap<>();
        Result<List<String>> lockedResult = controller.showLockedPlants();
        if (lockedResult.getStatus() && lockedResult.getData() != null) {
            for (String entry : lockedResult.getData()) {
                int separator = entry.indexOf(" - ");
                if (separator > 0) {
                    String typeName = entry.substring(0, separator);
                    try {
                        PlantType type = PlantType.valueOf(typeName);
                        lockReasons.put(type, entry.substring(separator + 3));
                    } catch (IllegalArgumentException ignored) {
                        // token didn't map to a PlantType; skip rather than guess
                    }
                }
            }
        }

        int column = 0;
        for (PlantDefinition definition : allDefinitions) {
            boolean selectable = availableTypes.contains(definition.getType());
            grid.add(buildGridCard(definition, selectable, lockReasons.get(definition.getType())))
                    .size(GRID_CARD_SIZE, GRID_CARD_SIZE + 24f).pad(6f);
            column++;
            if (column == GRID_COLUMNS) {
                column = 0;
                grid.row();
            }
        }
    }

    private PlantCardWidget buildGridCard(PlantDefinition definition, boolean selectable, String lockReason) {
        PlantCardWidget widget = new PlantCardWidget(skin, GRID_CARD_SIZE - 12f, cardNormalBg(), cardGoldBg());
        widget.setIcon(loadPlantIcon(definition.getType()));
        widget.setLocked(!selectable);
        widget.setBoosted(selectable && isBoosted(definition.getType()));
        widget.setSelected(controller.getSelection().contains(definition.getType()));
        widget.setBottomLabel(selectable ? String.valueOf(definition.getCost()) : "LOCKED");

        PlantCollectionView owned = ownedView(definition.getType());
        widget.setTopBadge(owned == null ? "" : "LV" + owned.getCard().getLevel());

        widget.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (!selectable) {
                    toast.showError(lockReason == null ? "This plant isn't available for this level." : lockReason);
                    return;
                }
                focusedDefinition = definition;
                refreshDetailPanel();
            }
        });
        return widget;
    }

    // ---------------------------------------------------------------- detail panel

    private void refreshDetailPanel() {
        detailPanel.clear();
        detailPanel.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        detailPanel.pad(14f);

        if (focusedDefinition == null) {
            detailPanel.add(new Label("Tap a plant below to select it, upgrade it, or boost it.",
                    skin, "medium_outline")).pad(10f);
            return;
        }

        PlantType type = focusedDefinition.getType();
        PlantCollectionView owned = ownedView(type);

        Image icon = new Image(loadPlantIcon(type));
        icon.setScaling(Scaling.fit);
        detailPanel.add(icon).size(90f).padRight(16f);

        Table info = new Table();
        info.top().left();
        info.add(new Label(focusedDefinition.getName(), skin, "medium_outline")).left().row();
        if (owned != null) {
            info.add(new Label("Level " + owned.getCard().getLevel(), skin, "medium_outline")).left().padTop(4f).row();
        }
        detailPanel.add(info).left().expandX();

        boolean selected = controller.getSelection().contains(type);
        TextButton selectButton = new TextButton(selected ? "REMOVE" : "SELECT", skin, selected ? "brown" : "purple");
        selectButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Result<String> result = selected ? controller.removePlant(type.name()) : controller.addPlant(type.name());
                if (result.getStatus()) {
                    refreshSidebar();
                    refreshGrid();
                    refreshDetailPanel();
                } else {
                    toast.showError(result.getMessage());
                }
            }
        });
        detailPanel.add(selectButton).width(140f).height(48f).padRight(10f);

        if (owned != null && owned.getCard().canUpgrade()) {
            TextButton upgradeButton = new TextButton(
                    "UPGRADE\n" + owned.getCard().getUpgradeCoinCost() + " coins", skin, "green_small");
            upgradeButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Result<PlantCollectionView> result = collectionController.upgradePlant(
                            Store.getLoggedInUser(), focusedDefinition.getName());
                    if (result.getStatus()) {
                        toast.showInfo(result.getMessage());
                        refreshGrid();
                        refreshDetailPanel();
                    } else {
                        toast.showError(result.getMessage());
                    }
                }
            });
            detailPanel.add(upgradeButton).width(150f).height(48f).padRight(10f);
        }

        if (selected) {
            TextButton boostButton = new TextButton(
                    "BOOST\n" + PlantSelectionController.DIAMOND_BOOST_COST + " gems", skin, "purple");
            boostButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Result<String> result = controller.boostPlant(type.name());
                    if (result.getStatus()) {
                        toast.showInfo(result.getMessage());
                        refreshGrid();
                        refreshSidebar();
                        refreshDetailPanel();
                    } else {
                        toast.showError(result.getMessage());
                    }
                }
            });
            detailPanel.add(boostButton).width(140f).height(48f);
        }
    }

    // ---------------------------------------------------------------- actions

    private void handleStartGame() {
        Result<GameSession> result = controller.startGame();
        if (result.getStatus()) {
            toast.showInfo(result.getMessage());
            game.goToScreenForCurrentMenu();
        } else {
            toast.showError(result.getMessage());
        }
    }

    // ---------------------------------------------------------------- helpers

    private PlantDefinition findDefinition(PlantType type) {
        List<PlantDefinition> all = collectionController.showAllPlants().getData();
        if (all == null) {
            return null;
        }
        for (PlantDefinition definition : all) {
            if (definition.getType() == type) {
                return definition;
            }
        }
        return null;
    }

    private PlantCollectionView ownedView(PlantType type) {
        User user = Store.getLoggedInUser();
        if (user == null) {
            return null;
        }
        Result<PlantCollectionView> result = collectionController.showPlant(user, typeDisplayName(type));
        return result.getStatus() ? result.getData() : null;
    }

    private String typeDisplayName(PlantType type) {
        PlantDefinition definition = findDefinition(type);
        return definition == null ? type.name() : definition.getName();
    }

    private boolean isBoosted(PlantType type) {
        User user = Store.getLoggedInUser();
        boolean greenhouseBoosted = user != null && user.getPlantBoosts().get(type) != null
                && user.getPlantBoosts().get(type) > 0;
        boolean diamondBoosted = controller.getSelection() != null && controller.getSelection().isDiamondBoosted(type);
        return greenhouseBoosted || diamondBoosted;
    }

    // ---------------------------------------------------------------- assets

    private Image icon(Texture texture, float width, float height) {
        Image image = new Image(texture);
        image.setScaling(Scaling.fit);
        image.setSize(width, height);
        return image;
    }

    private Texture cardNormalBg() {
        return loadTexture(ASSET_ROOT + "normalBg.png");
    }

    private Texture cardGoldBg() {
        return loadTexture(ASSET_ROOT + "goldBg.png");
    }

    private Texture loadPlantIcon(PlantType type) {
        return loadTexture(PLANT_ICON_ROOT + type.name().toLowerCase(Locale.ROOT) + ".png");
    }

    private Texture loadTexture(String path) {
        Texture cached = iconCache.get(path);
        if (cached != null) {
            return cached;
        }
        Texture texture = new Texture(Gdx.files.internal(path));
        iconCache.put(path, texture);
        textures.add(texture);
        return texture;
    }

    // ---------------------------------------------------------------- Screen

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1f);
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