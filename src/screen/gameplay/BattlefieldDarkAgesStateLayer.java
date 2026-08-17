package screen.gameplay;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;
import model.GameEngine;
import model.Tile;
import model.config.GameWorld;
import model.enums.ObstacleType;
import model.enums.TerrainType;
import model.level.ChapterRules;
import model.level.TileCoordinate;
import model.sim.adventure.AdventureRuntimeState;

/**
 * Dedicated Dark Ages visual layer.
 *
 * This intentionally sits above BattlefieldEnvironmentLayer and below entity actors.
 * The older environment renderer may still draw supplied PAM graves/runes, but this
 * layer is the authoritative Phase-2 presentation for Dark Ages and visually covers
 * those fallbacks with the generated grave families and cursed-ground sigils.
 */
public final class BattlefieldDarkAgesStateLayer extends Group {

    /*
     * DEV PREVIEW ONLY.
     *
     * The real game still gets grave placement/payloads exclusively from GameEngine.
     * These deterministic preview layouts simply let us inspect how the three grave
     * families read across several "waves" without having to run combat.
     *
     * Coordinates are stored as: column, row, column, row.
     * They deliberately avoid the real necromancy cells (4,1) and (6,3) so both
     * mechanics remain visible at the same time in the visual preview.
     */
    private static final int[][] PREVIEW_GRAVE_COORDINATES = {
            {6, 1, 8, 3}, // wave 1
            {2, 0, 7, 4}, // wave 2
            {5, 2, 8, 0}, // wave 3
            {3, 4, 7, 1}, // wave 4
            {1, 2, 5, 4}, // wave 5
            {2, 3, 8, 2}  // wave 6
    };

    private static final String[][] PREVIEW_GRAVE_PAYLOADS = {
            {"SUN_50", "PLANT_FOOD"},
            {"", "SUN_50"},
            {"PLANT_FOOD", ""},
            {"SUN_50", "PLANT_FOOD"},
            {"", "PLANT_FOOD"},
            {"SUN_50", ""}
    };
    private final BattlefieldTheme theme;
    private final BattlefieldLayout layout;
    private final Texture whiteTexture;
    private final Texture necromancyTexture;
    private final Texture graveNoopTexture;
    private final Texture graveSunTexture;
    private final Texture gravePlantFoodTexture;

    private String lastSignature = "";
    private int previewWave = 1;

    public BattlefieldDarkAgesStateLayer(
            BattlefieldTheme theme,
            BattlefieldLayout layout,
            Texture whiteTexture,
            Texture necromancyTexture,
            Texture graveNoopTexture,
            Texture graveSunTexture,
            Texture gravePlantFoodTexture
    ) {
        this.theme = theme;
        this.layout = layout;
        this.whiteTexture = whiteTexture;
        this.necromancyTexture = necromancyTexture;
        this.graveNoopTexture = graveNoopTexture;
        this.graveSunTexture = graveSunTexture;
        this.gravePlantFoodTexture = gravePlantFoodTexture;
        setSize(1280f, 720f);
        setTouchable(Touchable.disabled);
    }

    /** Advance the deterministic DEV preview by one representative Dark Ages wave. */
    public int nextPreviewWave() {
        previewWave++;
        if (previewWave > PREVIEW_GRAVE_COORDINATES.length) {
            previewWave = 1;
        }
        // Force the layer to rebuild immediately on the next sync.
        lastSignature = "";
        return previewWave;
    }

    /** Move backward through the deterministic DEV preview waves. */
    public int previousPreviewWave() {
        previewWave--;
        if (previewWave < 1) {
            previewWave = PREVIEW_GRAVE_COORDINATES.length;
        }
        lastSignature = "";
        return previewWave;
    }

    public int getPreviewWave() {
        return previewWave;
    }

    public void sync(GameEngine engine, boolean preview) {
        if (theme.world() != GameWorld.DARK_AGES) {
            if (getChildren().size > 0) {
                clearChildren();
            }
            lastSignature = "OFF";
            return;
        }

        String signature = signature(engine, preview);
        if (signature.equals(lastSignature)) {
            return;
        }
        lastSignature = signature;
        clearChildren();

        addAtmosphere();

        // Ground first: cursed tiles remain visible around/under graves and entities.
        for (int row = 0; row < BattlefieldLayout.ROWS; row++) {
            for (int column = 0; column < BattlefieldLayout.COLUMNS; column++) {
                if (isNecromancy(engine, preview, row, column)) {
                    addNecromancyTile(layout.cellBounds(row, column), row, column);
                }
            }
        }

        // Physical graves above cursed ground, but still below future plant/zombie actors.
        for (int row = 0; row < BattlefieldLayout.ROWS; row++) {
            for (int column = 0; column < BattlefieldLayout.COLUMNS; column++) {
                if (isGrave(engine, preview, row, column)) {
                    addGrave(layout.cellBounds(row, column), payloadAt(engine, preview, row, column));
                }
            }
        }
    }

    private void addAtmosphere() {
        Rectangle board = layout.boardBounds();

        // A restrained violet-blue moon wash makes the chapter feel colder/gothic
        // without replacing the supplied background artwork.
        Image wash = new Image(whiteTexture);
        wash.setColor(0.11f, 0.06f, 0.24f, 0.075f);
        wash.setBounds(board.x, board.y, board.width, board.height);
        addActor(wash);

        // Soft top/bottom haze adds depth and makes the bright sigils/graves pop.
        Image topHaze = new Image(whiteTexture);
        topHaze.setColor(0.18f, 0.08f, 0.30f, 0.055f);
        topHaze.setBounds(board.x, board.y + board.height * 0.68f, board.width, board.height * 0.32f);
        addActor(topHaze);

        Image bottomShade = new Image(whiteTexture);
        bottomShade.setColor(0.01f, 0.01f, 0.05f, 0.07f);
        bottomShade.setBounds(board.x, board.y, board.width, board.height * 0.20f);
        addActor(bottomShade);
    }

    private void addNecromancyTile(Rectangle cell, int row, int column) {
        /*
         * Necromancy is a PROPERTY OF THE CELL, not a grave object.
         * Use a full-tile cursed wash + oversized outer ring so it remains
         * identifiable even if a grave, plant, or zombie later occupies the cell.
         */
        Image cursedWash = new Image(whiteTexture);
        cursedWash.setColor(0.26f, 0.02f, 0.42f, 0.11f);
        cursedWash.setBounds(
                cell.x + 2f,
                cell.y + 2f,
                Math.max(0f, cell.width - 4f),
                Math.max(0f, cell.height - 4f)
        );
        cursedWash.addAction(Actions.forever(Actions.sequence(
                Actions.alpha(0.08f, 1.20f),
                Actions.alpha(0.14f, 1.20f)
        )));
        addActor(cursedWash);

        if (necromancyTexture != null) {
            Image sigil = new Image(necromancyTexture);
            sigil.setScaling(Scaling.fit);

            // Slightly larger than the cell so the outer rune tips remain visible
            // around an object standing in the center.
            float size = Math.min(cell.width, cell.height) * 0.98f;
            sigil.setBounds(
                    cell.x + (cell.width - size) * 0.5f,
                    cell.y + (cell.height - size) * 0.5f,
                    size,
                    size
            );
            sigil.setOrigin(size * 0.5f, size * 0.5f);
            sigil.setColor(1f, 1f, 1f, 0.82f);

            float turn = ((row + column) & 1) == 0 ? 2.0f : -2.0f;
            sigil.addAction(Actions.forever(Actions.parallel(
                    Actions.sequence(
                            Actions.alpha(0.72f, 1.20f),
                            Actions.alpha(0.94f, 1.20f)
                    ),
                    Actions.rotateBy(turn, 3.20f)
            )));
            addActor(sigil);
        }

        // Tiny cursed sparks keep the cell readable when its center is occupied,
        // without looking like a selection/debug bracket.
        Color mote = new Color(0.87f, 0.26f, 1f, 0.68f);
        addMote(cell.x + 6f, cell.y + 6f, mote);
        addMote(cell.x + cell.width - 10f, cell.y + 8f, mote);
        addMote(cell.x + 8f, cell.y + cell.height - 10f, mote);
        addMote(cell.x + cell.width - 11f, cell.y + cell.height - 11f, mote);
    }


    private void addMote(float x, float y, Color color) {
        Image mote = new Image(whiteTexture);
        mote.setColor(color);
        mote.setBounds(x, y, 4f, 4f);
        mote.addAction(Actions.forever(Actions.sequence(
                Actions.alpha(0.30f, 0.75f),
                Actions.alpha(0.90f, 0.75f)
        )));
        addActor(mote);
    }

    private void addGrave(Rectangle cell, String payload) {
        Texture texture;
        Color glowColor;
        float widthScale;
        float heightScale;

        if ("SUN_50".equals(payload)) {
            texture = graveSunTexture;
            glowColor = new Color(1f, 0.67f, 0.12f, 0.17f);
            widthScale = 0.70f;
            heightScale = 1.10f;
        } else if ("PLANT_FOOD".equals(payload)) {
            texture = gravePlantFoodTexture;
            glowColor = new Color(0.72f, 0.18f, 1f, 0.20f);
            widthScale = 0.73f;
            heightScale = 1.12f;
        } else {
            texture = graveNoopTexture;
            glowColor = new Color(0.15f, 0.34f, 0.47f, 0.14f);
            widthScale = 0.68f;
            heightScale = 1.06f;
        }

        // The dedicated layer is now the sole Dark Ages grave renderer, so no
        // rectangular cover plate is needed underneath the grave.

        Image glow = new Image(whiteTexture);
        glow.setColor(glowColor);
        glow.setBounds(
                cell.x + cell.width * 0.25f,
                cell.y + cell.height * 0.035f,
                cell.width * 0.50f,
                cell.height * 0.10f
        );
        addActor(glow);

        if (texture != null) {
            Image grave = new Image(texture);
            grave.setScaling(Scaling.fit);
            float width = cell.width * widthScale;
            float height = cell.height * heightScale;
            grave.setBounds(
                    cell.x + (cell.width - width) * 0.5f,
                    cell.y + cell.height * 0.01f,
                    width,
                    height
            );
            addActor(grave);
        }
    }

    private boolean isNecromancy(GameEngine engine, boolean preview, int row, int column) {
        if (preview) {
            // Exact Phase-1 chapter config: x:y = 4:1 and 6:3.
            return (column == 4 && row == 1) || (column == 6 && row == 3);
        }
        if (engine == null) {
            return false;
        }

        Tile tile = engine.getGameMap().getTile(row, column);
        if (tile.getTerrain() == TerrainType.NECROMANCY) {
            return true;
        }

        AdventureRuntimeState state = engine.getAdventureState();
        if (state == null || state.getConfig() == null) {
            return false;
        }
        ChapterRules rules = state.getConfig().getChapterRules();
        if (rules == null) {
            return false;
        }
        for (TileCoordinate coordinate : rules.getNecromancyTiles()) {
            if (coordinate.getX() == column && coordinate.getY() == row) {
                return true;
            }
        }
        return false;
    }

    private boolean isGrave(GameEngine engine, boolean preview, int row, int column) {
        if (preview) {
            int[] coordinates =
                    PREVIEW_GRAVE_COORDINATES[Math.max(0, previewWave - 1)];
            return (column == coordinates[0] && row == coordinates[1])
                    || (column == coordinates[2] && row == coordinates[3]);
        }
        if (engine == null) {
            return false;
        }
        Tile tile = engine.getGameMap().getTile(row, column);
        return tile.getObstacle() == ObstacleType.GRAVE
                || tile.getTerrain() == TerrainType.DARK_AGES_GRAVESTONE;
    }

    private String payloadAt(GameEngine engine, boolean preview, int row, int column) {
        if (preview) {
            int index = Math.max(0, previewWave - 1);
            int[] coordinates = PREVIEW_GRAVE_COORDINATES[index];
            String[] payloads = PREVIEW_GRAVE_PAYLOADS[index];

            if (column == coordinates[0] && row == coordinates[1]) {
                return payloads[0];
            }
            if (column == coordinates[2] && row == coordinates[3]) {
                return payloads[1];
            }
            return "";
        }
        if (engine == null) {
            return "";
        }
        String payload = engine.getGameMap().getTile(row, column).getObstaclePayload();
        return payload == null ? "" : payload;
    }

    private String signature(GameEngine engine, boolean preview) {
        StringBuilder builder = new StringBuilder("DARK|").append(preview)
                .append("|PW").append(preview ? previewWave : 0);
        for (int row = 0; row < BattlefieldLayout.ROWS; row++) {
            for (int column = 0; column < BattlefieldLayout.COLUMNS; column++) {
                if (isNecromancy(engine, preview, row, column)) {
                    builder.append("|N").append(column).append(':').append(row);
                }
                if (isGrave(engine, preview, row, column)) {
                    builder.append("|G").append(column).append(':').append(row)
                            .append(':').append(payloadAt(engine, preview, row, column));
                }
            }
        }
        return builder.toString();
    }
}
