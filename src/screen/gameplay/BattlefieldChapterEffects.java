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
    private final PamPlayer pamPlayer;
    private final FileHandle pamRoot;
    private final Group rearLayer = new Group();
    private final Group frontLayer = new Group();

    private GameEngine observedEngine;
    private int eventCursor;
    private int lastWaterColumns = -1;

    public BattlefieldChapterEffects(
            BattlefieldTheme theme,
            BattlefieldLayout layout,
            Texture whiteTexture,
            Texture runeTexture,
            Texture streakTexture,
            PamPlayer pamPlayer,
            FileHandle pamRoot
    ) {
        this.theme = theme;
        this.layout = layout;
        this.whiteTexture = whiteTexture;
        this.runeTexture = runeTexture;
        this.streakTexture = streakTexture;
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
            case ANCIENT_EGYPT -> spawnSandstorm(2, 3);
            case FROSTBITE_CAVES -> {
                spawnWind(1, 0f);
                spawnWind(3, 0.12f);
            }
            case BIG_WAVE_BEACH -> spawnTideTransition(2, 4);
            case DARK_AGES -> spawnDarkBurst(2, 4);
        }
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
        Rectangle cell = layout.cellBounds(row, 0);
        float width = layout.cellWidth() * 3.8f;
        float height = cell.height * 1.95f;
        float startX = board.x + board.width + width * 0.12f;
        int targetColumn = Math.max(0,
                Math.min(BattlefieldLayout.COLUMNS - 1, BattlefieldLayout.COLUMNS - advance));
        float endCenter = layout.columnCenterX(targetColumn);
        float endX = endCenter - width * 0.50f;
        float y = cell.y - cell.height * 0.48f;

        // Keep the supplied PVZ sandstorm PAMs as the core effect, but make them
        // materially larger so they read against the bright Egypt board.
        addMovingPam(rearLayer, SANDSTORM_REAR_PAM, "loop", 0.92f,
                startX, y, width, height, endX, y, 1.15f, 0f);
        addMovingPam(frontLayer, SANDSTORM_TOP_PAM, "loop", 0.92f,
                startX + 22f, y, width, height, endX + 10f, y, 1.15f, 0.03f);

        // Fast sand/dust streaks make the direction and affected lane readable
        // without covering the board with a rectangular color block.
        for (int i = 0; i < 12; i++) {
            float streakY = cell.y + 8f + (i % 6) * (cell.height - 16f) / 5f;
            float streakW = 42f + (i % 4) * 18f;
            float streakH = 3f + (i % 3);
            float delay = (i % 5) * 0.035f;
            Color color = (i % 2 == 0)
                    ? new Color(1f, 0.82f, 0.38f, 0.72f)
                    : new Color(1f, 0.95f, 0.72f, 0.58f);
            addMovingFlowStreak(frontLayer, color,
                    board.x + board.width + i * 13f, streakY,
                    streakW, Math.max(8f, streakH * 2.2f),
                    endCenter - streakW * 0.5f - i * 5f,
                    streakY + ((i % 3) - 1) * 8f,
                    1.02f + (i % 3) * 0.08f, delay,
                    -7f + (i % 4) * 4f);
        }

        // Brief golden target pulse at the cell the tornado carried toward.
        addRunePulse(row, targetColumn, new Color(1f, 0.72f, 0.16f, 0.72f), 1.35f);
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
        PamEnvironmentActor actor = pamActor(path, clip, scale);
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
        if (pamPlayer == null || pamRoot == null || path == null) {
            return null;
        }
        FileHandle direct = pamRoot.child(path);
        FileHandle images = pamRoot.child("IMAGES").child(path);
        if (!direct.exists() && !images.exists()) {
            return null;
        }
        return new PamEnvironmentActor(pamPlayer, path, clip, scale, 0f, 0f);
    }
}
