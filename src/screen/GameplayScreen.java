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
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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
import model.inGame.plant.Plant;
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
import screen.gameplay.*;
import model.inGame.plant.PlantDefinition;

import java.util.ArrayList;
import java.util.IdentityHashMap;
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
    private static final String PLANT_FOOD_FX_PAM =
            "768/INITIAL/EFFECTS/PLANTFOOD_FX/PLANTFOOD_FX.PAM";
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
    private BattlefieldAnnouncementLayer announcementLayer;
    private LawnMowerAnimationLayer lawnMowerAnimationLayer;

    private PamEnvironmentActor hoverHighlight;
    private PamEnvironmentActor selectedHighlight;
    private Image toolCursor;
    private Image plantFoodCursor;
    private Label sunLabel;
    private Label plantFoodLabel;
    private Label waveLabel;
    private Label previewLabel;
    private Label actionLabel;
    private Image waveFill;
    private Image waveHead;
    private Image shovelButtonBackground;
    private Image plantFoodButtonBackground;
    private Group plantFoodCheatButton;
    private final Map<String, PamEnvironmentActor> placedPlantActors = new LinkedHashMap<>();
    private PamEnvironmentActor dragGhost;

    private BoardController boardController;
    private GameplayController gameplayController;

    private boolean previewMode;
    private boolean paused;
    private int selectedRow = -1;
    private int selectedColumn = -1;
    private ToolMode toolMode = ToolMode.NONE;
    private final Map<Sun, PamEnvironmentActor> sunPickupActors = new IdentityHashMap<>();
    private float messageSeconds;
    private float tickAccumulator;
    private boolean outcomeShown;
    private boolean missionIntroActive;
    private boolean draggingPlant;
    private GameEngine finishedEngine;
    private BattlefieldSeedBank seedBank;
    private PlantType armedPlantType;
    private Texture darkTintTexture;
    private final Map<String, Double> spawnedEffectForAttackAt = new LinkedHashMap<>();
    private static final Set<PlantType> MINE_TYPES = Set.of(
            PlantType.POTATO_MINE, PlantType.PRIMAL_POTATO_MINE);

    /** Mines show a brief planting pose, then a short recover pose, then idle forever
     *  — this visual sequence is independent of the real (gameplay) arm timer. */
    private static final double MINE_PLANT_POSE_SECONDS = 0.5;
    private static final double MINE_RECOVER_POSE_SECONDS = 1.0;
    private static final Map<PlantType, Float> ATTACK_TIME_SCALE = Map.of(
            PlantType.SQUASH, 0.45f,
            PlantType.DOOM_SHROOM, 0.5f);
    private static final Map<PlantType, Double> ATTACK_WINDOW_SECONDS = Map.of(
            PlantType.SQUASH, 0.7,
            PlantType.DOOM_SHROOM, 1.1);

    private final Map<String, PlantType> placedPlantTypes = new LinkedHashMap<>();
    private Group imitaterPickerLayer;
    private int pendingImitaterRow = -1;
    private int pendingImitaterColumn = -1;
    private final Map<String, Boolean> cactusMeleeActive = new LinkedHashMap<>();
    private final Map<String, Float> cactusUpPoseRemaining = new LinkedHashMap<>();
    private static final double CACTUS_MELEE_RANGE = 1.0; // guessed adjacency threshold — verify
    private static final float CACTUS_UP_POSE_SECONDS = 0.4f; // guessed transition-pose duration — verify

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

    private String plantIdlePam(PlantType type) {
        String resolved = PlantAnimationCatalog.resolveExistingPamPath(type, pamRoot);
        return resolved != null ? resolved : PlantAnimationCatalog.pamPath(type);
    }

    private String plantIdleClip(PlantType type) {
        return PlantAnimationCatalog.clipName(type, PlantAnimationState.IDLE);
    }


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
        // Handle the few fixed gameplay HUD controls before Scene2D.  This is
        // intentional: full-screen presentation actors can legitimately sit above
        // the HUD, and relying only on Stage.hit() made Pause/Shovel unreliable.
        // BattlefieldKeys returns false for every other pointer event, so normal
        // Scene2D buttons, seed cards, dialogue and board interaction still work.
        input.addProcessor(new BattlefieldKeys());
        input.addProcessor(stage);
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
        sunPickupActors.clear();
        tickAccumulator = 0f;
        outcomeShown = false;
        missionIntroActive = true;
        finishedEngine = null;
        placedPlantActors.clear();
        placedPlantTypes.clear();
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

        // Mower runs are kept on their own layer so the idle mower can disappear
        // from the environment immediately while its activation animation continues
        // visibly across all nine lawn tiles.
        lawnMowerAnimationLayer = new LawnMowerAnimationLayer(
                theme, layout, pamPlayer, pamRoot);
        lawnMowerAnimationLayer.prime(engine(), previewMode);
        stage.addActor(lawnMowerAnimationLayer);

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

        announcementLayer = new BattlefieldAnnouncementLayer(
                whiteTexture,
                skin,
                pamPlayer
        );
        GameEngine liveEngine = engine();
        announcementLayer.queueLevelIntro(
                Store.getActiveSession(),
                !previewMode && liveEngine != null && liveEngine.areWavesStarted()
        );
        announcementLayer.setSuppressed(missionIntroActive);
        stage.addActor(announcementLayer.root());
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

        }

        // Use the supplied shovel texture for targeting instead of the zen-garden
        // removal PAM. The icon follows the mouse directly while the PAM tile
        // highlight (when available) still identifies the affected lawn cell.
        toolCursor = new Image(loadTexture(HUD_ROOT + "shovel.png"));
        toolCursor.setScaling(Scaling.fit);
        toolCursor.setTouchable(Touchable.disabled);
        toolCursor.setVisible(false);
        toolCursor.setOrigin(Align.center);
        group.addActor(toolCursor);

        plantFoodCursor = new Image(loadTexture(HUD_ROOT + "plantfood_leaf.png"));
        plantFoodCursor.setScaling(Scaling.fit);
        plantFoodCursor.setTouchable(Touchable.disabled);
        plantFoodCursor.setVisible(false);
        plantFoodCursor.setOrigin(Align.center);
        plantFoodCursor.addAction(Actions.forever(Actions.sequence(
                Actions.scaleTo(1.08f, 1.08f, 0.34f),
                Actions.scaleTo(0.94f, 0.94f, 0.34f)
        )));
        group.addActor(plantFoodCursor);
        return group;
    }

    private Group buildHud() {
        Group hud = new Group();
        hud.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        // The HUD container itself is visual-only. Only its actual controls should
        // be hit targets; otherwise this 1280x720 group can steal board input.
        hud.setTouchable(Touchable.childrenOnly);

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
            slot.setTouchable(Touchable.disabled);
            slot.setName("pf-slot-" + index);
            slot.setVisible(false);
            group.addActor(slot);
        }
    }

    private Actor buildShovelButton() {
        Group button = new Group();
        button.setBounds(1198f, 8f, 62f, 62f);
        button.setTouchable(Touchable.enabled);

        shovelButtonBackground = new Image(loadTexture(HUD_ROOT + "shovel_button.png"));
        shovelButtonBackground.setScaling(Scaling.fit);
        shovelButtonBackground.setBounds(0f, 0f, 62f, 62f);
        shovelButtonBackground.setTouchable(Touchable.disabled);
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
        // Width includes the debug '+' control as well as the 205px bank area.
        button.setBounds(244f, 8f, 250f, 68f);
        button.setTouchable(Touchable.enabled);

        plantFoodButtonBackground = new Image(loadTexture(HUD_ROOT + "plantfood_bank3.png"));
        plantFoodButtonBackground.setScaling(Scaling.fit);
        plantFoodButtonBackground.setBounds(0f, 0f, 150f, 68f);
        plantFoodButtonBackground.setTouchable(Touchable.disabled);
        button.addActor(plantFoodButtonBackground);
        addPlantFoodSlots(button);

        Image leaf = new Image(loadTexture(HUD_ROOT + "plantfood_leaf.png"));
        leaf.setScaling(Scaling.fit);
        leaf.setBounds(9f, 14f, 42f, 42f);
        leaf.setTouchable(Touchable.disabled);
        button.addActor(leaf);

        plantFoodLabel = new Label("0 / 3", skin, "medium_outline");
        plantFoodLabel.setAlignment(Align.left);
        plantFoodLabel.setBounds(151f, 21f, 54f, 26f);
        plantFoodLabel.setTouchable(Touchable.disabled);
        button.addActor(plantFoodLabel);

        plantFoodCheatButton = buildPlantFoodCheatButton();
        button.addActor(plantFoodCheatButton);

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

    /**
     * Debug-only Phase-1/Phase-2 Plant Food cheat.  It is deliberately a
     * separate child control so clicking '+' does not also arm Plant Food.
     */
    private Group buildPlantFoodCheatButton() {
        Group plus = new Group();
        plus.setBounds(207f, 14f, 40f, 40f);
        plus.setTouchable(Touchable.enabled);

        Image back = new Image(whiteTexture);
        back.setColor(0.17f, 0.48f, 0.12f, 0.96f);
        back.setBounds(0f, 0f, 40f, 40f);
        back.setTouchable(Touchable.disabled);
        plus.addActor(back);

        Image inner = new Image(whiteTexture);
        inner.setColor(0.35f, 0.76f, 0.22f, 0.95f);
        inner.setBounds(3f, 3f, 34f, 34f);
        inner.setTouchable(Touchable.disabled);
        plus.addActor(inner);

        Label glyph = new Label("+", skin, "medium_outline");
        glyph.setAlignment(Align.center);
        glyph.setFontScale(1.18f);
        glyph.setBounds(0f, 0f, 40f, 40f);
        glyph.setTouchable(Touchable.disabled);
        plus.addActor(glyph);

        plus.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Prevent this click from bubbling to the Plant Food bank,
                // which would otherwise arm/disarm the targeting tool too.
                event.stop();
                addPlantFoodCheat();
                animatePlantFoodCheatPickup();
            }
        });
        return plus;
    }

    private Actor buildPauseButton() {
        Group button = new Group();
        button.setBounds(1200f, 650f, 58f, 58f);
        button.setTouchable(Touchable.enabled);

        Image pause = new Image(loadTexture(HUD_ROOT + "pause_button.png"));
        pause.setScaling(Scaling.fit);
        pause.setBounds(0f, 0f, 58f, 58f);
        pause.setTouchable(Touchable.disabled);
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
        if (previewMode || paused || missionIntroActive || outcomeShown
                || gameplayController == null
                || (announcementLayer != null && announcementLayer.isGameplayBlocked())) {
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

        Result<List<String>> advanced = gameplayController.advanceTime(ticks);
        if (announcementLayer != null && advanced.getStatus()) {
            announcementLayer.consumeEvents(advanced.getData(), liveEngine.getCurrentWave());
        }

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
        if (toolMode != ToolMode.SHOVEL && toolCursor != null) {
            toolCursor.setVisible(false);
        }
        if (toolMode != ToolMode.PLANT_FOOD && plantFoodCursor != null) {
            plantFoodCursor.setVisible(false);
        }
    }

    private void updateHud() {
        GameEngine engine = engine();
        int sun = previewMode || engine == null ? 50 : engine.getSun();
        int plantFood = previewMode || engine == null ? 0 : engine.getPlantFood();
        sunLabel.setText(String.valueOf(sun));
        plantFoodLabel.setText(plantFood + "/" + GameEngine.MAX_PLANT_FOOD);
        if (plantFoodCheatButton != null) {
            plantFoodCheatButton.setVisible(isDebugModeEnabled());
        }

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

        // Zombies enter from the right side of the lawn, so the wave meter
        // advances from right to left as danger moves toward the house.
        final float fillLeft = 512f;
        final float fillMax = 244f;
        float fillWidth = fillMax * ratio;
        waveFill.setX(fillLeft + fillMax - fillWidth);
        waveFill.setWidth(fillWidth);
        if (waveHead != null) {
            float headCenterX = fillLeft + fillMax * (1f - ratio);
            waveHead.setX(headCenterX - 17f);
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
        if (previewMode || engine == null || pamPlayer == null) {
            removeAllSunActors();
            return;
        }

        List<Sun> liveSuns = engine.getSuns();

        // Remove visuals immediately when their model sun was collected or expired.
        var iterator = sunPickupActors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Sun, PamEnvironmentActor> entry = iterator.next();
            if (!liveSuns.contains(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }

        for (Sun sun : liveSuns) {
            PamEnvironmentActor actor = sunPickupActors.get(sun);
            if (actor == null) {
                actor = createSunActor(sun);
                sunPickupActors.put(sun, actor);
                pickupLayer.addActor(actor);
            }

            actor.setClip(sunClip(sun.getType()));
            positionSunActor(actor, sun);
        }
    }

    private PamEnvironmentActor createSunActor(Sun sun) {
        PamEnvironmentActor actor = new PamEnvironmentActor(
                pamPlayer, SUN_PAM, sunClip(sun.getType()), 0.34f, 0f, 0f);
        actor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float localX, float localY) {
                collectSunImmediately(actor, sun);
            }
        });
        return actor;
    }

    private void positionSunActor(PamEnvironmentActor actor, Sun sun) {
        Rectangle cell = layout.cellBounds(sun.getTileY(), sun.getTileX());
        float x = cell.x;
        float y = cell.y;

        if (sun.isFalling()) {
            float progress = (float) sun.getFallProgress();
            double fallDuration = sun.getFallDurationSeconds();
            if (fallDuration > 1e-9) {
                // GameEngine advances at 10 Hz. Add the residual accumulator so
                // the visual position still moves smoothly every render frame.
                progress += (float) (tickAccumulator / fallDuration);
                progress = Math.max(0f, Math.min(1f, progress));
            }

            // Sky suns start above the visible lawn and continuously travel all
            // the way to their target cell instead of teleporting on landing.
            float startY = VIRTUAL_HEIGHT + cell.height * 0.15f;
            y = startY + (cell.y - startY) * progress;

            // A very small side-to-side drift keeps the fall readable and natural
            // without changing the tile in which the sun will land.
            x += (float) Math.sin(progress * Math.PI * 3.0) * cell.width * 0.045f;
        }

        actor.setBounds(x, y, cell.width, cell.height);
    }

    private void collectSunImmediately(PamEnvironmentActor actor, Sun sun) {
        if (gameplayController == null || actor == null || sun == null) {
            return;
        }
        Result<Integer> result = gameplayController.collectSun(sun.getTileX(), sun.getTileY());
        if (!result.getStatus()) {
            return;
        }

        // The model removes the sun synchronously. Remove its Scene2D actor in
        // this same input frame as well, so there is never a lingering asset.
        actor.remove();
        sunPickupActors.remove(sun);
        showAction(result.getMessage());
    }

    private void removeAllSunActors() {
        for (PamEnvironmentActor actor : sunPickupActors.values()) {
            actor.remove();
        }
        sunPickupActors.clear();
    }

    /**
     * Phase-2 specifies sun collection on mouse-over. Polling the visual actor
     * bounds makes pickup work while the sun is falling as well as after it lands.
     */
    private void updateSunHoverCollection() {
        if (previewMode || paused || missionIntroActive || outcomeShown
                || gameplayController == null || pickupLayer == null
                || (announcementLayer != null && announcementLayer.isGameplayBlocked())) {
            return;
        }

        Vector2 pointer = stage.getViewport().unproject(
                new Vector2(Gdx.input.getX(), Gdx.input.getY()));

        for (Map.Entry<Sun, PamEnvironmentActor> entry
                : new ArrayList<>(sunPickupActors.entrySet())) {
            PamEnvironmentActor actor = entry.getValue();
            if (actor.getStage() == null) {
                continue;
            }

            // Match the visible centre of the PAM rather than treating the
            // entire board cell as a pickup hotspot.
            float hitWidth = actor.getWidth() * 0.62f;
            float hitHeight = actor.getHeight() * 0.62f;
            float hitX = actor.getX() + (actor.getWidth() - hitWidth) * 0.5f;
            float hitY = actor.getY() + (actor.getHeight() - hitHeight) * 0.5f;
            if (pointer.x < hitX || pointer.x > hitX + hitWidth
                    || pointer.y < hitY || pointer.y > hitY + hitHeight) {
                continue;
            }

            collectSunImmediately(actor, entry.getKey());
            return;
        }
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
        if (paused || missionIntroActive || outcomeShown || imitaterPickerLayer != null) {
            hoverHighlight.setVisible(false);
            if (toolCursor != null) {
                toolCursor.setVisible(false);
            }
            if (plantFoodCursor != null) {
                plantFoodCursor.setVisible(false);
            }
            return;
        }

        Vector2 pointer = stage.getViewport().unproject(
                new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        updateShovelCursor(pointer);

        int[] cell = layout.screenToCell(pointer.x, pointer.y);
        if (cell[0] < 0) {
            hoverHighlight.setVisible(false);
            // Keep the shovel attached to the pointer even when the pointer is
            // between the HUD and the board.  It disappears only when shovel
            // mode is cancelled/used or gameplay is blocked.
            if (plantFoodCursor != null) {
                plantFoodCursor.setVisible(false);
            }
            return;
        }

        Rectangle bounds = layout.cellBounds(cell[0], cell[1]);
        hoverHighlight.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        if (toolMode == ToolMode.PLANT_FOOD) {
            hoverHighlight.setColor(0.68f, 1f, 0.46f, 1f);
        } else if (toolMode == ToolMode.SHOVEL) {
            hoverHighlight.setColor(1f, 0.86f, 0.60f, 1f);
        } else {
            hoverHighlight.setColor(Color.WHITE);
        }
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

    private void updateShovelCursor(Vector2 pointer) {
        if (toolCursor == null) {
            return;
        }
        if (toolMode != ToolMode.SHOVEL) {
            toolCursor.setVisible(false);
            return;
        }

        // Keep the image close to the real OS pointer while leaving the pointer
        // itself unobstructed enough to make the target tile obvious.
        float width = 92f;
        float height = 49f;
        toolCursor.setBounds(
                pointer.x - width * 0.44f,
                pointer.y - height * 0.46f,
                width, height);
        toolCursor.setVisible(true);
        toolCursor.toFront();
    }

    private void updateToolCursor(Rectangle bounds) {
        if (plantFoodCursor != null) {
            if (toolMode == ToolMode.PLANT_FOOD) {
                float size = Math.min(bounds.width, bounds.height) * 0.58f;
                plantFoodCursor.setBounds(
                        bounds.x + bounds.width * 0.5f - size * 0.5f,
                        bounds.y + bounds.height * 0.5f - size * 0.5f,
                        size, size);
                plantFoodCursor.setVisible(true);
            } else {
                plantFoodCursor.setVisible(false);
            }
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
                if (selectedHighlight != null) {
                    selectedHighlight.setVisible(false);
                }
                updateToolButtonState();
            }
            return;
        }
        if (toolMode == ToolMode.PLANT_FOOD && boardController != null) {
            Result<String> result = boardController.feedPlant(column, row);
            showAction(result.getMessage());
            if (result.getStatus()) {
                spawnPlantFoodEffect(row, column);
                toolMode = ToolMode.NONE;
                if (selectedHighlight != null) {
                    selectedHighlight.setVisible(false);
                }
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
        if (!isDebugModeEnabled()) {
            showAction("ENABLE DEBUG MODE IN SETTINGS");
            return;
        }

        Result<String> result = boardController.cheatAddPlantFood();
        showAction(result.getMessage());
    }

    private boolean isDebugModeEnabled() {
        User user = Store.getLoggedInUser();
        return user != null
                && user.getSettings() != null
                && user.getSettings().isDebugMode();
    }

    /** Tiny pickup burst so the cheat reads as Plant Food entering the bank. */
    private void animatePlantFoodCheatPickup() {
        if (!isDebugModeEnabled() || pamPlayer == null || hudLayer == null) {
            return;
        }
        PamEnvironmentActor pickup = new PamEnvironmentActor(
                pamPlayer, PLANT_FOOD_PAM, "animation", 0.27f, 0f, 0f);
        pickup.setTouchable(Touchable.disabled);
        pickup.setBounds(286f, 78f, 90f, 90f);
        pickup.getColor().a = 0f;
        pickup.setScale(0.82f);
        pickup.setOrigin(Align.center);
        pickup.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.fadeIn(0.12f),
                        Actions.scaleTo(1f, 1f, 0.16f),
                        Actions.moveBy(0f, 24f, 0.16f)
                ),
                Actions.delay(0.24f),
                Actions.parallel(
                        Actions.fadeOut(0.22f),
                        Actions.moveTo(292f, 28f, 0.22f),
                        Actions.scaleTo(0.70f, 0.70f, 0.22f)
                ),
                Actions.removeActor()
        ));
        hudLayer.addActor(pickup);
        pickup.toFront();
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
                    pamPlayer, plantIdlePam(type), plantIdleClip(type), 0.42f, 0f, 0f);

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
    public void onDragEnd(PlantType type, float stageX, float stageY) {
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

        if (type == PlantType.IMITATER) {
            openImitaterPicker(row, column);
            return;
        }

        Result<String> result = boardController.plantPlant(type, column, row);
        showAction(result.getMessage());
        if (result.getStatus()) {
            spawnPlantedIdleActor(type, row, column);
        }
    }

    private void openImitaterPicker(int row, int column) {
        GameSession session = Store.getActiveSession();
        if (session == null || session.getSelection() == null) {
            showAction("NO LOADOUT AVAILABLE");
            return;
        }

        List<PlantType> options = new ArrayList<>();
        for (PlantType candidate : session.getSelection().getChosen()) {
            if (candidate != PlantType.IMITATER) {
                options.add(candidate);
            }
        }
        if (options.isEmpty()) {
            showAction("NOTHING TO IMITATE");
            return;
        }

        showImitaterPickerLayer(options);
        pendingImitaterRow = row;
        pendingImitaterColumn = column;
    }

    private void showImitaterPickerLayer(List<PlantType> options) {
        removeImitaterPickerActor();

        Group layer = new Group();
        layer.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        Image backdrop = new Image(darkTintTexture);
        backdrop.setScaling(Scaling.stretch);
        backdrop.setBounds(0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        backdrop.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                cancelImitaterPicker();
            }
        });
        layer.addActor(backdrop);

        Label prompt = new Label("CHOOSE A PLANT TO IMITATE", skin, "medium_outline");
        prompt.setAlignment(Align.center);

        float iconSize = 74f;
        float spacing = 12f;
        float totalWidth = options.size() * iconSize + Math.max(0, options.size() - 1) * spacing;
        float startX = (VIRTUAL_WIDTH - totalWidth) / 2f;
        float iconY = VIRTUAL_HEIGHT / 2f - iconSize / 2f;

        prompt.setBounds(0f, iconY + iconSize + 20f, VIRTUAL_WIDTH, 30f);
        layer.addActor(prompt);

        for (int i = 0; i < options.size(); i++) {
            PlantType option = options.get(i);
            Image icon = new Image(loadPlantIcon(option));
            icon.setScaling(Scaling.fit);
            icon.setBounds(startX + i * (iconSize + spacing), iconY, iconSize, iconSize);
            icon.setTouchable(Touchable.enabled);
            icon.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    event.stop();
                    confirmImitaterChoice(option);
                }
            });
            layer.addActor(icon);
        }

        imitaterPickerLayer = layer;
        stage.addActor(imitaterPickerLayer);
        imitaterPickerLayer.toFront();
    }

    private void removeImitaterPickerActor() {
        if (imitaterPickerLayer != null) {
            imitaterPickerLayer.remove();
            imitaterPickerLayer = null;
        }
    }

    private void confirmImitaterChoice(PlantType copiedType) {
        int row = pendingImitaterRow;
        int column = pendingImitaterColumn;
        closeImitaterPicker();

        if (row < 0 || column < 0 || boardController == null) {
            return;
        }

        Result<String> result = boardController.plantImitater(copiedType, column, row);
        showAction(result.getMessage());
        if (result.getStatus()) {
            spawnPlantedIdleActor(PlantType.IMITATER, row, column);
        }
    }

    private void cancelImitaterPicker() {
        closeImitaterPicker();
        showAction("");
    }

    private void closeImitaterPicker() {
        removeImitaterPickerActor();
        pendingImitaterRow = -1;
        pendingImitaterColumn = -1;
    }

    private void spawnPlantedIdleActor(PlantType type, int row, int column) {
        if (pamPlayer == null) {
            return;
        }
        removePlantedActor(row, column);
        Rectangle cell = layout.cellBounds(row, column);
        String initialClip = MINE_TYPES.contains(type) ? "plant" : "idle";
        PamEnvironmentActor idle = new PamEnvironmentActor(
                pamPlayer, plantIdlePam(type), initialClip, 0.42f, 0f, 0f);
        idle.setIdleClips(PlantAnimationCatalog.idleClipSequence(type));
        if (!MINE_TYPES.contains(type)) {
            idle.resumeIdleCycle();
        }
        idle.setBounds(cell.x, cell.y, cell.width, cell.height);
        entityLayer.addActor(idle);
        String key = row + "," + column;
        placedPlantActors.put(key, idle);
        placedPlantTypes.put(key, type);
    }

    private void removePlantedActor(int row, int column) {
        String key = row + "," + column;
        PamEnvironmentActor existing = placedPlantActors.remove(key);
        placedPlantTypes.remove(key);
        spawnedEffectForAttackAt.remove(key);
        cactusMeleeActive.remove(key);
        cactusUpPoseRemaining.remove(key);
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

                PlantType vanishedType = placedPlantTypes.get(key);
                boolean alreadyHandledViaAttackClip = spawnedEffectForAttackAt.containsKey(key);
                if (!alreadyHandledViaAttackClip
                        && vanishedType != null
                        && PlantAttackEffectCatalog.forType(vanishedType) != null) {
                    spawnAttackEffect(vanishedType, row, column);
                }
                removePlantedActor(row, column);
            }
        }
    }

    private void syncPlantAnimations() {
        if (previewMode) {
            return;
        }
        GameEngine currentEngine = engine();
        if (currentEngine == null) {
            return;
        }
        for (Map.Entry<String, PamEnvironmentActor> entry : placedPlantActors.entrySet()) {
            String key = entry.getKey();
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
            model.inGame.plant.Plant plant = currentEngine.getGameMap().getTile(row, column).getPrimaryPlant();
            if (plant == null) {
                continue;
            }
            PamEnvironmentActor actor = entry.getValue();
            PlantType type = plant.getEffectiveType();
            if (type == PlantType.GRAPESHOT) {
                System.out.println("[GrapeshotAnim] attackingWithin(0.6)=" + plant.isAttackingWithin(0.6)
                        + " age=" + plant.getAgeSeconds());
            }

            if (type == PlantType.SWEET_POTATO) {
                String damageClip = PlantAnimationCatalog.clipName(type, PlantAnimationState.DAMAGE);
                if (plant.isDamagedWithin(0.4)) {
                    actor.setClip(damageClip);
                } else {
                    actor.resumeIdleCycle();
                }
            } else if (DEFENDER_DAMAGE_TYPES.contains(type)) {
                double hpRatio = plant.getMaxHp() > 0
                        ? plant.getHp() / (double) plant.getMaxHp() : 1.0;
                String damageClip = PlantAnimationCatalog.damageStageClip(type, hpRatio);
                if (damageClip != null) {
                    actor.setClip(damageClip);
                } else {
                    actor.resumeIdleCycle();
                }
            }  else if (MINE_TYPES.contains(type)) {
                updateMineAnimation(actor, plant, type);
            }else if (type == PlantType.CACTUS) {
                updateCactusAnimation(actor, plant, key);
            }else {
                String attackClip = PlantAnimationCatalog.attackClipName(type);
                double window = ATTACK_WINDOW_SECONDS.getOrDefault(type, 0.6);
                if (plant.isAttackingWithin(window)) {
                    actor.setClip(attackClip);
                    actor.setTimeScale(ATTACK_TIME_SCALE.getOrDefault(type, 1f));
                } else {
                    actor.resumeIdleCycle();
                }
            }

            double lastAttackAt = plant.getState("LAST_ATTACK_AT", Double.class, Double.NEGATIVE_INFINITY);
            Double alreadySpawnedFor = spawnedEffectForAttackAt.get(key);
            if (lastAttackAt > Double.NEGATIVE_INFINITY
                    && (alreadySpawnedFor == null || alreadySpawnedFor < lastAttackAt)) {
                spawnedEffectForAttackAt.put(key, lastAttackAt);
                spawnAttackEffect(type, row, column);
            }
        }
    }

    private void updateCactusAnimation(PamEnvironmentActor actor, Plant plant, String key) {
        GameEngine currentEngine = engine();
        boolean meleeZombiePresent = currentEngine != null
                && currentEngine.getFirstZombieAhead(plant, CACTUS_MELEE_RANGE) != null;
        boolean wasMelee = cactusMeleeActive.getOrDefault(key, false);

        if (meleeZombiePresent) {
            cactusMeleeActive.put(key, true);
            cactusUpPoseRemaining.remove(key);
            boolean attacking = plant.isAttackingWithin(0.6);
            actor.setClip(PlantAnimationCatalog.clipName(PlantType.CACTUS,
                    attacking ? PlantAnimationState.DOWN_ATTACK : PlantAnimationState.DOWN_IDLE));
            return;
        }

        cactusMeleeActive.put(key, false);
        if (wasMelee) {
            cactusUpPoseRemaining.put(key, CACTUS_UP_POSE_SECONDS);
        }

        Float remaining = cactusUpPoseRemaining.get(key);
        if (remaining != null) {
            actor.setClip(PlantAnimationCatalog.clipName(PlantType.CACTUS, PlantAnimationState.UP));
            remaining -= Gdx.graphics.getDeltaTime();
            if (remaining <= 0f) {
                cactusUpPoseRemaining.remove(key);
            } else {
                cactusUpPoseRemaining.put(key, remaining);
            }
            return;
        }

        String attackClip = PlantAnimationCatalog.attackClipName(PlantType.CACTUS);
        if (plant.isAttackingWithin(ATTACK_WINDOW_SECONDS.getOrDefault(PlantType.CACTUS, 0.6))) {
            actor.setClip(attackClip);
        } else {
            actor.resumeIdleCycle();
        }
    }

    private void updateMineAnimation(PamEnvironmentActor actor, Plant plant, PlantType type) {
        if (plant.isAttackingWithin(0.6)) {
            actor.setClip(PlantAnimationCatalog.attackClipName(type));
            return;
        }
        double age = plant.getAgeSeconds();
        if (age < MINE_PLANT_POSE_SECONDS) {
            actor.setClip(PlantAnimationCatalog.clipName(type, PlantAnimationState.PLANT));
        } else if (age < MINE_PLANT_POSE_SECONDS + MINE_RECOVER_POSE_SECONDS) {
            actor.setClip(PlantAnimationCatalog.clipName(type, PlantAnimationState.RECOVER));
        } else {
            actor.resumeIdleCycle();
        }
    }

    private static final Set<PlantType> DEFENDER_DAMAGE_TYPES = Set.of(
            PlantType.WALL_NUT, PlantType.TALL_NUT, PlantType.ENDURIAN,
            PlantType.GARLIC, PlantType.EXPLODE_O_NUT);

    private void spawnAttackEffect(PlantType type, int row, int column) {
        if (pamPlayer == null) {
            return;
        }
        PlantAttackEffectCatalog.EffectSpec spec = PlantAttackEffectCatalog.forType(type);
        if (spec == null) {
            return;
        }
        Rectangle cell = layout.cellBounds(row, column);
        PamTransientEffectActor effect = new PamTransientEffectActor(
                pamPlayer, spec.pamPath(), spec.clip(), 0.42f, 0f, 0f, spec.duration());
        effect.setBounds(cell.x, cell.y, cell.width, cell.height);
        entityLayer.addActor(effect);
    }

    /**
     * Generic PVZ2 Plant Food glow. The gameplay effect itself remains model-owned;
     * this actor only makes a successful feed visually obvious and sits behind plants.
     */
    private void spawnPlantFoodEffect(int row, int column) {
        if (pamPlayer == null || entityLayer == null) {
            return;
        }
        Rectangle cell = layout.cellBounds(row, column);
        float width = cell.width * 1.28f;
        float height = cell.height * 1.42f;
        PamTransientEffectActor effect = new PamTransientEffectActor(
                pamPlayer, PLANT_FOOD_FX_PAM, "plantfood", 0.43f, 0f, 3f, 2.5f);
        effect.setBounds(
                cell.x + (cell.width - width) * 0.5f,
                cell.y + (cell.height - height) * 0.5f,
                width, height);
        entityLayer.addActor(effect);
        effect.toBack();
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
        if (announcementLayer != null) {
            announcementLayer.setSuppressed(paused || missionIntroActive || outcomeShown);
            announcementLayer.update(delta);
        }
        advanceGameplay(delta);

        GameEngine displayEngine = engine() != null ? engine() : finishedEngine;

        if (displayEngine != null) {
            // FIRST: detect attacks and spawn their visual effects
            syncPlantAnimations();

            // SECOND: remove plants that no longer exist
            removeFinishedPlantActors(displayEngine);
        }


        if (zombieActorManager != null && displayEngine != null) {
            zombieActorManager.sync(displayEngine);
        }

        environmentLayer.sync(displayEngine, previewMode);
        if (lawnMowerAnimationLayer != null) {
            lawnMowerAnimationLayer.sync(displayEngine, previewMode);
        }
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
        updateSunHoverCollection();
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

    /**
     * Returns true while a modal/dialogue owns pointer input.  Fixed HUD hit boxes
     * must respect those overlays instead of clicking through them.
     */
    private boolean hudPointerBlocked() {
        return missionIntroActive
                || outcomeShown
                || (announcementLayer != null && announcementLayer.isGameplayBlocked());
    }

    private boolean pointerInside(Vector2 point, float x, float y, float width, float height) {
        return point.x >= x && point.x <= x + width
                && point.y >= y && point.y <= y + height;
    }

    private final class BattlefieldKeys extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (imitaterPickerLayer != null) {
                return false; // let Stage/backdrop handle it
            }

            if (button != Input.Buttons.LEFT || stage == null) {
                return false;
            }

            Vector2 point = stage.getViewport().unproject(new Vector2(screenX, screenY));

            // Do not click through mission/dialogue/outcome overlays.
            if (hudPointerBlocked()) {
                return false;
            }

            // Pause: top-right 58x58 HUD control. This mirrors ESC and is handled
            // before Stage so no decorative Scene2D actor can swallow the click.
            if (pointerInside(point, 1200f, 650f, 58f, 58f)) {
                togglePause();
                return true;
            }

            if (paused) {
                return false;
            }

            // Shovel: bottom-right 62x62 HUD control.
            if (pointerInside(point, 1198f, 8f, 62f, 62f)) {
                toggleTool(ToolMode.SHOVEL);
                return true;
            }

            // Debug Plant Food '+'. Its stage-space bounds are the parent bank
            // position (244,8) plus the child's local bounds (207,14,40,40).
            if (isDebugModeEnabled()
                    && pointerInside(point, 451f, 22f, 40f, 40f)) {
                addPlantFoodCheat();
                animatePlantFoodCheatPickup();
                return true;
            }

            // Plant Food bank itself. Keep the '+' region separate above so a
            // cheat click never arms the targeting tool.
            if (pointerInside(point, 244f, 8f, 205f, 68f)) {
                if (!previewMode && engine() != null && engine().getPlantFood() <= 0) {
                    showAction("NO PLANT FOOD");
                } else {
                    toggleTool(ToolMode.PLANT_FOOD);
                }
                return true;
            }

            return false;
        }

        @Override
        public boolean keyDown(int keycode) {
            if (imitaterPickerLayer != null) {
                return keycode == Input.Keys.ESCAPE;
            }

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