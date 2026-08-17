package screen.gameplay;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;
import model.GameEngine;
import model.Position;
import model.config.GameWorld;
import model.inGame.plant.Plant;
import model.inGame.zombie.Zombie;
import pvz.libpvz.pam.PamPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Frostbite entity-state visuals that must sit ABOVE plant/zombie actors.
 *
 * <p>EnvironmentLayer supplies the "behind" half of a frozen plant's ice shell.
 * This layer supplies the front half, so the plant remains visible inside the ice.
 * Frozen zombies deliberately receive an opaque/full ice block above the zombie,
 * matching the Phase-2 requirement that the zombie itself need not be shown.</p>
 */
public final class BattlefieldFrostbiteStateLayer extends Group {
    private static final String PLANT_ICE_FRONT_PAM =
            "768/FULL/EFFECTS/FROSTBITE_ICE_BLOCK_PLANT/FROSTBITE_ICE_BLOCK_PLANT.PAM";
    private static final String PLANT_CHILL_PAM =
            "768/FULL/EFFECTS/FROSTBITE_CHILL_PLANT/FROSTBITE_CHILL_PLANT.PAM";
    private static final String ZOMBIE_ICE_FRONT_PAM =
            "768/FULL/EFFECTS/FROSTBITE_ICE_BLOCK_ZOMBIE/FROSTBITE_ICE_BLOCK_ZOMBIE.PAM";

    private final BattlefieldTheme theme;
    private final BattlefieldLayout layout;
    private final Texture whiteTexture;
    private final Texture frozenZombieTexture;
    private final PamPlayer pamPlayer;
    private final FileHandle pamRoot;

    private String lastSignature = "";

    public BattlefieldFrostbiteStateLayer(
            BattlefieldTheme theme,
            BattlefieldLayout layout,
            Texture whiteTexture,
            Texture frozenZombieTexture,
            PamPlayer pamPlayer,
            FileHandle pamRoot
    ) {
        this.theme = theme;
        this.layout = layout;
        this.whiteTexture = whiteTexture;
        this.frozenZombieTexture = frozenZombieTexture;
        this.pamPlayer = pamPlayer;
        this.pamRoot = pamRoot;
        setSize(1280f, 720f);
    }

    public void sync(GameEngine engine, boolean preview) {
        if (theme.world() != GameWorld.FROSTBITE_CAVES) {
            if (getChildren().size > 0) {
                clearChildren();
            }
            lastSignature = "";
            return;
        }

        String signature = signature(engine, preview);
        if (signature.equals(lastSignature)) {
            return;
        }
        lastSignature = signature;
        clearChildren();

        if (preview || engine == null) {
            // Exact configured chapter preview: NORMAL@7.5:2.
            addFrozenZombieBlock(2, 7.5);
            return;
        }

        // Plants: preserve the plant actor underneath and draw only the FRONT ice shell here.
        for (Plant plant : engine.getGameMap().getPlants()) {
            if (plant == null || plant.isDead() || plant.getPosition() == null) {
                continue;
            }
            Position pos = plant.getPosition();
            if (plant.getBooleanState("FROZEN")) {
                addFrozenPlantFront(pos.getRow(), pos.getColumn());
            } else {
                int hits = plant.getState("ICE_HITS", Integer.class, 0);
                if (hits > 0) {
                    addPlantChill(pos.getRow(), pos.getColumn(), hits);
                }
            }
        }

        // Zombies: full ice block is deliberately above the zombie actor.
        for (Zombie zombie : engine.getZombies()) {
            if (zombie == null || zombie.isDead() || !zombie.isFrozen()) {
                continue;
            }
            addFrozenZombieBlock(zombie.getRow(), zombie.getX());
        }
    }

    private void addFrozenPlantFront(int row, int column) {
        if (!inside(row, column)) {
            return;
        }
        Rectangle cell = layout.cellBounds(row, column);
        PamEnvironmentActor ice = pamActor(PLANT_ICE_FRONT_PAM, "freeze_idle", 0.47f);
        if (ice != null) {
            ice.setBounds(
                    cell.x - cell.width * 0.13f,
                    cell.y - cell.height * 0.15f,
                    cell.width * 1.26f,
                    cell.height * 1.42f
            );
            addActor(ice);
        } else {
            addFallbackIce(cell, false);
        }
    }

    /** First/second ice hit gets a small frost cue; third hit becomes the full shell. */
    private void addPlantChill(int row, int column, int hits) {
        if (!inside(row, column)) {
            return;
        }
        Rectangle cell = layout.cellBounds(row, column);
        PamEnvironmentActor chill = pamActor(PLANT_CHILL_PAM, "animation", 0.24f);
        if (chill != null) {
            chill.setBounds(
                    cell.x - cell.width * 0.06f,
                    cell.y - cell.height * 0.02f,
                    cell.width * 1.12f,
                    cell.height * 1.12f
            );
            chill.setColor(1f, 1f, 1f, hits >= 2 ? 0.72f : 0.48f);
            addActor(chill);
        }
    }

    private void addFrozenZombieBlock(int row, double x) {
        if (row < 0 || row >= BattlefieldLayout.ROWS) {
            return;
        }

        Rectangle board = layout.boardBounds();
        int column = Math.max(
                0,
                Math.min(BattlefieldLayout.COLUMNS - 1, (int) Math.floor(x))
        );
        Rectangle cell = layout.cellBounds(row, column);
        float centerX = board.x + (float) (x * layout.cellWidth());

        /*
         * Use the intact ice-block sprite directly from the supplied Frostbite atlas.
         * This is intentionally entity-sized: it hides the frozen zombie but does not
         * resemble a full-cell terrain rectangle.
         */
        float width = layout.cellWidth() * 0.92f;
        float height = cell.height * 1.24f;
        float drawX = centerX - width * 0.50f;
        float drawY = cell.y - cell.height * 0.07f;

        // Small contact shadow / cold glow anchors the block to the tile.
        Image glow = new Image(whiteTexture);
        glow.setColor(0.32f, 0.91f, 1f, 0.18f);
        glow.setBounds(
                centerX - cell.width * 0.43f,
                cell.y + cell.height * 0.02f,
                cell.width * 0.86f,
                cell.height * 0.12f
        );
        addActor(glow);

        if (frozenZombieTexture != null) {
            Image block = new Image(frozenZombieTexture);
            block.setScaling(Scaling.stretch);
            block.setBounds(drawX, drawY, width, height);
            addActor(block);
            return;
        }

        /*
         * If the cropped texture is unavailable for any reason, prefer the native
         * PAM before falling back to a procedural shape.
         */
        PamEnvironmentActor ice = pamActor(
                ZOMBIE_ICE_FRONT_PAM,
                "ice_block_full",
                0.46f
        );
        if (ice == null) {
            ice = pamActor(ZOMBIE_ICE_FRONT_PAM, "idle", 0.46f);
        }
        if (ice != null) {
            ice.setBounds(drawX, drawY, width, height);
            addActor(ice);
        } else {
            addFallbackIce(new Rectangle(drawX, drawY, width, height), true);
        }
    }

    private void addFallbackIce(Rectangle bounds, boolean opaque) {
        Image block = new Image(whiteTexture);
        block.setScaling(Scaling.stretch);
        block.setColor(0.58f, 0.91f, 1f, opaque ? 0.92f : 0.44f);
        block.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        addActor(block);

        Image shine = new Image(whiteTexture);
        shine.setColor(0.92f, 1f, 1f, opaque ? 0.64f : 0.48f);
        shine.setBounds(
                bounds.x + bounds.width * 0.18f,
                bounds.y + bounds.height * 0.18f,
                Math.max(2f, bounds.width * 0.08f),
                bounds.height * 0.62f
        );
        shine.setRotation(-11f);
        addActor(shine);
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

    private boolean inside(int row, int column) {
        return row >= 0 && row < BattlefieldLayout.ROWS
                && column >= 0 && column < BattlefieldLayout.COLUMNS;
    }

    private String signature(GameEngine engine, boolean preview) {
        if (preview || engine == null) {
            return "preview|frozen-zombie@7.5:2";
        }

        StringBuilder builder = new StringBuilder("frostbite");

        List<Plant> plants = new ArrayList<>(engine.getGameMap().getPlants());
        plants.sort(Comparator.comparingInt((Plant p) -> p.getPosition() == null
                        ? Integer.MAX_VALUE : p.getPosition().getRow())
                .thenComparingInt(p -> p.getPosition() == null
                        ? Integer.MAX_VALUE : p.getPosition().getColumn()));
        for (Plant plant : plants) {
            if (plant == null || plant.getPosition() == null || plant.isDead()) {
                continue;
            }
            builder.append("|p:")
                    .append(plant.getPosition().getRow()).append(',')
                    .append(plant.getPosition().getColumn()).append(',')
                    .append(plant.getBooleanState("FROZEN")).append(',')
                    .append(plant.getState("ICE_HITS", Integer.class, 0));
        }

        List<Zombie> zombies = new ArrayList<>(engine.getZombies());
        zombies.sort(Comparator.comparingLong(Zombie::getId));
        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.isDead() || !zombie.isFrozen()) {
                continue;
            }
            builder.append("|z:")
                    .append(zombie.getId()).append(',')
                    .append(zombie.getRow()).append(',')
                    .append(Math.round(zombie.getX() * 20.0) / 20.0);
        }
        return builder.toString();
    }
}
