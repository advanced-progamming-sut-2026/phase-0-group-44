package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import controller.App;
import controller.MenuController;
import controller.ShopController;
import model.Result;
import model.Store;
import model.enums.MenuName;
import model.enums.PlantType;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRegistry;
import model.miniGame.GreenHouse;
import model.shop.ShopItem;
import model.user.DailyShopState;
import model.user.PlantCard;
import model.user.User;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Phase-2 graphical Shop.
 *
 * Phase-1 ShopController remains the source of truth.  The top row is now a
 * real category selector, like Figure 9: choosing a tab changes the catalogue
 * below it instead of treating every product as a tab.
 */
public final class ShopScreen implements Screen {

    private static final float WIDTH = 1280f;
    private static final float HEIGHT = 720f;
    private static final String ROOT = "ui/shop/";
    private static final String MAIN_MENU_ROOT = "ui/mainmenu/";

    private static final String PAM_SUNFLOWER = "768/INITIAL/PLANT/SUNFLOWER/SUNFLOWER.PAM";
    private static final String PAM_PEASHOOTER = "768/INITIAL/PLANT/PEASHOOTER/PEASHOOTER.PAM";
    private static final String PAM_WALLNUT = "768/INITIAL/PLANT/WALLNUT/WALLNUT.PAM";
    private static final String PAM_PUFFSHROOM = "768/INITIAL/PLANT/PUFFSHROOM/PUFFSHROOM.PAM";
    private static final String PAM_LILYPAD = "768/FULL/PLANT/LILYPAD/LILYPAD.PAM";
    private static final String PAM_POTATOMINE = "768/INITIAL/PLANT/POTATOMINE/POTATOMINE.PAM";
    private static final String PAM_BONKCHOY = "768/INITIAL/PLANT/BONKCHOY/BONKCHOY.PAM";
    private static final String PAM_KERNELPULT = "768/INITIAL/PLANT/KERNALPULT/KERNALPULT.PAM";

    private static final Color BODY_TEXT = new Color(0.28f, 0.17f, 0.07f, 1f);

    // Compact layout approved for the moonlit-garden shop background.
    private static final float PANEL_X = 180f;
    private static final float PANEL_Y = 120f;
    private static final float PANEL_W = 920f;
    private static final float PANEL_H = 455f;
    private static final float PRODUCT_CARD_SCALE = 0.65f;
    private static final float PRODUCT_CARD_W = 194f * PRODUCT_CARD_SCALE;
    private static final float PRODUCT_CARD_H = 390f * PRODUCT_CARD_SCALE;

    private final PvzGame game;
    private final App app;
    private final ShopController shopController;
    private final MenuController menuController;
    private final Clock clock;
    private final Map<String, Texture> textures = new LinkedHashMap<>();
    private final Set<String> loadedPams = new HashSet<>();

    private Stage stage;
    private Skin skin;
    private ToastManager toast;
    private ShopCategory category = ShopCategory.ALL;
    private Group modal;
    private Label dailyTimerLabel;
    private float timerAccumulator;

    private TextureBank pamTextures;
    private PamPlayer pamPlayer;
    private boolean pamAvailable;

    public ShopScreen(PvzGame game, App app) {
        this.game = game;
        this.app = app;
        this.shopController = app.getShopController();
        this.menuController = app.getMenuController();
        this.clock = app.getUserService().getClock();
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(WIDTH, HEIGHT));
        skin = PvzSkin.get();
        toast = new ToastManager(stage, skin);
        Gdx.input.setInputProcessor(stage);
        initializePam();
        category = ShopCategory.ALL;
        rebuild();
    }

    private void initializePam() {
        try {
            FileHandle assets = Gdx.files.internal("pvz-asset-browser/pvz-assets");
            if (!assets.exists()) {
                assets = Gdx.files.local("pvz-asset-browser/pvz-assets");
            }
            if (!assets.exists()) {
                pamAvailable = false;
                return;
            }
            pamTextures = new TextureBank("768", assets);
            pamPlayer = new PamPlayer(pamTextures, assets);
            pamAvailable = true;
        } catch (RuntimeException ex) {
            pamAvailable = false;
            Gdx.app.error("ShopScreen", "Could not initialize libPVZ plant rendering", ex);
        }
    }

    private void rebuild() {
        stage.clear();
        modal = null;
        dailyTimerLabel = null;

        // Sync the persisted daily offer with the system date before rendering.
        shopController.daily();

        stage.addActor(buildBackground());
        stage.addActor(buildMainPanel());
        stage.addActor(buildTopHud());
    }

    private Actor buildBackground() {
        Image image = new Image(loadTexture(ROOT + "background.png"));
        image.setScaling(Scaling.fill);
        image.setBounds(0f, 0f, WIDTH, HEIGHT);
        return image;
    }

    private Group buildMainPanel() {
        Group group = new Group();
        group.setSize(WIDTH, HEIGHT);

        Image panel = new Image(loadTexture(ROOT + "panel.png"));
        panel.setScaling(Scaling.stretch);
        panel.setBounds(PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
        group.addActor(panel);

        // Real clickable category tabs remain above the panel.
        addCategoryTabs(group);

        // Keep the heading simple. The previous patch used a second stretched strip here,
        // which is what produced the large empty/orange band in the screenshot.
        Label heading = new Label(category.heading(), skin, "medium_outline");
        heading.setAlignment(Align.center);
        heading.setColor(BODY_TEXT);
        heading.setBounds(PANEL_X + 70f, PANEL_Y + PANEL_H - 46f, PANEL_W - 140f, 30f);
        group.addActor(heading);

        List<ProductEntry> products = productsForCategory();
        if (products.isEmpty()) {
            Label empty = new Label("No items are available in this category.", skin, "medium_outline");
            empty.setAlignment(Align.center);
            empty.setBounds(PANEL_X + 60f, PANEL_Y + 175f, PANEL_W - 120f, 80f);
            group.addActor(empty);
            return group;
        }

        // Keep the catalogue comfortably below the title area.
        float rowY = PANEL_Y + 10f;

        // Five-card pages use explicit positioning. This prevents the frames/shadows
        // from visually touching each other and gives the horizontal breathing room
        // approved in the mockup.
        if (products.size() <= 5) {
            final float cardGap = 44f;
            float totalWidth = products.size() * PRODUCT_CARD_W
                    + Math.max(0, products.size() - 1) * cardGap;
            float rowX = PANEL_X + (PANEL_W - totalWidth) / 2f - 22f;
            for (int i = 0; i < products.size(); i++) {
                Actor card = buildCompactProductCard(products.get(i));
                card.setPosition(rowX + i * (PRODUCT_CARD_W + cardGap), rowY);
                group.addActor(card);
            }
        } else {
            // Only long seed catalogues need horizontal scrolling.
            Table catalogue = new Table();
            catalogue.left();
            catalogue.defaults().padRight(36f);
            for (ProductEntry entry : products) {
                catalogue.add(buildCompactProductCard(entry))
                        .width(PRODUCT_CARD_W).height(PRODUCT_CARD_H);
            }
            catalogue.pack();
            ScrollPane scroll = new ScrollPane(catalogue, skin);
            scroll.setScrollingDisabled(false, true);
            scroll.setFadeScrollBars(true);
            scroll.setOverscroll(false, false);
            scroll.setClamp(true);
            scroll.setBounds(PANEL_X + 28f, rowY - 3f,
                    PANEL_W - 56f, PRODUCT_CARD_H + 16f);
            group.addActor(scroll);
        }

        return group;
    }

    private Actor buildCompactProductCard(ProductEntry entry) {
        Group wrapper = new Group();
        wrapper.setSize(PRODUCT_CARD_W, PRODUCT_CARD_H);

        Actor card = buildProductCard(entry);
        card.setBounds(0f, 0f, 194f, 390f);
        card.setOrigin(0f, 0f);
        card.setScale(PRODUCT_CARD_SCALE);
        wrapper.addActor(card);
        return wrapper;
    }

    private void addCategoryTabs(Group group) {
        ShopCategory[] categories = ShopCategory.values();

        // Compact, centered tab strip like the approved mockup.
        // The active texture is still the only one that contains the downward arrow.
        final float tabW = 88f;
        final float tabH = 72f;
        final float tabGap = 2f;
        final float totalTabsW = categories.length * tabW
                + Math.max(0, categories.length - 1) * tabGap;
        final float startX = PANEL_X + (PANEL_W - totalTabsW) / 2f;
        final float y = PANEL_Y + PANEL_H - 31f;

        for (int i = 0; i < categories.length; i++) {
            ShopCategory tabCategory = categories[i];
            ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
            style.up = new TextureRegionDrawable(loadTexture(ROOT + tabCategory.normalAsset()));
            style.down = new TextureRegionDrawable(loadTexture(ROOT + tabCategory.activeAsset()));
            style.checked = new TextureRegionDrawable(loadTexture(ROOT + tabCategory.activeAsset()));

            ImageButton tab = new ImageButton(style);
            tab.setChecked(tabCategory == category);
            tab.setBounds(startX + i * (tabW + tabGap), y, tabW, tabH);
            tab.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (category != tabCategory) {
                        category = tabCategory;
                        rebuild();
                    } else {
                        tab.setChecked(true);
                    }
                }
            });
            group.addActor(tab);
        }
    }

    private List<ProductEntry> productsForCategory() {
        List<ProductEntry> products = new ArrayList<>();
        switch (category) {
            case ALL -> {
                products.add(ProductEntry.fixed(ShopItem.POT));
                products.add(ProductEntry.fixed(ShopItem.PLANT_FOOD));
                products.add(ProductEntry.fixed(ShopItem.RANDOM_SEEDS));
                products.add(ProductEntry.selectedChooser());
                products.add(ProductEntry.fixed(ShopItem.COIN_PACK));
            }
            case GREENHOUSE -> products.add(ProductEntry.fixed(ShopItem.POT));
            case BOOSTS -> products.add(ProductEntry.fixed(ShopItem.PLANT_FOOD));
            case SEEDS -> {
                products.add(ProductEntry.fixed(ShopItem.RANDOM_SEEDS));
                for (PlantDefinition plant : unlockedPlants()) {
                    products.add(ProductEntry.selectedPlant(plant));
                }
            }
            case CURRENCY -> products.add(ProductEntry.fixed(ShopItem.COIN_PACK));
            case DAILY -> {
                PlantDefinition plant = dailyPlant();
                if (plant != null) {
                    products.add(ProductEntry.daily(plant));
                }
            }
        }
        return products;
    }

    private Actor buildProductCard(ProductEntry entry) {
        Stack stack = new Stack();
        Image background = new Image(loadTexture(ROOT
                + (entry.item() == ShopItem.DAILY ? "card_daily.png" : "card.png")));
        background.setScaling(Scaling.stretch);
        stack.add(background);

        Group content = new Group();
        content.setSize(194f, 390f);

        Label title = new Label(entryTitle(entry), skin, "medium_outline");
        title.setWrap(true);
        title.setAlignment(Align.center);
        title.setFontScale(0.80f);
        title.setBounds(14f, 340f, 166f, 34f);
        content.addActor(title);

        String exactVisual = exactPermanentVisual(entry);
        if (exactVisual != null) {
            // These five images are direct crops from the approved first mockup,
            // so the item artwork matches it exactly rather than being re-drawn.
            Image display = new Image(loadTexture(ROOT + exactVisual));
            display.setScaling(Scaling.fit);
            display.setBounds(20f, 195f, 154f, 138f);
            content.addActor(display);
        } else {
            Image display = new Image(loadTexture(ROOT + "image_slot.png"));
            display.setScaling(Scaling.stretch);
            display.setBounds(20f, 195f, 154f, 138f);
            content.addActor(display);
            addProductArtwork(content, entry);
        }

        Label description = new Label(entryDescription(entry), skin);
        description.setColor(BODY_TEXT);
        description.setWrap(true);
        description.setAlignment(Align.center);
        description.setBounds(20f, 104f, 154f, 80f);
        content.addActor(description);

        if (entry.item() == ShopItem.DAILY) {
            Label timer = new Label(dailyStatusLine(), skin, "medium_outline");
            timer.setAlignment(Align.center);
            timer.setFontScale(0.72f);
            timer.setColor(Color.WHITE);
            timer.setBounds(38f, 195f, 118f, 22f);
            content.addActor(timer);
            dailyTimerLabel = timer;
        }

        addPrice(content, entry);

        String buttonText = entry.chooser() ? "CHOOSE" :
                (entry.item() == ShopItem.DAILY && dailyPurchased() ? "SOLD TODAY" : "BUY");
        TextButton buy = new TextButton(buttonText, skin,
                entry.item() == ShopItem.DAILY ? "brown" : "green");
        buy.setBounds(38f, 17f, 118f, 31f);
        buy.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleProductClick(entry);
            }
        });
        content.addActor(buy);

        stack.add(content);
        return stack;
    }

    private String exactPermanentVisual(ProductEntry entry) {
        if (entry.item() == ShopItem.POT) return "product_pot_exact.png";
        if (entry.item() == ShopItem.PLANT_FOOD) return "product_food_exact.png";
        if (entry.item() == ShopItem.RANDOM_SEEDS) return "product_random_exact.png";
        if (entry.item() == ShopItem.COIN_PACK) return "product_coin_exact.png";
        if (entry.item() == ShopItem.SELECTED_SEEDS && entry.chooser()) {
            return "product_selected_exact.png";
        }
        return null;
    }

    private void addProductArtwork(Group content, ProductEntry entry) {
        if (entry.plant() != null && (entry.item() == ShopItem.SELECTED_SEEDS || entry.item() == ShopItem.DAILY)) {
            addPlantArtwork(content, entry.plant());
            return;
        }

        switch (entry.item()) {
            case POT -> addCenteredImage(content, ROOT + "pot_real.png", 46f, 220f, 105f, 95f);
            case PLANT_FOOD -> addCenteredImage(content, ROOT + "plant_food_real.png", 52f, 222f, 92f, 92f);
            case RANDOM_SEEDS -> addCenteredImage(content, ROOT + "random_packet_real.png", 58f, 218f, 80f, 100f);
            case SELECTED_SEEDS -> {
                // The ALL page shows this as a gateway into the plant catalogue.
                addCenteredImage(content, ROOT + "random_packet_real.png", 34f, 222f, 70f, 92f);
                PlantDefinition plant = firstUnlockedPlant();
                if (plant != null) {
                    addPlantArtworkAt(content, plant, 88f, 216f, 90f, 110f, 0.72f);
                }
            }
            case COIN_PACK -> addCurrencyArtwork(content);
            case DAILY -> { }
        }
    }

    private void addCurrencyArtwork(Group content) {
        Image gem = new Image(loadTexture(MAIN_MENU_ROOT + "gem.png"));
        gem.setScaling(Scaling.fit);
        gem.setBounds(42f, 247f, 45f, 54f);
        content.addActor(gem);

        Label arrow = new Label(">", skin, "medium_outline");
        arrow.setAlignment(Align.center);
        arrow.setBounds(84f, 254f, 30f, 35f);
        content.addActor(arrow);

        Image coin1 = new Image(loadTexture(MAIN_MENU_ROOT + "coin.png"));
        coin1.setScaling(Scaling.fit);
        coin1.setBounds(112f, 246f, 46f, 46f);
        content.addActor(coin1);
        Image coin2 = new Image(loadTexture(MAIN_MENU_ROOT + "coin.png"));
        coin2.setScaling(Scaling.fit);
        coin2.setBounds(129f, 266f, 34f, 34f);
        content.addActor(coin2);
    }

    private void addPlantArtwork(Group content, PlantDefinition plant) {
        addPlantArtworkAt(content, plant, 24f, 205f, 146f, 125f, 1f);
    }

    private void addPlantArtworkAt(Group content, PlantDefinition plant,
                                   float x, float y, float width, float height, float multiplier) {
        PamSpec spec = pamSpec(plant.getType());
        if (pamAvailable && spec != null) {
            ensurePamLoaded(spec.path());
            PamPlantActor actor = new PamPlantActor(
                    pamPlayer,
                    spec.path(),
                    spec.clip(),
                    spec.scale() * multiplier,
                    spec.yOffset()
            );
            actor.setBounds(x, y, width, height);
            content.addActor(actor);
        } else {
            Image fallback = new Image(loadTexture(ROOT + "plant_food_real.png"));
            fallback.setScaling(Scaling.fit);
            fallback.setBounds(x + width * 0.2f, y + 10f, width * 0.6f, height * 0.75f);
            content.addActor(fallback);
        }
    }

    private void addCenteredImage(Group content, String path,
                                  float x, float y, float width, float height) {
        Image image = new Image(loadTexture(path));
        image.setScaling(Scaling.fit);
        image.setBounds(x, y, width, height);
        content.addActor(image);
    }

    private void addPrice(Group content, ProductEntry entry) {
        ShopItem item = entry.item();
        Stack price = new Stack();
        Image plate = new Image(loadTexture(ROOT + (item.getCurrency() == ShopItem.Currency.COINS
                ? "price_coin.png" : "price_gem.png")));
        plate.setScaling(Scaling.stretch);
        price.add(plate);

        Table row = new Table();
        Image icon = new Image(loadTexture(item.getCurrency() == ShopItem.Currency.COINS
                ? MAIN_MENU_ROOT + "coin.png" : MAIN_MENU_ROOT + "gem.png"));
        icon.setScaling(Scaling.fit);
        row.add(icon).size(17f, 17f).padRight(5f);
        Label amount = new Label(String.format(Locale.US, "%,d", item.getUnitPrice()),
                skin, "medium_outline");
        amount.setFontScale(0.86f);
        row.add(amount);
        price.add(row);

        price.setBounds(35f, 56f, 124f, 34f);
        content.addActor(price);
    }

    private void handleProductClick(ProductEntry entry) {
        if (entry.chooser()) {
            category = ShopCategory.SEEDS;
            rebuild();
            return;
        }
        String plantName = entry.plant() == null ? null : entry.plant().getName();
        String reason = purchaseBlockReason(entry.item(), plantName);
        if (reason != null) {
            toast.showError(reason);
            return;
        }
        showConfirmation(entry);
    }

    private void showConfirmation(ProductEntry entry) {
        modal = modalShell("confirm_panel.png", 560f, 290f);

        Label title = new Label("PURCHASE CONFIRMATION", skin, "medium_outline");
        title.setAlignment(Align.center);
        title.setBounds(370f, 446f, 540f, 40f);
        modal.addActor(title);

        String product = entry.plant() != null && entry.item() == ShopItem.SELECTED_SEEDS
                ? ShopItem.SELECTED_BUNDLE_PACKETS + " seed packets for " + entry.plant().getName()
                : confirmationProduct(entry.item());
        Label prompt = new Label("Purchase " + product + "?\n" + priceText(entry.item()), skin);
        prompt.setWrap(true);
        prompt.setAlignment(Align.center);
        prompt.setColor(BODY_TEXT);
        prompt.setBounds(410f, 352f, 460f, 70f);
        modal.addActor(prompt);

        TextButton cancel = new TextButton("CANCEL", skin, "brown");
        cancel.setBounds(470f, 292f, 150f, 40f);
        cancel.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                closeModal();
            }
        });
        modal.addActor(cancel);

        TextButton confirm = new TextButton("BUY", skin, "green");
        confirm.setBounds(660f, 292f, 150f, 40f);
        confirm.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                performPurchase(entry);
            }
        });
        modal.addActor(confirm);

        stage.addActor(modal);
    }

    private Group modalShell(String asset, float width, float height) {
        Group group = new Group();
        group.setSize(WIDTH, HEIGHT);

        Image overlay = new Image(loadTexture(ROOT + "overlay.png"));
        overlay.setScaling(Scaling.stretch);
        overlay.setBounds(0f, 0f, WIDTH, HEIGHT);
        group.addActor(overlay);

        Image panel = new Image(loadTexture(ROOT + asset));
        panel.setScaling(Scaling.stretch);
        panel.setBounds((WIDTH - width) / 2f, (HEIGHT - height) / 2f, width, height);
        group.addActor(panel);
        return group;
    }

    private void performPurchase(ProductEntry entry) {
        String plantName = entry.plant() == null ? null : entry.plant().getName();
        String reason = purchaseBlockReason(entry.item(), plantName);
        if (reason != null) {
            closeModal();
            toast.showError(reason);
            return;
        }

        Result<String> result = shopController.buy(entry.item().getId(), 1, plantName);
        closeModal();
        if (result.getStatus()) {
            rebuild();
            toast.showInfo(result.getMessage());
        } else {
            toast.showError(result.getMessage());
        }
    }

    private Table buildTopHud() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().pad(12f, 22f, 0f, 22f);

        TextButton back = new TextButton("<", skin, "brown");
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                navigateBack();
            }
        });
        hud.add(back).size(58f, 46f);

        hud.add().expandX();

        User user = Store.getLoggedInUser();
        int gems = user == null ? 0 : user.getGems();
        int coins = user == null ? 0 : user.getCoins();
        hud.add(wallet(MAIN_MENU_ROOT + "gem.png", gems)).size(150f, 48f).padRight(10f);
        hud.add(wallet(MAIN_MENU_ROOT + "coin.png", coins)).size(150f, 48f);

        // Figure-style layout uses only Back navigation; there is deliberately no X button.
        return hud;
    }

    private Stack wallet(String iconPath, int value) {
        Stack stack = new Stack();
        Image background = new Image(loadTexture(ROOT + "wallet_box.png"));
        background.setScaling(Scaling.stretch);
        stack.add(background);

        Table row = new Table();
        Image icon = new Image(loadTexture(iconPath));
        icon.setScaling(Scaling.fit);
        row.add(icon).size(27f, 27f).padRight(6f);
        Label amount = new Label(String.format(Locale.US, "%,d", value), skin, "medium_outline");
        amount.setFontScale(0.90f);
        row.add(amount);
        stack.add(row);
        return stack;
    }

    private void navigateBack() {
        Result<String> result = menuController.exitMenu();
        if (!result.getStatus() || Store.getCurrentMenu() != MenuName.GREENHOUSE) {
            // Keep the model menu state coherent even if a previous graphical
            // shortcut opened Shop without entering the Phase-1 menu first.
            Store.setCurrentMenu(MenuName.GREENHOUSE);
        }
        game.setScreen(new GreenhouseScreen(game, app));
    }

    private String purchaseBlockReason(ShopItem item, String plantName) {
        User user = Store.getLoggedInUser();
        if (user == null) {
            return "You must log in first.";
        }

        if (item == ShopItem.POT
                && user.getGreenHouse().getSlotCount() >= GreenHouse.TOTAL_SLOTS) {
            return "You already own all greenhouse pots.";
        }
        if (item == ShopItem.PLANT_FOOD
                && user.getPlantFood() >= ShopItem.MAX_STORED_PLANT_FOOD) {
            return "You cannot store more than " + ShopItem.MAX_STORED_PLANT_FOOD
                    + " plant foods.";
        }
        if (item == ShopItem.RANDOM_SEEDS && unlockedPlants().isEmpty()) {
            return "No unlocked plant is available.";
        }
        if (item == ShopItem.SELECTED_SEEDS) {
            if (plantName == null || plantName.isBlank()) {
                return "Choose an unlocked plant first.";
            }
            PlantDefinition selected = PlantRegistry.getDefault().findByName(plantName);
            if (selected == null || !user.getCollection().hasPlant(selected.getType())) {
                return "That plant is not unlocked.";
            }
        }
        if (item == ShopItem.DAILY) {
            DailyShopState state = user.getDailyShop();
            if (state.getOfferPlant() == null) {
                return "No daily offer is available.";
            }
            if (state.getOfferId() != null && state.isPurchased(state.getOfferId())) {
                return "The daily offer can be purchased only once per day.";
            }
        }

        long balance = item.getCurrency() == ShopItem.Currency.COINS
                ? user.getCoins() : user.getGems();
        if (balance < item.getUnitPrice()) {
            return "Not enough " + item.getCurrency().getLabel() + ".";
        }
        return null;
    }

    private List<PlantDefinition> unlockedPlants() {
        List<PlantDefinition> unlocked = new ArrayList<>();
        User user = Store.getLoggedInUser();
        if (user == null) {
            return unlocked;
        }
        for (PlantDefinition definition : PlantRegistry.getDefault().findAll()) {
            if (user.getCollection().hasPlant(definition.getType())) {
                unlocked.add(definition);
            }
        }
        return unlocked;
    }

    private PlantDefinition firstUnlockedPlant() {
        List<PlantDefinition> plants = unlockedPlants();
        return plants.isEmpty() ? null : plants.get(0);
    }

    private PlantDefinition dailyPlant() {
        User user = Store.getLoggedInUser();
        if (user == null) {
            return null;
        }
        DailyShopState state = user.getDailyShop();
        if (state.getOfferPlant() == null) {
            return null;
        }
        try {
            return PlantRegistry.getDefault().findByType(PlantType.valueOf(state.getOfferPlant()));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String entryTitle(ProductEntry entry) {
        if (entry.plant() != null) {
            return entry.plant().getName().toUpperCase(Locale.ROOT);
        }
        return switch (entry.item()) {
            case POT -> "GREENHOUSE POT";
            case PLANT_FOOD -> "PLANT FOOD";
            case RANDOM_SEEDS -> "RANDOM SEEDS";
            case SELECTED_SEEDS -> "SELECTED SEEDS";
            case COIN_PACK -> "COIN PACK";
            case DAILY -> "DAILY OFFER";
        };
    }

    private String entryDescription(ProductEntry entry) {
        User user = Store.getLoggedInUser();
        if (entry.item() == ShopItem.POT) {
            int owned = user == null ? 0 : user.getGreenHouse().getSlotCount();
            return "Permanently unlock one greenhouse pot.\nOwned " + owned + "/20";
        }
        if (entry.item() == ShopItem.PLANT_FOOD) {
            int stored = user == null ? 0 : user.getPlantFood();
            return "One Plant Food for the next level.\nStored " + stored + "/3";
        }
        if (entry.item() == ShopItem.RANDOM_SEEDS) {
            return ShopItem.RANDOM_BUNDLE_PACKETS + " seed packets for one random unlocked plant.";
        }
        if (entry.item() == ShopItem.SELECTED_SEEDS && entry.plant() == null) {
            return "Choose an unlocked plant, then buy " + ShopItem.SELECTED_BUNDLE_PACKETS
                    + " seed packets for it.";
        }
        if (entry.item() == ShopItem.SELECTED_SEEDS && entry.plant() != null) {
            int stored = 0;
            if (user != null) {
                PlantCard card = user.getCollection().getPlantCard(entry.plant().getType());
                if (card != null) {
                    stored = card.getSeedPackets();
                }
            }
            return ShopItem.SELECTED_BUNDLE_PACKETS + " seed packets.\nYou currently have " + stored + ".";
        }
        if (entry.item() == ShopItem.COIN_PACK) {
            return "Exchange 5 diamonds for " + ShopItem.COIN_PACK_COINS + " coins.";
        }
        if (entry.item() == ShopItem.DAILY && entry.plant() != null) {
            return ShopItem.DAILY_PACKETS + " seed packets for " + entry.plant().getName()
                    + ".\n20% off the 2,000 coin base price.";
        }
        return entry.item().getDescription();
    }

    private String confirmationProduct(ShopItem item) {
        return switch (item) {
            case POT -> "one greenhouse pot";
            case PLANT_FOOD -> "one Plant Food";
            case RANDOM_SEEDS -> "one random seed bundle";
            case SELECTED_SEEDS -> "selected seed packets";
            case COIN_PACK -> ShopItem.COIN_PACK_COINS + " coins";
            case DAILY -> "today's discounted seed offer";
        };
    }

    private String priceText(ShopItem item) {
        return String.format(Locale.US, "%,d %s", item.getUnitPrice(),
                item.getCurrency().getLabel().toUpperCase(Locale.ROOT));
    }

    private boolean dailyPurchased() {
        User user = Store.getLoggedInUser();
        if (user == null) {
            return false;
        }
        DailyShopState state = user.getDailyShop();
        return state.getOfferId() != null && state.isPurchased(state.getOfferId());
    }

    private String dailyStatusLine() {
        if (dailyPurchased()) {
            return "SOLD TODAY";
        }
        Duration left = untilMidnight();
        long seconds = Math.max(0L, left.getSeconds());
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        return String.format(Locale.US, "%02d:%02d:%02d LEFT", hours, minutes, secs);
    }

    private Duration untilMidnight() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(clock.getZone());
        return Duration.between(now, midnight);
    }

    private void ensurePamLoaded(String path) {
        if (!pamAvailable || loadedPams.contains(path)) {
            return;
        }
        try {
            pamPlayer.loadSync(path);
            loadedPams.add(path);
        } catch (RuntimeException ex) {
            Gdx.app.error("ShopScreen", "Could not load PAM: " + path, ex);
        }
    }

    private PamSpec pamSpec(PlantType type) {
        if (type == null) {
            return new PamSpec(PAM_SUNFLOWER, "idle", 0.48f, 4f);
        }
        return switch (type) {
            case SUNFLOWER, TWIN_SUNFLOWER, SUN_SHROOM, PRIMAL_SUNFLOWER, GOLD_BLOOM ->
                    new PamSpec(PAM_SUNFLOWER, "idle", 0.48f, 4f);
            case PEASHOOTER, REPEATER, THREEPEATER, SNOW_PEA, ROTOBAGA, PEA_POD, SPLIT_PEA,
                    CITRON, CAULIPOWER, ELECTRIC_BLUEBERRY, BOWLING_BULB, FIRE_PEASHOOTER,
                    STARFRUIT, GOO_PEASHOOTER, MEGA_GATLING_PEA ->
                    new PamSpec(PAM_PEASHOOTER, "idle", 0.47f, 6f);
            case WALL_NUT, TALL_NUT, ENDURIAN, GARLIC, SWEET_POTATO, EXPLODE_O_NUT, PUMPKIN ->
                    new PamSpec(PAM_WALLNUT, "idle", 0.51f, -2f);
            case PUFF_SHROOM, FUME_SHROOM, MAGNET_SHROOM, HYPNO_SHROOM, ICE_SHROOM,
                    DOOM_SHROOM -> new PamSpec(PAM_PUFFSHROOM, "idle_stage1", 0.50f, 6f);
            case LILY_PAD, SEA_SHROOM, TANGLE_KELP -> new PamSpec(PAM_LILYPAD, "idle3", 0.54f, -8f);
            case POTATO_MINE, PRIMAL_POTATO_MINE, HOT_POTATO, CHERRY_BOMB, SQUASH,
                    GRAPESHOT, JALAPENO -> new PamSpec(PAM_POTATOMINE, "idle", 0.50f, 1f);
            case CABBAGE_PULT, KERNEL_PULT, MELON_PULT, WINTER_MELON, PEPPER_PULT ->
                    new PamSpec(PAM_KERNELPULT, "idle", 0.45f, 5f);
            case BONK_CHOY, PHAT_BEET, CHOMPER, WASABI_WHIP, KIWIBEAST, CACTUS ->
                    new PamSpec(PAM_BONKCHOY, "idle", 0.48f, 5f);
            default -> new PamSpec(PAM_SUNFLOWER, "idle", 0.48f, 4f);
        };
    }

    private void closeModal() {
        if (modal != null) {
            modal.remove();
            modal = null;
        }
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
        Gdx.gl.glClearColor(0.01f, 0.03f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (pamTextures != null) {
            pamTextures.update();
        }

        timerAccumulator += delta;
        if (timerAccumulator >= 1f) {
            timerAccumulator = 0f;
            if (dailyTimerLabel != null) {
                dailyTimerLabel.setText(dailyStatusLine());
            }
        }

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
        if (pamTextures != null) {
            pamTextures.dispose();
            pamTextures = null;
        }
    }

    private enum ShopCategory {
        ALL("ALL PERMANENT ITEMS", "all"),
        GREENHOUSE("GREENHOUSE", "greenhouse"),
        BOOSTS("BOOSTS", "boosts"),
        SEEDS("SEED PACKETS", "seeds"),
        CURRENCY("CURRENCY", "currency"),
        DAILY("TODAY'S SPECIAL", "daily");

        private final String heading;
        private final String assetToken;

        ShopCategory(String heading, String assetToken) {
            this.heading = heading;
            this.assetToken = assetToken;
        }

        String heading() {
            return heading;
        }

        String normalAsset() {
            return "tab_" + assetToken + "_normal.png";
        }

        String activeAsset() {
            return "tab_" + assetToken + "_active.png";
        }
    }

    private record ProductEntry(ShopItem item, PlantDefinition plant, boolean chooser) {
        static ProductEntry fixed(ShopItem item) {
            return new ProductEntry(item, null, false);
        }

        static ProductEntry selectedChooser() {
            return new ProductEntry(ShopItem.SELECTED_SEEDS, null, true);
        }

        static ProductEntry selectedPlant(PlantDefinition plant) {
            return new ProductEntry(ShopItem.SELECTED_SEEDS, plant, false);
        }

        static ProductEntry daily(PlantDefinition plant) {
            return new ProductEntry(ShopItem.DAILY, plant, false);
        }
    }

    private record PamSpec(String path, String clip, float scale, float yOffset) { }
}
