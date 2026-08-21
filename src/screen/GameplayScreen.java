package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import controller.App;
import controller.BoardController;
import controller.GameplayController;
import model.GameEngine;
import model.Result;
import model.Store;
import model.config.GameWorld;
import model.enums.MenuName;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.inGame.GameOutcome;
import model.inGame.GameSession;
import model.inGame.Sun;
import model.sim.Simulation;
import model.sim.adventure.AdventureInitializer;
import model.sim.adventure.AdventureRuleSystem;
import model.sim.wave.WaveSystem;
import model.sim.zombie.ChapterZombieSpecSource;
import model.sim.zombie.DefaultZombieSpecSource;
import model.sim.sun.SunType;
import model.inGame.zombie.ZombieRegistry;
import model.user.User;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;
import screen.gameplay.BattlefieldChapterEffects;
import screen.gameplay.BattlefieldDarkAgesStateLayer;
import screen.gameplay.BattlefieldEnvironmentLayer;
import screen.gameplay.BattlefieldFrostbiteStateLayer;
import screen.gameplay.BattlefieldLayout;
import screen.gameplay.BattlefieldMissionObjectives;
import screen.gameplay.BattlefieldMissionStartLayer;
import screen.gameplay.BattlefieldPauseOutcomeLayer;
import screen.gameplay.BattlefieldSpecialLevelLayer;
import screen.gameplay.BattlefieldTheme;
import screen.gameplay.PamEnvironmentActor;
import screen.gameplay.ZombieActorManager;
import model.inGame.plant.PlantDefinition;
import screen.gameplay.BattlefieldSeedBank;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import util.SeededRandomSource;

/**
 * Phase-2 battlefield screen: shared environment, gameplay HUD and interaction tools.
 * Plants, zombies and projectiles intentionally remain outside this class and should
 * attach to {@link #entityLayer} using the shared {@link BattlefieldLayout}.
 */
public final class GameplayScreen implements Screen, BattlefieldSeedBank.SeedDragHandler {
    public static final float VIRTUAL_WIDTH = 1280f;
    public static final float VIRTUAL_HEIGHT = 720f;

    private static final String HUD_ROOT = "ui/gameplay/hud/";
    private static final String SUN_PAM = "768/INITIAL/EFFECTS/SUN/SUN.PAM";
    private static final String PLANT_FOOD_PAM =
            "768/INITIAL/EFFECTS/PLANTFOOD_PICKUP/PLANTFOOD_PICKUP.PAM";
    private static final String HIGHLIGHT_PAM =
            "768/INITIAL/ZEN_GARDEN/HIGHLIGHT/HIGHLIGHT.PAM";
    private static final String REMOVAL_CURSOR_PAM =
            "768/INITIAL/ZEN_GARDEN/CURSORS/REMOVAL_CURSOR/REMOVAL_CURSOR.PAM";

    private final PvzGame game;
    private final App app;
    private final Map<String, Texture> textures = new LinkedHashMap<>();

    private Stage stage;
    private Skin skin;
    private Texture whiteTexture;
    private Texture runeTexture;
    private TextureBank pamTextures;
    private PamPlayer pamPlayer;
    private FileHandle pamRoot;

    private BattlefieldTheme theme;
    private BattlefieldLayout layout;
    private BattlefieldEnvironmentLayer environmentLayer;
    private BattlefieldDarkAgesStateLayer darkAgesStateLayer;
    private BattlefieldFrostbiteStateLayer frostbiteStateLayer;
    private BattlefieldChapterEffects chapterEffects;
    private BattlefieldSpecialLevelLayer specialLevelLayer;
    private Group entityLayer;
    private ZombieActorManager zombieActorManager;
    private Group pickupLayer;
    private Group interactionLayer;
    private Group hudLayer;
    private BattlefieldPauseOutcomeLayer pauseOutcomeLayer;
    private BattlefieldMissionStartLayer missionStartLayer;

    private PamEnvironmentActor hoverHighlight;
    private PamEnvironmentActor selectedHighlight;
    private PamEnvironmentActor toolCursor;
    private Label sunLabel;
    private Label plantFoodLabel;
    private Label waveLabel;
    private Label previewLabel;
    private Label actionLabel;
    private Image waveFill;
    private Image waveHead;
    private Image shovelButtonBackground;
    private Image plantFoodButtonBackground;
    private static final String PLANT_PAM_ROOT = "768/INITIAL/PLANT/";
    private final Map<String, PamEnvironmentActor> placedPlantActors = new LinkedHashMap<>();
    private PamEnvironmentActor dragGhost;

    private BoardController boardController;
    private GameplayController gameplayController;

    private boolean previewMode;
    private boolean paused;
    private int selectedRow = -1;
    private int selectedColumn = -1;
    private ToolMode toolMode = ToolMode.NONE;
    private String pickupSignature = "";
    private float messageSeconds;
    private float tickAccumulator;
    private boolean outcomeShown;
    private boolean missionIntroActive;
    private boolean draggingPlant;
    private GameEngine finishedEngine;
    private BattlefieldSeedBank seedBank;
    private PlantType armedPlantType;
    private Texture darkTintTexture;

    /*
     * Developer zombie graphics tester.
     * LEFT / RIGHT selects; Z force-spawns regardless of level restrictions.
     */
    private static final ZombieType[] ZOMBIE_TEST_TYPES = {
            ZombieType.NORMAL,
            ZombieType.CONEHEAD,
            ZombieType.BUCKETHEAD,
            ZombieType.KNIGHT,
            ZombieType.BLOCKHEAD,
            ZombieType.GARGANTUAR,
            ZombieType.IMP,
            ZombieType.ALL_STAR,
            ZombieType.ARCADE_ZOMBIE,
            ZombieType.PARASOL_ZOMBIE,
            ZombieType.TURQUOISE_ZOMBIE,
            ZombieType.PROSPECTOR,
            ZombieType.PIANIST,
            ZombieType.NEWSPAPER_ZOMBIE,
            ZombieType.BARREL_ROLLER,
            ZombieType.RA_ZOMBIE,
            ZombieType.EXPLORER,
            ZombieType.TOMBRAISER,
            ZombieType.DODO_RIDER,
            ZombieType.HUNTER,
            ZombieType.TROGLOBITE,
            ZombieType.FISHERMAN,
            ZombieType.SNORKEL,
            ZombieType.OCTOPUS_ZOMBIE,
            ZombieType.JESTER,
            ZombieType.WIZARD,
            ZombieType.KING,
            ZombieType.DRAGON_IMP
    };

    private static final int TEST_CHEAT_SUN = 999_999;

    private int zombieTestIndex;
    private boolean plantTestingCheatsEnabled;

    private enum ToolMode {
        NONE,
        SHOVEL,
        PLANT_FOOD,
    }

    public GameplayScreen(PvzGame game, App app) {
        this.game = game;
        this.app = app;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        skin = PvzSkin.get();
        whiteTexture = makeWhiteTexture();
        runeTexture = makeRuneTexture();
        darkTintTexture = makeDarkTintTexture();
        initializePam();
        resolveInitialTheme();
        initializeGameplayControllers();
        rebuildScene();

        InputMultiplexer input = new InputMultiplexer();
        input.addProcessor(stage);
        input.addProcessor(new BattlefieldKeys());
        Gdx.input.setInputProcessor(input);
    }

    private void resolveInitialTheme() {
        GameSession session = Store.getActiveSession();
        Simulation simulation = Store.getActiveSimulation();
        GameWorld world = session == null || session.getLevel() == null
                ? null : session.getLevel().getWorld();
        previewMode = simulation == null || session == null || world == null;
        theme = BattlefieldTheme.forWorld(world);
    }

    private void initializeGameplayControllers() {
        boardController = null;
        gameplayController = null;
        if (previewMode) {
            return;
        }
        GameEngine engine = engine();
        GameSession session = Store.getActiveSession();
        if (engine == null || session == null) {
            return;
        }
        boardController = new BoardController(
                engine,
                session.getSelection(),
                engine.getAdventureState(),
                app.getDomainEvents(),
                Store.getLoggedInUser(),
                session
        );
        gameplayController = new GameplayController(
                Store.getActiveSimulation(),
                boardController,
                app.getRewardService(),
                app.getConclusionService(),
                session,
                Store.getLoggedInUser(),
                app.getDomainEvents()
        );
    }

    private void rebuildScene() {
        stage.clear();
        selectedRow = -1;
        selectedColumn = -1;
        paused = false;
        toolMode = ToolMode.NONE;
        pickupSignature = "";
        tickAccumulator = 0f;
        outcomeShown = false;
        missionIntroActive = true;
        finishedEngine = null;
        placedPlantActors.clear();
        dragGhost = null;
        plantTestingCheatsEnabled = false;

        layout = new BattlefieldLayout(theme, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        stage.addActor(buildBackground());

        environmentLayer = new BattlefieldEnvironmentLayer(
                theme,
                layout,
                whiteTexture,
                runeTexture,
                loadTexture("ui/gameplay/frostbite/slippery_up.png"),
                loadTexture("ui/gameplay/frostbite/slippery_down.png"),
                loadTexture("ui/gameplay/dark_ages/necromancy_tile.png"),
                loadTexture("ui/gameplay/dark_ages/grave_noop.png"),
                loadTexture("ui/gameplay/dark_ages/grave_sun.png"),
                loadTexture("ui/gameplay/dark_ages/grave_plant_food.png"),
                skin,
                pamPlayer,
                pamRoot
        );
        environmentLayer.sync(engine(), previewMode);
        stage.addActor(environmentLayer);

        darkAgesStateLayer = new BattlefieldDarkAgesStateLayer(
                theme,
                layout,
                whiteTexture,
                loadTexture("ui/gameplay/dark_ages/necromancy_tile.png"),
                loadTexture("ui/gameplay/dark_ages/grave_noop.png"),
                loadTexture("ui/gameplay/dark_ages/grave_sun.png"),
                loadTexture("ui/gameplay/dark_ages/grave_plant_food.png")
        );
        darkAgesStateLayer.sync(engine(), previewMode);
        stage.addActor(darkAgesStateLayer);

        specialLevelLayer = new BattlefieldSpecialLevelLayer(
                layout,
                whiteTexture,
                loadTexture(HUD_ROOT + "sun.png"),
                loadTexture(HUD_ROOT + "progress_head.png"),
                skin,
                pamPlayer,
                pamRoot,
                this::showAction
        );
        specialLevelLayer.sync(
                engine(), Store.getActiveSession(), previewMode, gameplayController);
        stage.addActor(specialLevelLayer.boardLayer());

        chapterEffects = new BattlefieldChapterEffects(
                theme, layout, whiteTexture, runeTexture,
                loadTexture(HUD_ROOT + "effect_streak.png"),
                loadTexture("ui/gameplay/dark_ages/necromancy_active_aura.png"),
                loadTexture("ui/gameplay/dark_ages/necromancy_active_sigil.png"),
                pamPlayer, pamRoot);
        stage.addActor(chapterEffects.rearLayer());

        entityLayer = new Group();
        entityLayer.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        stage.addActor(entityLayer);

        zombieActorManager = null;

        if (!previewMode && pamPlayer != null && engine() != null) {
            zombieActorManager =
                    new ZombieActorManager(
                            entityLayer,
                            layout,
                            pamPlayer
                    );
            zombieActorManager.sync(engine());
        }

        frostbiteStateLayer = new BattlefieldFrostbiteStateLayer(
                theme,
                layout,
                whiteTexture,
                loadTexture("ui/gameplay/frostbite/frozen_zombie_block.png"),
                pamPlayer,
                pamRoot
        );
        frostbiteStateLayer.sync(engine(), previewMode);
        stage.addActor(frostbiteStateLayer);

        stage.addActor(chapterEffects.frontLayer());

        pickupLayer = new Group();
        pickupLayer.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        stage.addActor(pickupLayer);

        interactionLayer = buildInteractionLayer();
        stage.addActor(interactionLayer);

        hudLayer = buildHud();
        stage.addActor(hudLayer);

        stage.addActor(specialLevelLayer.hudLayer());

        BattlefieldMissionObjectives.MissionInfo missionInfo =
                BattlefieldMissionObjectives.forSession(
                        Store.getActiveSession(), theme.world());

        pauseOutcomeLayer = new BattlefieldPauseOutcomeLayer(
                whiteTexture,
                loadTexture(HUD_ROOT + "pause_board_reference.png"),
                loadTexture(HUD_ROOT + "result_win.png"),
                loadTexture(HUD_ROOT + "progress_head.png"),
                loadTexture(HUD_ROOT + "objective_bullet.png"),
                loadTexture(HUD_ROOT + "button_brown.png"),
                loadTexture(HUD_ROOT + "button_brown_down.png"),
                loadTexture(HUD_ROOT + "button_purple_ref.png"),
                loadTexture(HUD_ROOT + "button_purple_ref_down.png"),
                loadTexture(HUD_ROOT + "audio_slider_track.png"),
                loadTexture(HUD_ROOT + "audio_slider_fill.png"),
                loadTexture(HUD_ROOT + "audio_slider_knob.png"),
                skin,
                previewMode,
                missionInfo,
                this::resumeFromPause,
                this::restartCurrentSession,
                this::saveAndExitGameplay,
                this::leaveGameplay
        );
        stage.addActor(pauseOutcomeLayer.root());

        missionStartLayer = new BattlefieldMissionStartLayer(
                whiteTexture,
                loadTexture(HUD_ROOT + "objectives_board_reference.png"),
                loadTexture(HUD_ROOT + "objective_bullet.png"),
                loadTexture(HUD_ROOT + "button_purple_ref.png"),
                loadTexture(HUD_ROOT + "button_purple_ref_down.png"),
                skin,
                this::continueFromMissionIntro
        );
        missionStartLayer.show(missionInfo);
        stage.addActor(missionStartLayer.root());

        seedBank = buildSeedBank();
        if (seedBank != null) {
            stage.addActor(seedBank.actor());
        }
    }

    /** Public integration hook for plant/zombie/projectile actors. */
    public Group getEntityLayer() {
        return entityLayer;
    }

    /** Public integration hook for the shared 5x9 coordinate contract. */
    public BattlefieldLayout getBattlefieldLayout() {
        return layout;
    }

    private Actor buildBackground() {
        Image background = new Image(loadTexture(theme.backgroundAsset()));
        background.setScaling(Scaling.stretch);
        background.setBounds(0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        return background;
    }

    private Group buildInteractionLayer() {
        Group group = new Group();
        group.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        if (pamPlayer != null) {
            hoverHighlight = new PamEnvironmentActor(
                    pamPlayer, HIGHLIGHT_PAM, "hover", 0.47f, 0f, 0f);
            hoverHighlight.setVisible(false);
            group.addActor(hoverHighlight);

            selectedHighlight = new PamEnvironmentActor(
                    pamPlayer, HIGHLIGHT_PAM, "default", 0.44f, 0f, 0f);
            selectedHighlight.setVisible(false);
            group.addActor(selectedHighlight);

            toolCursor = new PamEnvironmentActor(
                    pamPlayer, REMOVAL_CURSOR_PAM, "idle", 0.46f, 0f, 12f);
            toolCursor.setVisible(false);
            group.addActor(toolCursor);
        }
        return group;
    }

    private Group buildHud() {
        Group hud = new Group();
        hud.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // Compact real PVZ HUD: use the supplied in-game atlas pieces rather
        // than a large generic dark panel.
        Image sunIcon = new Image(loadTexture(HUD_ROOT + "sun.png"));
        sunIcon.setScaling(Scaling.fit);
        sunIcon.setBounds(18f, 654f, 52f, 52f);
        hud.addActor(sunIcon);

        Image sunNumberBack = new Image(whiteTexture);
        sunNumberBack.setColor(0.02f, 0.03f, 0.025f, 0.58f);
        sunNumberBack.setBounds(62f, 662f, 74f, 36f);
        hud.addActor(sunNumberBack);

        sunLabel = new Label("50", skin, "medium_outline");
        sunLabel.setAlignment(Align.center);
        sunLabel.setBounds(62f, 663f, 74f, 34f);
        hud.addActor(sunLabel);

        hud.addActor(buildPlantFoodButton());

        // Chapter title stays independent from the wave meter so the grass/dirt
        // loading art no longer looks like a title plaque.
        Label chapterTitle = new Label(theme.world().getDisplayName().toUpperCase(),
                skin, "medium_outline");
        chapterTitle.setAlignment(Align.center);
        chapterTitle.setBounds(440f, 638f, 400f, 32f);
        hud.addActor(chapterTitle);

        previewLabel = new Label("", skin);
        previewLabel.setAlignment(Align.center);
        previewLabel.setBounds(480f, 613f, 320f, 22f);
        hud.addActor(previewLabel);

        // The supplied actual in-game wave meter sits at the bottom-right.
        Image waveTrack = new Image(loadTexture(HUD_ROOT + "progress_meter.png"));
        waveTrack.setScaling(Scaling.stretch);
        waveTrack.setBounds(500f, 681f, 273f, 33f);
        hud.addActor(waveTrack);

        waveFill = new Image(loadTexture(HUD_ROOT + "progress_fill.png"));
        waveFill.setScaling(Scaling.stretch);
        waveFill.setBounds(512f, 689f, 0f, 17f);
        hud.addActor(waveFill);

        Image pole = new Image(loadTexture(HUD_ROOT + "progress_pole.png"));
        pole.setScaling(Scaling.fit);
        pole.setBounds(763f, 674f, 25f, 39f);
        hud.addActor(pole);

        Image flag = new Image(loadTexture(HUD_ROOT + "progress_flag.png"));
        flag.setScaling(Scaling.fit);
        flag.setBounds(767f, 702f, 27f, 18f);
        hud.addActor(flag);

        waveHead = new Image(loadTexture(HUD_ROOT + "progress_head.png"));
        waveHead.setScaling(Scaling.fit);
        waveHead.setBounds(491f, 666f, 34f, 38f);
        hud.addActor(waveHead);

        waveLabel = new Label("WAVE", skin, "medium_outline");
        waveLabel.setAlignment(Align.center);
        waveLabel.setBounds(510f, 594f, 260f, 20f);
        hud.addActor(waveLabel);

        actionLabel = new Label("", skin, "medium_outline");
        actionLabel.setAlignment(Align.center);
        actionLabel.setColor(1f, 0.94f, 0.62f, 1f);
        actionLabel.setBounds(470f, 12f, 360f, 28f);
        hud.addActor(actionLabel);

        hud.addActor(buildShovelButton());
        hud.addActor(buildPauseButton());
        return hud;
    }

    private void addPlantFoodSlots(Group group) {
        // Holes in the displayed compact 3-slot bank are centered around x=79, 102, 124
        // in the local 163x88 source image.  The real filled-slot atlas sprite
        // is layered over those holes when Plant Food is available.
        float[] centers = {79f, 102f, 124f};
        for (int index = 0; index < GameEngine.MAX_PLANT_FOOD; index++) {
            Image slot = new Image(loadTexture(HUD_ROOT + "plantfood_filled.png"));
            slot.setScaling(Scaling.fit);
            slot.setBounds(centers[index] - 9f, 24f, 18f, 18f);
            slot.setName("pf-slot-" + index);
            slot.setVisible(false);
            group.addActor(slot);
        }
    }

    private Actor buildShovelButton() {
        Group button = new Group();
        button.setBounds(1198f, 8f, 62f, 62f);

        shovelButtonBackground = new Image(loadTexture(HUD_ROOT + "shovel_button.png"));
        shovelButtonBackground.setScaling(Scaling.fit);
        shovelButtonBackground.setBounds(0f, 0f, 62f, 62f);
        button.addActor(shovelButtonBackground);

        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleTool(ToolMode.SHOVEL);
            }
        });
        return button;
    }

    /** The real Plant Food bank doubles as the Plant Food targeting button. */
    private Actor buildPlantFoodButton() {
        Group button = new Group();
        button.setBounds(244f, 8f, 205f, 68f);

        plantFoodButtonBackground = new Image(loadTexture(HUD_ROOT + "plantfood_bank3.png"));
        plantFoodButtonBackground.setScaling(Scaling.fit);
        plantFoodButtonBackground.setBounds(0f, 0f, 150f, 68f);
        button.addActor(plantFoodButtonBackground);
        addPlantFoodSlots(button);

        Image leaf = new Image(loadTexture(HUD_ROOT + "plantfood_leaf.png"));
        leaf.setScaling(Scaling.fit);
        leaf.setBounds(9f, 14f, 42f, 42f);
        button.addActor(leaf);

        plantFoodLabel = new Label("0 / 3", skin, "medium_outline");
        plantFoodLabel.setAlignment(Align.left);
        plantFoodLabel.setBounds(151f, 21f, 54f, 26f);
        button.addActor(plantFoodLabel);

        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!previewMode && engine() != null && engine().getPlantFood() <= 0) {
                    showAction("NO PLANT FOOD");
                    return;
                }
                toggleTool(ToolMode.PLANT_FOOD);
            }
        });
        return button;
    }

    private Actor buildPauseButton() {
        Group button = new Group();
        button.setBounds(1200f, 650f, 58f, 58f);

        Image pause = new Image(loadTexture(HUD_ROOT + "pause_button.png"));
        pause.setScaling(Scaling.fit);
        pause.setBounds(0f, 0f, 58f, 58f);
        button.addActor(pause);

        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                togglePause();
            }
        });
        return button;
    }

    private void togglePause() {
        if (outcomeShown || missionIntroActive) {
            return;
        }
        paused = !paused;
        if (pauseOutcomeLayer != null) {
            pauseOutcomeLayer.setPaused(paused);
        }
        if (paused || outcomeShown) {
            toolMode = ToolMode.NONE;
            updateToolButtonState();
        }
    }

    private void continueFromMissionIntro() {
        if (!missionIntroActive) {
            return;
        }
        missionIntroActive = false;
        tickAccumulator = 0f;
        if (missionStartLayer != null) {
            missionStartLayer.hide();
        }
    }

    private void saveAndExitGameplay() {
        if (!previewMode) {
            // The current model persists user/progression data, not a resumable
            // mid-battle snapshot. Save the supported persistent state before exit.
            app.getUserService().saveUsers();
        }
        leaveGameplay();
    }

    private void resumeFromPause() {
        if (!paused || outcomeShown) {
            return;
        }
        paused = false;
        if (pauseOutcomeLayer != null) {
            pauseOutcomeLayer.setPaused(false);
        }
    }

    private void showOutcome(GameOutcome outcome) {
        if (outcome == null || outcome == GameOutcome.RUNNING || outcomeShown) {
            return;
        }
        outcomeShown = true;
        paused = true;
        toolMode = ToolMode.NONE;
        updateToolButtonState();

        String levelName = theme.world().getDisplayName();
        GameSession session = Store.getActiveSession();
        if (session != null && session.getLevel() != null
                && session.getLevel().getName() != null) {
            levelName = session.getLevel().getName();
        }

        if (pauseOutcomeLayer != null) {
            pauseOutcomeLayer.showOutcome(outcome, levelName);
        }
        if (hoverHighlight != null) {
            hoverHighlight.setVisible(false);
        }
        if (toolCursor != null) {
            toolCursor.setVisible(false);
        }
    }

    private void leaveGameplay() {
        if (previewMode) {
            game.setScreen(new CheatScreen(game, app));
            return;
        }
        Store.setActiveSimulation(null);
        Store.setActiveSession(null);
        Store.setCurrentMenu(MenuName.GAME);
        game.goToScreenForCurrentMenu();
    }

    private void restartCurrentSession() {
        if (previewMode) {
            rebuildScene();
            return;
        }

        GameSession previous = Store.getActiveSession();
        User user = Store.getLoggedInUser();
        if (previous == null || previous.getLevel() == null || user == null) {
            leaveGameplay();
            return;
        }

        Set<PlantType> pendingBoosts = new LinkedHashSet<>();
        for (PlantType type : previous.getSelection().getChosen()) {
            Integer stored = user.getPlantBoosts().get(type);
            if (stored != null && stored > 0) {
                pendingBoosts.add(type);
            }
        }

        GameSession freshSession = new GameSession(
                previous.getLevel(),
                previous.getSelection(),
                previous.getDifficulty(),
                pendingBoosts
        );

        GameEngine freshEngine = new GameEngine();
        if (previous.getLevel().getAdventureConfig() != null) {
            AdventureInitializer.initialize(
                    freshEngine,
                    previous.getLevel().getAdventureConfig(),
                    user,
                    new SeededRandomSource()
            );
            freshEngine.setSkySunEnabled(
                    previous.getLevel().getAdventureConfig().isSkySunEnabled());
            freshEngine.setAdventureRuleSystem(new AdventureRuleSystem());
        }

        if (previous.getLevel().getWaveConfig() != null) {
            freshEngine.setWaveSystem(new WaveSystem(
                    previous.getLevel().getWaveConfig(),
                    new ChapterZombieSpecSource(
                            new DefaultZombieSpecSource(ZombieRegistry.getDefault()),
                            previous.getLevel().getName()
                    )
            ));
        }

        Store.setActiveSession(freshSession);
        Store.setActiveSimulation(new Simulation(freshEngine));
        Store.setCurrentMenu(MenuName.GAMEPLAY);

        previewMode = false;
        theme = BattlefieldTheme.forWorld(previous.getLevel().getWorld());
        initializeGameplayControllers();
        rebuildScene();
    }

    private void advanceGameplay(float delta) {
        if (previewMode || paused || missionIntroActive || outcomeShown || gameplayController == null) {
            return;
        }

        GameEngine liveEngine = engine();
        if (liveEngine == null) {
            return;
        }
        if (!liveEngine.isRunning()) {
            finishedEngine = liveEngine;
            showOutcome(liveEngine.getOutcome());
            return;
        }

        tickAccumulator += Math.min(delta, 0.25f);
        int ticks = (int) (tickAccumulator * GameEngine.TICKS_PER_SECOND);
        if (ticks <= 0) {
            return;
        }

        // Avoid a huge simulation catch-up after dragging/resizing the window.
        ticks = Math.min(ticks, 4);
        tickAccumulator -= ticks / (float) GameEngine.TICKS_PER_SECOND;

        gameplayController.advanceTime(ticks);

        // GameplayController performs rewards/conclusion and may clear Store's
        // active simulation, but the captured engine still carries the outcome.
        if (liveEngine.getOutcome() != GameOutcome.RUNNING) {
            finishedEngine = liveEngine;
            showOutcome(liveEngine.getOutcome());
        }
    }

    private void toggleTool(ToolMode requested) {
        toolMode = toolMode == requested ? ToolMode.NONE : requested;
        updateToolButtonState();
        if (toolMode == ToolMode.SHOVEL) {
            showAction("SHOVEL SELECTED");
        } else if (toolMode == ToolMode.PLANT_FOOD) {
            showAction("PLANT FOOD SELECTED");
        } else {
            showAction("");
        }
    }

    private void updateToolButtonState() {
        if (shovelButtonBackground != null) {
            shovelButtonBackground.setColor(
                    toolMode == ToolMode.SHOVEL ? 1f : 0.82f,
                    toolMode == ToolMode.SHOVEL ? 1f : 0.82f,
                    toolMode == ToolMode.SHOVEL ? 1f : 0.82f,
                    1f);
        }
        if (plantFoodButtonBackground != null) {
            plantFoodButtonBackground.setColor(
                    toolMode == ToolMode.PLANT_FOOD ? 1f : 0.84f,
                    toolMode == ToolMode.PLANT_FOOD ? 1f : 0.84f,
                    toolMode == ToolMode.PLANT_FOOD ? 1f : 0.84f,
                    1f);
        }
    }

    private void updateHud() {
        GameEngine engine = engine();
        int sun = previewMode || engine == null ? 50 : engine.getSun();
        int plantFood = previewMode || engine == null ? 0 : engine.getPlantFood();
        sunLabel.setText(String.valueOf(sun));
        plantFoodLabel.setText(plantFood + "/" + GameEngine.MAX_PLANT_FOOD);

        for (int index = 0; index < GameEngine.MAX_PLANT_FOOD; index++) {
            Actor slot = hudLayer.findActor("pf-slot-" + index);
            if (slot != null) {
                slot.setVisible(index < plantFood);
            }
        }
        sunLabel.setText(String.valueOf(sun));
        if (seedBank != null) {
            GameEngine liveEngine = engine();
            seedBank.sync(sun, type ->
                    !previewMode && liveEngine != null && liveEngine.isOnCooldown(type));
        }

        int currentWave = 0;
        int totalWaves = 0;
        GameSession session = Store.getActiveSession();
        if (!previewMode && engine != null) {
            currentWave = engine.getCurrentWave();
            totalWaves = session == null || session.getLevel() == null
                    || session.getLevel().getWaveConfig() == null
                    ? 0 : session.getLevel().getWaveConfig().getWaveCount();
        }

        float ratio;
        if (previewMode) {
            if (theme == BattlefieldTheme.DARK_AGES && darkAgesStateLayer != null) {
                previewLabel.setText(
                        "DEV PREVIEW  •  WAVE " + darkAgesStateLayer.getPreviewWave()
                                + "  •  N NEXT  •  SHIFT+N PREV");
            } else if (theme == BattlefieldTheme.ANCIENT_EGYPT) {
                previewLabel.setText(
                        "DEV PREVIEW  •  E TORNADO  •  PHASE-1 ADVANCE 1–4 CELLS");
            } else {
                previewLabel.setText("DEV PREVIEW");
            }
            waveLabel.setText("");
            ratio = 0.22f;
        } else {
            previewLabel.setText("");
            if (totalWaves > 0) {
                waveLabel.setText("WAVE " + currentWave + " / " + totalWaves);
                ratio = Math.max(0f, Math.min(1f, currentWave / (float) totalWaves));
            } else {
                waveLabel.setText("WAVE " + currentWave);
                ratio = Math.max(0f, Math.min(1f, currentWave / 5f));
            }
        }

        final float fillMax = 244f;
        waveFill.setWidth(fillMax * ratio);
        if (waveHead != null) {
            waveHead.setX(495f + fillMax * ratio - 17f);
        }

        if (messageSeconds > 0f) {
            messageSeconds -= Gdx.graphics.getDeltaTime();
            if (messageSeconds <= 0f && toolMode == ToolMode.NONE) {
                actionLabel.setText("");
            }
        }
    }

    private void syncPickups() {
        GameEngine engine = engine();
        String signature = pickupSignature(engine);
        if (signature.equals(pickupSignature)) {
            return;
        }
        pickupSignature = signature;
        pickupLayer.clearChildren();
        if (previewMode || engine == null || pamPlayer == null) {
            return;
        }

        for (Sun sun : engine.getSuns()) {
            int row = sun.getTileY();
            int column = sun.getTileX();
            Rectangle cell = layout.cellBounds(row, column);
            String clip = sunClip(sun.getType());
            PamEnvironmentActor actor = new PamEnvironmentActor(
                    pamPlayer, SUN_PAM, clip, 0.34f, 0f,
                    sun.isFalling() ? cell.height * 0.18f : 0f);
            actor.setBounds(cell.x, cell.y, cell.width, cell.height);
            final int x = column;
            final int y = row;
            actor.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float localX, float localY) {
                    if (gameplayController == null) {
                        return;
                    }
                    Result<Integer> result = gameplayController.collectSun(x, y);
                    showAction(result.getMessage());
                    pickupSignature = "";
                }
            });
            pickupLayer.addActor(actor);
        }
    }

    private String pickupSignature(GameEngine engine) {
        if (previewMode || engine == null) {
            return "preview";
        }
        StringBuilder signature = new StringBuilder();
        for (Sun sun : engine.getSuns()) {
            signature.append(sun.getTileX()).append(',')
                    .append(sun.getTileY()).append(',')
                    .append(sun.getType()).append(',')
                    .append(sun.getState()).append(';');
        }
        return signature.toString();
    }

    private String sunClip(SunType type) {
        if (type == SunType.SPECIAL) {
            return "blue";
        }
        if (type == SunType.RADIOACTIVE) {
            return "transition_red";
        }
        return "animation";
    }

    private void updatePointerHighlights() {
        if (hoverHighlight == null) {
            return;
        }
        if (paused || missionIntroActive || outcomeShown) {
            hoverHighlight.setVisible(false);
            if (toolCursor != null) {
                toolCursor.setVisible(false);
            }
            return;
        }

        Vector2 pointer = stage.getViewport().unproject(
                new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        int[] cell = layout.screenToCell(pointer.x, pointer.y);
        if (cell[0] < 0) {
            hoverHighlight.setVisible(false);
            if (toolCursor != null) {
                toolCursor.setVisible(false);
            }
            return;
        }

        Rectangle bounds = layout.cellBounds(cell[0], cell[1]);
        hoverHighlight.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        hoverHighlight.setVisible(true);
        updateToolCursor(bounds);

        if (Gdx.input.justTouched() && !draggingPlant) {
            selectedRow = cell[0];
            selectedColumn = cell[1];
            if (selectedHighlight != null) {
                selectedHighlight.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
                selectedHighlight.setVisible(true);
            }
            handleBoardAction(selectedRow, selectedColumn);
        }
    }

    private void updateToolCursor(Rectangle bounds) {
        if (toolCursor == null) {
            return;
        }
        if (toolMode == ToolMode.SHOVEL) {
            toolCursor.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
            toolCursor.setVisible(true);
        } else {
            toolCursor.setVisible(false);
        }
    }

    private void handleBoardAction(int row, int column) {
        if (previewMode) {
            return;
        }
        if (toolMode == ToolMode.SHOVEL && boardController != null) {
            Result<String> result = boardController.pluckPlant(column, row);
            showAction(result.getMessage());
            if (result.getStatus()) {
                removePlantedActor(row, column);
                toolMode = ToolMode.NONE;
                updateToolButtonState();
            }
            return;
        }
        if (toolMode == ToolMode.PLANT_FOOD && boardController != null) {
            Result<String> result = boardController.feedPlant(column, row);
            showAction(result.getMessage());
            if (result.getStatus()) {
                toolMode = ToolMode.NONE;
                updateToolButtonState();
            }
        }
    }

    /**
     * Direct key polling makes the test selector reliable even when Stage
     * consumes arrow-key events.
     */
    private void updateZombieTestSelection() {
        if (previewMode || missionIntroActive || paused || outcomeShown) {
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            zombieTestIndex--;

            if (zombieTestIndex < 0) {
                zombieTestIndex =
                        ZOMBIE_TEST_TYPES.length - 1;
            }

            showSelectedZombie();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            zombieTestIndex =
                    (zombieTestIndex + 1)
                            % ZOMBIE_TEST_TYPES.length;

            showSelectedZombie();
        }
    }

    private void showSelectedZombie() {
        ZombieType selected =
                ZOMBIE_TEST_TYPES[zombieTestIndex];

        showAction(
                "TEST ZOMBIE "
                        + (zombieTestIndex + 1)
                        + "/"
                        + ZOMBIE_TEST_TYPES.length
                        + ": "
                        + selected.name()
        );

        System.out.println(
                "[ZombieTest] Selected "
                        + (zombieTestIndex + 1)
                        + "/"
                        + ZOMBIE_TEST_TYPES.length
                        + ": "
                        + selected
        );
    }

    /**
     * Developer-only force spawn.
     *
     * It intentionally bypasses GameplayController.spawnZombie(), because that
     * controller enforces the active level's allowed-zombie roster.
     */
    private void spawnSelectedZombieCheat() {
        GameEngine currentEngine = engine();

        if (previewMode || currentEngine == null) {
            showAction("ZOMBIE TEST REQUIRES ACTIVE GAMEPLAY");
            return;
        }

        ZombieType selected =
                ZOMBIE_TEST_TYPES[zombieTestIndex];

        try {
            currentEngine.spawnZombie(
                    selected,
                    2,
                    8.0
            );

            showAction(
                    "CHEAT SPAWN: "
                            + selected.name()
            );

            System.out.println(
                    "[ZombieTest] Force-spawned "
                            + selected
            );

            if (zombieActorManager != null) {
                zombieActorManager.sync(currentEngine);
            }
        } catch (RuntimeException exception) {
            showAction(
                    "CHEAT SPAWN FAILED: "
                            + selected.name()
            );

            System.out.println(
                    "[ZombieTest] Force-spawn failed for "
                            + selected
                            + ": "
                            + exception.getMessage()
            );
        }
    }

    private void enablePlantTestingCheats() {
        GameEngine currentEngine = engine();

        if (previewMode || currentEngine == null) {
            showAction("PLANT TEST CHEATS REQUIRE ACTIVE GAMEPLAY");
            return;
        }

        currentEngine.disableCooldowns();
        currentEngine.setSun(TEST_CHEAT_SUN);
        plantTestingCheatsEnabled = true;
        showAction("CHEAT: INFINITE SUN + NO COOLDOWN");
    }

    private void addPlantFoodCheat() {
        if (previewMode || boardController == null) {
            showAction("PLANT FOOD CHEAT REQUIRES ACTIVE GAMEPLAY");
            return;
        }

        Result<String> result = boardController.cheatAddPlantFood();
        showAction(result.getMessage());
    }

    private void maintainPlantTestingCheats() {
        GameEngine currentEngine = engine();
        if (plantTestingCheatsEnabled && currentEngine != null) {
            currentEngine.setSun(TEST_CHEAT_SUN);
        }
    }

    private void showAction(String message) {
        if (actionLabel == null) {
            return;
        }
        String text = message == null ? "" : message.trim();
        if (text.length() > 52) {
            text = text.substring(0, 49) + "...";
        }
        actionLabel.setText(text.toUpperCase());
        messageSeconds = text.isEmpty() ? 0f : 2.3f;
    }

    private GameEngine engine() {
        Simulation simulation = Store.getActiveSimulation();
        return simulation == null ? null : simulation.getWorld();
    }

    private void initializePam() {
        try {
            FileHandle assets = Gdx.files.internal("pvz-asset-browser/pvz-assets");
            if (!assets.exists()) {
                assets = Gdx.files.local("pvz-asset-browser/pvz-assets");
            }
            if (!assets.exists()) {
                pamRoot = null;
                pamPlayer = null;
                return;
            }
            pamRoot = assets;
            pamTextures = new TextureBank("768", assets);
            pamPlayer = new PamPlayer(pamTextures, assets);
        } catch (RuntimeException exception) {
            pamRoot = null;
            pamPlayer = null;
            Gdx.app.error("GameplayScreen", "Could not initialize gameplay PAM assets", exception);
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

    private Texture makeWhiteTexture() {
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        Texture texture = new Texture(pixel);
        pixel.dispose();
        return texture;
    }

    private Texture makeRuneTexture() {
        Pixmap pixmap = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setColor(Color.WHITE);
        pixmap.drawCircle(64, 64, 52);
        pixmap.drawCircle(64, 64, 38);
        pixmap.drawLine(64, 10, 64, 118);
        pixmap.drawLine(10, 64, 118, 64);
        pixmap.drawLine(25, 25, 103, 103);
        pixmap.drawLine(103, 25, 25, 103);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private Texture makeDarkTintTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0.65f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void switchPreview(BattlefieldTheme next) {
        if (!previewMode || next == null || next == theme) {
            return;
        }
        theme = next;
        rebuildScene();
    }

    private BattlefieldSeedBank buildSeedBank() {
        GameSession session = Store.getActiveSession();

        System.out.println("========== BUILD SEED BANK ==========");
        System.out.println("previewMode = " + previewMode);
        System.out.println("session = " + session);

        if (previewMode || session == null || session.getSelection() == null) {
            System.out.println("SEED BANK RETURNED NULL");
            return null;
        }

        List<PlantDefinition> chosen = new ArrayList<>();

        for (PlantType type : session.getSelection().getChosen()) {
            PlantDefinition definition = findDefinition(type);

            System.out.println(
                    "chosen type = " + type
                            + " definition = " + definition
            );

            if (definition != null) {
                chosen.add(definition);
            }
        }

        System.out.println("FINAL CHOSEN SIZE = " + chosen.size());

        if (chosen.isEmpty()) {
            System.out.println("SEED BANK EMPTY");
            return null;
        }

        return new BattlefieldSeedBank(
                skin,
                loadTexture("ui/collection/ready.png"),
                loadTexture("ui/collection/selected.png"),
                darkTintTexture,
                this::loadPlantIcon,
                chosen,
                this
        );
    }

    private PlantDefinition findDefinition(PlantType type) {
        Result<ArrayList<PlantDefinition>> all = app.getCollectionController().showAllPlants();
        if (!all.getStatus() || all.getData() == null) {
            return null;
        }
        for (PlantDefinition definition : all.getData()) {
            if (definition.getType() == type) {
                return definition;
            }
        }
        return null;
    }

    private Texture loadPlantIcon(PlantType type) {
        return loadTexture("ui/collection/plants/" + type.name().toLowerCase(Locale.ROOT) + ".png");
    }

    private String plantIdlePam(PlantType type) {
        String name = type.name();
        return PLANT_PAM_ROOT + name + "/" + name + ".PAM";
    }

    @Override
    public boolean onDragStart(PlantType type) {
        if (previewMode || boardController == null) {
            return false;
        }

        GameEngine liveEngine = engine();

        if (liveEngine == null) {
            return false;
        }

        PlantDefinition definition = findDefinition(type);

        int cost = definition == null
                ? Integer.MAX_VALUE
                : definition.getCost();

        if (liveEngine.getSun() < cost) {
            showAction("NOT ENOUGH SUN");
            return false;
        }

        if (liveEngine.isOnCooldown(type)) {
            showAction("STILL RECHARGING");
            return false;
        }

        draggingPlant = true;

        if (pamPlayer != null) {
            dragGhost = new PamEnvironmentActor(
                    pamPlayer,
                    plantIdlePam(type),
                    "idle",
                    0.42f,
                    0f,
                    0f
            );

            dragGhost.setVisible(true);
            pickupLayer.addActor(dragGhost);
        }

        return true;
    }

    @Override
    public void onDragMove(PlantType type, float stageX, float stageY) {
        int[] cell = layout.screenToCell(stageX, stageY);
        if (cell[0] >= 0) {
            Rectangle bounds = layout.cellBounds(cell[0], cell[1]);
            if (hoverHighlight != null) {
                hoverHighlight.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
                hoverHighlight.setVisible(true);
            }
            if (dragGhost != null) {
                dragGhost.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        } else {
            if (hoverHighlight != null) {
                hoverHighlight.setVisible(false);
            }
            if (dragGhost != null) {
                float size = layout.cellBounds(0, 0).width;
                dragGhost.setBounds(stageX - size / 2f, stageY - size / 2f, size, size);
            }
        }
    }

    @Override
    public void onDragEnd(
            PlantType type,
            float stageX,
            float stageY
    ) {
        draggingPlant = false;

        if (dragGhost != null) {
            dragGhost.remove();
            dragGhost = null;
        }

        if (hoverHighlight != null) {
            hoverHighlight.setVisible(false);
        }

        int[] cell = layout.screenToCell(stageX, stageY);

        if (cell[0] < 0 || boardController == null) {
            return;
        }

        int row = cell[0];
        int column = cell[1];

        Result<String> result = boardController.plantPlant(type, cell[1], cell[0]);
        System.out.println("plantPlant result status=" + result.getStatus() + " msg=" + result.getMessage());

        showAction(result.getMessage());

        if (result.getStatus()) {
            spawnPlantedIdleActor(type, row, column);
        }
    }

    private void spawnPlantedIdleActor(PlantType type, int row, int column) {
        if (pamPlayer == null) {
            return;
        }
        removePlantedActor(row, column);
        Rectangle cell = layout.cellBounds(row, column);
        PamEnvironmentActor idle = new PamEnvironmentActor(
                pamPlayer, plantIdlePam(type), "idle", 0.42f, 0f, 0f);
        idle.setBounds(cell.x, cell.y, cell.width, cell.height);
        entityLayer.addActor(idle);
        placedPlantActors.put(row + "," + column, idle);
    }

    private void removePlantedActor(int row, int column) {
        PamEnvironmentActor existing = placedPlantActors.remove(row + "," + column);
        if (existing != null) {
            existing.remove();
        }
    }

    private void removeFinishedPlantActors(GameEngine currentEngine) {
        if (currentEngine == null || placedPlantActors.isEmpty()) {
            return;
        }

        for (String key : new ArrayList<>(placedPlantActors.keySet())) {
            String[] coordinates = key.split(",");
            if (coordinates.length != 2) {
                continue;
            }

            int row;
            int column;

            try {
                row = Integer.parseInt(coordinates[0]);
                column = Integer.parseInt(coordinates[1]);
            } catch (NumberFormatException exception) {
                continue;
            }

            if (!currentEngine.getGameMap()
                    .getTile(row, column)
                    .hasAnyPlant()) {
                removePlantedActor(row, column);
            }
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (pamTextures != null) {
            pamTextures.update();
        }

        updateZombieTestSelection();
        maintainPlantTestingCheats();
        advanceGameplay(delta);

        GameEngine displayEngine = engine() != null ? engine() : finishedEngine;
        removeFinishedPlantActors(displayEngine);

        if (zombieActorManager != null && displayEngine != null) {
            zombieActorManager.sync(displayEngine);
        }

        environmentLayer.sync(displayEngine, previewMode);
        if (darkAgesStateLayer != null) {
            darkAgesStateLayer.sync(displayEngine, previewMode);
        }
        if (frostbiteStateLayer != null) {
            frostbiteStateLayer.sync(displayEngine, previewMode);
        }
        if (chapterEffects != null) {
            chapterEffects.sync(displayEngine, previewMode);
        }
        if (specialLevelLayer != null) {
            specialLevelLayer.sync(
                    displayEngine, Store.getActiveSession(), previewMode, gameplayController);
        }
        syncPickups();
        stage.act(paused && !outcomeShown ? 0f : delta);
        updateHud();
        updatePointerHighlights();
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
        if (whiteTexture != null) {
            whiteTexture.dispose();
            whiteTexture = null;
        }
        if (runeTexture != null) {
            runeTexture.dispose();
            runeTexture = null;
        }
        if (pamTextures != null) {
            pamTextures.dispose();
            pamTextures = null;
        }
        if (darkTintTexture != null) {
            darkTintTexture.dispose();
            darkTintTexture = null;
        }
    }

    private final class BattlefieldKeys extends InputAdapter {
        @Override
        public boolean keyDown(int keycode) {
            if (missionIntroActive) {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                    continueFromMissionIntro();
                }
                return true;
            }
            if (keycode == Input.Keys.ESCAPE) {
                togglePause();
                return true;
            }
            if (keycode == Input.Keys.S) {
                toggleTool(ToolMode.SHOVEL);
                return true;
            }
            if (keycode == Input.Keys.F) {
                if (previewMode || engine() == null || engine().getPlantFood() > 0) {
                    toggleTool(ToolMode.PLANT_FOOD);
                }
                return true;
            }
            if (keycode == Input.Keys.E && previewMode && chapterEffects != null) {
                chapterEffects.previewPulse();
                if (theme == BattlefieldTheme.ANCIENT_EGYPT) {
                    showAction(
                            "EGYPT TORNADO  •  LANE "
                                    + (chapterEffects.getLastEgyptPreviewLane() + 1)
                                    + "  •  CARRIED "
                                    + chapterEffects.getLastEgyptPreviewAdvance()
                                    + " CELL"
                                    + (chapterEffects.getLastEgyptPreviewAdvance() == 1 ? "" : "S")
                    );
                } else {
                    showAction("");
                }
                return true;
            }
            if (keycode == Input.Keys.N
                    && previewMode
                    && theme == BattlefieldTheme.DARK_AGES
                    && darkAgesStateLayer != null) {
                boolean backwards =
                        Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

                int wave = backwards
                        ? darkAgesStateLayer.previousPreviewWave()
                        : darkAgesStateLayer.nextPreviewWave();

                // Re-sync immediately so the grave pair visibly changes on this key press.
                darkAgesStateLayer.sync(engine(), true);
                showAction("DARK AGES  •  PREVIEW WAVE " + wave
                        + (backwards ? "  •  PREVIOUS" : "  •  NEXT"));
                return true;
            }
            if (keycode == Input.Keys.T && previewMode && specialLevelLayer != null) {
                specialLevelLayer.cyclePreview();
                if (actionLabel != null) {
                    actionLabel.setText("");
                }
                return true;
            }
            if (keycode == Input.Keys.V && previewMode) {
                showOutcome(GameOutcome.WON);
                return true;
            }
            if (keycode == Input.Keys.B && previewMode) {
                showOutcome(GameOutcome.LOST);
                return true;
            }

            if (keycode == Input.Keys.Z && !previewMode) {
                spawnSelectedZombieCheat();
                return true;
            }
            if (keycode == Input.Keys.C && !previewMode) {
                enablePlantTestingCheats();
                return true;
            }
            if (keycode == Input.Keys.P && !previewMode) {
                addPlantFoodCheat();
                return true;
            }

            if (!previewMode) {
                return false;
            }
            if (keycode == Input.Keys.NUM_1) {
                switchPreview(BattlefieldTheme.ANCIENT_EGYPT);
                return true;
            }
            if (keycode == Input.Keys.NUM_2) {
                switchPreview(BattlefieldTheme.FROSTBITE_CAVES);
                return true;
            }
            if (keycode == Input.Keys.NUM_3) {
                switchPreview(BattlefieldTheme.BIG_WAVE_BEACH);
                return true;
            }
            if (keycode == Input.Keys.NUM_4) {
                switchPreview(BattlefieldTheme.DARK_AGES);
                return true;
            }
            return false;
        }
    }
}