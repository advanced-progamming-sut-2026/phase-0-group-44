package pvz.browser.widgets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import pvz.browser.core.UiManager;

public class SplitPane extends Table {

    private final Cell<Actor> leftCell;
    private final Cell<Actor> secondCell;
    private float splitAmount = 0.5f;
    private Texture whitePixelTexture;

    public SplitPane(Actor leftWidget, Actor rightWidget) {
        Actor notch = createNotchActor();
        notch.setColor(1, 1, 1, 0);

        leftCell = add(leftWidget).growY().expandX();
        add(notch).width(10f).fillY();
        secondCell = add(rightWidget).growY().expandX();

        notch.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer != -1)
                    return;
                notch.addAction(
                        Actions.alpha(1, 0.4f, Interpolation.smooth));
                Gdx.graphics.setSystemCursor(SystemCursor.HorizontalResize);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer != -1)
                    return;
                notch.addAction(
                        Actions.alpha(0, 0.4f, Interpolation.smooth));
                Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                com.badlogic.gdx.math.Vector2 localCoords = new com.badlogic.gdx.math.Vector2(event.getStageX(),
                        event.getStageY());
                SplitPane.this.stageToLocalCoordinates(localCoords);
                splitAmount = Math.max(0.1f, Math.min(0.9f, localCoords.x / getWidth()));
                invalidate();
            }
        });

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        pixmap.setColor(Color.WHITE);
        pixmap.fill();

        whitePixelTexture = new Texture(pixmap);

        pixmap.dispose();
    }

    private Actor createNotchActor() {
        return new Actor() {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                Color color = getColor();
                // Combine actor's alpha with parent's alpha
                batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

                float cx = getX() + getWidth() / 2f;
                float cy = getY() + getHeight() / 2f;

                // Uses LibGDX's built-in 1x1 white pixel region
                batch.draw(
                        whitePixelTexture,
                        cx - 2,
                        cy - 30,
                        4,
                        60);
            }
        };
    }

    @Override
    public void layout() {
        float availableWidth = getWidth() - 12f;
        leftCell.width(availableWidth * splitAmount);
        secondCell.width(availableWidth * (1f - splitAmount));
        super.layout();
    }

    public void setSplitAmount(float splitAmount) {
        this.splitAmount = splitAmount;
    }
}