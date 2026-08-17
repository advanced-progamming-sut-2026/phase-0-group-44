package screen.gameplay;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import model.GameEngine;
import model.Tile;
import model.config.GameWorld;
import model.enums.ObstacleType;
import model.enums.TerrainType;
import pvz.libpvz.pam.PamPlayer;

/**
 * Environment-only battlefield renderer. It deliberately does not render plants,
 * zombies or projectiles so teammates can attach those actors to the entity layer.
 */
public final class BattlefieldEnvironmentLayer extends Group {
    private static final String EGYPT_GRAVE_PAM =
            "768/INITIAL/GRAVESTONES/EGYPT_HIEROGLYPH/EGYPT_HIEROGLYPH.PAM";
    private static final String DARK_NOOP_PAM =
            "768/FULL/GRAVESTONES/DARK_NOOP/DARK_NOOP.PAM";
    private static final String DARK_SUN_PAM =
            "768/FULL/GRAVESTONES/DARK_SUN/DARK_SUN.PAM";
    private static final String DARK_PLANT_FOOD_PAM =
            "768/FULL/GRAVESTONES/DARK_PLANTFOOD/DARK_PLANTFOOD.PAM";
    private static final String FROST_ICE_PAM =
            "768/FULL/EFFECTS/FROSTBITE_ICE_BLOCK_PLANT/FROSTBITE_ICE_BLOCK_PLANT.PAM";
    private static final String FROST_SLIDER_UP_PAM =
            "768/FULL/EFFECTS/TILESLIDER_ICEAGE_UP/TILESLIDER_ICEAGE_UP.PAM";
    private static final String FROST_SLIDER_DOWN_PAM =
            "768/FULL/EFFECTS/TILESLIDER_ICEAGE_DOWN/TILESLIDER_ICEAGE_DOWN.PAM";
    private static final String DARK_SPAWN_PAM =
            "768/FULL/EFFECTS/TOMBSTONE_DARK_SPAWN_EFFECT/TOMBSTONE_DARK_SPAWN_EFFECT.PAM";
    private static final String BEACH_TIDE_PAM =
            "768/FULL/BACKGROUNDS/WATER_TIDE_LINE/WATER_TIDE_LINE.PAM";

    private final BattlefieldTheme theme;
    private final BattlefieldLayout layout;
    private final Texture whiteTexture;
    private final Texture runeTexture;
    private final Skin skin;
    private final PamPlayer pamPlayer;
    private final FileHandle pamRoot;

    private String lastSignature = "";

    public BattlefieldEnvironmentLayer(
            BattlefieldTheme theme,
            BattlefieldLayout layout,
            Texture whiteTexture,
            Texture runeTexture,
            Skin skin,
            PamPlayer pamPlayer,
            FileHandle pamRoot
    ) {
        this.theme = theme;
        this.layout = layout;
        this.whiteTexture = whiteTexture;
        this.runeTexture = runeTexture;
        this.skin = skin;
        this.pamPlayer = pamPlayer;
        this.pamRoot = pamRoot;
        setSize(1280f, 720f);
    }

    /** Rebuilds only when environment state changes, preserving PAM animation time. */
    public void sync(GameEngine engine, boolean preview) {
        String signature = signature(engine, preview);
        if (signature.equals(lastSignature)) {
            return;
        }
        lastSignature = signature;
        clearChildren();

        for (int row = 0; row < BattlefieldLayout.ROWS; row++) {
            for (int column = 0; column < BattlefieldLayout.COLUMNS; column++) {
                TerrainType terrain = terrainAt(engine, preview, row, column);
                ObstacleType obstacle = obstacleAt(engine, preview, row, column);
                String payload = payloadAt(engine, preview, row, column);
                Rectangle cell = layout.cellBounds(row, column);
                addTerrain(cell, terrain);
                addObstacle(cell, terrain, obstacle, payload);
            }
        }

        addTideLine(engine, preview);
        addMowers(engine, preview);
    }

    private void addTerrain(Rectangle cell, TerrainType terrain) {
        if (terrain == null) {
            return;
        }
        switch (terrain) {
            case WATER -> addTint(cell, new Color(0.18f, 0.75f, 1f, 0.14f));
            case LOW_TIDE -> addTint(cell, new Color(0.12f, 0.70f, 0.72f, 0.06f));
            case SLIPPERY_UP -> {
                addTint(cell, new Color(0.68f, 0.93f, 1f, 0.10f));
                if (!addPam(cell, FROST_SLIDER_UP_PAM, "idle", 0.54f, 0f, -2f)) {
                    addDirectionMarker(cell, "^");
                }
            }
            case SLIPPERY_DOWN -> {
                addTint(cell, new Color(0.68f, 0.93f, 1f, 0.10f));
                if (!addPam(cell, FROST_SLIDER_DOWN_PAM, "idle", 0.54f, 0f, 2f)) {
                    addDirectionMarker(cell, "v");
                }
            }
            case FROZEN -> addTint(cell, new Color(0.62f, 0.88f, 1f, 0.22f));
            case NECROMANCY -> {
                addNecromancy(cell);
                // The supplied Dark Ages tombstone spawn effect gives necromancy
                // a PVZ-native visual instead of relying only on a procedural rune.
                addPam(cell, DARK_SPAWN_PAM, "animation", 0.16f, 0f, -5f);
            }
            default -> {
                // The official background already supplies the normal terrain art.
            }
        }
    }

    private void addTint(Rectangle cell, Color color) {
        Image tint = new Image(whiteTexture);
        tint.setColor(color);
        tint.setBounds(cell.x + 2f, cell.y + 2f,
                Math.max(0f, cell.width - 4f), Math.max(0f, cell.height - 4f));
        addActor(tint);
    }

    private void addDirectionMarker(Rectangle cell, String marker) {
        Label label = new Label(marker, skin, "medium_outline");
        label.setColor(0.87f, 0.98f, 1f, 0.84f);
        label.setFontScale(1.05f);
        label.setBounds(cell.x, cell.y + cell.height * 0.20f, cell.width, cell.height * 0.60f);
        label.setAlignment(com.badlogic.gdx.utils.Align.center);
        addActor(label);
    }

    private void addNecromancy(Rectangle cell) {
        addTint(cell, new Color(0.48f, 0.22f, 0.72f, 0.18f));
        Image rune = new Image(runeTexture);
        float size = Math.min(cell.width, cell.height) * 0.56f;
        rune.setBounds(cell.x + (cell.width - size) * 0.5f,
                cell.y + (cell.height - size) * 0.5f, size, size);
        rune.setColor(0.68f, 0.38f, 1f, 0.72f);
        rune.addAction(Actions.forever(Actions.sequence(
                Actions.alpha(0.38f, 0.85f),
                Actions.alpha(0.86f, 0.85f)
        )));
        addActor(rune);
    }

    private void addObstacle(
            Rectangle cell,
            TerrainType terrain,
            ObstacleType obstacle,
            String payload
    ) {
        boolean graveTerrain = terrain == TerrainType.GRAVESTONE
                || terrain == TerrainType.DARK_AGES_GRAVESTONE;
        if (obstacle == ObstacleType.GRAVE || graveTerrain) {
            addGrave(cell, payload);
            return;
        }
        if (obstacle == ObstacleType.ICE) {
            if (!addPam(cell, FROST_ICE_PAM, "freeze_idle", 0.42f, 0f, -4f)) {
                addFallbackBadge(cell, "ICE", new Color(0.70f, 0.94f, 1f, 0.55f));
            }
        }
    }

    private void addGrave(Rectangle cell, String payload) {
        String pam;
        float scale;
        if (theme.world() == GameWorld.DARK_AGES) {
            if ("SUN_50".equals(payload)) {
                pam = DARK_SUN_PAM;
            } else if ("PLANT_FOOD".equals(payload)) {
                pam = DARK_PLANT_FOOD_PAM;
            } else {
                pam = DARK_NOOP_PAM;
            }
            scale = 0.50f;
        } else {
            pam = EGYPT_GRAVE_PAM;
            scale = 0.50f;
        }
        if (!addPam(cell, pam, "undamaged", scale, 0f, -2f)) {
            addFallbackBadge(cell, "GRAVE", new Color(0.32f, 0.26f, 0.22f, 0.72f));
        }
    }

    private void addFallbackBadge(Rectangle cell, String text, Color color) {
        Image block = new Image(whiteTexture);
        block.setColor(color);
        block.setBounds(cell.x + cell.width * 0.24f, cell.y + cell.height * 0.13f,
                cell.width * 0.52f, cell.height * 0.70f);
        addActor(block);

        Label label = new Label(text, skin);
        label.setAlignment(com.badlogic.gdx.utils.Align.center);
        label.setBounds(cell.x, cell.y, cell.width, cell.height);
        addActor(label);
    }

    private boolean addPam(
            Rectangle bounds,
            String pamPath,
            String clip,
            float scale,
            float xOffset,
            float yOffset
    ) {
        if (pamPlayer == null || pamRoot == null || pamPath == null) {
            return false;
        }
        // pvz-asset-browser stores PAM files under IMAGES/768/..., but libPVZ
        // intentionally receives paths beginning at 768/... (the same convention
        // already used successfully by GreenhouseScreen). The old guard checked
        // the wrong physical location, so every valid PAM fell back to a box.
        FileHandle direct = pamRoot.child(pamPath);
        FileHandle imagesPath = pamRoot.child("IMAGES").child(pamPath);
        if (!direct.exists() && !imagesPath.exists()) {
            return false;
        }
        PamEnvironmentActor actor = new PamEnvironmentActor(
                pamPlayer, pamPath, clip, scale, xOffset, yOffset);
        actor.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        addActor(actor);
        return true;
    }

    private void addMowers(GameEngine engine, boolean preview) {
        for (int row = 0; row < BattlefieldLayout.ROWS; row++) {
            boolean used = !preview && engine != null && engine.isLawnMowerUsed(row);
            if (used) {
                continue;
            }

            Rectangle bounds = layout.mowerBounds(row);
            String clip = "idle";
            float scale = theme.mowerScale();
            if (theme.world() == GameWorld.FROSTBITE_CAVES) {
                scale *= 1.24f;
            } else if (theme.world() == GameWorld.BIG_WAVE_BEACH) {
                scale *= 1.18f;
            }

            boolean rendered = addPam(bounds, theme.mowerPam(), clip, scale, 0f, 0f);
            if (!rendered) {
                addFallbackMower(bounds);
                continue;
            }

            // These two supplied mower idle clips are intentionally quite soft.
            // Drawing the same native PAM a second time makes them read clearly
            // against the bright ice/sand without replacing them with generated art.
            if (theme.world() == GameWorld.FROSTBITE_CAVES
                    || theme.world() == GameWorld.BIG_WAVE_BEACH) {
                addPam(bounds, theme.mowerPam(), clip, scale, 0f, 0f);
            }
        }
    }

    private void addFallbackMower(Rectangle bounds) {
        Image mower = new Image(whiteTexture);
        mower.setColor(0.92f, 0.86f, 0.47f, 0.78f);
        mower.setBounds(bounds.x + bounds.width * 0.14f,
                bounds.y + bounds.height * 0.32f,
                bounds.width * 0.72f, bounds.height * 0.36f);
        addActor(mower);
    }

    private void addTideLine(GameEngine engine, boolean preview) {
        if (theme.world() != GameWorld.BIG_WAVE_BEACH) {
            return;
        }
        int firstWaterColumn = firstWaterColumn(engine, preview);
        if (firstWaterColumn < 0) {
            return;
        }
        Rectangle board = layout.boardBounds();
        float x = board.x + firstWaterColumn * layout.cellWidth();
        Rectangle tideBounds = new Rectangle(
                x - layout.cellWidth() * 0.16f,
                board.y,
                layout.cellWidth() * 0.32f,
                board.height
        );
        if (!addPam(tideBounds, BEACH_TIDE_PAM, "idle", 0.22f, 0f, 0f)) {
            Image line = new Image(whiteTexture);
            line.setColor(0.82f, 0.98f, 1f, 0.72f);
            line.setBounds(x - 2f, board.y, 4f, board.height);
            addActor(line);
        }
    }

    private int firstWaterColumn(GameEngine engine, boolean preview) {
        for (int column = 0; column < BattlefieldLayout.COLUMNS; column++) {
            for (int row = 0; row < BattlefieldLayout.ROWS; row++) {
                if (terrainAt(engine, preview, row, column) == TerrainType.WATER) {
                    return column;
                }
            }
        }
        return -1;
    }

    private TerrainType terrainAt(GameEngine engine, boolean preview, int row, int column) {
        if (!preview && engine != null) {
            return engine.getGameMap().getTile(row, column).getTerrain();
        }
        return previewTerrain(row, column);
    }

    private ObstacleType obstacleAt(GameEngine engine, boolean preview, int row, int column) {
        if (!preview && engine != null) {
            return engine.getGameMap().getTile(row, column).getObstacle();
        }
        if (theme.world() == GameWorld.ANCIENT_EGYPT
                && ((row == 1 && column == 5) || (row == 3 && column == 7))) {
            return ObstacleType.GRAVE;
        }
        if (theme.world() == GameWorld.FROSTBITE_CAVES && row == 2 && column == 5) {
            return ObstacleType.ICE;
        }
        if (theme.world() == GameWorld.DARK_AGES
                && ((row == 1 && column == 5) || (row == 3 && column == 6))) {
            return ObstacleType.GRAVE;
        }
        return ObstacleType.NONE;
    }

    private String payloadAt(GameEngine engine, boolean preview, int row, int column) {
        if (!preview && engine != null) {
            Tile tile = engine.getGameMap().getTile(row, column);
            return tile.getObstaclePayload();
        }
        if (theme.world() == GameWorld.DARK_AGES && row == 1 && column == 5) {
            return "SUN_50";
        }
        if (theme.world() == GameWorld.DARK_AGES && row == 3 && column == 6) {
            return "PLANT_FOOD";
        }
        return "";
    }

    private TerrainType previewTerrain(int row, int column) {
        return switch (theme.world()) {
            case ANCIENT_EGYPT -> TerrainType.NORMAL_EGYPT;
            case FROSTBITE_CAVES -> {
                if (row == 1 && column == 4) {
                    yield TerrainType.SLIPPERY_UP;
                }
                if (row == 3 && column == 6) {
                    yield TerrainType.SLIPPERY_DOWN;
                }
                yield TerrainType.NORMAL_FROSTBITE;
            }
            case BIG_WAVE_BEACH -> {
                if (column >= 6) {
                    yield TerrainType.WATER;
                }
                if (column == 5) {
                    yield TerrainType.LOW_TIDE;
                }
                yield TerrainType.NORMAL_BEACH;
            }
            case DARK_AGES -> {
                if (row == 2 && column == 4) {
                    yield TerrainType.NECROMANCY;
                }
                yield TerrainType.NORMAL_DARK_AGES;
            }
        };
    }

    private String signature(GameEngine engine, boolean preview) {
        StringBuilder builder = new StringBuilder(theme.name()).append('|').append(preview);
        for (int row = 0; row < BattlefieldLayout.ROWS; row++) {
            if (!preview && engine != null) {
                builder.append('|').append(engine.isLawnMowerUsed(row));
            }
            for (int column = 0; column < BattlefieldLayout.COLUMNS; column++) {
                builder.append(';').append(terrainAt(engine, preview, row, column));
                builder.append(',').append(obstacleAt(engine, preview, row, column));
                builder.append(',').append(payloadAt(engine, preview, row, column));
            }
        }
        return builder.toString();
    }
}
