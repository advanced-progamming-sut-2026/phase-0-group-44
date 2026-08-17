package screen.gameplay;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import model.GameEngine;
import model.config.GameWorld;
import pvz.libpvz.pam.PamPlayer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transient chapter visuals driven by the real Adventure event stream.
 *
 * <p>The permanent terrain/obstacle state stays in {@link BattlefieldEnvironmentLayer};
 * this helper only owns short-lived effects such as Egypt tornadoes, Frostbite
 * wind, Beach tide transitions and Dark Ages grave/necromancy bursts.</p>
 */
public final class BattlefieldChapterEffects {
    private static final String SANDSTORM_REAR_PAM =
            "768/INITIAL/EFFECTS/SANDSTORM_REAR/SANDSTORM_REAR.PAM";
    private static final String SANDSTORM_TOP_PAM =
            "768/INITIAL/EFFECTS/SANDSTORM_TOP/SANDSTORM_TOP.PAM";
    private static final String CHILL_WIND_PAM =
            "768/FULL/EFFECTS/FROSTBITE_CHILL_WIND/FROSTBITE_CHILL_WIND.PAM";
    private static final String BEACH_TIDE_PAM =
            "768/FULL/BACKGROUNDS/WATER_TIDE_LINE/WATER_TIDE_LINE.PAM";
    private static final String DARK_SPAWN_PAM =
            "768/FULL/EFFECTS/TOMBSTONE_DARK_SPAWN_EFFECT/TOMBSTONE_DARK_SPAWN_EFFECT.PAM";

    private static final Pattern TORNADO_EVENT = Pattern.compile(
            "A tornado carried .+? (\\d+) columns into lane (\\d+)\\.");
    private static final Pattern ICY_WIND_EVENT = Pattern.compile(
            "Icy wind affected rows \\[(.*?)]\\.");
    private static final Pattern WATER_EVENT = Pattern.compile(
            "Water level now covers (\\d+) right-side columns\\.");
    private static final Pattern DARK_EVENT = Pattern.compile(
            "(?:Necromancy spawned a zombie beneath grave|A Dark Ages grave appeared at) "
                    + "\\((\\d+),\\s*(\\d+)\\).*");

    private final BattlefieldTheme theme;
    private final BattlefieldLayout layout;
    private final Texture whiteTexture;
    private final Texture runeTexture;
    private final Texture streakTexture;
    private final Texture necromancyActiveAuraTexture;
    private final Texture necromancyActiveSigilTexture;
    private final PamPlayer pamPlayer;
    private final FileHandle pamRoot;
    private final Group rearLayer = new Group();
    private final Group frontLayer = new Group();

    private GameEngine observedEngine;
    private int eventCursor;
    private int lastWaterColumns = -1;

    // DEV PREVIEW only: demonstrate every legal Egypt tornado advance (1..4 cells)
    // on different lanes without touching the real WaveSystem.
    private int egyptPreviewIndex;
    private int lastEgyptPreviewAdvance = 3;
    private int lastEgyptPreviewLane = 2;

    public BattlefieldChapterEffects(
            BattlefieldTheme theme,
            BattlefieldLayout layout,
            Texture whiteTexture,
            Texture runeTexture,
            Texture streakTexture,
            Texture necromancyActiveAuraTexture,
            Texture necromancyActiveSigilTexture,
            PamPlayer pamPlayer,
            FileHandle pamRoot
    ) {
        this.theme = theme;
        this.layout = layout;
        this.whiteTexture = whiteTexture;
        this.runeTexture = runeTexture;
        this.streakTexture = streakTexture;
        this.necromancyActiveAuraTexture = necromancyActiveAuraTexture;
        this.necromancyActiveSigilTexture = necromancyActiveSigilTexture;
        this.pamPlayer = pamPlayer;
        this.pamRoot = pamRoot;
        rearLayer.setSize(1280f, 720f);
        frontLayer.setSize(1280f, 720f);
    }

    public Group rearLayer() {
        return rearLayer;
    }

    public Group frontLayer() {
        return frontLayer;
    }

    /** Polls only newly recorded engine events and turns them into short visuals. */
    public void sync(GameEngine engine, boolean preview) {
        if (preview || engine == null) {
            observedEngine = null;
            eventCursor = 0;
            return;
        }
        if (engine != observedEngine) {
            observedEngine = engine;
            List<String> events = engine.getEvents();
            // Keep a small tail so an effect that fired immediately before the screen
            // appeared is still visible once, without replaying a whole match history.
            eventCursor = Math.max(0, events.size() - 10);
            lastWaterColumns = engine.getAdventureState() == null
                    ? -1 : engine.getAdventureState().getWaterColumns();
        }

        List<String> events = engine.getEvents();
        while (eventCursor < events.size()) {
            processEvent(events.get(eventCursor++));
        }
    }

    /** Developer-only visual replay; does not mutate Store, GameEngine or terrain. */
    public void previewPulse() {
        switch (theme.world()) {
            case ANCIENT_EGYPT -> {
                // Phase-1 final-wave tornado logic advances a carried zombie by
                // exactly 1..4 columns. Cycle all four possibilities in DEV preview.
                int[] lanes = {2, 0, 4, 1};
                int[] advances = {3, 1, 4, 2};
                int index = egyptPreviewIndex % advances.length;
                lastEgyptPreviewLane = lanes[index];
                lastEgyptPreviewAdvance = advances[index];
                egyptPreviewIndex = (egyptPreviewIndex + 1) % advances.length;
                spawnSandstorm(lastEgyptPreviewLane, lastEgyptPreviewAdvance);
            }
            case FROSTBITE_CAVES -> {
                spawnWind(1, 0f);
                spawnWind(3, 0.12f);
            }
            case BIG_WAVE_BEACH -> spawnTideTransition(2, 4);
            case DARK_AGES -> spawnDarkBurst(2, 4);
        }
    }

    public int getLastEgyptPreviewAdvance() {
        return lastEgyptPreviewAdvance;
    }

    public int getLastEgyptPreviewLane() {
        return lastEgyptPreviewLane;
    }

    private void processEvent(String event) {
        if (event == null || event.isBlank()) {
            return;
        }

        Matcher tornado = TORNADO_EVENT.matcher(event);
        if (tornado.matches()) {
            int advance = Integer.parseInt(tornado.group(1));
            int lane = Integer.parseInt(tornado.group(2));
            spawnSandstorm(lane, advance);
            return;
        }

        Matcher wind = ICY_WIND_EVENT.matcher(event);
        if (wind.matches()) {
            String body = wind.group(1).trim();
            if (!body.isEmpty()) {
                String[] parts = body.split(",");
                for (int index = 0; index < parts.length; index++) {
                    try {
                        int row = Integer.parseInt(parts[index].trim());
                        spawnWind(row, index * 0.10f);
                    } catch (NumberFormatException ignored) {
                        // A malformed optional visual event must not affect gameplay.
                    }
                }
            }
            return;
        }

        Matcher water = WATER_EVENT.matcher(event);
        if (water.matches()) {
            int nextColumns = Integer.parseInt(water.group(1));
            int previousColumns = lastWaterColumns < 0 ? nextColumns : lastWaterColumns;
            spawnTideTransition(previousColumns, nextColumns);
            lastWaterColumns = nextColumns;
            return;
        }

        Matcher dark = DARK_EVENT.matcher(event);
        if (dark.matches()) {
            int column = Integer.parseInt(dark.group(1));
            int row = Integer.parseInt(dark.group(2));
            spawnDarkBurst(row, column);
        }
    }

    private void spawnSandstorm(int row, int advance) {
        if (theme.world() != GameWorld.ANCIENT_EGYPT
                || row < 0 || row >= BattlefieldLayout.ROWS) {
            return;
        }

        Rectangle board = layout.boardBounds();
        Rectangle laneCell = layout.cellBounds(row, 0);

        // WaveSystem may only choose 1..4. Clamp visuals defensively without
        // changing the model/event itself.
        int resolvedAdvance = Math.max(1, Math.min(4, advance));
        /*
         * IMPORTANT MODEL ALIGNMENT:
         *
         * WaveSystem does NOT spawn the carried zombie at a tile center.
         * It stores continuous zombie x as:
         *
         *     spawnX = columns - advance
         *
         * so with a 9-column board the legal tornado x values are
         * 8.0, 7.0, 6.0 and 5.0.
         *
         * The previous visual converted that value into a column and then used
         * that column's CENTER, shifting the tornado +0.5 cell to the right.
         * Draw against the exact continuous model x instead.
         */
        float modelSpawnX = BattlefieldLayout.COLUMNS - resolvedAdvance;
        float targetCenterX = board.x + modelSpawnX * layout.cellWidth();
        float laneCenterY = laneCell.y + laneCell.height * 0.5f;

        // A one-cell visual landing footprint centered on the real continuous x.
        Rectangle targetCell = new Rectangle(
                targetCenterX - layout.cellWidth() * 0.5f,
                laneCell.y,
                layout.cellWidth(),
                laneCell.height
        );

        // Only used by the small residual rune pulse.
        int targetColumn = Math.max(
                0,
                Math.min(
                        BattlefieldLayout.COLUMNS - 1,
                        (int) Math.floor(modelSpawnX)
                )
        );

        /*
         * Core PVZ sandstorm.
         *
         * The Phase-2 document explicitly says continuous tornado movement is not
         * required; the important requirement is that the player understands why
         * a zombie appeared several cells forward. We therefore show:
         *
         *   board edge -> moving sandstorm -> concentrated landing vortex
         *
         * ending on the exact cell implied by the real Phase-1 "advance" value.
         */
        float stormWidth = layout.cellWidth() * 2.70f;

        /*
         * Keep the tornado visually lane-sized.  The old 1.68-cell height looked
         * acceptable in the middle row, but on rows 4/5 it extended beneath the
         * lawn and appeared vertically misplaced.
         */
        float stormHeight = laneCell.height * 1.22f;
        float startX = board.x + board.width + stormWidth * 0.16f;
        float endX = targetCenterX - stormWidth * 0.49f;

        float desiredStormY = laneCenterY - stormHeight * 0.5f;
        float minStormY = board.y + 2f;
        float maxStormY = board.y + board.height - stormHeight - 2f;

        // Clamp only the actor bounds; the landing vortex remains centered on
        // the exact model lane/cell position.
        float stormY = Math.max(minStormY, Math.min(maxStormY, desiredStormY));

        /*
         * SANDSTORM_REAR/TOP are not visually centered inside their PAM origin.
         * On model row 4 (the bottom visible lawn row) the artwork's funnel/base
         * hangs below the lawn even though the Scene2D actor bounds are valid.
         *
         * Correct the PAM DRAW ORIGIN only for that bottom row.  Do not move the
         * logical landing cell, model spawn x, streaks, rune, or other rows.
         */
        float sandstormRenderYOffset =
                row == BattlefieldLayout.ROWS - 1
                        ? laneCell.height * 0.55f
                        : 0f;

        addMovingPam(
                rearLayer,
                SANDSTORM_REAR_PAM,
                "loop",
                0.88f,
                startX,
                stormY,
                stormWidth,
                stormHeight,
                endX,
                stormY,
                1.05f,
                0f,
                sandstormRenderYOffset
        );
        addMovingPam(
                frontLayer,
                SANDSTORM_TOP_PAM,
                "loop",
                0.90f,
                startX + 12f,
                stormY,
                stormWidth,
                stormHeight,
                endX + 6f,
                stormY,
                1.05f,
                0.025f,
                sandstormRenderYOffset
        );

        // Sand streaks travel only through the affected lane. Their slightly curved
        // end points read as turbulent wind instead of debug-like parallel lines.
        for (int i = 0; i < 10; i++) {
            float fraction = (i % 5) / 4f;
            float streakY = laneCell.y + 12f
                    + fraction * Math.max(12f, laneCell.height - 24f);
            float streakW = 34f + (i % 4) * 15f;
            float streakH = 7f + (i % 2) * 2f;
            float startStreakX = board.x + board.width + 18f + i * 16f;
            float endStreakX = targetCenterX
                    - targetCell.width * (0.12f + (i % 3) * 0.08f)
                    - streakW * 0.50f;
            float endStreakY = streakY + ((i % 4) - 1.5f) * 6f;

            Color sand = (i % 3 == 0)
                    ? new Color(1f, 0.93f, 0.67f, 0.70f)
                    : new Color(0.94f, 0.66f, 0.26f, 0.64f);

            addMovingFlowStreak(
                    frontLayer,
                    sand,
                    startStreakX,
                    streakY,
                    streakW,
                    streakH,
                    endStreakX,
                    endStreakY,
                    0.90f + (i % 3) * 0.07f,
                    (i % 5) * 0.025f,
                    -10f + (i % 5) * 5f
            );
        }

        // A short-lived funnel forms exactly where the carried zombie enters.
        addEgyptLandingVortex(targetCell);

        // Small dust flashes on the entry boundary and destination give the effect
        // a clear "from outside the lawn -> into this cell" visual grammar.
        addEdgeFlash(
                board.x + board.width - 5f,
                laneCell.y + laneCell.height * 0.18f,
                4f,
                laneCell.height * 0.64f,
                new Color(1f, 0.80f, 0.32f, 0.55f),
                0f
        );

        // Retain the existing circular pulse, but reduce it so it behaves like
        // residual magic/sand at the landing position rather than a target UI.
        addRunePulse(
                row,
                targetColumn,
                new Color(1f, 0.70f, 0.18f, 0.50f),
                0.82f
        );
    }

    /**
     * Builds a brief layered funnel from soft horizontal sand bands.
     * It is deliberately transient: Phase 2 only needs the reason for the
     * advanced zombie spawn to be visually understandable.
     */
    private void addEgyptLandingVortex(Rectangle cell) {
        float centerX = cell.x + cell.width * 0.5f;
        float baseY = cell.y + cell.height * 0.10f;

        for (int i = 0; i < 7; i++) {
            float level = i / 6f;
            float width = cell.width * (0.70f - level * 0.36f);
            float height = 6f + (i % 2) * 2f;
            float x = centerX - width * 0.5f
                    + ((i % 2 == 0) ? -3f : 3f);
            float y = baseY + level * cell.height * 0.70f;

            Image band = new Image(streakTexture != null ? streakTexture : whiteTexture);
            band.setColor(
                    i % 2 == 0
                            ? new Color(1f, 0.82f, 0.37f, 0f)
                            : new Color(1f, 0.94f, 0.72f, 0f)
            );
            band.setBounds(x, y, width, height);
            band.setOrigin(width * 0.5f, height * 0.5f);
            band.setRotation(i % 2 == 0 ? -8f : 8f);

            float delay = 0.54f + i * 0.025f;
            band.addAction(Actions.sequence(
                    Actions.delay(delay),
                    Actions.parallel(
                            Actions.alpha(i % 2 == 0 ? 0.82f : 0.68f, 0.10f),
                            Actions.rotateBy(i % 2 == 0 ? -54f : 54f, 0.34f)
                    ),
                    Actions.parallel(
                            Actions.moveBy(
                                    i % 2 == 0 ? 7f : -7f,
                                    cell.height * 0.05f,
                                    0.34f
                            ),
                            Actions.scaleTo(0.78f, 0.78f, 0.34f),
                            Actions.alpha(0f, 0.34f),
                            Actions.rotateBy(i % 2 == 0 ? -58f : 58f, 0.34f)
                    ),
                    Actions.removeActor()
            ));
            frontLayer.addActor(band);
        }

        // Low dust skirt grounds the funnel on the lawn.
        for (int i = 0; i < 5; i++) {
            float w = cell.width * (0.18f + i * 0.07f);
            Image dust = new Image(streakTexture != null ? streakTexture : whiteTexture);
            dust.setColor(new Color(0.92f, 0.61f, 0.22f, 0f));
            dust.setBounds(
                    centerX - w * 0.5f + (i - 2) * 5f,
                    cell.y + 6f + (i % 2) * 4f,
                    w,
                    7f
            );
            dust.addAction(Actions.sequence(
                    Actions.delay(0.72f + i * 0.025f),
                    Actions.alpha(0.54f, 0.08f),
                    Actions.parallel(
                            Actions.moveBy((i - 2) * 5f, 8f, 0.32f),
                            Actions.alpha(0f, 0.32f)
                    ),
                    Actions.removeActor()
            ));
            rearLayer.addActor(dust);
        }
    }

    private void spawnWind(int row, float delay) {
        if (theme.world() != GameWorld.FROSTBITE_CAVES
                || row < 0 || row >= BattlefieldLayout.ROWS) {
            return;
        }
        Rectangle board = layout.boardBounds();
        Rectangle cell = layout.cellBounds(row, 0);
        float width = layout.cellWidth() * 4.4f;
        float height = cell.height * 1.70f;
        float startX = board.x + board.width + width * 0.10f;
        float endX = board.x - width;
        float y = cell.y - cell.height * 0.34f;

        addMovingPam(frontLayer, CHILL_WIND_PAM, "animation", 0.96f,
                startX, y, width, height, endX, y, 1.22f, delay);

        // Bright ice streaks remain readable even over cyan Frostbite tiles.
        for (int i = 0; i < 9; i++) {
            float streakY = cell.y + 10f + (i % 5) * (cell.height - 20f) / 4f;
            float streakW = 58f + (i % 4) * 22f;
            float streakH = 2f + (i % 2) * 2f;
            Color color = (i % 3 == 0)
                    ? new Color(1f, 1f, 1f, 0.86f)
                    : new Color(0.62f, 0.94f, 1f, 0.70f);
            addMovingFlowStreak(frontLayer, color,
                    board.x + board.width + 45f + i * 18f, streakY,
                    streakW, Math.max(7f, streakH * 2.4f),
                    board.x - streakW - i * 10f,
                    streakY + ((i % 3) - 1) * 9f,
                    1.08f + (i % 3) * 0.07f,
                    delay + (i % 4) * 0.025f,
                    -6f + (i % 5) * 3f);
        }

        // Thin cyan edge flashes at both ends of the affected row rather than
        // tinting the entire lane as one opaque rectangle.
        addEdgeFlash(board.x + 3f, cell.y + 10f, 3f, cell.height - 20f,
                new Color(0.72f, 0.98f, 1f, 0.34f), delay);
        addEdgeFlash(board.x + board.width - 6f, cell.y + 10f, 3f, cell.height - 20f,
                new Color(0.72f, 0.98f, 1f, 0.34f), delay);
    }

    private void spawnTideTransition(int oldColumns, int newColumns) {
        if (theme.world() != GameWorld.BIG_WAVE_BEACH) {
            return;
        }
        int oldCount = Math.max(0, Math.min(BattlefieldLayout.COLUMNS, oldColumns));
        int newCount = Math.max(0, Math.min(BattlefieldLayout.COLUMNS, newColumns));
        Rectangle board = layout.boardBounds();
        float oldX = board.x + (BattlefieldLayout.COLUMNS - oldCount) * layout.cellWidth();
        float newX = board.x + (BattlefieldLayout.COLUMNS - newCount) * layout.cellWidth();
        float width = layout.cellWidth() * 0.44f;

        PamEnvironmentActor tide = pamActor(BEACH_TIDE_PAM, "idle", 0.32f);
        if (tide != null) {
            tide.setBounds(oldX - width * 0.5f, board.y, width, board.height);
            tide.addAction(Actions.sequence(
                    Actions.moveTo(newX - width * 0.5f, board.y, 0.86f),
                    Actions.delay(0.30f),
                    Actions.removeActor()
            ));
            frontLayer.addActor(tide);
        }

        // White/cyan foam markers travel with the tide boundary. They are thin,
        // high contrast and visible both on tan sand and blue water tiles.
        for (int i = 0; i < 10; i++) {
            float foamY = board.y + 8f + i * (board.height - 22f) / 9f;
            float foamW = 18f + (i % 3) * 8f;
            float foamH = 3f + (i % 2) * 2f;
            Color foam = (i % 2 == 0)
                    ? new Color(1f, 1f, 1f, 0.92f)
                    : new Color(0.45f, 0.94f, 1f, 0.82f);
            float foamStartJitter = ((i * 17) % 13) - 6f;
            float foamEndJitter = ((i * 11) % 17) - 8f;
            addMovingFlowStreak(frontLayer, foam,
                    oldX - foamW * 0.5f + foamStartJitter, foamY,
                    foamW * (0.88f + (i % 3) * 0.10f),
                    7f + (i % 3) * 2f,
                    newX - foamW * 0.5f + foamEndJitter,
                    foamY + ((i % 3) - 1) * 4f,
                    0.86f, i * 0.018f,
                    -8f + (i % 5) * 4f);
        }

    }

    private void spawnDarkBurst(int row, int column) {
        if (theme.world() != GameWorld.DARK_AGES
                || row < 0 || row >= BattlefieldLayout.ROWS
                || column < 0 || column >= BattlefieldLayout.COLUMNS) {
            return;
        }
        Rectangle cell = layout.cellBounds(row, column);

        // Generated Dark-Ages art is used only during a REAL necromancy/grave event,
        // so the quiet ground marker and the active spawn state remain distinct.
        if (necromancyActiveAuraTexture != null) {
            Image aura = new Image(necromancyActiveAuraTexture);
            float size = Math.min(cell.width, cell.height) * 1.42f;
            aura.setBounds(cell.x + (cell.width - size) * 0.5f,
                    cell.y + (cell.height - size) * 0.5f, size, size);
            aura.setOrigin(size * 0.5f, size * 0.5f);
            aura.setColor(1f, 1f, 1f, 0f);
            aura.addAction(Actions.sequence(
                    Actions.parallel(
                            Actions.alpha(0.78f, 0.16f),
                            Actions.scaleTo(1.08f, 1.08f, 0.36f),
                            Actions.rotateBy(18f, 0.36f)
                    ),
                    Actions.parallel(
                            Actions.alpha(0f, 0.72f),
                            Actions.scaleTo(1.22f, 1.22f, 0.72f),
                            Actions.rotateBy(22f, 0.72f)
                    ),
                    Actions.removeActor()
            ));
            frontLayer.addActor(aura);
        }
        if (necromancyActiveSigilTexture != null) {
            Image sigil = new Image(necromancyActiveSigilTexture);
            float size = Math.min(cell.width, cell.height) * 1.08f;
            sigil.setBounds(cell.x + (cell.width - size) * 0.5f,
                    cell.y + (cell.height - size) * 0.5f, size, size);
            sigil.setColor(1f, 1f, 1f, 0f);
            sigil.addAction(Actions.sequence(
                    Actions.alpha(0.90f, 0.12f),
                    Actions.delay(0.28f),
                    Actions.alpha(0f, 0.55f),
                    Actions.removeActor()
            ));
            frontLayer.addActor(sigil);
        }

        // Two expanding supplied-style rune pulses create a clear high-contrast
        // necromancy target even on Dark Ages' purple/blue tiles.
        addRunePulse(row, column, new Color(0.94f, 0.28f, 1f, 0.86f), 1.55f);
        addRunePulse(row, column, new Color(0.30f, 0.95f, 1f, 0.72f), 1.18f);

        PamEnvironmentActor burst = pamActor(DARK_SPAWN_PAM, "animation", 0.46f);
        if (burst != null) {
            burst.setBounds(cell.x - cell.width * 0.10f, cell.y - cell.height * 0.18f,
                    cell.width * 1.20f, cell.height * 1.42f);
            burst.addAction(Actions.sequence(
                    Actions.delay(1.45f),
                    Actions.removeActor()
            ));
            frontLayer.addActor(burst);
        }

        // Vertical magic wisps make the spawn direction obvious without a square tint.
        for (int i = 0; i < 7; i++) {
            float w = 3f + (i % 2) * 2f;
            float h = 18f + (i % 3) * 8f;
            float x = cell.x + cell.width * (0.18f + i * 0.10f);
            float y = cell.y + 8f + (i % 2) * 6f;
            Image wisp = new Image(whiteTexture);
            wisp.setColor(i % 2 == 0
                    ? new Color(1f, 0.32f, 1f, 0.78f)
                    : new Color(0.35f, 0.92f, 1f, 0.70f));
            wisp.setBounds(x, y, w, h);
            wisp.addAction(Actions.sequence(
                    Actions.delay(i * 0.035f),
                    Actions.parallel(
                            Actions.moveBy(((i % 3) - 1) * 7f, cell.height * 0.58f, 0.62f),
                            Actions.alpha(0f, 0.62f)
                    ),
                    Actions.removeActor()
            ));
            frontLayer.addActor(wisp);
        }
    }

    private void addMovingFlowStreak(
            Group layer,
            Color color,
            float startX, float startY,
            float width, float height,
            float endX, float endY,
            float duration, float delay,
            float rotation
    ) {
        Image streak = new Image(streakTexture != null ? streakTexture : whiteTexture);
        streak.setColor(color.r, color.g, color.b, color.a);
        streak.setBounds(startX, startY, width, height);
        streak.setOrigin(width * 0.5f, height * 0.5f);
        streak.setRotation(rotation);

        float midX = startX + (endX - startX) * 0.52f;
        float midY = startY + (endY - startY) * 0.52f
                + ((rotation >= 0f) ? 7f : -7f);

        streak.addAction(Actions.sequence(
                Actions.delay(delay),
                Actions.parallel(
                        Actions.sequence(
                                Actions.moveTo(midX, midY, duration * 0.52f),
                                Actions.moveTo(endX, endY, duration * 0.48f)
                        ),
                        Actions.sequence(
                                Actions.alpha(Math.min(1f, color.a + 0.08f), duration * 0.16f),
                                Actions.alpha(color.a * 0.76f, duration * 0.56f),
                                Actions.alpha(0f, duration * 0.28f)
                        ),
                        Actions.rotateBy(rotation >= 0f ? 5f : -5f, duration)
                ),
                Actions.removeActor()
        ));
        layer.addActor(streak);
    }

    private void addMovingStrip(
            Group layer,
            Color color,
            float startX, float startY,
            float width, float height,
            float endX, float endY,
            float duration, float delay,
            float rotation
    ) {
        Image strip = new Image(whiteTexture);
        strip.setColor(color);
        strip.setBounds(startX, startY, width, height);
        strip.setOrigin(width * 0.5f, height * 0.5f);
        strip.setRotation(rotation);
        strip.addAction(Actions.sequence(
                Actions.delay(delay),
                Actions.parallel(
                        Actions.moveTo(endX, endY, duration),
                        Actions.sequence(
                                Actions.alpha(Math.min(1f, color.a + 0.12f), duration * 0.20f),
                                Actions.alpha(color.a * 0.80f, duration * 0.55f),
                                Actions.alpha(0f, duration * 0.25f)
                        )
                ),
                Actions.removeActor()
        ));
        layer.addActor(strip);
    }

    private void addEdgeFlash(float x, float y, float width, float height,
                              Color color, float delay) {
        Image flash = new Image(whiteTexture);
        flash.setColor(color.r, color.g, color.b, 0f);
        flash.setBounds(x, y, width, height);
        flash.addAction(Actions.sequence(
                Actions.delay(delay),
                Actions.alpha(color.a, 0.10f),
                Actions.alpha(0f, 0.78f),
                Actions.removeActor()
        ));
        frontLayer.addActor(flash);
    }

    private void addRunePulse(int row, int column, Color color, float scale) {
        if (runeTexture == null || row < 0 || row >= BattlefieldLayout.ROWS
                || column < 0 || column >= BattlefieldLayout.COLUMNS) {
            return;
        }
        Rectangle cell = layout.cellBounds(row, column);
        float size = Math.min(cell.width, cell.height) * scale;
        Image rune = new Image(runeTexture);
        rune.setColor(color.r, color.g, color.b, color.a);
        rune.setBounds(cell.x + cell.width * 0.5f - size * 0.5f,
                cell.y + cell.height * 0.5f - size * 0.5f, size, size);
        rune.setOrigin(size * 0.5f, size * 0.5f);
        rune.setScale(0.48f);
        rune.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.scaleTo(1f, 1f, 0.34f),
                        Actions.rotateBy(38f, 0.34f),
                        Actions.alpha(Math.min(1f, color.a + 0.10f), 0.12f)
                ),
                Actions.parallel(
                        Actions.scaleTo(1.18f, 1.18f, 0.54f),
                        Actions.rotateBy(34f, 0.54f),
                        Actions.alpha(0f, 0.54f)
                ),
                Actions.removeActor()
        ));
        frontLayer.addActor(rune);
    }

    private void addMovingPam(
            Group layer,
            String path,
            String clip,
            float scale,
            float startX,
            float startY,
            float width,
            float height,
            float endX,
            float endY,
            float duration,
            float delay
    ) {
        addMovingPam(
                layer, path, clip, scale,
                startX, startY, width, height,
                endX, endY, duration, delay,
                0f
        );
    }

    private void addMovingPam(
            Group layer,
            String path,
            String clip,
            float scale,
            float startX,
            float startY,
            float width,
            float height,
            float endX,
            float endY,
            float duration,
            float delay,
            float renderYOffset
    ) {
        PamEnvironmentActor actor = pamActor(
                path, clip, scale,
                0f, renderYOffset
        );
        if (actor == null) {
            return;
        }
        actor.setBounds(startX, startY, width, height);
        actor.addAction(Actions.sequence(
                Actions.delay(delay),
                Actions.moveTo(endX, endY, duration),
                Actions.removeActor()
        ));
        layer.addActor(actor);
    }

    private PamEnvironmentActor pamActor(String path, String clip, float scale) {
        return pamActor(path, clip, scale, 0f, 0f);
    }

    private PamEnvironmentActor pamActor(
            String path,
            String clip,
            float scale,
            float xOffset,
            float yOffset
    ) {
        if (pamPlayer == null || pamRoot == null || path == null) {
            return null;
        }
        FileHandle direct = pamRoot.child(path);
        FileHandle images = pamRoot.child("IMAGES").child(path);
        if (!direct.exists() && !images.exists()) {
            return null;
        }
        return new PamEnvironmentActor(
                pamPlayer, path, clip, scale, xOffset, yOffset
        );
    }
}
