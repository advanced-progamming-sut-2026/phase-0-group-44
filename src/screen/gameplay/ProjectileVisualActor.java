package screen.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import model.inGame.projectile.Projectile;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

/**
 * Scene2D projectile actor that fits the real PAM animation bounds into the
 * requested tile-sized actor bounds. This mirrors the reference project's
 * PamAnimationActor approach instead of treating catalog tile sizes as raw
 * libPVZ scale factors.
 */
final class ProjectileVisualActor extends Actor {
    private final PamPlayer pamPlayer;
    private final TextureBank textureBank;
    private ProjectileAssetCatalog.VisualSpec visualSpec;
    private Rectangle animationBounds;
    private TextureRegion staticRegion;

    private float targetX;
    private float targetY;
    private float targetSize;
    private float stateTime;
    private boolean initialized;
    private boolean failed;

    ProjectileVisualActor(PamPlayer pamPlayer, TextureBank textureBank) {
        this.pamPlayer = pamPlayer;
        this.textureBank = textureBank;
        setTouchable(Touchable.disabled);
    }

    void updateStyle(Projectile projectile) {
        if (projectile == null || pamPlayer == null) {
            return;
        }
        ProjectileAssetCatalog.VisualSpec wanted =
                ProjectileAssetCatalog.forProjectile(projectile);
        if (wanted.equals(visualSpec)
                && (animationBounds != null || staticRegion != null)
                && !failed) {
            return;
        }

        visualSpec = wanted;
        stateTime = 0f;
        failed = false;
        staticRegion = null;
        animationBounds = null;

        // Kernel-pult butter is stored as a standalone atlas region. Resolve
        // it through the same libPVZ TextureBank that backs the PAM player.
        // If a reduced asset pack does not contain the image ID, fall back to
        // the PAM supplied in the same VisualSpec instead of disappearing.
        if (wanted.usesStaticImage() && textureBank != null) {
            try {
                staticRegion = textureBank.region(wanted.imageId());
            } catch (RuntimeException exception) {
                Gdx.app.error("ProjectileVisualActor",
                        "Could not load projectile image " + wanted.imageId(), exception);
            }
            if (staticRegion != null
                    && staticRegion.getRegionWidth() > 0
                    && staticRegion.getRegionHeight() > 0) {
                return;
            }
            staticRegion = null;
        }

        try {
            animationBounds = pamPlayer.bounds(wanted.pamPath(), wanted.clip());
            if (animationBounds == null || animationBounds.width <= 0f
                    || animationBounds.height <= 0f) {
                failed = true;
            }
        } catch (RuntimeException exception) {
            failed = true;
            Gdx.app.error("ProjectileVisualActor",
                    "Could not inspect PAM " + wanted.pamPath()
                            + " clip " + wanted.clip(), exception);
        }
    }

    float getProjectileSizeTiles() {
        return visualSpec == null ? 0.46f : visualSpec.sizeTiles();
    }

    void setTarget(float centerX, float centerY, float size) {
        targetSize = Math.max(8f, size);
        targetX = centerX - targetSize * 0.5f;
        targetY = centerY - targetSize * 0.5f;
        if (!initialized) {
            initialized = true;
            setBounds(targetX, targetY, targetSize, targetSize);
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += Math.max(0f, delta);
        if (!initialized) {
            return;
        }
        float follow = 1f - (float) Math.exp(-24f * Math.max(0f, delta));
        setBounds(
                MathUtils.lerp(getX(), targetX, follow),
                MathUtils.lerp(getY(), targetY, follow),
                MathUtils.lerp(getWidth(), targetSize, follow),
                MathUtils.lerp(getHeight(), targetSize, follow)
        );
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (failed || visualSpec == null
                || getWidth() <= 0f || getHeight() <= 0f) {
            return;
        }

        if (staticRegion != null) {
            drawStaticRegion(batch, parentAlpha);
            return;
        }

        if (pamPlayer == null || animationBounds == null
                || animationBounds.width <= 0f || animationBounds.height <= 0f) {
            return;
        }

        float fitScale = Math.min(
                getWidth() / animationBounds.width,
                getHeight() / animationBounds.height
        );
        float centerX = getX() + getWidth() * 0.5f;
        float centerY = getY() + getHeight() * 0.5f;

        Color oldColor = new Color(batch.getColor());
        Matrix4 oldTransform = new Matrix4(batch.getTransformMatrix());
        Matrix4 scaledTransform = new Matrix4(oldTransform);
        scaledTransform.translate(centerX, centerY, 0f);
        scaledTransform.scale(fitScale, fitScale, 1f);
        scaledTransform.translate(-centerX, -centerY, 0f);

        batch.flush();
        batch.setTransformMatrix(scaledTransform);
        Color actorColor = getColor();
        batch.setColor(actorColor.r, actorColor.g, actorColor.b,
                actorColor.a * parentAlpha);
        try {
            pamPlayer.draw(batch, visualSpec.pamPath(), visualSpec.clip(),
                    stateTime, centerX, centerY, true);
        } catch (RuntimeException exception) {
            failed = true;
            Gdx.app.error("ProjectileVisualActor",
                    "Could not render PAM " + visualSpec.pamPath()
                            + " clip " + visualSpec.clip(), exception);
        } finally {
            batch.flush();
            batch.setTransformMatrix(oldTransform);
            batch.setColor(oldColor);
        }
    }
    private void drawStaticRegion(Batch batch, float parentAlpha) {
        float sourceWidth = staticRegion.getRegionWidth();
        float sourceHeight = staticRegion.getRegionHeight();
        float fit = Math.min(getWidth() / sourceWidth, getHeight() / sourceHeight);
        float width = sourceWidth * fit;
        float height = sourceHeight * fit;
        float x = getX() + (getWidth() - width) * 0.5f;
        float y = getY() + (getHeight() - height) * 0.5f;

        Color oldColor = new Color(batch.getColor());
        Color actorColor = getColor();
        batch.setColor(actorColor.r, actorColor.g, actorColor.b,
                actorColor.a * parentAlpha);
        batch.draw(staticRegion, x, y, width, height);
        batch.setColor(oldColor);
    }

}
