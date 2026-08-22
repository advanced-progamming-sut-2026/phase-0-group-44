package screen.gameplay;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controller.GameplayController;
import model.GameEngine;
import model.Result;
import model.inGame.GameSession;
import model.level.AdventureLevelConfig;
import model.level.SpecialLevelType;
import model.level.TileCoordinate;
import model.level.TimedWarObjective;
import model.sim.TickContext;
import model.sim.adventure.AdventureRuntimeState;
import pvz.libpvz.pam.PamPlayer;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Phase-2 visual layer for Adventure special-level rules.
 *
 * <p>The layer only reads the already-existing Phase-1 model state.  It does not
 * duplicate special-level rules or mutate the map, except for invoking the
 * existing GameplayController action when the player presses START WAVE.</p>
 */
public final class BattlefieldSpecialLevelLayer {
    private static final String PROTECT_TILE_PAM =
            "768/INITIAL/BACKGROUNDS/PROTECT_TILE/PROTECT_TILE.PAM";
    private static final String CLOCK_PAM =
            "768/INITIAL/UI/QUESTS/DAILY_QUEST_CLOCK_ICON/DAILY_QUEST_CLOCK_ICON.PAM";

    private static final SpecialLevelType[] PREVIEW_ORDER = {
            SpecialLevelType.SAVE_OUR_SEEDS,
            SpecialLevelType.DEAD_LINE,
            SpecialLevelType.TIMED_WAR,
            SpecialLevelType.LOVE_YOUR_PLANTS,
            SpecialLevelType.PLANT_WHAT_YOU_GET,
            SpecialLevelType.CONVEYOR_BELT,
            SpecialLevelType.LOCKED_PLANTS,
            SpecialLevelType.NIGHT_OPS,
            null
    };

    private final BattlefieldLayout layout;
    private final Texture whiteTexture;
    private final Texture sunTexture;
    private final Texture zombieHeadTexture;
    private final Skin skin;
    private final PamPlayer pamPlayer;
    private final FileHandle pamRoot;
    private final Consumer<String> messageSink;

    private final Group boardLayer = new Group();
    private final Group hudLayer = new Group();
    private final Group objectiveLayer = new Group();
    private final Group introLayer = new Group();

    private SpecialLevelType previewType;
    private boolean previewWavesStarted;
    private String renderedSignature = "";
    private SpecialLevelType lastIntroType;

    private GameEngine currentEngine;
    private GameSession currentSession;
    private GameplayController currentGameplayController;
    private boolean currentPreviewMode;

    public BattlefieldSpecialLevelLayer(
            BattlefieldLayout layout,
            Texture whiteTexture,
            Texture sunTexture,
            Texture zombieHeadTexture,
            Skin skin,
            PamPlayer pamPlayer,
            FileHandle pamRoot,
            Consumer<String> messageSink
    ) {
        this.layout = layout;
        this.whiteTexture = whiteTexture;
        this.sunTexture = sunTexture;
        this.zombieHeadTexture = zombieHeadTexture;
        this.skin = skin;
        this.pamPlayer = pamPlayer;
        this.pamRoot = pamRoot;
        this.messageSink = messageSink;
        boardLayer.setSize(1280f, 720f);
        hudLayer.setSize(1280f, 720f);
        objectiveLayer.setSize(1280f, 720f);
        introLayer.setSize(1280f, 720f);
        // These are full-screen presentation containers.  They should only
        // intercept input where they actually have an interactive child.
        // Leaving them Touchable.enabled makes Stage.hit() block Pause,
        // Shovel, Plant Food and other HUD buttons underneath.
        hudLayer.setTouchable(Touchable.childrenOnly);
        objectiveLayer.setTouchable(Touchable.childrenOnly);
        introLayer.setTouchable(Touchable.childrenOnly);
        hudLayer.addActor(objectiveLayer);
        hudLayer.addActor(introLayer);
    }

    public Group boardLayer() {
        return boardLayer;
    }

    public Group hudLayer() {
        return hudLayer;
    }

    /** Cycles a harmless visual-only special-level preview. */
    public String cyclePreview() {
        int index = -1;
        for (int i = 0; i < PREVIEW_ORDER.length; i++) {
            if (PREVIEW_ORDER[i] == previewType) {
                index = i;
                break;
            }
        }
        previewType = PREVIEW_ORDER[(index + 1) % PREVIEW_ORDER.length];
        previewWavesStarted = false;
        renderedSignature = "";
        lastIntroType = null;
        return previewType == null ? "NORMAL LEVEL" : displayName(previewType);
    }

    public void sync(
            GameEngine engine,
            GameSession session,
            boolean previewMode,
            GameplayController gameplayController
    ) {
        currentEngine = engine;
        currentSession = session;
        currentPreviewMode = previewMode;
        currentGameplayController = gameplayController;

        SpecialLevelType type = resolveType(session, previewMode);
        String signature = signature(type, engine, session, previewMode);
        if (signature.equals(renderedSignature)) {
            return;
        }
        renderedSignature = signature;

        boardLayer.clearChildren();
        objectiveLayer.clearChildren();
        if (type == null) {
            lastIntroType = null;
            return;
        }

        buildBoardMarkers(type, engine, session, previewMode);
        buildObjectiveHud(type, engine, session, previewMode);
        if (type != lastIntroType) {
            if (!previewMode) {
                addMissionIntro(type, objectiveDetail(type, engine, session, false));
            }
            lastIntroType = type;
        }
    }

    private SpecialLevelType resolveType(GameSession session, boolean previewMode) {
        if (previewMode) {
            return previewType;
        }
        AdventureLevelConfig config = config(session);
        return config == null ? null : config.getSpecialType();
    }

    private String signature(
            SpecialLevelType type,
            GameEngine engine,
            GameSession session,
            boolean previewMode
    ) {
        if (type == null) {
            return "none";
        }
        StringBuilder value = new StringBuilder(type.name());
        if (previewMode) {
            value.append("|preview|").append(previewWavesStarted);
            return value.toString();
        }
        AdventureLevelConfig config = config(session);
        AdventureRuntimeState state = engine == null ? null : engine.getAdventureState();
        if (config == null) {
            return value.append("|no-config").toString();
        }
        switch (type) {
            case SAVE_OUR_SEEDS -> {
                if (state != null) {
                    value.append('|').append(state.getProtectedTiles());
                }
            }
            case DEAD_LINE -> value.append('|').append(config.getDeadLineColumn());
            case TIMED_WAR -> {
                long elapsed = state == null ? 0L : state.getElapsedTicks();
                long remainingTicks = Math.max(0L, config.getTimedWarTicks() - elapsed);
                long seconds = (remainingTicks + TickContext.TICKS_PER_SECOND - 1L)
                        / TickContext.TICKS_PER_SECOND;
                value.append('|').append(seconds)
                        .append('|').append(timedProgress(engine, config));
            }
            case LOVE_YOUR_PLANTS -> value.append('|')
                    .append(engine == null ? 0 : engine.getPlantLossCount());
            case PLANT_WHAT_YOU_GET -> value.append('|')
                    .append(engine != null && engine.areWavesStarted());
            case CONVEYOR_BELT -> {
                if (state != null) {
                    value.append('|').append(state.getConveyorPackets());
                }
            }
            default -> {
                // Static objective card only.
            }
        }
        return value.toString();
    }

    private void buildBoardMarkers(
            SpecialLevelType type,
            GameEngine engine,
            GameSession session,
            boolean previewMode
    ) {
        if (type == SpecialLevelType.SAVE_OUR_SEEDS) {
            addProtectedTiles(engine, previewMode);
        } else if (type == SpecialLevelType.DEAD_LINE) {
            addDeadLine(session, previewMode);
        }
    }

    private void addProtectedTiles(GameEngine engine, boolean previewMode) {
        if (previewMode || engine == null || engine.getAdventureState() == null) {
            addProtectMarker(2, 0);
            addProtectMarker(2, 2);
            addProtectMarker(2, 4);
            return;
        }
        for (TileCoordinate coordinate : engine.getAdventureState().getProtectedTiles()) {
            addProtectMarker(coordinate.getX(), coordinate.getY());
        }
    }

    private void addProtectMarker(int column, int row) {
        Rectangle cell = layout.cellBounds(row, column);

        // Keep the plant itself visible: mark the protected CELL rather than
        // covering it with the full caution-tile animation.
        float inset = 5f;
        float line = 4f;
        Color gold = new Color(1f, 0.78f, 0.08f, 0.92f);

        addOutlinePart(cell.x + inset, cell.y + inset,
                cell.width - inset * 2f, line, gold);
        addOutlinePart(cell.x + inset, cell.y + cell.height - inset - line,
                cell.width - inset * 2f, line, gold);
        addOutlinePart(cell.x + inset, cell.y + inset,
                line, cell.height - inset * 2f, gold);
        addOutlinePart(cell.x + cell.width - inset - line, cell.y + inset,
                line, cell.height - inset * 2f, gold);

        if (pamPlayer != null && pamRoot != null) {
            PamEnvironmentActor marker = new PamEnvironmentActor(
                    pamPlayer, PROTECT_TILE_PAM, "animation", 0.20f, 0f, 0f);
            marker.setBounds(cell.x + 7f, cell.y + cell.height - 39f, 32f, 32f);
            marker.getColor().a = 0.88f;
            boardLayer.addActor(marker);
        }
    }

    private void addOutlinePart(float x, float y, float width, float height, Color color) {
        Image line = new Image(whiteTexture);
        line.setColor(color);
        line.setBounds(x, y, width, height);
        line.addAction(Actions.forever(Actions.sequence(
                Actions.alpha(0.98f, 0.55f),
                Actions.alpha(0.58f, 0.55f)
        )));
        boardLayer.addActor(line);
    }

    private void addDeadLine(GameSession session, boolean previewMode) {
        AdventureLevelConfig config = config(session);
        int column = previewMode || config == null ? 2 : config.getDeadLineColumn();
        column = Math.max(0, Math.min(8, column));
        Rectangle board = layout.boardBounds();
        Rectangle cell = layout.cellBounds(0, column);
        float lineX = cell.x;

        Image glow = new Image(whiteTexture);
        glow.setColor(1f, 0.05f, 0.02f, 0.18f);
        glow.setBounds(lineX - 8f, board.y, 16f, board.height);
        glow.addAction(Actions.forever(Actions.sequence(
                Actions.alpha(0.38f, 0.45f),
                Actions.alpha(0.12f, 0.45f)
        )));
        boardLayer.addActor(glow);

        Image core = new Image(whiteTexture);
        core.setColor(1f, 0.12f, 0.04f, 0.88f);
        core.setBounds(lineX - 2f, board.y, 4f, board.height);
        boardLayer.addActor(core);

        for (int row = 0; row < 5; row++) {
            Rectangle rowCell = layout.cellBounds(row, column);
            Label warning = new Label("!", skin, "medium_outline");
            warning.setAlignment(Align.center);
            warning.setColor(1f, 0.32f, 0.10f, 0.95f);
            warning.setBounds(lineX - 21f, rowCell.y + rowCell.height * 0.5f - 17f, 42f, 34f);
            boardLayer.addActor(warning);
        }
    }

    private void buildObjectiveHud(
            SpecialLevelType type,
            GameEngine engine,
            GameSession session,
            boolean previewMode
    ) {
        final float x = 982f;
        final float y = 566f;
        final float width = 242f;
        final float height = type == SpecialLevelType.TIMED_WAR ? 84f : 68f;

        Image shadow = new Image(whiteTexture);
        shadow.setColor(0f, 0f, 0f, 0.62f);
        shadow.setBounds(x + 4f, y - 4f, width, height);
        objectiveLayer.addActor(shadow);

        Image panel = new Image(whiteTexture);
        panel.setColor(0.08f, 0.12f, 0.09f, 0.86f);
        panel.setBounds(x, y, width, height);
        objectiveLayer.addActor(panel);

        Image topRule = new Image(whiteTexture);
        topRule.setColor(0.98f, 0.74f, 0.20f, 0.92f);
        topRule.setBounds(x, y + height - 4f, width, 4f);
        objectiveLayer.addActor(topRule);

        Label title = new Label(displayName(type), skin, "medium_outline");
        title.setColor(1f, 0.90f, 0.48f, 1f);
        title.setBounds(x + 12f, y + height - 34f, width - 24f, 28f);
        objectiveLayer.addActor(title);

        String detail = objectiveDetail(type, engine, session, previewMode);
        Label detailLabel = new Label(detail, skin);
        detailLabel.setColor(Color.WHITE);
        detailLabel.setWrap(true);
        detailLabel.setAlignment(Align.left);
        detailLabel.setBounds(x + 12f, y + 8f, width - 24f, height - 40f);
        objectiveLayer.addActor(detailLabel);

        if (type == SpecialLevelType.TIMED_WAR) {
            addTimedWarIcon(x + width - 58f, y + 10f, engine, session, previewMode);
        }

        if (type == SpecialLevelType.PLANT_WHAT_YOU_GET
                && !(previewMode ? previewWavesStarted : engine != null && engine.areWavesStarted())) {
            addStartWaveButton();
        }
    }

    private void addTimedWarIcon(
            float x,
            float y,
            GameEngine engine,
            GameSession session,
            boolean previewMode
    ) {
        AdventureLevelConfig config = config(session);
        if (pamPlayer != null && pamRoot != null) {
            PamEnvironmentActor clock = new PamEnvironmentActor(
                    pamPlayer, CLOCK_PAM, "default", 0.30f, 0f, 0f);
            clock.setBounds(x, y, 42f, 42f);
            objectiveLayer.addActor(clock);
        }

        TimedWarObjective objective = previewMode || config == null
                ? TimedWarObjective.ZOMBIE_KILLS : config.getTimedWarObjective();
        Texture iconTexture = objective == TimedWarObjective.SUN_PRODUCED
                ? sunTexture : zombieHeadTexture;
        if (iconTexture != null) {
            Image icon = new Image(iconTexture);
            icon.setScaling(Scaling.fit);
            icon.setBounds(x - 40f, y + 2f, 34f, 34f);
            objectiveLayer.addActor(icon);
        }
    }

    private void addStartWaveButton() {
        TextButton start = new TextButton("START WAVE", skin, "purple");
        start.setBounds(530f, 82f, 220f, 50f);
        start.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (currentPreviewMode) {
                    previewWavesStarted = true;
                    renderedSignature = "";
                    if (messageSink != null) {
                        messageSink.accept("ZOMBIE WAVES STARTED");
                    }
                    return;
                }
                if (currentGameplayController == null) {
                    return;
                }
                Result<String> result = currentGameplayController.startZombieWaves();
                if (messageSink != null) {
                    messageSink.accept(result.getMessage());
                }
                renderedSignature = "";
            }
        });
        objectiveLayer.addActor(start);
    }

    private void addMissionIntro(SpecialLevelType type, String detail) {
        Group intro = new Group();
        intro.setBounds(425f, 515f, 430f, 82f);

        Image shadow = new Image(whiteTexture);
        shadow.setColor(0f, 0f, 0f, 0.68f);
        shadow.setBounds(4f, -4f, 430f, 82f);
        intro.addActor(shadow);

        Image panel = new Image(whiteTexture);
        panel.setColor(0.10f, 0.11f, 0.075f, 0.94f);
        panel.setBounds(0f, 0f, 430f, 82f);
        intro.addActor(panel);

        Image top = new Image(whiteTexture);
        top.setColor(0.98f, 0.72f, 0.12f, 1f);
        top.setBounds(0f, 77f, 430f, 5f);
        intro.addActor(top);

        Label heading = new Label(displayName(type), skin, "medium_outline");
        heading.setAlignment(Align.center);
        heading.setBounds(20f, 45f, 390f, 28f);
        intro.addActor(heading);

        Label mission = new Label(detail, skin);
        mission.setAlignment(Align.center);
        mission.setWrap(true);
        mission.setBounds(28f, 10f, 374f, 34f);
        intro.addActor(mission);

        intro.getColor().a = 0f;
        intro.addAction(Actions.sequence(
                Actions.fadeIn(0.18f),
                Actions.delay(1.15f),
                Actions.fadeOut(0.24f),
                Actions.removeActor()
        ));
        introLayer.clearChildren();
        introLayer.addActor(intro);
    }

    private String objectiveDetail(
            SpecialLevelType type,
            GameEngine engine,
            GameSession session,
            boolean previewMode
    ) {
        AdventureLevelConfig config = config(session);
        AdventureRuntimeState state = engine == null ? null : engine.getAdventureState();
        return switch (type) {
            case SAVE_OUR_SEEDS -> "PROTECT THE MARKED PLANTS";
            case DEAD_LINE -> "DO NOT LET ZOMBIES CROSS THE RED LINE";
            case TIMED_WAR -> timedWarDetail(engine, config, state, previewMode);
            case LOVE_YOUR_PLANTS -> {
                int maximum = previewMode || config == null ? 3 : config.getMaximumPlantLosses();
                int losses = previewMode || engine == null ? 1 : engine.getPlantLossCount();
                int remaining = Math.max(0, maximum - losses);
                yield "LOSSES LEFT  " + remaining + " / " + maximum;
            }
            case PLANT_WHAT_YOU_GET -> {
                boolean started = previewMode ? previewWavesStarted
                        : engine != null && engine.areWavesStarted();
                yield started ? "ZOMBIE WAVES ARE ACTIVE"
                        : "SET UP FREELY, THEN PRESS START WAVE";
            }
            case CONVEYOR_BELT -> {
                int packets = 0;
                if (!previewMode && state != null) {
                    for (Map.Entry<?, Integer> entry : state.getConveyorPackets().entrySet()) {
                        packets += entry.getValue();
                    }
                } else if (previewMode) {
                    packets = 3;
                }
                yield "PLANT FROM THE CONVEYOR  •  READY " + packets;
            }
            case LOCKED_PLANTS -> "PLAY WITH THE GIVEN / LOCKED PLANT SELECTION";
            case NIGHT_OPS -> "NIGHT OPS  •  SKY SUN IS DISABLED";
        };
    }

    private String timedWarDetail(
            GameEngine engine,
            AdventureLevelConfig config,
            AdventureRuntimeState state,
            boolean previewMode
    ) {
        TimedWarObjective objective = previewMode || config == null
                ? TimedWarObjective.ZOMBIE_KILLS : config.getTimedWarObjective();
        int target = previewMode || config == null ? 10 : config.getTimedWarTarget();
        int progress = previewMode ? 4 : timedProgress(engine, config);
        long seconds;
        if (previewMode || config == null) {
            seconds = 18;
        } else {
            long elapsed = state == null ? 0L : state.getElapsedTicks();
            long ticks = Math.max(0L, config.getTimedWarTicks() - elapsed);
            seconds = (ticks + TickContext.TICKS_PER_SECOND - 1L) / TickContext.TICKS_PER_SECOND;
        }
        String label = objective == TimedWarObjective.SUN_PRODUCED ? "SUN" : "KILLS";
        return "TIME  " + formatTime(seconds) + "    " + label + "  "
                + Math.min(progress, target) + " / " + target;
    }

    private int timedProgress(GameEngine engine, AdventureLevelConfig config) {
        if (engine == null || config == null || config.getTimedWarObjective() == null) {
            return 0;
        }
        return config.getTimedWarObjective() == TimedWarObjective.ZOMBIE_KILLS
                ? engine.getZombieKillCount() : engine.getProducedSunTotal();
    }

    private String formatTime(long seconds) {
        long safe = Math.max(0L, seconds);
        return String.format("%02d:%02d", safe / 60L, safe % 60L);
    }

    private AdventureLevelConfig config(GameSession session) {
        return session == null || session.getLevel() == null
                ? null : session.getLevel().getAdventureConfig();
    }

    private String displayName(SpecialLevelType type) {
        return switch (type) {
            case CONVEYOR_BELT -> "CONVEYOR BELT";
            case LOCKED_PLANTS -> "LOCKED PLANTS";
            case SAVE_OUR_SEEDS -> "SAVE OUR SEEDS";
            case TIMED_WAR -> "TIMED WAR";
            case NIGHT_OPS -> "NIGHT OPS";
            case DEAD_LINE -> "DEAD LINE";
            case LOVE_YOUR_PLANTS -> "DON'T LOSE PLANTS";
            case PLANT_WHAT_YOU_GET -> "PLANT WHAT YOU GET";
        };
    }
}
