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
import com.badlogic.gdx.utils.Scaling;
import model.GameEngine;
import model.Tile;
import model.config.GameWorld;
import model.enums.ObstacleType;
import model.enums.TerrainType;
import model.level.ChapterRules;
import model.sim.adventure.AdventureRuntimeState;
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
    private static final String FROST_ICE_BEHIND_PAM =
            "768/FULL/EFFECTS/FROSTBITE_ICE_BLOCK_PLANT_BEHIND/FROSTBITE_ICE_BLOCK_PLANT_BEHIND.PAM";
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
    private final Texture slipperyUpTexture;
    private final Texture slipperyDownTexture;
    private final Skin skin;
    private final PamPlayer pamPlayer;
    private final FileHandle pamRoot;

    private String lastSignature = "";

    public BattlefieldEnvironmentLayer(
            BattlefieldTheme theme,
            BattlefieldLayout layout,
            Texture whiteTexture,
            Texture runeTexture,
            Texture slipperyUpTexture,
            Texture slipperyDownTexture,
            Skin skin,
            PamPlayer pamPlayer,
            FileHandle pamRoot
    ) {
        this.theme = theme;
        this.layout = layout;
        this.whiteTexture = whiteTexture;
        this.runeTexture = runeTexture;
        this.slipperyUpTexture = slipperyUpTexture;
        this.slipperyDownTexture = slipperyDownTexture;
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

        // Big Wave Beach always has the ocean immediately to the right of the lawn.
        // It is drawn first so tile/obstacle actors remain above it.
        addBeachSeaBody();

        for (int row = 0; row < BattlefieldLayout.ROWS; row++) {
            for (int column = 0; column < BattlefieldLayout.COLUMNS; column++) {
                TerrainType terrain = terrainAt(engine, preview, row, column);
                ObstacleType obstacle = obstacleAt(engine, preview, row, column);
                String payload = payloadAt(engine, preview, row, column);
                Rectangle cell = layout.cellBounds(row, column);
                addTerrain(cell, terrain);
                if (theme.world() == GameWorld.BIG_WAVE_BEACH
                        && isLowTideCoordinate(engine, preview, row, column)) {
                    addLowTideIdentityMarker(cell, terrain == TerrainType.WATER);
                }
                addObstacle(cell, terrain, obstacle, payload);
            }
        }

        addCurrentTideLine(engine, preview);
        addMaximumTideLine(engine, preview);
        addMowers(engine, preview);
    }

    private void addTerrain(Rectangle cell, TerrainType terrain) {
        if (terrain == null) {
            return;
        }
        switch (terrain) {
            case WATER -> addWaterCell(cell);
            case LOW_TIDE -> addLowTideCell(cell);
            case SLIPPERY_UP -> addSlipperyTile(cell, true);
            case SLIPPERY_DOWN -> addSlipperyTile(cell, false);
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

    private void addSlipperyTile(Rectangle cell, boolean up) {
        /*
         * Phase-2 calls these slippery GROUND cells, so the visual is deliberately
         * embedded into the tile instead of looking like a placed object or debug icon.
         */
        Texture texture = up ? slipperyUpTexture : slipperyDownTexture;
        if (texture != null) {
            Image ground = new Image(texture);
            ground.setScaling(Scaling.fit);
            ground.setBounds(
                    cell.x + cell.width * 0.02f,
                    cell.y + cell.height * 0.02f,
                    cell.width * 0.96f,
                    cell.height * 0.96f
            );
            ground.setColor(1f, 1f, 1f, 0.90f);
            addActor(ground);
        } else {
            addTint(cell, new Color(0.72f, 0.97f, 1f, 0.11f));
            addDirectionMarker(cell, up ? "^" : "v");
        }

        // Very light edge shine so the special ground remains readable on bright tiles.
        Color rim = new Color(0.88f, 1f, 1f, 0.38f);
        addThinBar(
                cell.x + cell.width * 0.14f,
                cell.y + cell.height * 0.12f,
                cell.width * 0.46f,
                1.5f,
                rim
        );
        addThinBar(
                cell.x + cell.width * 0.48f,
                cell.y + cell.height * 0.84f,
                cell.width * 0.34f,
                1.3f,
                new Color(rim.r, rim.g, rim.b, 0.28f)
        );
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
            if ("FROZEN_PLANT".equals(payload)) {
                if (!addPam(cell, FROST_ICE_BEHIND_PAM, "idle", 0.47f, 0f, -4f)) {
                    addTint(cell, new Color(0.70f, 0.94f, 1f, 0.22f));
                }
            } else if (!addPam(cell, FROST_ICE_BEHIND_PAM, "idle", 0.47f, 0f, -4f)) {
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

    /**
     * Permanent ocean body at the right edge of Big Wave Beach.
     *
     * The background supplied for the board is mostly sand in this margin, while
     * Phase 2 explicitly requires the sea to be visible on the right side.
     * This translucent layer preserves the original artwork underneath.
     */
    private void addBeachSeaBody() {
        if (theme.world() != GameWorld.BIG_WAVE_BEACH) {
            return;
        }

        Rectangle board = layout.boardBounds();
        float seaX = board.x + board.width;
        float seaWidth = Math.max(0f, 1280f - seaX);
        if (seaWidth <= 0f) {
            return;
        }

        float band = seaWidth / 5f;
        Color[] depth = {
                new Color(0.05f, 0.73f, 0.84f, 0.28f),
                new Color(0.03f, 0.67f, 0.82f, 0.32f),
                new Color(0.02f, 0.60f, 0.79f, 0.35f),
                new Color(0.02f, 0.53f, 0.75f, 0.38f),
                new Color(0.01f, 0.46f, 0.69f, 0.42f)
        };
        for (int i = 0; i < depth.length; i++) {
            Image sea = new Image(whiteTexture);
            sea.setColor(depth[i]);
            sea.setBounds(
                    seaX + i * band,
                    board.y - 10f,
                    i == depth.length - 1 ? seaWidth - i * band : band + 1f,
                    board.height + 20f
            );
            addActor(sea);
        }

        Image shorelineHaze = new Image(whiteTexture);
        shorelineHaze.setColor(0.72f, 0.98f, 1f, 0.18f);
        shorelineHaze.setBounds(
                seaX, board.y - 4f,
                Math.min(14f, seaWidth),
                board.height + 8f
        );
        addActor(shorelineHaze);

        addFoamStroke(seaX + 5f,  board.y + board.height * 0.13f, 35f, 0.38f);
        addFoamStroke(seaX + 28f, board.y + board.height * 0.16f, 22f, 0.23f);
        addFoamStroke(seaX + 8f,  board.y + board.height * 0.37f, 47f, 0.34f);
        addFoamStroke(seaX + 42f, board.y + board.height * 0.40f, 27f, 0.20f);
        addFoamStroke(seaX + 3f,  board.y + board.height * 0.61f, 39f, 0.35f);
        addFoamStroke(seaX + 33f, board.y + board.height * 0.64f, 31f, 0.22f);
        addFoamStroke(seaX + 10f, board.y + board.height * 0.84f, 44f, 0.32f);
    }

    private void addFoamStroke(float x, float y, float width, float alpha) {
        Image foam = new Image(whiteTexture);
        foam.setColor(0.82f, 0.98f, 1f, alpha);
        foam.setBounds(x, y, width, 2.1f);
        addActor(foam);
    }

    /** Water must match the model's WATER tiles exactly, cell for cell. */
    private void addWaterCell(Rectangle cell) {
        Image body = new Image(whiteTexture);
        body.setColor(0.02f, 0.66f, 0.84f, 0.28f);
        body.setBounds(
                cell.x + 1f, cell.y + 1f,
                Math.max(0f, cell.width - 2f),
                Math.max(0f, cell.height - 2f)
        );
        addActor(body);

        Image lowerDepth = new Image(whiteTexture);
        lowerDepth.setColor(0.01f, 0.45f, 0.72f, 0.11f);
        lowerDepth.setBounds(
                cell.x + 2f,
                cell.y + 2f,
                Math.max(0f, cell.width - 4f),
                Math.max(0f, cell.height * 0.48f)
        );
        addActor(lowerDepth);

        addThinBar(
                cell.x + cell.width * 0.10f,
                cell.y + cell.height * 0.72f,
                cell.width * 0.24f,
                1.8f,
                new Color(0.84f, 1f, 1f, 0.34f)
        );
        addThinBar(
                cell.x + cell.width * 0.47f,
                cell.y + cell.height * 0.56f,
                cell.width * 0.28f,
                1.5f,
                new Color(0.75f, 0.98f, 1f, 0.24f)
        );
        addThinBar(
                cell.x + cell.width * 0.24f,
                cell.y + cell.height * 0.25f,
                cell.width * 0.18f,
                1.3f,
                new Color(0.77f, 0.98f, 1f, 0.19f)
        );
    }

    /** Base wet-sand wash used while a configured LOW_TIDE cell is exposed. */
    private void addLowTideCell(Rectangle cell) {
        Image wash = new Image(whiteTexture);
        wash.setColor(0.12f, 0.66f, 0.68f, 0.07f);
        wash.setBounds(
                cell.x + 4f, cell.y + 4f,
                Math.max(0f, cell.width - 8f),
                Math.max(0f, cell.height - 8f)
        );
        addActor(wash);

        Image wetEdge = new Image(whiteTexture);
        wetEdge.setColor(0.08f, 0.50f, 0.55f, 0.10f);
        wetEdge.setBounds(
                cell.x + cell.width * 0.10f,
                cell.y + cell.height * 0.08f,
                cell.width * 0.80f,
                3f
        );
        addActor(wetEdge);
    }

    /**
     * Low-tide identity is based on ChapterRules, not merely the current TerrainType.
     * This matters because AdventureRuleSystem changes those same cells to WATER
     * while they are flooded, and those coordinates are exactly where low-tide
     * zombies can emerge.
     */
    private void addLowTideIdentityMarker(Rectangle cell, boolean flooded) {
        Color ripple = flooded
                ? new Color(0.90f, 1f, 0.82f, 0.62f)
                : new Color(0.09f, 0.56f, 0.62f, 0.62f);

        float cx = cell.x + cell.width * 0.50f;
        float y = cell.y + cell.height * 0.18f;

        addThinBar(cx - cell.width * 0.26f, y,
                cell.width * 0.52f, 2.2f, ripple);
        addThinBar(cx - cell.width * 0.19f, y + 7f,
                cell.width * 0.38f, 1.9f,
                new Color(ripple.r, ripple.g, ripple.b, ripple.a * 0.76f));
        addThinBar(cx - cell.width * 0.12f, y + 13f,
                cell.width * 0.24f, 1.6f,
                new Color(ripple.r, ripple.g, ripple.b, ripple.a * 0.54f));

        addThinBar(
                cell.x + cell.width * 0.16f,
                cell.y + cell.height * 0.62f,
                cell.width * 0.18f,
                1.6f,
                new Color(ripple.r, ripple.g, ripple.b, ripple.a * 0.46f)
        );
        addThinBar(
                cell.x + cell.width * 0.22f,
                cell.y + cell.height * 0.67f,
                cell.width * 0.13f,
                1.3f,
                new Color(ripple.r, ripple.g, ripple.b, ripple.a * 0.32f)
        );
    }

    /** Current shoreline: follows the actual WATER tiles as the Phase-1 state changes. */
    private void addCurrentTideLine(GameEngine engine, boolean preview) {
        if (theme.world() != GameWorld.BIG_WAVE_BEACH) {
            return;
        }

        int firstWaterColumn = firstWaterColumn(engine, preview);
        if (firstWaterColumn < 0) {
            return;
        }

        Rectangle board = layout.boardBounds();
        float x = board.x + firstWaterColumn * layout.cellWidth();

        Image surfGlow = new Image(whiteTexture);
        surfGlow.setColor(0.64f, 0.97f, 1f, 0.17f);
        surfGlow.setBounds(x - 8f, board.y, 16f, board.height);
        addActor(surfGlow);

        float[] yFractions = {
                0.06f, 0.17f, 0.30f, 0.43f,
                0.58f, 0.70f, 0.84f, 0.94f
        };
        float[] widths = {18f, 28f, 15f, 24f, 31f, 17f, 26f, 14f};
        float[] xOffsets = {-5f, -1f, -4f, 1f, -3f, 0f, -5f, -1f};

        for (int i = 0; i < yFractions.length; i++) {
            float y = board.y + board.height * yFractions[i];
            addThinBar(
                    x + xOffsets[i],
                    y,
                    widths[i],
                    i % 3 == 0 ? 2.3f : 1.8f,
                    new Color(0.86f, 1f, 1f, 0.56f - (i % 2) * 0.10f)
            );
        }

        Rectangle tideBounds = new Rectangle(
                x - layout.cellWidth() * 0.20f,
                board.y,
                layout.cellWidth() * 0.40f,
                board.height
        );

        if (!addPam(tideBounds, BEACH_TIDE_PAM, "idle", 0.25f, 0f, 0f)) {
            Image line = new Image(whiteTexture);
            line.setColor(0.78f, 0.99f, 1f, 0.76f);
            line.setBounds(x - 1.5f, board.y, 3f, board.height);
            addActor(line);
        }
    }

    /**
     * Fixed maximum tide reach required by Phase 2.
     *
     * Current shoreline is aqua/white and animated. Maximum reach is a warm,
     * quiet dashed rope marker so the two meanings cannot be confused.
     */
    private void addMaximumTideLine(GameEngine engine, boolean preview) {
        if (theme.world() != GameWorld.BIG_WAVE_BEACH) {
            return;
        }

        int maximumColumns = maximumWaterColumns(engine, preview);
        if (maximumColumns <= 0) {
            return;
        }

        maximumColumns = Math.min(BattlefieldLayout.COLUMNS, maximumColumns);
        Rectangle board = layout.boardBounds();
        float x = board.x
                + (BattlefieldLayout.COLUMNS - maximumColumns) * layout.cellWidth();

        Color marker = new Color(0.98f, 0.72f, 0.28f, 0.82f);
        float dashHeight = 13f;
        float gap = 11f;
        for (float y = board.y + 7f;
             y < board.y + board.height - 7f;
             y += dashHeight + gap) {
            float h = Math.min(dashHeight, board.y + board.height - 7f - y);
            addThinBar(x - 1.3f, y, 2.6f, h, marker);
        }

        // Tiny top tag; less intrusive than "MAX TIDE" written across the lawn.
        Image tag = new Image(whiteTexture);
        tag.setColor(0.98f, 0.72f, 0.28f, 0.86f);
        tag.setBounds(x - 10f, board.y + board.height - 4f, 20f, 4f);
        addActor(tag);

        Label label = new Label("MAX", skin);
        label.setColor(1f, 0.88f, 0.56f, 0.92f);
        label.setFontScale(0.46f);
        label.setAlignment(com.badlogic.gdx.utils.Align.center);
        label.setBounds(x - 24f, board.y + board.height + 1f, 48f, 15f);
        addActor(label);
    }

    private int maximumWaterColumns(GameEngine engine, boolean preview) {
        ChapterRules rules = beachRules(engine, preview);
        if (rules != null) {
            return rules.getMaximumWaterColumns();
        }

        // Exact Phase-1 fallback from adventure-chapters.csv:
        // BIG_WAVE_BEACH max_water_columns = 5.
        return 5;
    }

    private boolean isLowTideCoordinate(
            GameEngine engine,
            boolean preview,
            int row,
            int column
    ) {
        ChapterRules rules = beachRules(engine, preview);
        if (rules != null) {
            return rules.getLowTideTiles()
                    .contains(new model.level.TileCoordinate(column, row));
        }

        // Exact Phase-1 fallback from adventure-chapters.csv:
        // low_tide = 5:1;6:3  (x:row)
        return (column == 5 && row == 1)
                || (column == 6 && row == 3);
    }

    private ChapterRules beachRules(GameEngine engine, boolean preview) {
        if (!preview && engine != null) {
            AdventureRuntimeState state = engine.getAdventureState();
            if (state != null && state.getConfig() != null
                    && state.getConfig().getChapterRules() != null
                    && state.getConfig().getChapterRules().getWorld()
                    == GameWorld.BIG_WAVE_BEACH) {
                return state.getConfig().getChapterRules();
            }
        }
        return null;
    }

    private void addThinBar(float x, float y, float width, float height, Color color) {
        Image bar = new Image(whiteTexture);
        bar.setColor(color);
        bar.setBounds(x, y, Math.max(0f, width), Math.max(0f, height));
        addActor(bar);
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
                if (row == 3 && column == 5) {
                    yield TerrainType.SLIPPERY_DOWN;
                }
                yield TerrainType.NORMAL_FROSTBITE;
            }
            case BIG_WAVE_BEACH -> {
                // Exact Phase-1 wave-1 state: waterSchedule starts at 3 columns,
                // so columns 6..8 are flooded. Of the two configured low-tide
                // coordinates, only (5,1) is exposed; (6,3) is currently WATER.
                if (column >= 6) {
                    yield TerrainType.WATER;
                }
                if (column == 5 && row == 1) {
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
        if (theme.world() == GameWorld.BIG_WAVE_BEACH) {
            builder.append("|maxWater=").append(maximumWaterColumns(engine, preview));
        }
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
