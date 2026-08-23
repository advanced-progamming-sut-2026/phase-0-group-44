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
import model.inGame.plant.Plant;
import model.inGame.zombie.Zombie;
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
    private int lastPlantLossCount = -1;

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
                    for (TileCoordinate coordinate : state.getProtectedTiles()) {
                        Plant plant = protectedPlant(engine, coordinate);
                        value.append('|').append(coordinate.getX()).append(',')
                                .append(coordinate.getY()).append(':')
                                .append(plant == null ? -1 : plant.getHp()).append('/')
                                .append(plant == null ? 0 : plant.getMaxHp());
                    }
                }
            }
            case DEAD_LINE -> value.append('|')
                    .append(config.getDeadLineColumn())
                    .append('|').append(deadLineThreat(engine, config.getDeadLineColumn()));
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
            addDeadLine(engine, session, previewMode);
        } else if (type == SpecialLevelType.NIGHT_OPS) {
            addNightOpsAtmosphere();
        }
    }

    /**
     * Night Ops uses the same beach battlefield, but presents it as a readable
     * night battle. This layer sits below plants/zombies, so the environment
     * darkens without making important entities hard to see.
     */
    private void addNightOpsAtmosphere() {
        Image nightWash = new Image(whiteTexture);
        nightWash.setTouchable(Touchable.disabled);
        nightWash.setColor(0.015f, 0.035f, 0.12f, 0.34f);
        nightWash.setBounds(0f, 0f, 1280f, 720f);
        boardLayer.addActor(nightWash);

        Rectangle board = layout.boardBounds();

        Image horizon = new Image(whiteTexture);
        horizon.setTouchable(Touchable.disabled);
        horizon.setColor(0.18f, 0.40f, 0.62f, 0.12f);
        horizon.setBounds(
                board.x,
                board.y + board.height * 0.78f,
                board.width,
                board.height * 0.22f
        );
        boardLayer.addActor(horizon);

        Image leftShade = new Image(whiteTexture);
        leftShade.setTouchable(Touchable.disabled);
        leftShade.setColor(0f, 0f, 0.04f, 0.16f);
        leftShade.setBounds(0f, 0f, 150f, 720f);
        boardLayer.addActor(leftShade);

        Image bottomShade = new Image(whiteTexture);
        bottomShade.setTouchable(Touchable.disabled);
        bottomShade.setColor(0f, 0f, 0.04f, 0.12f);
        bottomShade.setBounds(0f, 0f, 1280f, 90f);
        boardLayer.addActor(bottomShade);
    }

    private void addProtectedTiles(GameEngine engine, boolean previewMode) {
        if (previewMode || engine == null || engine.getAdventureState() == null) {
            addProtectMarker(2, 0, 100, 100);
            addProtectMarker(2, 2, 72, 100);
            addProtectMarker(2, 4, 100, 100);
            return;
        }
        for (TileCoordinate coordinate : engine.getAdventureState().getProtectedTiles()) {
            Plant plant = protectedPlant(engine, coordinate);
            addProtectMarker(
                    coordinate.getX(),
                    coordinate.getY(),
                    plant == null ? 0 : plant.getHp(),
                    plant == null ? 1 : plant.getMaxHp());
        }
    }

    private Plant protectedPlant(GameEngine engine, TileCoordinate coordinate) {
        if (engine == null || coordinate == null) {
            return null;
        }
        var tile = engine.getGameMap().getTile(coordinate.getY(), coordinate.getX());
        if (tile == null) {
            return null;
        }
        if (tile.getPrimaryPlant() != null) {
            return tile.getPrimaryPlant();
        }
        if (tile.getArmorPlant() != null) {
            return tile.getArmorPlant();
        }
        return tile.getSupportPlant();
    }

    private void addProtectMarker(int column, int row, int hp, int maxHp) {
        Rectangle cell = layout.cellBounds(row, column);
        float ratio = maxHp <= 0 ? 0f : Math.max(0f, Math.min(1f, (float) hp / maxHp));
        boolean danger = ratio <= 0.35f;
        boolean warning = !danger && ratio <= 0.70f;

        Color accent = danger
                ? new Color(1f, 0.16f, 0.08f, 0.96f)
                : warning
                ? new Color(1f, 0.55f, 0.06f, 0.96f)
                : new Color(1f, 0.82f, 0.10f, 0.96f);

        // A faint fill makes the objective tile unmistakable without hiding the plant.
        Image tint = new Image(whiteTexture);
        tint.setColor(accent.r, accent.g, accent.b, danger ? 0.16f : 0.10f);
        tint.setBounds(cell.x + 3f, cell.y + 3f, cell.width - 6f, cell.height - 6f);
        tint.addAction(Actions.forever(Actions.sequence(
                Actions.alpha(danger ? 0.22f : 0.13f, danger ? 0.32f : 0.70f),
                Actions.alpha(danger ? 0.10f : 0.06f, danger ? 0.32f : 0.70f)
        )));
        boardLayer.addActor(tint);

        addCornerBrackets(cell, accent, danger);

        // Small objective badge above the protected plant.
        Label badge = new Label(danger ? "PROTECT!" : "PROTECT", skin, "medium_outline");
        badge.setAlignment(Align.center);
        badge.setColor(accent);
        badge.setFontScale(0.72f);
        badge.setBounds(cell.x + 6f, cell.y + cell.height - 27f, cell.width - 12f, 23f);
        badge.addAction(Actions.forever(Actions.sequence(
                Actions.alpha(1f, 0.55f),
                Actions.alpha(danger ? 0.48f : 0.72f, 0.55f)
        )));
        boardLayer.addActor(badge);

        // Health strip gives the player useful warning before the protected plant is lost.
        float barX = cell.x + 12f;
        float barY = cell.y + 8f;
        float barWidth = cell.width - 24f;
        Image barBack = new Image(whiteTexture);
        barBack.setColor(0.02f, 0.02f, 0.02f, 0.76f);
        barBack.setBounds(barX - 2f, barY - 2f, barWidth + 4f, 8f);
        boardLayer.addActor(barBack);

        Image bar = new Image(whiteTexture);
        bar.setColor(danger ? 1f : warning ? 1f : 0.40f,
                danger ? 0.12f : warning ? 0.58f : 0.92f,
                danger ? 0.05f : warning ? 0.06f : 0.18f, 0.98f);
        bar.setBounds(barX, barY, barWidth * ratio, 4f);
        boardLayer.addActor(bar);

        if (pamPlayer != null && pamRoot != null) {
            PamEnvironmentActor marker = new PamEnvironmentActor(
                    pamPlayer, PROTECT_TILE_PAM, "animation", 0.20f, 0f, 0f);
            marker.setBounds(cell.x + 5f, cell.y + cell.height - 42f, 34f, 34f);
            marker.getColor().a = danger ? 1f : 0.88f;
            boardLayer.addActor(marker);
        }
    }

    private void addCornerBrackets(Rectangle cell, Color color, boolean danger) {
        float inset = 5f;
        float thickness = 4f;
        float arm = Math.min(18f, cell.width * 0.22f);
        float left = cell.x + inset;
        float right = cell.x + cell.width - inset;
        float bottom = cell.y + inset;
        float top = cell.y + cell.height - inset;

        addOutlinePart(left, bottom, arm, thickness, color, danger);
        addOutlinePart(left, bottom, thickness, arm, color, danger);
        addOutlinePart(right - arm, bottom, arm, thickness, color, danger);
        addOutlinePart(right - thickness, bottom, thickness, arm, color, danger);
        addOutlinePart(left, top - thickness, arm, thickness, color, danger);
        addOutlinePart(left, top - arm, thickness, arm, color, danger);
        addOutlinePart(right - arm, top - thickness, arm, thickness, color, danger);
        addOutlinePart(right - thickness, top - arm, thickness, arm, color, danger);
    }

    private void addOutlinePart(
            float x, float y, float width, float height, Color color, boolean danger
    ) {
        Image line = new Image(whiteTexture);
        line.setColor(color);
        line.setBounds(x, y, width, height);
        line.addAction(Actions.forever(Actions.sequence(
                Actions.alpha(1f, danger ? 0.28f : 0.55f),
                Actions.alpha(danger ? 0.42f : 0.62f, danger ? 0.28f : 0.55f)
        )));
        boardLayer.addActor(line);
    }

    private void addDeadLine(GameEngine engine, GameSession session, boolean previewMode) {
        AdventureLevelConfig config = config(session);
        int column = previewMode || config == null ? 2 : config.getDeadLineColumn();
        column = Math.max(0, Math.min(8, column));

        Rectangle board = layout.boardBounds();
        Rectangle cell = layout.cellBounds(0, column);
        float lineX = cell.x;
        DeadLineThreat threat = previewMode ? DeadLineThreat.WARNING : deadLineThreat(engine, column);

        // The area to the left of the model's deadline is the forbidden zone.
        // The model loses as soon as zombie.getX() < deadLineColumn, so the
        // boundary must be drawn on the LEFT edge of that column.
        Image forbidden = new Image(whiteTexture);
        forbidden.setColor(0.72f, 0.015f, 0.015f,
                threat == DeadLineThreat.DANGER ? 0.18f : 0.09f);
        forbidden.setBounds(board.x, board.y, Math.max(0f, lineX - board.x), board.height);
        boardLayer.addActor(forbidden);

        // Subtle stripes make the unsafe side readable without obscuring the board.
        float stripeSpacing = Math.max(22f, layout.cellWidth() * 0.42f);
        for (float x = board.x + 10f; x < lineX - 4f; x += stripeSpacing) {
            Image stripe = new Image(whiteTexture);
            stripe.setColor(1f, 0.18f, 0.06f,
                    threat == DeadLineThreat.DANGER ? 0.16f : 0.08f);
            stripe.setBounds(x, board.y, 5f, board.height);
            stripe.setRotation(-14f);
            boardLayer.addActor(stripe);
        }

        Image glow = new Image(whiteTexture);
        glow.setColor(1f, 0.05f, 0.02f,
                threat == DeadLineThreat.DANGER ? 0.42f : 0.22f);
        glow.setBounds(lineX - 10f, board.y, 20f, board.height);
        glow.addAction(Actions.forever(Actions.sequence(
                Actions.alpha(threat == DeadLineThreat.DANGER ? 0.62f : 0.38f,
                        threat == DeadLineThreat.DANGER ? 0.20f : 0.42f),
                Actions.alpha(threat == DeadLineThreat.DANGER ? 0.18f : 0.10f,
                        threat == DeadLineThreat.DANGER ? 0.20f : 0.42f)
        )));
        boardLayer.addActor(glow);

        // Draw the boundary as bright dashes so it remains visible over water,
        // ice, graves, plants and zombie sprites.
        float dashHeight = Math.max(20f, layout.cellHeight() * 0.27f);
        float gap = Math.max(8f, layout.cellHeight() * 0.10f);
        for (float y = board.y; y < board.y + board.height; y += dashHeight + gap) {
            Image dash = new Image(whiteTexture);
            dash.setColor(1f,
                    threat == DeadLineThreat.DANGER ? 0.06f : 0.12f,
                    0.03f, 0.96f);
            dash.setBounds(lineX - 3f, y, 6f,
                    Math.min(dashHeight, board.y + board.height - y));
            boardLayer.addActor(dash);
        }

        for (int row = 0; row < 5; row++) {
            Rectangle rowCell = layout.cellBounds(row, column);

            Label warning = new Label(threat == DeadLineThreat.DANGER ? "!!" : "!",
                    skin, "medium_outline");
            warning.setAlignment(Align.center);
            warning.setColor(1f,
                    threat == DeadLineThreat.DANGER ? 0.08f : 0.38f,
                    0.08f, 1f);
            warning.setBounds(lineX - 48f,
                    rowCell.y + rowCell.height * 0.5f - 17f, 38f, 34f);
            if (threat != DeadLineThreat.SAFE) {
                warning.addAction(Actions.forever(Actions.sequence(
                        Actions.scaleTo(1.18f, 1.18f, 0.22f),
                        Actions.scaleTo(1f, 1f, 0.22f)
                )));
            }
            boardLayer.addActor(warning);

            Label arrow = new Label(">", skin, "medium_outline");
            arrow.setAlignment(Align.center);
            arrow.setColor(1f, 0.80f, 0.26f, 0.92f);
            arrow.setBounds(lineX + 8f,
                    rowCell.y + rowCell.height * 0.5f - 16f, 32f, 32f);
            boardLayer.addActor(arrow);
        }

        Label zoneLabel = new Label("NO ZOMBIES", skin, "medium_outline");
        zoneLabel.setColor(1f, 0.45f, 0.22f, 0.84f);
        zoneLabel.setAlignment(Align.center);
        zoneLabel.setBounds(board.x + 4f, board.y + board.height - 30f,
                Math.max(90f, lineX - board.x - 8f), 28f);
        boardLayer.addActor(zoneLabel);
    }

    private DeadLineThreat deadLineThreat(GameEngine engine, int deadlineColumn) {
        if (engine == null) {
            return DeadLineThreat.SAFE;
        }
        double nearest = Double.POSITIVE_INFINITY;
        for (Zombie zombie : engine.getZombies()) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }
            nearest = Math.min(nearest, zombie.getX());
        }
        if (!Double.isFinite(nearest)) {
            return DeadLineThreat.SAFE;
        }
        double tilesAway = nearest - deadlineColumn;
        if (tilesAway <= 0.80d) {
            return DeadLineThreat.DANGER;
        }
        if (tilesAway <= 2.0d) {
            return DeadLineThreat.WARNING;
        }
        return DeadLineThreat.SAFE;
    }

    private String deadLineDetail(GameEngine engine, AdventureLevelConfig config, boolean previewMode) {
        int column = previewMode || config == null ? 2 : config.getDeadLineColumn();
        DeadLineThreat threat = previewMode ? DeadLineThreat.WARNING : deadLineThreat(engine, column);
        return switch (threat) {
            case SAFE -> "KEEP ZOMBIES RIGHT OF THE RED LINE  •  SAFE";
            case WARNING -> "WARNING  •  ZOMBIES APPROACHING THE LINE";
            case DANGER -> "DANGER!  •  ZOMBIE AT THE RED LINE";
        };
    }

    private enum DeadLineThreat {
        SAFE,
        WARNING,
        DANGER
    }

    private void buildObjectiveHud(
            SpecialLevelType type,
            GameEngine engine,
            GameSession session,
            boolean previewMode
    ) {
        if (type == SpecialLevelType.TIMED_WAR) {
            lastPlantLossCount = -1;
            buildTimedWarHud(engine, session, previewMode);
            return;
        }
        if (type == SpecialLevelType.LOVE_YOUR_PLANTS) {
            buildLoveYourPlantsHud(engine, session, previewMode);
            return;
        }
        if (type == SpecialLevelType.PLANT_WHAT_YOU_GET) {
            lastPlantLossCount = -1;
            buildPlantWhatYouGetHud(engine, previewMode);
            return;
        }
        lastPlantLossCount = -1;

        final float x = 982f;
        final float y = 566f;
        final float width = 242f;
        final float height = 68f;

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

    }

    /**
     * Dedicated setup/battle HUD for Plant What You Get. Before the player
     * starts the waves, planting is unlimited: no sun is charged and cards do
     * not wait for cooldown. Once START WAVE is pressed, the normal rules resume.
     */
    private void buildPlantWhatYouGetHud(GameEngine engine, boolean previewMode) {
        boolean started = previewMode
                ? previewWavesStarted
                : engine != null && engine.areWavesStarted();

        final float x = 944f;
        final float y = started ? 558f : 500f;
        final float width = 280f;
        final float height = started ? 92f : 156f;
        Color accent = started
                ? new Color(0.32f, 0.92f, 0.42f, 1f)
                : new Color(0.74f, 0.42f, 1f, 1f);

        Image shadow = new Image(whiteTexture);
        shadow.setColor(0f, 0f, 0f, 0.66f);
        shadow.setBounds(x + 5f, y - 5f, width, height);
        objectiveLayer.addActor(shadow);

        Image panel = new Image(whiteTexture);
        panel.setColor(0.055f, 0.055f, 0.075f, 0.96f);
        panel.setBounds(x, y, width, height);
        objectiveLayer.addActor(panel);

        Image topRule = new Image(whiteTexture);
        topRule.setColor(accent);
        topRule.setBounds(x, y + height - 5f, width, 5f);
        objectiveLayer.addActor(topRule);

        Label title = new Label("PLANT WHAT YOU GET", skin, "medium_outline");
        title.setColor(new Color(1f, 0.90f, 0.48f, 1f));
        title.setBounds(x + 12f, y + height - 38f, width - 24f, 30f);
        objectiveLayer.addActor(title);

        Label phase = new Label(started ? "BATTLE ACTIVE" : "SETUP PHASE", skin, "medium_outline");
        phase.setColor(accent);
        phase.setBounds(x + 12f, y + height - 68f, width - 24f, 24f);
        objectiveLayer.addActor(phase);

        if (started) {
            Label normal = new Label("NORMAL SUN COSTS & COOLDOWNS", skin);
            normal.setColor(0.82f, 0.88f, 0.84f, 1f);
            normal.setBounds(x + 12f, y + 12f, width - 24f, 24f);
            objectiveLayer.addActor(normal);
            return;
        }

        Label free = new Label("FREE PLANTING  •  NO COOLDOWN", skin);
        free.setColor(0.92f, 0.88f, 1f, 1f);
        free.setBounds(x + 12f, y + 60f, width - 24f, 24f);
        objectiveLayer.addActor(free);

        Label hint = new Label("ARRANGE YOUR DEFENSE, THEN START", skin);
        hint.setColor(0.78f, 0.80f, 0.86f, 1f);
        hint.setBounds(x + 12f, y + 40f, width - 24f, 22f);
        objectiveLayer.addActor(hint);

        addStartWaveButton(x + 38f, y + 8f, width - 76f, 38f);
    }

    /**
     * Dedicated HUD for the "Don't Lose Plants" adventure rule.  The Phase-2
     * requirement is that the player can always tell how many losses remain,
     * so this uses a large number, a shrinking meter, and one pip per allowed
     * plant loss instead of hiding the state in a small sentence.
     */
    private void buildLoveYourPlantsHud(
            GameEngine engine,
            GameSession session,
            boolean previewMode
    ) {
        AdventureLevelConfig config = config(session);
        int maximum = previewMode || config == null
                ? 5 : Math.max(1, config.getMaximumPlantLosses());
        int losses = previewMode || engine == null
                ? 2 : Math.max(0, engine.getPlantLossCount());
        int remaining = Math.max(0, maximum - losses);
        float ratio = Math.max(0f, Math.min(1f, (float) remaining / (float) maximum));

        boolean danger = remaining <= 1;
        boolean warning = !danger && remaining <= Math.max(2, maximum / 2);
        Color accent = danger
                ? new Color(1f, 0.18f, 0.10f, 1f)
                : warning
                        ? new Color(1f, 0.62f, 0.10f, 1f)
                        : new Color(0.34f, 0.92f, 0.36f, 1f);

        final float x = 944f;
        final float y = 532f;
        final float width = 280f;
        final float height = 126f;

        Image shadow = new Image(whiteTexture);
        shadow.setColor(0f, 0f, 0f, 0.66f);
        shadow.setBounds(x + 5f, y - 5f, width, height);
        objectiveLayer.addActor(shadow);

        Image panel = new Image(whiteTexture);
        panel.setColor(0.055f, 0.085f, 0.065f, 0.95f);
        panel.setBounds(x, y, width, height);
        objectiveLayer.addActor(panel);

        Image topRule = new Image(whiteTexture);
        topRule.setColor(accent);
        topRule.setBounds(x, y + height - 5f, width, 5f);
        if (danger) {
            topRule.addAction(Actions.forever(Actions.sequence(
                    Actions.alpha(0.42f, 0.34f),
                    Actions.alpha(1f, 0.34f)
            )));
        }
        objectiveLayer.addActor(topRule);

        Label title = new Label("DON'T LOSE PLANTS", skin, "medium_outline");
        title.setColor(danger ? accent : new Color(1f, 0.90f, 0.48f, 1f));
        title.setBounds(x + 12f, y + 88f, width - 24f, 28f);
        objectiveLayer.addActor(title);

        Label caption = new Label("PLANT LOSSES LEFT", skin);
        caption.setColor(0.80f, 0.86f, 0.80f, 1f);
        caption.setBounds(x + 12f, y + 64f, 150f, 20f);
        objectiveLayer.addActor(caption);

        Label number = new Label(remaining + " / " + maximum, skin, "medium_outline");
        number.setAlignment(Align.right);
        number.setFontScale(1.08f);
        number.setColor(accent);
        number.setBounds(x + 174f, y + 60f, 92f, 28f);
        objectiveLayer.addActor(number);

        addHudMeter(x + 12f, y + 52f, width - 24f, 8f, ratio, accent);

        float gap = 5f;
        float totalPipWidth = width - 24f;
        float pipWidth = Math.max(8f, (totalPipWidth - gap * (maximum - 1)) / maximum);
        for (int i = 0; i < maximum; i++) {
            Image pip = new Image(whiteTexture);
            boolean available = i < remaining;
            pip.setColor(available
                    ? new Color(accent.r, accent.g, accent.b, 0.94f)
                    : new Color(0.20f, 0.23f, 0.20f, 0.72f));
            pip.setBounds(x + 12f + i * (pipWidth + gap), y + 31f, pipWidth, 11f);
            objectiveLayer.addActor(pip);
        }

        String state = remaining <= 0
                ? "LIMIT REACHED"
                : danger ? "LAST CHANCE"
                : warning ? "BE CAREFUL"
                : "PLANTS ARE SAFE";
        Label status = new Label(state, skin);
        status.setAlignment(Align.right);
        status.setColor(accent);
        status.setBounds(x + 92f, y + 7f, 174f, 18f);
        objectiveLayer.addActor(status);

        if (!previewMode) {
            if (lastPlantLossCount >= 0 && losses > lastPlantLossCount) {
                addPlantLostFlash(losses - lastPlantLossCount);
            }
            lastPlantLossCount = losses;
        }
    }

    private void addPlantLostFlash(int amount) {
        Group flash = new Group();
        flash.setTouchable(Touchable.disabled);
        flash.setBounds(452f, 430f, 376f, 64f);
        flash.getColor().a = 0f;

        Image back = new Image(whiteTexture);
        back.setColor(0.36f, 0.015f, 0.01f, 0.90f);
        back.setBounds(0f, 0f, 376f, 64f);
        flash.addActor(back);

        Image rule = new Image(whiteTexture);
        rule.setColor(1f, 0.20f, 0.08f, 1f);
        rule.setBounds(0f, 58f, 376f, 6f);
        flash.addActor(rule);

        Label text = new Label(amount > 1 ? amount + " PLANTS LOST!" : "PLANT LOST!",
                skin, "medium_outline");
        text.setAlignment(Align.center);
        text.setColor(1f, 0.86f, 0.74f, 1f);
        text.setBounds(12f, 12f, 352f, 40f);
        flash.addActor(text);

        flash.addAction(Actions.sequence(
                Actions.fadeIn(0.10f),
                Actions.delay(0.70f),
                Actions.fadeOut(0.28f),
                Actions.removeActor()
        ));
        objectiveLayer.addActor(flash);
    }

    /**
     * A dedicated Timed War HUD.  Phase 2 requires both the time remaining and
     * the live mission progress to remain readable during gameplay; showing the
     * two as independent meters makes it much harder to confuse the objective
     * with the normal zombie-wave progress bar.
     */
    private void buildTimedWarHud(
            GameEngine engine,
            GameSession session,
            boolean previewMode
    ) {
        AdventureLevelConfig config = config(session);
        AdventureRuntimeState state = engine == null ? null : engine.getAdventureState();
        TimedWarObjective objective = previewMode || config == null
                ? TimedWarObjective.ZOMBIE_KILLS : config.getTimedWarObjective();
        int target = previewMode || config == null ? 10 : Math.max(1, config.getTimedWarTarget());
        int progress = previewMode ? 4 : timedProgress(engine, config);
        progress = Math.max(0, progress);

        long totalTicks = previewMode || config == null
                ? 30L * TickContext.TICKS_PER_SECOND : Math.max(1, config.getTimedWarTicks());
        long elapsedTicks = previewMode
                ? 12L * TickContext.TICKS_PER_SECOND
                : state == null ? 0L : Math.max(0L, state.getElapsedTicks());
        long remainingTicks = Math.max(0L, totalTicks - elapsedTicks);
        long seconds = (remainingTicks + TickContext.TICKS_PER_SECOND - 1L)
                / TickContext.TICKS_PER_SECOND;

        float timeRatio = Math.max(0f, Math.min(1f, (float) remainingTicks / (float) totalTicks));
        float goalRatio = Math.max(0f, Math.min(1f, (float) progress / (float) target));
        boolean urgent = seconds <= 10L && progress < target;
        boolean completed = progress >= target;

        final float x = 944f;
        final float y = 520f;
        final float width = 280f;
        final float height = 138f;

        Image shadow = new Image(whiteTexture);
        shadow.setColor(0f, 0f, 0f, 0.66f);
        shadow.setBounds(x + 5f, y - 5f, width, height);
        objectiveLayer.addActor(shadow);

        Image panel = new Image(whiteTexture);
        panel.setColor(0.055f, 0.085f, 0.075f, 0.94f);
        panel.setBounds(x, y, width, height);
        objectiveLayer.addActor(panel);

        Image topRule = new Image(whiteTexture);
        if (completed) {
            topRule.setColor(0.32f, 0.94f, 0.38f, 1f);
        } else if (urgent) {
            topRule.setColor(1f, 0.18f, 0.08f, 1f);
            topRule.addAction(Actions.forever(Actions.sequence(
                    Actions.alpha(0.42f, 0.35f),
                    Actions.alpha(1f, 0.35f)
            )));
        } else {
            topRule.setColor(0.98f, 0.74f, 0.20f, 1f);
        }
        topRule.setBounds(x, y + height - 5f, width, 5f);
        objectiveLayer.addActor(topRule);

        Label title = new Label("TIMED WAR", skin, "medium_outline");
        title.setColor(completed
                ? new Color(0.52f, 1f, 0.56f, 1f)
                : urgent ? new Color(1f, 0.42f, 0.25f, 1f)
                : new Color(1f, 0.90f, 0.48f, 1f));
        title.setBounds(x + 12f, y + height - 34f, 150f, 28f);
        objectiveLayer.addActor(title);

        addTimedWarClock(x + 184f, y + height - 42f);

        Label timeCaption = new Label("TIME LEFT", skin);
        timeCaption.setColor(0.78f, 0.84f, 0.80f, 1f);
        timeCaption.setBounds(x + 12f, y + 82f, 72f, 20f);
        objectiveLayer.addActor(timeCaption);

        Label time = new Label(formatTime(seconds), skin, "medium_outline");
        time.setAlignment(Align.right);
        time.setFontScale(1.10f);
        time.setColor(urgent ? new Color(1f, 0.30f, 0.18f, 1f) : Color.WHITE);
        time.setBounds(x + 162f, y + 78f, 104f, 28f);
        if (urgent) {
            time.addAction(Actions.forever(Actions.sequence(
                    Actions.alpha(0.42f, 0.32f),
                    Actions.alpha(1f, 0.32f)
            )));
        }
        objectiveLayer.addActor(time);

        Color timeColor = urgent
                ? new Color(1f, 0.20f, 0.08f, 1f)
                : timeRatio <= 0.35f
                        ? new Color(1f, 0.62f, 0.08f, 1f)
                        : new Color(0.28f, 0.88f, 0.34f, 1f);
        addHudMeter(x + 12f, y + 70f, width - 24f, 8f, timeRatio, timeColor);

        Texture iconTexture = objective == TimedWarObjective.SUN_PRODUCED
                ? sunTexture : zombieHeadTexture;
        if (iconTexture != null) {
            Image icon = new Image(iconTexture);
            icon.setScaling(Scaling.fit);
            icon.setBounds(x + 10f, y + 22f, 34f, 34f);
            objectiveLayer.addActor(icon);
        }

        String goalName = objective == TimedWarObjective.SUN_PRODUCED
                ? "SUN PRODUCED" : "ZOMBIES DEFEATED";
        Label goal = new Label(goalName, skin);
        goal.setColor(0.86f, 0.90f, 0.86f, 1f);
        goal.setBounds(x + 50f, y + 40f, 150f, 20f);
        objectiveLayer.addActor(goal);

        Label numbers = new Label(Math.min(progress, target) + " / " + target,
                skin, "medium_outline");
        numbers.setAlignment(Align.right);
        numbers.setColor(completed
                ? new Color(0.48f, 1f, 0.52f, 1f) : Color.WHITE);
        numbers.setBounds(x + 192f, y + 38f, 74f, 24f);
        objectiveLayer.addActor(numbers);

        addHudMeter(x + 50f, y + 28f, width - 62f, 8f, goalRatio,
                completed
                        ? new Color(0.30f, 0.96f, 0.38f, 1f)
                        : new Color(0.98f, 0.76f, 0.16f, 1f));

        int remaining = Math.max(0, target - progress);
        Label status = new Label(completed
                ? "TARGET COMPLETE"
                : remaining + (objective == TimedWarObjective.SUN_PRODUCED
                        ? " SUN TO GO" : " TO GO"), skin);
        status.setAlignment(Align.right);
        status.setColor(completed
                ? new Color(0.52f, 1f, 0.56f, 1f)
                : urgent ? new Color(1f, 0.45f, 0.28f, 1f)
                : new Color(0.78f, 0.82f, 0.78f, 1f));
        status.setBounds(x + 112f, y + 6f, 154f, 18f);
        objectiveLayer.addActor(status);
    }

    private void addTimedWarClock(float x, float y) {
        if (pamPlayer == null || pamRoot == null) {
            return;
        }
        PamEnvironmentActor clock = new PamEnvironmentActor(
                pamPlayer, CLOCK_PAM, "default", 0.27f, 0f, 0f);
        clock.setBounds(x, y, 34f, 34f);
        objectiveLayer.addActor(clock);
    }

    private void addHudMeter(
            float x, float y, float width, float height, float ratio, Color fillColor
    ) {
        Image back = new Image(whiteTexture);
        back.setColor(0.015f, 0.02f, 0.018f, 0.92f);
        back.setBounds(x, y, width, height);
        objectiveLayer.addActor(back);

        float inset = 2f;
        Image fill = new Image(whiteTexture);
        fill.setColor(fillColor);
        fill.setBounds(x + inset, y + inset,
                Math.max(0f, (width - inset * 2f) * Math.max(0f, Math.min(1f, ratio))),
                Math.max(1f, height - inset * 2f));
        objectiveLayer.addActor(fill);
    }

    private void addStartWaveButton(float x, float y, float width, float height) {
        TextButton start = new TextButton("START WAVE", skin, "purple");
        start.setBounds(x, y, width, height);
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
            case SAVE_OUR_SEEDS -> protectedPlantsDetail(engine, state, previewMode);
            case DEAD_LINE -> deadLineDetail(engine, config, previewMode);
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
            case LOCKED_PLANTS -> lockedPlantsDetail(session, previewMode);
            case NIGHT_OPS -> "NO SKY SUN  •  PRODUCE SUN WITH PLANTS";
        };
    }

    private String lockedPlantsDetail(GameSession session, boolean previewMode) {
        if (previewMode || session == null || session.getLevel() == null) {
            return "REQUIRED PLANT SELECTED  •  EXPLOSIVE PLANTS LOCKED";
        }
        var rules = session.getLevel().getSelectionRules();
        String required = rules.getForcedPlants().stream()
                .map(Enum::name)
                .map(name -> name.replace('_', ' '))
                .reduce((a, b) -> a + ", " + b)
                .orElse("NONE");
        String locked = rules.getExcludedCategories().stream()
                .map(Enum::name)
                .map(name -> name.replace('_', ' '))
                .reduce((a, b) -> a + ", " + b)
                .orElse("NONE");
        return "REQUIRED  " + required + "  •  LOCKED  " + locked;
    }

    private String protectedPlantsDetail(
            GameEngine engine, AdventureRuntimeState state, boolean previewMode
    ) {
        if (previewMode || engine == null || state == null) {
            return "PROTECTED  3 / 3  •  KEEP THEM ALIVE";
        }
        int total = state.getProtectedTiles().size();
        int alive = 0;
        boolean damaged = false;
        boolean danger = false;
        for (TileCoordinate coordinate : state.getProtectedTiles()) {
            Plant plant = protectedPlant(engine, coordinate);
            if (plant == null || plant.isDead()) {
                danger = true;
                continue;
            }
            alive++;
            float ratio = plant.getMaxHp() <= 0 ? 0f : (float) plant.getHp() / plant.getMaxHp();
            damaged |= ratio < 0.999f;
            danger |= ratio <= 0.35f;
        }
        String status = danger ? "DANGER" : damaged ? "UNDER ATTACK" : "ALL SAFE";
        return "PROTECTED  " + alive + " / " + total + "  •  " + status;
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
