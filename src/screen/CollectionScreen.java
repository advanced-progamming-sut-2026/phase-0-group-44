package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import controller.App;
import controller.CollectionMenuController;
import controller.MainMenuController;
import controller.MenuController;
import model.Result;
import model.Store;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.inGame.plant.PlantCollectionView;
import model.inGame.plant.PlantDefinition;
import model.inGame.zombie.ZombieDefinition;
import model.user.User;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Graphical collection screen: Plants / Zombies tabs, a filterable grid, and a
 * detail popup per creature, styled consistently with MainMenuScreen / NewsDialog.
 */
public final class CollectionScreen implements Screen {

    private static final String ASSET_ROOT = "ui/collection/";
    private static final String PLANT_ICON_ROOT = ASSET_ROOT + "plants/";
    private static final String ZOMBIE_ICON_ROOT = ASSET_ROOT + "zombies/";
    private static final String MAIN_MENU_ASSET_ROOT = "ui/mainmenu/";
    private static final int GRID_COLUMNS = 8;
    private static final float CARD_SIZE = 148f;

    private enum Tab { PLANTS, ZOMBIES }
    private enum LockFilter { ALL, UNLOCKED, LOCKED }

    private final PvzGame game;
    private final App app;
    private final CollectionMenuController controller;
    private final MainMenuController mainController;
    private final MenuController menuController;
    private final List<Texture> textures = new ArrayList<>();
    private final Map<String, Texture> iconCache = new LinkedHashMap<>();

    private Stage stage;
    private Skin skin;
    private ToastManager toast;

    private Tab activeTab = Tab.PLANTS;
    private String familyFilter = "ALL";
    private LockFilter lockFilter = LockFilter.ALL;
    private boolean upgradeableOnly;

    private Table root;
    private Table tabBar;
    private Table filterBar;
    private Table gridHost;
    private TextButton plantsTabButton;
    private TextButton zombiesTabButton;
    private Table footerBar;
    private Label collectedLabel;
    private static PlantType focusedPlantType;
    private static ZombieType focusedZombieType;
    private Image plantsTabBg;
    private Image zombiesTabBg;

    private static final float TAB_ICON_SIZE = 64f;

    public CollectionScreen(PvzGame game, App app) {
        this.game = game;
        this.app = app;
        this.controller = app.getCollectionController();
        this.mainController = app.getMainController();
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

    private void buildScreen() {
        root = new Table();
        root.setFillParent(true);
        root.top();
        root.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(loadTexture(ASSET_ROOT + "redBg.png"))));
        stage.addActor(root);

        root.add(buildTopBar()).growX().row();
        tabBar = buildTabBar();
        root.add(tabBar).growX().padTop(6f).row();
        filterBar = new Table();
        root.add(filterBar).growX().padTop(6f).row();
        gridHost = new Table();
        root.add(gridHost).grow().pad(10f, 18f, 14f, 18f).row();
        footerBar = new Table();
        root.add(footerBar).growX().row();

        rebuildFilterBar();
        rebuildFooterBar();
        refreshGrid();
    }


    private Stack buildTabButton(boolean isPlantsTab) {
        Stack stack = new Stack();

        Image background = new Image(cardBgTexture(activeTab == (isPlantsTab ? Tab.PLANTS : Tab.ZOMBIES)));
        if (isPlantsTab) {
            plantsTabBg = background;
        } else {
            zombiesTabBg = background;
        }
        stack.add(background);

        Image icon = new Image(loadTexture(ASSET_ROOT + (isPlantsTab ? "sun.png" : "zombie.png")));
        icon.setScaling(Scaling.fit);
        stack.add(icon);

        stack.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                switchTab(isPlantsTab ? Tab.PLANTS : Tab.ZOMBIES);
            }
        });
        return stack;
    }

    private Texture cardBgTexture(boolean active) {
        return active ? cardSelectedBg() : cardReadyBg();
    }

    // switchTab -- rebuild the footer alongside the filter bar
    private void switchTab(Tab tab) {
        if (activeTab == tab) {
            return;
        }
        activeTab = tab;
        plantsTabBg.setDrawable(new TextureRegionDrawable(cardBgTexture(tab == Tab.PLANTS)));
        zombiesTabBg.setDrawable(new TextureRegionDrawable(cardBgTexture(tab == Tab.ZOMBIES)));
        familyFilter = "ALL";
        lockFilter = LockFilter.ALL;
        upgradeableOnly = false;
        rebuildFilterBar();
        rebuildFooterBar();
        refreshGrid();
    }

    private void rebuildFooterBar() {
        footerBar.clear();
        footerBar.pad(0f, 20f, 10f, 20f);

        collectedLabel = new Label("", skin, "medium_outline");
        footerBar.add(collectedLabel).expandX().right();
    }

    private void addComingSoonListener(Actor actor, String name) {
        actor.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor source) {
                toast.showInfo(name + " is not connected yet.");
            }
        });
    }

    // ---------------------------------------------------------------- top bar

    // buildTopBar -- swap the "BACK" text button for a corner close button
    private Table buildTopBar() {
        Table topBar = new Table();
        topBar.pad(16f, 20f, 0f, 20f);

        Image logo = new Image(loadTexture(ASSET_ROOT + "collectionLogo.png"));
        logo.setScaling(Scaling.fit);
        topBar.add(logo).height(130f).expandX();

        topBar.add(buildResourceArea()).right().padRight(14f);

        ImageButton closeButton = imageButton(ASSET_ROOT + "close_btn.png", ASSET_ROOT + "close_down.png");
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                goBackToAdventure();
            }
        });
        topBar.add(closeButton).size(52f, 52f).right();

        return topBar;
    }

    private ImageButton imageButton(String normalPath, String downPath) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(loadTexture(normalPath));
        style.imageDown = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(loadTexture(downPath));
        style.imageOver = style.imageDown;
        ImageButton button = new ImageButton(style);
        button.getImage().setScaling(Scaling.fit);
        return button;
    }

    private Table buildResourceArea() {
        Table resources = new Table();
        User user = Store.getLoggedInUser();
        int gems = user == null ? 0 : user.getGems();
        int coins = user == null ? 0 : user.getCoins();

        resources.add(icon(loadTexture(MAIN_MENU_ASSET_ROOT + "gem.png"), 30f, 39f)).padRight(5f);
        resources.add(new Label(String.valueOf(gems), skin, "medium_outline")).padRight(18f);
        resources.add(icon(loadTexture(MAIN_MENU_ASSET_ROOT + "coin.png"), 30f, 30f)).padRight(5f);
        resources.add(new Label(String.valueOf(coins), skin, "medium_outline"));
        return resources;
    }

    /** Collection is opened from the Adventure screen's HUD; MenuGraph already
     *  knows COLLECTION exits back to GAME, so defer to it instead of
     *  hardcoding the destination here. */
    private void goBackToAdventure() {
        Result<String> result = menuController.exitMenu();
        if (result.getStatus()) {
            game.goToScreenForCurrentMenu();
        } else {
            toast.showError(result.getMessage());
        }
    }

    // ---------------------------------------------------------------- tabs

    private Table buildTabBar() {
        Table tabs = new Table();
        tabs.left();
        tabs.pad(0f, 20f, 0f, 20f);

        tabs.add(buildTabButton(true)).size(TAB_ICON_SIZE).padRight(10f);
        tabs.add(buildTabButton(false)).size(TAB_ICON_SIZE);
        return tabs;
    }
    // ---------------------------------------------------------------- filters

    private void rebuildFilterBar() {
        filterBar.clear();
        filterBar.pad(0f, 20f, 0f, 20f);

        if (activeTab == Tab.PLANTS) {
            filterBar.add(new Label("Family:", skin, "medium_outline")).padRight(6f);
            SelectBox<String> familyBox = new SelectBox<>(skin);
            familyBox.setItems(collectPlantFamilies());
            familyBox.setSelected(familyFilter);
            familyBox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    familyFilter = familyBox.getSelected();
                    refreshGrid();
                }
            });
            filterBar.add(familyBox).width(220f).padRight(18f);

            filterBar.add(new Label("Show:", skin, "medium_outline")).padRight(6f);
            SelectBox<String> lockBox = new SelectBox<>(skin);
            lockBox.setItems("ALL", "UNLOCKED", "LOCKED");
            lockBox.setSelected(lockFilter.name());
            lockBox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    lockFilter = LockFilter.valueOf(lockBox.getSelected());
                    refreshGrid();
                }
            });
            filterBar.add(lockBox).width(160f).padRight(18f);

            CheckBox upgradeableBox = new CheckBox(" Upgradeable only", skin);
            upgradeableBox.setChecked(upgradeableOnly);
            upgradeableBox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    upgradeableOnly = upgradeableBox.isChecked();
                    refreshGrid();
                }
            });
            filterBar.add(upgradeableBox);
        } else {
            TextButton showAllZombiesButton = new TextButton("SHOW ALL ZOMBIES", skin, "purple");
            showAllZombiesButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    handleShowAllZombies();
                }
            });
            filterBar.add(showAllZombiesButton).height(44f);
        }
    }

    private String[] collectPlantFamilies() {
        Set<String> families = new LinkedHashSet<>();
        families.add("ALL");
        Result<ArrayList<PlantDefinition>> all = controller.showAllPlants();
        if (all.getStatus() && all.getData() != null) {
            for (PlantDefinition definition : all.getData()) {
                if (definition.getCategory() != null) {
                    families.add(definition.getCategory().name());
                }
            }
        }
        return families.toArray(new String[0]);
    }

    // ---------------------------------------------------------------- grid

    private void refreshGrid() {
        gridHost.clear();

        Table panel = new Table();
        panel.top();
        panel.pad(16f);

        if (activeTab == Tab.PLANTS) {
            populatePlantGrid(panel);
        } else {
            populateZombieGrid(panel);
        }

        ScrollPane scrollPane = new ScrollPane(panel, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        gridHost.add(scrollPane).grow();
    }

    // populatePlantGrid -- set the counter from the unfiltered totals, not the
// filtered loop count, so it always reads "197 / 197" regardless of the
// Family/Show/Upgradeable filters currently applied
    private void populatePlantGrid(Table panel) {
        Result<ArrayList<PlantDefinition>> allResult = controller.showAllPlants();
        Result<ArrayList<PlantCollectionView>> ownedResult = controller.showPlants(Store.getLoggedInUser());

        if (!allResult.getStatus() || allResult.getData() == null) {
            panel.add(new Label(allResult.getMessage(), skin, "medium_outline")).pad(20f);
            return;
        }

        Map<PlantType, PlantCollectionView> owned = new LinkedHashMap<>();
        if (ownedResult.getStatus() && ownedResult.getData() != null) {
            for (PlantCollectionView view : ownedResult.getData()) {
                owned.put(view.getDefinition().getType(), view);
            }
        }

        if (collectedLabel != null) {
            collectedLabel.setText("Plants Collected: " + owned.size() + " / " + allResult.getData().size());
        }

        List<PlantCollectionView> unlockedInOrder = new ArrayList<>();
        int column = 0;
        boolean any = false;
        for (PlantDefinition definition : allResult.getData()) {
            PlantCollectionView view = owned.get(definition.getType());
            boolean unlocked = view != null;

            if (!passesPlantFilters(definition, view, unlocked)) {
                continue;
            }
            any = true;

            if (unlocked) {
                unlockedInOrder.add(view);
            }
            int detailIndex = unlockedInOrder.size() - 1;

            panel.add(buildPlantCard(definition, view, unlocked, unlockedInOrder, detailIndex))
                    .size(CARD_SIZE, CARD_SIZE + 24f).pad(6f);
            column++;
            if (column == GRID_COLUMNS) {
                column = 0;
                panel.row();
            }
        }

        if (!any) {
            panel.add(new Label("No plants match this filter.", skin, "medium_outline")).pad(20f);
        }
    }

    private boolean passesPlantFilters(PlantDefinition definition, PlantCollectionView view, boolean unlocked) {
        if (!"ALL".equals(familyFilter)
                && (definition.getCategory() == null || !definition.getCategory().name().equals(familyFilter))) {
            return false;
        }
        if (lockFilter == LockFilter.UNLOCKED && !unlocked) {
            return false;
        }
        if (lockFilter == LockFilter.LOCKED && unlocked) {
            return false;
        }
        if (upgradeableOnly && (view == null || !view.getCard().canUpgrade())) {
            return false;
        }
        return true;
    }

    // populateZombieGrid -- same idea for the zombies tab, and swap the unseen icon
    private void populateZombieGrid(Table panel) {
        Result<ArrayList<ZombieDefinition>> allResult = controller.showAllZombies();
        Result<ArrayList<ZombieDefinition>> seenResult = controller.showZombies(Store.getLoggedInUser());

        if (!allResult.getStatus() || allResult.getData() == null) {
            panel.add(new Label(allResult.getMessage(), skin, "medium_outline")).pad(20f);
            return;
        }

        Set<ZombieType> seen = new LinkedHashSet<>();
        if (seenResult.getStatus() && seenResult.getData() != null) {
            for (ZombieDefinition definition : seenResult.getData()) {
                seen.add(definition.getType());
            }
        }

        if (collectedLabel != null) {
            collectedLabel.setText("Zombies Collected: " + seen.size() + " / " + allResult.getData().size());
        }

        List<ZombieDefinition> spottedInOrder = new ArrayList<>();
        int column = 0;
        for (ZombieDefinition definition : allResult.getData()) {
            boolean spotted = seen.contains(definition.getType());
            if (spotted) {
                spottedInOrder.add(definition);
            }
            int detailIndex = spottedInOrder.size() - 1;

            panel.add(buildZombieCard(definition, spotted, spottedInOrder, detailIndex))
                    .size(CARD_SIZE, CARD_SIZE + 24f).pad(6f);
            column++;
            if (column == GRID_COLUMNS) {
                column = 0;
                panel.row();
            }
        }
    }

    // ---------------------------------------------------------------- cards

    private Table buildPlantCard(PlantDefinition definition, PlantCollectionView view, boolean unlocked,
                                 List<PlantCollectionView> unlockedInOrder, int detailIndex) {
        PlantCardWidget widget = new PlantCardWidget(skin, CARD_SIZE - 12f, cardReadyBg(), cardSelectedBg(), cardGoldBg());
        widget.setIcon(loadPlantIcon(definition.getType()));
        widget.setLocked(!unlocked);
        widget.setBoosted(unlocked && isBoosted(definition.getType()));
        widget.setSelected(definition.getType() == focusedPlantType);
        widget.setTopBadge(unlocked ? "LV" + view.getCard().getLevel() : "");
        widget.setBottomLabel(unlocked
                ? view.getCard().getSeedPackets() + "/" + view.getCard().getUpgradeSeedPacketCost()
                : "LOCKED");

        widget.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                focusedPlantType = definition.getType();
                if (unlocked) {
                    game.setScreen(new PlantDetailScreen(game, app, unlockedInOrder, detailIndex));
                } else {
                    refreshGrid();
                    openPlantPurchase(definition);
                }
            }
        });
        return widget;
    }

    /** Reflects a persistent greenhouse-style boost (User.getPlantBoosts()); the
     *  transient diamond boost from an in-progress plant selection is not shown
     *  here since it only exists within PlantSelectionScreen's active session. */
    private boolean isBoosted(PlantType type) {
        User user = Store.getLoggedInUser();
        if (user == null) {
            return false;
        }
        Integer stored = user.getPlantBoosts().get(type);
        return stored != null && stored > 0;
    }

    private PlantCardWidget buildZombieCard(ZombieDefinition definition, boolean spotted,
                                            List<ZombieDefinition> spottedInOrder, int detailIndex) {
        PlantCardWidget widget = new PlantCardWidget(skin, CARD_SIZE - 12f, cardReadyBg(), cardSelectedBg(), cardGoldBg());
        widget.setIcon(spotted ? loadZombieIcon(definition.getType()) : loadTexture(ASSET_ROOT + "zombie.png"));
        widget.setLocked(!spotted);
        widget.setSelected(spotted && definition.getType() == focusedZombieType);
        widget.setBottomLabel(spotted ? definition.getName() : "???");

        if (spotted) {
            widget.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    focusedZombieType = definition.getType();
                    game.setScreen(new ZombieDetailScreen(game, app, spottedInOrder, detailIndex));
                }
            });
        }
        return widget;
    }
    private Image icon(Texture texture, float width, float height) {
        Image image = new Image(texture);
        image.setScaling(Scaling.fit);
        image.setSize(width, height);
        return image;
    }


    // ---------------------------------------------------------------- detail / purchase

    private void openPlantPurchase(PlantDefinition definition) {
        Dialog confirm = new Dialog("BUY PLANT", skin) {
            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    handlePurchase(definition);
                }
            }
        };
        confirm.getTitleTable().pad(10f, 18f, 8f, 14f);
        Table content = confirm.getContentTable();
        content.pad(24f);
        Label message = new Label("Buy " + definition.getName() + " for 2000 coins?", skin, "medium_outline");
        message.setWrap(true);
        content.add(message).width(320f);
        confirm.button("Buy", Boolean.TRUE);
        confirm.button("Cancel", Boolean.FALSE);
        confirm.show(stage);
        confirm.pack();
        confirm.setPosition(
                Math.round((stage.getWidth() - confirm.getWidth()) / 2f),
                Math.round((stage.getHeight() - confirm.getHeight()) / 2f)
        );
    }

    private void handlePurchase(PlantDefinition definition) {
        Result<PlantCollectionView> result = controller.purchasePlant(Store.getLoggedInUser(), definition.getName());
        if (result.getStatus()) {
            toast.showInfo(result.getMessage());
            refreshGrid();
        } else {
            toast.showError(result.getMessage());
        }
    }

    private void handleShowAllZombies() {
        Result<String> result = controller.cheatSeeAllZombies(Store.getLoggedInUser());
        if (result.getStatus()) {
            toast.showInfo(result.getMessage());
            refreshGrid();
        } else {
            toast.showError(result.getMessage());
        }
    }

    // ---------------------------------------------------------------- assets

    private Texture loadPlantIcon(PlantType type) {
        return loadTexture(PLANT_ICON_ROOT + type.name().toLowerCase() + ".png");
    }

    private Texture loadZombieIcon(ZombieType type) {
        return loadTexture(ZOMBIE_ICON_ROOT + type.name().toLowerCase() + ".png");
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