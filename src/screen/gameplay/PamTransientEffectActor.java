package screen.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import pvz.libpvz.pam.PamPlayer;

/** One-shot PAM effect (projectile flash, explosion, steam, etc.) that removes itself after playing once. */
public final class PamTransientEffectActor extends Actor {
    private final PamPlayer player;
    private final String pamPath;
    private final String clip;
    private final float scale;
    private final float xOffset;
    private final float yOffset;
    private final float duration;
    private float stateTime;
    private boolean failed;

    public PamTransientEffectActor(
            PamPlayer player, String pamPath, String clip,
            float scale, float xOffset, float yOffset, float duration
    ) {
        this.player = player;
        this.pamPath = pamPath;
        this.clip = clip;
        this.scale = scale;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.duration = duration;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;
        if (stateTime >= duration) {
            remove();
        }
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
            player.draw(batch, pamPath, clip, stateTime, centerX, centerY, scale, scale, true);
        } catch (RuntimeException exception) {
            failed = true;
            Gdx.app.error("PamTransientEffectActor",
                    "Could not render PAM " + pamPath + " clip " + clip, exception);
        }
    }
}