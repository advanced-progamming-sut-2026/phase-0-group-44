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
import model.Result;
import model.Store;
import model.enums.MenuName;
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
    private static final float CARD_SIZE = 78f;

    private enum Tab { PLANTS, ZOMBIES }
    private enum LockFilter { ALL, UNLOCKED, LOCKED }

    private final PvzGame game;
    private final App app;
    private final CollectionMenuController controller;
    private final MainMenuController mainController;
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

    public CollectionScreen(PvzGame game, App app) {
        this.game = game;
        this.app = app;
        this.controller = app.getCollectionController();
        this.mainController = app.getMainController();
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
        stage.addActor(root);

        root.add(buildTopBar()).growX().row();
        tabBar = buildTabBar();
        root.add(tabBar).growX().padTop(6f).row();
        filterBar = new Table();
        root.add(filterBar).growX().padTop(6f).row();
        gridHost = new Table();
        root.add(gridHost).grow().pad(10f, 18f, 14f, 18f).row();

        rebuildFilterBar();
        refreshGrid();
    }

    // ---------------------------------------------------------------- top bar

    private Table buildTopBar() {
        Table topBar = new Table();
        topBar.pad(16f, 20f, 0f, 20f);

        TextButton backButton = new TextButton("BACK", skin, "brown");
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                goBackToMainMenu();
            }
        });
        topBar.add(backButton).height(46f).left();

        topBar.add(new Label("COLLECTION", skin, "big_outline")).expandX();

        topBar.add(buildResourceArea()).right();
        return topBar;
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

    private void goBackToMainMenu() {
        Store.setCurrentMenu(MenuName.MAIN);
        game.goToScreenForCurrentMenu();
    }

    // ---------------------------------------------------------------- tabs

    private Table buildTabBar() {
        Table tabs = new Table();
        tabs.pad(0f, 20f, 0f, 20f);

        plantsTabButton = new TextButton("PLANTS", skin, "purple");
        plantsTabButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchTab(Tab.PLANTS);
            }
        });

        zombiesTabButton = new TextButton("ZOMBIES", skin, "brown");
        zombiesTabButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchTab(Tab.ZOMBIES);
            }
        });

        tabs.add(plantsTabButton).height(46f).width(180f).padRight(10f);
        tabs.add(zombiesTabButton).height(46f).width(180f);
        return tabs;
    }

    private void switchTab(Tab tab) {
        if (activeTab == tab) {
            return;
        }
        activeTab = tab;
        plantsTabButton.setStyle(skin.get(tab == Tab.PLANTS ? "purple" : "brown", TextButton.TextButtonStyle.class));
        zombiesTabButton.setStyle(skin.get(tab == Tab.ZOMBIES ? "purple" : "brown", TextButton.TextButtonStyle.class));
        familyFilter = "ALL";
        lockFilter = LockFilter.ALL;
        upgradeableOnly = false;
        rebuildFilterBar();
        refreshGrid();
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
        panel.top().left();
        panel.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
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
        PlantCardWidget widget = new PlantCardWidget(skin, CARD_SIZE - 12f, cardNormalBg(), cardGoldBg());
        widget.setIcon(loadPlantIcon(definition.getType()));
        widget.setLocked(!unlocked);
        widget.setBoosted(unlocked && isBoosted(definition.getType()));
        widget.setTopBadge(unlocked ? "LV" + view.getCard().getLevel() : "");
        widget.setBottomLabel(unlocked
                ? view.getCard().getSeedPackets() + "/" + view.getCard().getUpgradeSeedPacketCost()
                : "LOCKED");

        widget.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (unlocked) {
                    game.setScreen(new PlantDetailScreen(game, app, unlockedInOrder, detailIndex));
                } else {
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

    private Texture cardNormalBg() {
        return loadTexture(ASSET_ROOT + "normalBg.png");
    }

    private Texture cardGoldBg() {
        return loadTexture(ASSET_ROOT + "goldBg.png");
    }

    private Table buildZombieCard(ZombieDefinition definition, boolean spotted,
                                  List<ZombieDefinition> spottedInOrder, int detailIndex) {
        Table card = new Table();
        card.setBackground(skin.getDrawable("image_ui_mainmenu_mm_settings_tab_10"));

        Image icon = spotted
                ? icon(loadZombieIcon(definition.getType()), CARD_SIZE - 12f, CARD_SIZE - 12f)
                : icon(loadTexture(ASSET_ROOT + "unseen_slot.png"), CARD_SIZE - 12f, CARD_SIZE - 12f);
        card.add(icon).size(CARD_SIZE - 12f).padTop(6f).row();

        card.add(new Label(spotted ? definition.getName() : "???", skin, "medium_outline")).padTop(2f);

        if (spotted) {
            card.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    game.setScreen(new ZombieDetailScreen(game, app, spottedInOrder, detailIndex));
                }
            });
        }
        return card;
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