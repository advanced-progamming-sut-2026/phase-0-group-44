package screen.gameplay;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import model.enums.DamageType;
import model.inGame.projectile.ProjectileImpact;

/** Small expanding collision flash at the model's exact projectile impact position. */
final class ProjectileImpactActor extends Group {
    private final Image outer;
    private final Image inner;

    ProjectileImpactActor(Texture circleTexture, ProjectileImpact impact) {
        outer = new Image(circleTexture);
        inner = new Image(circleTexture);
        addActor(outer);
        addActor(inner);
        setTouchable(Touchable.disabled);
        update(impact);
    }

    void update(ProjectileImpact impact) {
        if (impact == null) {
            return;
        }
        float life = (float) Math.max(0.0, Math.min(1.0,
                impact.getRemainingSeconds() / impact.getLifetimeSeconds()));
        float progress = 1f - life;

        Color color = switch (impact.getDamageType()) {
            case FIRE -> new Color(1f, 0.35f, 0.05f, 0.72f * life);
            case ICE -> new Color(0.40f, 0.92f, 1f, 0.70f * life);
            case POISON -> new Color(0.52f, 0.90f, 0.22f, 0.70f * life);
            case TRUE -> new Color(0.86f, 0.38f, 1f, 0.72f * life);
            default -> new Color(0.72f, 1f, 0.38f, 0.62f * life);
        };
        outer.setColor(color);
        inner.setColor(1f, 1f, 1f, 0.74f * life);

        float w = getWidth();
        float h = getHeight();
        float outerScale = 0.78f + progress * 0.75f;
        float innerScale = 0.42f + progress * 0.34f;
        outer.setBounds(
                w * (0.5f - outerScale * 0.5f),
                h * (0.5f - outerScale * 0.5f),
                w * outerScale,
                h * outerScale
        );
        inner.setBounds(
                w * (0.5f - innerScale * 0.5f),
                h * (0.5f - innerScale * 0.5f),
                w * innerScale,
                h * innerScale
        );
    }
}
