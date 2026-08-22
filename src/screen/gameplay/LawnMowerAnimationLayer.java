package screen.gameplay;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import model.GameEngine;
import model.config.GameWorld;
import pvz.libpvz.pam.PamPlayer;

/**
 * Visual-only continuous lawn-mower sweep.
 *
 * <p>The Phase-1 model intentionally resolves a mower hit immediately. This layer
 * watches the existing used/not-used flag and mirrors that transition with the
 * supplied chapter mower PAM moving across all nine cells. It therefore adds the
 * Phase-2 beauty animation without changing combat semantics.</p>
 */
public final class LawnMowerAnimationLayer extends Group {
    private static final float RUN_SECONDS = 4.60f;
    private static final float ACTIVATION_SECONDS = 0.28f;

    private final BattlefieldTheme theme;
    private final BattlefieldLayout layout;
    private final PamPlayer pamPlayer;
    private final FileHandle pamRoot;
    private final boolean[] previousUsed = new boolean[BattlefieldLayout.ROWS];
    private boolean primed;

    public LawnMowerAnimationLayer(
            BattlefieldTheme theme,
            BattlefieldLayout layout,
            PamPlayer pamPlayer,
            FileHandle pamRoot
    ) {
        this.theme = theme;
        this.layout = layout;
        this.pamPlayer = pamPlayer;
        this.pamRoot = pamRoot;
        setSize(1280f, 720f);
        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    }

    /** Prevents a loaded/resumed game from replaying mowers that were already spent. */
    public void prime(GameEngine engine, boolean preview) {
        for (int row = 0; row < previousUsed.length; row++) {
            previousUsed[row] = !preview && engine != null && engine.isLawnMowerUsed(row);
        }
        primed = true;
    }

    /** Starts an animation exactly when a row changes from mower-ready to mower-used. */
    public void sync(GameEngine engine, boolean preview) {
        if (!primed) {
            prime(engine, preview);
            return;
        }
        if (preview || engine == null) {
            return;
        }

        for (int row = 0; row < previousUsed.length; row++) {
            boolean used = engine.isLawnMowerUsed(row);
            if (used && !previousUsed[row]) {
                launch(row);
            }
            previousUsed[row] = used;
        }
    }

    private void launch(int row) {
        if (pamPlayer == null || theme.mowerPam() == null || !mowerAssetExists()) {
            return;
        }

        Rectangle start = layout.mowerBounds(row);
        Rectangle board = layout.boardBounds();
        float scale = adjustedScale();

        PamEnvironmentActor mower = new PamEnvironmentActor(
                pamPlayer, theme.mowerPam(), "transition", scale, 0f, 0f);
        mower.setBounds(start.x, start.y, start.width, start.height);
        addActor(mower);

        // Switch to the mower's attack loop after its short activation clip.
        mower.addAction(Actions.sequence(
                Actions.delay(ACTIVATION_SECONDS),
                Actions.run(() -> mower.setClip("attack"))
        ));

        // Travel from the mower slot through the full 9-column board and just
        // off the right edge, then remove the visual. Stage delta=0 while paused,
        // so the sweep freezes correctly with the rest of gameplay.
        float finishX = board.x + board.width + start.width * 0.45f;
        mower.addAction(Actions.sequence(
                // Let the activation clip read before the mower starts crossing
                // the lawn, then keep a clearly visible constant-speed sweep.
                Actions.delay(ACTIVATION_SECONDS * 0.75f),
                Actions.moveTo(finishX, start.y, RUN_SECONDS, Interpolation.linear),
                Actions.fadeOut(0.16f),
                Actions.removeActor()
        ));
    }

    private boolean mowerAssetExists() {
        if (pamRoot == null) {
            return true;
        }
        FileHandle direct = pamRoot.child(theme.mowerPam());
        FileHandle images = pamRoot.child("IMAGES").child(theme.mowerPam());
        return direct.exists() || images.exists();
    }

    private float adjustedScale() {
        float scale = theme.mowerScale();
        if (theme.world() == GameWorld.FROSTBITE_CAVES) {
            return scale * 1.24f;
        }
        if (theme.world() == GameWorld.BIG_WAVE_BEACH) {
            return scale * 1.18f;
        }
        return scale;
    }
}
