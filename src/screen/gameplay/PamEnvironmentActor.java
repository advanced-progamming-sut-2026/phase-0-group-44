package screen.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import pvz.libpvz.pam.PamPlayer;

/** Scene2D wrapper for supplied non-plant PAM assets such as mowers and graves. */
public final class PamEnvironmentActor extends Actor {
    private final PamPlayer player;
    private final String pamPath;
    private final String clip;
    private final float scale;
    private final float xOffset;
    private final float yOffset;
    private float stateTime;
    private boolean failed;

    public PamEnvironmentActor(
            PamPlayer player,
            String pamPath,
            String clip,
            float scale,
            float xOffset,
            float yOffset
    ) {
        this.player = player;
        this.pamPath = pamPath;
        this.clip = clip;
        this.scale = scale;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (failed || player == null || pamPath == null || clip == null) {
            return;
        }
        float centerX = getX() + getWidth() * 0.5f + xOffset;
        float centerY = getY() + getHeight() * 0.5f + yOffset;
        try {
            player.draw(batch, pamPath, clip, stateTime,
                    centerX, centerY, scale, scale, true);
        } catch (RuntimeException exception) {
            // A missing optional asset should never take down the gameplay screen.
            failed = true;
            Gdx.app.error("PamEnvironmentActor",
                    "Could not render PAM " + pamPath + " clip " + clip, exception);
        }
    }
}
