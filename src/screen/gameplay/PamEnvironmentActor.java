package screen.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import pvz.libpvz.pam.PamPlayer;

/** Scene2D wrapper for supplied non-plant PAM assets such as mowers and graves. */
public final class PamEnvironmentActor extends Actor {
    private final PamPlayer player;
    private final String pamPath;
    private String clip;
    private final float scale;
    private final float xOffset;
    private final float yOffset;
    private float stateTime;
    private boolean failed;
    private java.util.List<String> idleClips = java.util.List.of();
    private float idleCycleSeconds = 2.2f;
    private int idleIndex;
    private float idleTimer;
    private boolean idleMode;
    private float timeScale = 1f;

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

    public void setIdleClips(java.util.List<String> clips) {
        this.idleClips = clips == null || clips.isEmpty() ? java.util.List.of("idle") : clips;
    }

    public void setTimeScale(float timeScale) {
        this.timeScale = timeScale <= 0f ? 1f : timeScale;
    }

    public void resumeIdleCycle() {
        if (!idleMode) {
            idleMode = true;
            idleIndex = 0;
            idleTimer = 0f;
            this.clip = idleClips.get(0);
            this.stateTime = 0f;
            this.failed = false;
        }
        this.timeScale = 1f; // idle always plays at normal speed
    }

    public void setClip(String clip) {
        if (clip != null && !clip.equals(this.clip)) {
            this.clip = clip;
            this.stateTime = 0f;
            this.failed = false;
            this.idleMode = false;
        }
    }

    public String getClip() {
        return clip;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta * timeScale;
        if (idleMode) {
            idleTimer += delta;
            if (idleTimer >= idleCycleSeconds) {
                idleTimer -= idleCycleSeconds;
                idleIndex = (idleIndex + 1) % idleClips.size();
                this.clip = idleClips.get(idleIndex);
                this.stateTime = 0f;
                this.failed = false;
            }
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

        /*
         * Scene2D shares one Batch across every actor. Tinted translucent overlays
         * (Dark Ages atmosphere, Night Ops, mower glows, etc.) can leave that Batch
         * with a low alpha. PAM drawing does not reset it by itself, so mowers and
         * other PAM environment actors can accidentally render ghost-like.
         */
        Color originalBatchColor = new Color(batch.getColor());
        Color actorColor = getColor();
        batch.setColor(
                actorColor.r,
                actorColor.g,
                actorColor.b,
                actorColor.a * parentAlpha
        );
        try {
            player.draw(batch, pamPath, clip, stateTime,
                    centerX, centerY, scale, scale, true);
        } catch (RuntimeException exception) {
            // A missing optional asset should never take down the gameplay screen.
            failed = true;
            Gdx.app.error("PamEnvironmentActor",
                    "Could not render PAM " + pamPath + " clip " + clip, exception);
        } finally {
            batch.setColor(originalBatchColor);
        }
    }
}
