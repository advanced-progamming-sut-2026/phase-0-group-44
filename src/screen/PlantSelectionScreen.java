package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import controller.App;
import controller.CollectionMenuController;
import controller.MenuController;
import controller.PlantSelectionController;
import model.Result;
import model.Store;
import model.enums.PlantCategory;
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
    private static final float GRID_CARD_SIZE = 98f;
    private static final float SIDEBAR_CARD_SIZE = 78f;
    private static final int DEFAULT_CAPACITY = 8;
    private static final int GRID_COLUMNS = 8;

    private final PvzGame game;
    private final App app;
    private final PlantSelectionController controller;
    private final CollectionMenuController collectionController;
    private final MenuController menuController;
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
        this.menuController = app.getMenuController();
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

        if (controller.isLockedPlantsLevel()) {
            root.add(buildLockedPlantsBanner()).growX().colspan(2)
                    .pad(6f, 20f, 0f, 20f).row();
        }

        sidebar = new Table();
        sidebar.top();

        // Keep the chosen-plant column scrollable so eight slots never force the
        // whole screen taller than the window and push LET'S ROCK off-screen.
        ScrollPane sidebarScroll = new ScrollPane(sidebar, skin);
        sidebarScroll.setFadeScrollBars(false);
        sidebarScroll.setScrollingDisabled(true, false);
        sidebarScroll.setOverscroll(false, false);
        root.add(sidebarScroll)
                .width(SIDEBAR_CARD_SIZE + 30f)
                .growY()
                .minHeight(0f)
                .top()
                .pad(10f, 12f, 4f, 6f);

        Table right = new Table();
        right.top();

        detailPanel = new Table();
        right.add(detailPanel).growX().padBottom(10f).row();

        ScrollPane gridScroll = buildGridScroll();
        right.add(gridScroll).grow().minHeight(0f).row();

        root.add(right)
                .grow()
                .minHeight(0f)
                .pad(10f, 6f, 4f, 12f)
                .row();

        // Reserve a fixed row for the start control. Previously this lived inside
        // the right column after the large plant grid, so on shorter windows the
        // table's preferred height placed it below the visible viewport.
        root.add(buildBottomBar())
                .growX()
                .colspan(2)
                .height(72f)
                .pad(0f, 20f, 8f, 20f)
                .row();

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
                Result<String> result = menuController.exitMenu();
                if (result.getStatus()) {
                    game.goToScreenForCurrentMenu();
                } else {
                    toast.showError(result.getMessage());
                }
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

    private Table buildLockedPlantsBanner() {
        Table banner = new Table();
        banner.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        banner.pad(9f, 14f, 9f, 14f);

        Label title = new Label("LOCKED PLANTS CHALLENGE", skin, "medium_outline");
        title.setColor(1f, 0.82f, 0.30f, 1f);
        banner.add(title).left().padRight(22f);

        String required = controller.getForcedPlants().isEmpty()
                ? "NO REQUIRED PLANTS"
                : "REQUIRED: " + controller.getForcedPlants().stream()
                .map(this::typeDisplayName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        Label requiredLabel = new Label(required, skin);
        requiredLabel.setColor(0.88f, 1f, 0.76f, 1f);
        banner.add(requiredLabel).left().padRight(22f);

        String locked = controller.getExcludedCategories().isEmpty()
                ? ""
                : "LOCKED: " + controller.getExcludedCategories().stream()
                .map(PlantCategory::name)
                .map(name -> name.replace('_', ' '))
                .reduce((a, b) -> a + ", " + b)
                .orElse("") + " PLANTS";
        Label lockedLabel = new Label(locked, skin);
        lockedLabel.setColor(1f, 0.58f, 0.45f, 1f);
        banner.add(lockedLabel).left().expandX();

        Label hint = new Label("Required plants cannot be removed.", skin);
        hint.setColor(0.78f, 0.82f, 0.82f, 1f);
        banner.add(hint).right();
        return banner;
    }

    private Table buildBottomBar() {
        Table bottomBar = new Table();
        bottomBar.pad(4f, 0f, 4f, 0f);

        User user = Store.getLoggedInUser();
        boolean debugMode = user != null
                && user.getSettings() != null
                && user.getSettings().isDebugMode();

        if (debugMode) {
            TextButton unlockAllButton = new TextButton(
                    "DEBUG: UNLOCK ALL PLANTS",
                    skin,
                    "green_small"
            );
            unlockAllButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Result<String> result =
                            collectionController.cheatBuyAllPlants(Store.getLoggedInUser());
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
            bottomBar.add(unlockAllButton)
                    .height(52f)
                    .width(250f)
                    .left()
                    .padRight(18f);
        }

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
        int capacity = Math.max(chosen.size(), controller.getSelectionCapacity());
        for (int i = chosen.size(); i < capacity; i++) {
            sidebar.add(buildEmptySlot()).size(SIDEBAR_CARD_SIZE, SIDEBAR_CARD_SIZE + 24f).padBottom(8f).row();
        }
    }

    private PlantCardWidget buildSidebarCard(PlantType type) {
        PlantDefinition definition = findDefinition(type);
        // buildSidebarCard
        PlantCardWidget widget = new PlantCardWidget(skin, SIDEBAR_CARD_SIZE - 12f,
                cardReadyBg(), cardSelectedBg(), cardGoldBg());
        if (definition != null) {
            widget.setIcon(loadPlantIcon(type));
            widget.setBottomLabel(String.valueOf(definition.getCost()));
        }
        boolean forced = controller.isForcedPlant(type);
        widget.setBoosted(isBoosted(type));
        widget.setSelected(true);
        if (forced) {
            widget.setTopBadge("REQ");
            widget.setBottomLabel("REQUIRED");
        }

        widget.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (forced) {
                    toast.showInfo(typeDisplayName(type) + " is required by this level.");
                    return;
                }
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
                new com.badlogic.gdx.graphics.g2d.TextureRegion(cardReadyBg())));
        slot.setColor(1f, 1f, 1f, 0.35f);
        return slot;
    }

    // ---------------------------------------------------------------- grid

    private ScrollPane buildGridScroll() {
        grid = new Table();
        grid.top();
        grid.setBackground(new TextureRegionDrawable(
                new TextureRegion(loadTexture(ASSET_ROOT + "plantDetailBg.png"))
        ));
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
        // buildGridCard
        PlantCardWidget widget = new PlantCardWidget(skin, GRID_CARD_SIZE - 12f,
                cardReadyBg(), cardSelectedBg(), cardGoldBg());
        widget.setIcon(loadPlantIcon(definition.getType()));
        boolean forced = controller.isForcedPlant(definition.getType());
        widget.setLocked(!selectable);
        widget.setBoosted(selectable && isBoosted(definition.getType()));
        widget.setSelected(controller.getSelection().contains(definition.getType()));
        widget.setBottomLabel(!selectable ? "LOCKED"
                : forced ? "REQUIRED" : String.valueOf(definition.getCost()));

        PlantCollectionView owned = ownedView(definition.getType());
        widget.setTopBadge(forced ? "REQ" : owned == null ? "" : "LV" + owned.getCard().getLevel());

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
            info.add(new Label("Level " + owned.getCard().getLevel(), skin, "medium_outline"))
                    .left().padTop(4f).row();
            if (owned.getCard().canUpgrade()) {
                int currentSeeds = owned.getCard().getSeedPackets();
                int requiredSeeds = owned.getCard().getUpgradeSeedPacketCost();
                int requiredCoins = owned.getCard().getUpgradeCoinCost();
                info.add(new Label("Seed packets: " + currentSeeds + " / " + requiredSeeds, skin))
                        .left().padTop(3f).row();
                info.add(new Label("Upgrade cost: " + requiredCoins + " coins", skin))
                        .left().padTop(2f).row();
            } else {
                info.add(new Label("MAX LEVEL", skin, "medium_outline")).left().padTop(3f).row();
            }
        }
        detailPanel.add(info).left().expandX();

        boolean selected = controller.getSelection().contains(type);
        boolean forced = controller.isForcedPlant(type);
        TextButton selectButton = new TextButton(
                forced ? "REQUIRED" : selected ? "REMOVE" : "SELECT",
                skin, forced ? "green_small" : selected ? "brown" : "purple");
        if (forced) {
            selectButton.setDisabled(true);
        } else {
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
        }
        detailPanel.add(selectButton).width(140f).height(48f).padRight(10f);

        if (owned != null && owned.getCard().canUpgrade()) {
            User user = Store.getLoggedInUser();
            int seedPackets = owned.getCard().getSeedPackets();
            int seedCost = owned.getCard().getUpgradeSeedPacketCost();
            int coinCost = owned.getCard().getUpgradeCoinCost();
            int coins = user == null ? 0 : user.getCoins();
            boolean enoughSeeds = seedPackets >= seedCost;
            boolean enoughCoins = coins >= coinCost;
            boolean upgradeReady = enoughSeeds && enoughCoins;

            String upgradeText;
            if (!enoughSeeds) {
                upgradeText = "NEED " + (seedCost - seedPackets) + " SEEDS";
            } else if (!enoughCoins) {
                upgradeText = "NEED " + (coinCost - coins) + " COINS";
            } else {
                upgradeText = "UPGRADE";
            }

            TextButton upgradeButton = new TextButton(upgradeText, skin,
                    upgradeReady ? "green_small" : "brown");
            upgradeButton.setDisabled(!upgradeReady);
            if (upgradeReady) {
                upgradeButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        Result<PlantCollectionView> result = collectionController.upgradePlant(
                                Store.getLoggedInUser(), focusedDefinition.getName());
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
            }
            detailPanel.add(upgradeButton).width(170f).height(48f).padRight(10f);
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


    private Texture cardReadyBg() {
        return loadTexture(ASSET_ROOT + "ready.png");
    }

    private Texture cardSelectedBg() {
        return loadTexture(ASSET_ROOT + "selected.png");
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
        Texture texture = TextureQuality.load(path);
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