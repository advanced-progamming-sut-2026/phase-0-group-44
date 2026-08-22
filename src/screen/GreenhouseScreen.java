package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import controller.App;
import controller.GreenhouseController;
import controller.MenuController;
import controller.ShopController;
import model.Result;
import model.Store;
import model.enums.PlantType;
import model.miniGame.GreenHouse;
import model.miniGame.GreenhouseSlot;
import model.shop.ShopItem;
import model.user.User;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Figure-8 inspired graphical Greenhouse / Zen Garden.
 *
 * The Phase-1 GreenHouse and controllers remain the source of truth.  This
 * screen only renders those states over the original 12-stand Zen Garden art.
 */
public final class GreenhouseScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;
    private static final String ASSET_ROOT = "ui/greenhouse/";
    private static final String ADVENTURE_ROOT = "ui/adventure/";
    private static final String ADVENTURE_HUD_ROOT = ADVENTURE_ROOT + "hud/";
    private static final String MAIN_MENU_ROOT = "ui/mainmenu/";

    private static final int VISIBLE_SLOTS = 12;
    private static final int PAGE_COUNT = 2;

    // Original greenhouse background dimensions.
    private static final float SOURCE_WIDTH = 1160f;
    private static final float SOURCE_HEIGHT = 760f;
    private static final float SCALE_X = VIRTUAL_WIDTH / SOURCE_WIDTH;
    private static final float SCALE_Y = VIRTUAL_HEIGHT / SOURCE_HEIGHT;

    // Centres of the 4 x 3 wooden stands in the original background.
    private static final float[] SOURCE_SLOT_X = {455f, 625f, 795f, 965f};
    private static final float[] SOURCE_SLOT_Y_FROM_TOP = {307f, 477f, 644f};

    private static final String PAM_MARIGOLD = "768/INITIAL/PLANT/MARIGOLD/MARIGOLD.PAM";
    private static final String PAM_SUNFLOWER = "768/INITIAL/PLANT/SUNFLOWER/SUNFLOWER.PAM";
    private static final String PAM_PEASHOOTER = "768/INITIAL/PLANT/PEASHOOTER/PEASHOOTER.PAM";
    private static final String PAM_WALLNUT = "768/INITIAL/PLANT/WALLNUT/WALLNUT.PAM";
    private static final String PAM_PUFFSHROOM = "768/INITIAL/PLANT/PUFFSHROOM/PUFFSHROOM.PAM";
    private static final String PAM_LILYPAD = "768/FULL/PLANT/LILYPAD/LILYPAD.PAM";
    private static final String PAM_POTATOMINE = "768/INITIAL/PLANT/POTATOMINE/POTATOMINE.PAM";
    private static final String PAM_BONKCHOY = "768/INITIAL/PLANT/BONKCHOY/BONKCHOY.PAM";
    private static final String PAM_KERNELPULT = "768/INITIAL/PLANT/KERNALPULT/KERNALPULT.PAM";

    private final PvzGame game;
    private final GreenhouseController greenhouseController;
    private final ShopController shopController;
    private final MenuController menuController;
    private final Clock clock;
    private final Map<String, Texture> textures = new LinkedHashMap<>();
    private final Map<Integer, Label> timerLabels = new LinkedHashMap<>();
    private final Set<String> loadedPams = new HashSet<>();

    private Stage stage;
    private Skin skin;
    private ToastManager toast;
    private int page;
    private float refreshAccumulator;

    private TextureBank pamTextures;
    private PamPlayer pamPlayer;
    private boolean pamAvailable;

    public GreenhouseScreen(PvzGame game, App app) {
        this.game = game;
        this.greenhouseController = app.getGreenhouseController();
        this.shopController = app.getShopController();
        this.menuController = app.getMenuController();
        this.clock = app.getUserService().getClock();
    }

    @Override
    public void show() {
        // StretchViewport is intentional here: unlike the battlefield, this is
        // a decorative menu background and the reference fills the whole frame.
        stage = new Stage(new StretchViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        skin = PvzSkin.get();
        toast = new ToastManager(stage, skin);
        Gdx.input.setInputProcessor(stage);
        page = 0;
        initializePam();
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
            pamTextures = new TextureBank(PamAssetQuality.bestResolution(assets), assets);
            pamPlayer = new PamPlayer(pamTextures, assets);
            pamAvailable = true;
        } catch (RuntimeException ex) {
            pamAvailable = false;
            Gdx.app.error("GreenhouseScreen", "Could not initialize libPVZ plant rendering", ex);
        }
    }

    private void rebuild() {
        stage.clear();
        timerLabels.clear();

        User user = Store.getLoggedInUser();
        if (user != null) {
            user.getGreenHouse().applyDefaults();
        }

        stage.addActor(buildBackground());
        stage.addActor(buildSlotsLayer());
        stage.addActor(buildTopHud());
        stage.addActor(buildPageControls());
        stage.addActor(buildToolButton());
    }

    private Group buildBackground() {
        Group layer = new Group();
        layer.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        Image background = new Image(loadTexture(ASSET_ROOT + "background.png"));
        background.setScaling(Scaling.fill);
        background.setBounds(-72f, 0f, VIRTUAL_WIDTH + 72f, VIRTUAL_HEIGHT);
        layer.addActor(background);
        return layer;
    }

    private Group buildSlotsLayer() {
        Group layer = new Group();
        layer.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        User user = Store.getLoggedInUser();
        if (user == null) {
            Label missing = new Label("Log in to use the greenhouse.", skin, "medium_outline");
            missing.setPosition(500f, 350f);
            layer.addActor(missing);
            return layer;
        }

        GreenHouse greenHouse = user.getGreenHouse();
        greenHouse.applyDefaults();
        int firstLockedIndex = firstLockedIndex(greenHouse);

        for (int local = 0; local < VISIBLE_SLOTS; local++) {
            int index = page * VISIBLE_SLOTS + local;
            if (index >= GreenHouse.TOTAL_SLOTS) {
                continue;
            }

            GreenhouseSlot slot = greenHouse.getSlot(index);
            int column = local % 4;
            int row = local / 4;
            float centerX = SOURCE_SLOT_X[column] * SCALE_X;
            float centerY = (SOURCE_HEIGHT - SOURCE_SLOT_Y_FROM_TOP[row]) * SCALE_Y;

            Actor actor = buildSlotActor(slot, index == firstLockedIndex);
            actor.setBounds(centerX - 74f, centerY - 72f, 148f, 152f);
            layer.addActor(actor);
        }
        return layer;
    }

    private Actor buildSlotActor(GreenhouseSlot slot, boolean nextPurchasable) {
        if (slot.isLocked()) {
            return buildLockedSlot(nextPurchasable);
        }
        if (!slot.isOccupied()) {
            return buildEmptySlot(slot);
        }
        return buildOccupiedSlot(slot);
    }

    private Actor buildLockedSlot(boolean nextPurchasable) {
        Table table = new Table();
        table.top();

        Image lock = new Image(loadTexture(ASSET_ROOT + "locked_pot.png"));
        lock.setScaling(Scaling.fit);
        table.add(lock).size(36f, 47f).padTop(38f).row();

        if (nextPurchasable) {
            TextButton buy = new TextButton(String.format("%,d", ShopItem.POT.getUnitPrice()), skin, "purple");
            buy.getLabel().setAlignment(Align.left);
            buy.getLabelCell().padLeft(20f);
            buy.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Result<String> result = shopController.buy(ShopItem.POT.getId(), 1, null);
                    if (result.getStatus()) {
                        rebuild();
                        toast.showInfo(result.getMessage());
                    } else {
                        toast.showError(result.getMessage());
                    }
                }
            });

            Stack buyStack = new Stack();
            buyStack.add(buy);
            Table coinOverlay = new Table();
            Image coin = new Image(loadTexture(MAIN_MENU_ROOT + "coin.png"));
            coin.setScaling(Scaling.fit);
            coinOverlay.left();
            coinOverlay.add(coin).size(15f, 15f).padLeft(6f);
            buyStack.add(coinOverlay);
            table.add(buyStack).width(108f).height(29f).padTop(11f);
        }
        return table;
    }

    private Actor buildEmptySlot(GreenhouseSlot slot) {
        Table table = new Table();
        table.top();

        Image pot = new Image(loadTexture(ASSET_ROOT + "pot.png"));
        pot.setScaling(Scaling.fit);
        table.add(pot).size(90f, 72f).padTop(18f).row();

        TextButton plant = new TextButton("PLANT", skin, "brown");
        plant.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Result<String> result = greenhouseController.plantPot(slot.getX(), slot.getY());
                if (result.getStatus()) {
                    rebuild();
                    toast.showInfo("Planted " + result.getData());
                } else {
                    toast.showError(result.getMessage());
                }
            }
        });
        table.add(plant).width(92f).height(29f).padTop(1f);
        return table;
    }

    private Actor buildOccupiedSlot(GreenhouseSlot slot) {
        Instant now = clock.instant();
        boolean ready = slot.isReady(now);

        Table table = new Table();
        table.top();
        table.add(buildPottedPlant(slot)).size(126f, 122f).padTop(-2f).row();

        if (ready) {
            TextButton collect = new TextButton("COLLECT", skin, "green");
            collect.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Result<String> result = greenhouseController.collect(slot.getX(), slot.getY());
                    if (result.getStatus()) {
                        rebuild();
                        toast.showInfo(result.getData());
                    } else {
                        toast.showError(result.getMessage());
                    }
                }
            });
            table.add(collect).width(94f).height(28f).padTop(7f);
            return table;
        }

        long remaining = slot.remainingSeconds(now);
        Table timingRow = new Table();

        Stack timer = new Stack();
        Image timerBackground = new Image(loadTexture(ASSET_ROOT + "timer_background.png"));
        timerBackground.setScaling(Scaling.stretch);
        timer.add(timerBackground);
        Label countdown = new Label(formatDurationCompact(remaining), skin);
        countdown.setAlignment(Align.center);
        timer.add(countdown);
        timingRow.add(timer).width(68f).height(25f).padRight(4f);
        timerLabels.put(slot.getIndex(), countdown);

        int cost = accelerationCost(remaining);
        TextButton finish = new TextButton(String.valueOf(cost), skin, "purple");
        finish.getLabel().setAlignment(Align.left);
        finish.getLabelCell().padLeft(18f);
        finish.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Result<Integer> result = greenhouseController.grow(slot.getX(), slot.getY());
                if (result.getStatus()) {
                    rebuild();
                    toast.showInfo(result.getMessage());
                } else {
                    toast.showError(result.getMessage());
                }
            }
        });

        Stack finishStack = new Stack();
        finishStack.add(finish);
        Table gemOverlay = new Table();
        Image gem = new Image(loadTexture(MAIN_MENU_ROOT + "gem.png"));
        gem.setScaling(Scaling.fit);
        gemOverlay.left();
        gemOverlay.add(gem).size(15f, 15f).padLeft(7f);
        finishStack.add(gemOverlay);

        timingRow.add(finishStack).width(68f).height(30f);

        table.add(timingRow).padTop(4f);
        return table;
    }

    private Stack buildPottedPlant(GreenhouseSlot slot) {
        Stack stack = new Stack();

        Table potLayer = new Table();
        Image pot = new Image(loadTexture(ASSET_ROOT + "pot.png"));
        pot.setScaling(Scaling.fit);
        potLayer.add(pot).size(104f, 84f).padTop(40f);
        stack.add(potLayer);

        PamSpec spec = pamSpec(slot);
        if (pamAvailable && spec != null) {
            ensurePamLoaded(spec.path());
            PamPlantActor actor = new PamPlantActor(pamPlayer, spec.path(), spec.clip(), spec.scale(), spec.xOffset(), spec.yOffset());
            stack.add(actor);
        } else {
            Table fallback = new Table();
            Image sprout = new Image(loadTexture(ASSET_ROOT + "sprout.png"));
            sprout.setScaling(Scaling.fit);
            fallback.add(sprout).size(68f, 68f).padBottom(23f);
            stack.add(fallback);
        }
        return stack;
    }

    private void ensurePamLoaded(String path) {
        if (!pamAvailable || loadedPams.contains(path)) {
            return;
        }
        try {
            pamPlayer.loadSync(path);
            loadedPams.add(path);
        } catch (RuntimeException ex) {
            Gdx.app.error("GreenhouseScreen", "Could not load PAM: " + path, ex);
        }
    }

    private PamSpec pamSpec(GreenhouseSlot slot) {
        if (slot.isMarigold()) {
            return new PamSpec(PAM_MARIGOLD, "idle", 0.62f, 0f, 3f);
        }

        PlantType type = slot.getPlantType();
        if (type == null) {
            return new PamSpec(PAM_SUNFLOWER, "idle", 0.64f, 0f, 3f);
        }

        return switch (type) {
            case SUNFLOWER, TWIN_SUNFLOWER, SUN_SHROOM, PRIMAL_SUNFLOWER, GOLD_BLOOM ->
                    new PamSpec(PAM_SUNFLOWER, "idle", 0.64f, 0f, 3f);
            case PEASHOOTER, REPEATER, THREEPEATER, SNOW_PEA, ROTOBAGA, PEA_POD, SPLIT_PEA,
                    CITRON, CAULIPOWER, ELECTRIC_BLUEBERRY, BOWLING_BULB, FIRE_PEASHOOTER,
                    STARFRUIT, GOO_PEASHOOTER, MEGA_GATLING_PEA ->
                    new PamSpec(PAM_PEASHOOTER, "idle", 0.63f, -3f, 2f);
            case WALL_NUT, TALL_NUT, ENDURIAN, GARLIC, SWEET_POTATO, EXPLODE_O_NUT, PUMPKIN ->
                    new PamSpec(PAM_WALLNUT, "idle", 0.69f, 0f, -6f);
            case PUFF_SHROOM, FUME_SHROOM, MAGNET_SHROOM, HYPNO_SHROOM, ICE_SHROOM,
                    DOOM_SHROOM -> new PamSpec(PAM_PUFFSHROOM, "idle_stage1", 0.69f, 0f, 0f);
            case LILY_PAD, SEA_SHROOM, TANGLE_KELP ->
                    new PamSpec(PAM_LILYPAD, "idle3", 0.70f, 0f, -10f);
            case POTATO_MINE, PRIMAL_POTATO_MINE, HOT_POTATO, CHERRY_BOMB, SQUASH,
                    GRAPESHOT, JALAPENO ->
                    new PamSpec(PAM_POTATOMINE, "idle", 0.70f, 0f, -7f);
            case CABBAGE_PULT, KERNEL_PULT, MELON_PULT, WINTER_MELON, PEPPER_PULT ->
                    new PamSpec(PAM_KERNELPULT, "idle", 0.61f, 0f, 3f);
            case BONK_CHOY, PHAT_BEET, CHOMPER, WASABI_WHIP, KIWIBEAST, CACTUS ->
                    new PamSpec(PAM_BONKCHOY, "idle", 0.66f, 0f, -1f);
            default -> new PamSpec(PAM_SUNFLOWER, "idle", 0.64f, 0f, 3f);
        };
    }

    private Table buildTopHud() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().pad(7f, 13f, 0f, 13f);

        Table left = new Table();
        ImageButton back = imageButton(
                ADVENTURE_ROOT + "back_normal.png",
                ADVENTURE_ROOT + "back_selected.png"
        );
        back.addListener(new ChangeListener() {
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
        left.add(hudIcon(back, "BACK", 58f, 56f)).padRight(5f);

        ImageButton collection = imageButton(
                ADVENTURE_HUD_ROOT + "collection_normal.png",
                ADVENTURE_HUD_ROOT + "collection_selected.png"
        );
        collection.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toast.showInfo("Collection shortcut is visual here until its Phase-2 screen is connected.");
            }
        });
        left.add(hudIcon(collection, "", 54f, 54f));

        hud.add(left).left().top();
        hud.add().expandX();
        hud.add(buildWalletAndShop()).right().top();
        return hud;
    }

    private Table buildWalletAndShop() {
        Table right = new Table();
        User user = Store.getLoggedInUser();
        int gems = user == null ? 0 : user.getGems();
        int coins = user == null ? 0 : user.getCoins();
        int unlockedPots = user == null ? 0 : user.getGreenHouse().getSlotCount();

        Table resourceRow = new Table();
        resourceRow.add(sproutCapacityPill(unlockedPots)).width(112f).height(46f).padRight(5f);
        resourceRow.add(resourceWithPlus(MAIN_MENU_ROOT + "gem.png", gems,
                "Gem earning is outside the greenhouse rules.", 86f)).width(111f).padRight(4f);
        resourceRow.add(resourceWithPlus(MAIN_MENU_ROOT + "coin.png", coins,
                "Coin earning is outside the greenhouse rules.", 112f)).width(137f).padRight(7f);

        ImageButton shop = imageButton(
                ADVENTURE_HUD_ROOT + "shop_normal.png",
                ADVENTURE_HUD_ROOT + "shop_selected.png"
        );
        shop.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Result<String> result = menuController.enterMenu("shop");
                if (result.getStatus()) {
                    game.goToScreenForCurrentMenu();
                } else {
                    toast.showError(result.getMessage());
                }
            }
        });
        resourceRow.add(shopWithSale(shop)).width(70f).height(78f);
        right.add(resourceRow).row();

        Table earnRow = new Table();
        earnRow.add().width(112f);
        TextButton earnGems = new TextButton("EARN GEMS!", skin, "purple");
        earnGems.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toast.showInfo("EARN GEMS is shown to match the reference HUD; it does not change greenhouse rules.");
            }
        });
        earnRow.add(earnGems).width(92f).height(27f).padRight(7f);
        TextButton earnCoins = new TextButton("EARN COINS!", skin, "brown");
        earnCoins.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toast.showInfo("EARN COINS is shown to match the reference HUD; Marigold still awards exactly 500 coins on collection.");
            }
        });
        earnRow.add(earnCoins).width(96f).height(27f).padRight(74f);
        right.add(earnRow).top();
        return right;
    }

    private Stack sproutCapacityPill(int unlockedPots) {
        Stack stack = new Stack();
        Image base = new Image(loadTexture(ASSET_ROOT + "sprout_hud.png"));
        base.setScaling(Scaling.fit);
        stack.add(base);

        Table overlay = new Table();
        overlay.add().width(57f);
        Label count = new Label(String.valueOf(unlockedPots), skin, "medium_outline");
        overlay.add(count).width(38f);
        overlay.add().expandX();
        stack.add(overlay);
        return stack;
    }

    private Table resourceWithPlus(String iconPath, int value, String message, float pillWidth) {
        Table row = new Table();
        row.add(resourcePill(iconPath, value)).width(pillWidth).height(34f);
        TextButton plus = new TextButton("+", skin, "green");
        plus.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toast.showInfo(message);
            }
        });
        row.add(plus).size(25f, 25f).padLeft(-3f);
        return row;
    }

    private Stack shopWithSale(ImageButton shop) {
        Stack stack = new Stack();
        Table buttonLayer = new Table();
        buttonLayer.add(shop).size(62f, 62f).padBottom(13f);
        stack.add(buttonLayer);

        Table ribbonLayer = new Table();
        ribbonLayer.bottom();
        TextButton ribbon = new TextButton("SALE", skin, "brown");
        ribbon.setDisabled(true);
        ribbonLayer.add(ribbon).width(58f).height(24f);
        stack.add(ribbonLayer);
        return stack;
    }

    private Stack resourcePill(String iconPath, int value) {
        Stack stack = new Stack();
        Image background = new Image(loadTexture(ASSET_ROOT + "timer_background.png"));
        background.setScaling(Scaling.stretch);
        stack.add(background);

        Table content = new Table();
        Image icon = new Image(loadTexture(iconPath));
        icon.setScaling(Scaling.fit);
        content.add(icon).size(28f, 28f).padRight(5f);
        content.add(new Label(String.format("%,d", value), skin, "medium_outline"));
        stack.add(content);
        return stack;
    }

    private Table buildPageControls() {
        Table controls = new Table();
        controls.setFillParent(true);
        controls.top().padTop(82f);

        ImageButton previous = imageButton(
                ADVENTURE_HUD_ROOT + "carousel_left.png",
                ADVENTURE_HUD_ROOT + "carousel_left_down.png"
        );
        ImageButton next = imageButton(
                ADVENTURE_HUD_ROOT + "carousel_right.png",
                ADVENTURE_HUD_ROOT + "carousel_right_down.png"
        );

        previous.setDisabled(page == 0);
        previous.getColor().a = page == 0 ? 0.18f : 1f;
        previous.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (page > 0) {
                    page--;
                    rebuild();
                }
            }
        });

        next.setDisabled(page >= PAGE_COUNT - 1);
        next.getColor().a = page >= PAGE_COUNT - 1 ? 0.18f : 1f;
        next.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (page < PAGE_COUNT - 1) {
                    page++;
                    rebuild();
                }
            }
        });

        int first = page * VISIBLE_SLOTS + 1;
        int last = Math.min(GreenHouse.TOTAL_SLOTS, (page + 1) * VISIBLE_SLOTS);
        Label pageLabel = new Label("POTS " + first + " - " + last, skin, "medium_outline");
        controls.add(previous).size(25f, 25f).padRight(5f);
        controls.add(pageLabel).padLeft(3f).padRight(3f);
        controls.add(next).size(25f, 25f).padLeft(5f);
        return controls;
    }

    private Table buildToolButton() {
        Table tools = new Table();
        tools.setFillParent(true);
        tools.bottom().right().pad(0f, 13f, 11f, 0f);
        ImageButton shovel = imageButton(
                ASSET_ROOT + "shovel_button.png",
                ASSET_ROOT + "shovel_button_down.png"
        );
        shovel.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toast.showInfo("The shovel is visual in this phase; ready plants are removed with COLLECT.");
            }
        });
        tools.add(shovel).size(66f, 66f);
        return tools;
    }

    private Table hudIcon(ImageButton button, String caption, float width, float height) {
        Table slot = new Table();
        slot.add(button).size(width, height).row();
        Label label = new Label(caption, skin);
        label.setAlignment(Align.center);
        slot.add(label).width(Math.max(width + 10f, 68f)).padTop(0f);
        return slot;
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

    private int firstLockedIndex(GreenHouse greenHouse) {
        for (GreenhouseSlot slot : greenHouse.getSlots()) {
            if (slot != null && slot.isLocked()) {
                return slot.getIndex();
            }
        }
        return -1;
    }

    private int accelerationCost(long remainingSeconds) {
        return Math.max(1, (int) ((remainingSeconds + 3599L) / 3600L));
    }

    private String formatDurationCompact(long seconds) {
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        }
        return Math.max(1L, minutes) + "m";
    }

    private void refreshTimers() {
        if (timerLabels.isEmpty()) {
            return;
        }
        User user = Store.getLoggedInUser();
        if (user == null) {
            return;
        }
        GreenHouse greenHouse = user.getGreenHouse();
        Instant now = clock.instant();
        boolean becameReady = false;
        for (Map.Entry<Integer, Label> entry : timerLabels.entrySet()) {
            GreenhouseSlot slot = greenHouse.getSlot(entry.getKey());
            if (slot == null || !slot.isOccupied()) {
                continue;
            }
            long remaining = slot.remainingSeconds(now);
            entry.getValue().setText(formatDurationCompact(remaining));
            if (remaining == 0L) {
                becameReady = true;
            }
        }
        if (becameReady) {
            rebuild();
        }
    }

    private Texture loadTexture(String path) {
        Texture cached = textures.get(path);
        if (cached != null) {
            return cached;
        }
        Texture texture = TextureQuality.load(path);
        textures.put(path, texture);
        return texture;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.04f, 0.02f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (pamTextures != null) {
            pamTextures.update();
        }

        refreshAccumulator += delta;
        if (refreshAccumulator >= 1f) {
            refreshAccumulator = 0f;
            refreshTimers();
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

    private record PamSpec(String path, String clip, float scale, float xOffset, float yOffset) { }
}
