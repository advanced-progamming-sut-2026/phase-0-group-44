package pvz.browser.widgets;

import org.lwjgl.opengl.GL20;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import pvz.browser.core.BrowserContext;
import pvz.browser.utils.InterpolatedValue;
import pvz.browser.core.UiManager;
import pvz.libpvz.pam.PamPlayer;

public class WorldActor extends WidgetGroup {
    private final ShapeRenderer shapeRenderer;
    private final PamPlayer player;
    private final OrthographicCamera camera;
    private final ScreenViewport worldViewport;

    private final ShaderProgram gridShader;
    private final Texture dummyTexture;

    private boolean middleBtnDown;
    private boolean leftBtnDown;
    private int lastScreenX;
    private int lastScreenY;
    private InterpolatedValue zoom;

    private final Vector3 boxBottomLeft = new Vector3();
    private final Vector3 boxTopRight = new Vector3();
    private boolean sizing;
    private BitmapFont font;

    private final Vector2 tempCoords = new Vector2();
    private final Matrix4 oldProjection = new Matrix4();

    public WorldActor() {
        this.player = BrowserContext.player;

        String vert = Gdx.files.internal("shaders/grid.vert").readString();
        String frag = Gdx.files.internal("shaders/grid.frag").readString();
        this.gridShader = new ShaderProgram(vert, frag);

        if (!gridShader.isCompiled()) {
            Gdx.app.error("Shader", "Grid compilation failed:\n" + gridShader.getLog());
        }

        camera = new OrthographicCamera();
        worldViewport = new ScreenViewport(camera);

        this.dummyTexture = new Texture(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        zoom = new InterpolatedValue(1, 0.5f, 0.5f, 20f);
        shapeRenderer = new ShapeRenderer();
        font = UiManager.skin.getFont("FBUSV8C5EI_1_outline");

        this.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (getStage() != null) {
                    getStage().setScrollFocus(WorldActor.this);
                }

                if (button == Input.Buttons.MIDDLE) {
                    middleBtnDown = true;
                    lastScreenX = Gdx.input.getX();
                    lastScreenY = Gdx.input.getY();
                    return true;
                } else if (button == Input.Buttons.LEFT) {
                    leftBtnDown = true;
                    return true;
                }
                return false;
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (getStage() != null) {
                    getStage().setScrollFocus(WorldActor.this);
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.MIDDLE) {
                    middleBtnDown = false;
                } else if (button == Input.Buttons.LEFT) {
                    leftBtnDown = false;
                    sizing = false;
                }
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                int screenX = Gdx.input.getX();
                int screenY = Gdx.input.getY();

                if (middleBtnDown) {
                    float deltaX = screenX - lastScreenX;
                    float deltaY = screenY - lastScreenY;

                    camera.translate(-deltaX * camera.zoom, deltaY * camera.zoom);

                    lastScreenX = screenX;
                    lastScreenY = screenY;
                }
                if (leftBtnDown) {
                    if (!sizing) {
                        sizing = true;
                        boxBottomLeft.set(screenX, screenY, 0);
                        worldViewport.unproject(boxBottomLeft);
                    }
                }
                if (sizing) {
                    boxTopRight.set(screenX, screenY, 0);
                    worldViewport.unproject(boxTopRight);
                }
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                zoom.setTarget(zoom.getTarget() + 0.1f * amountY);
                return true;
            }
        });
    }

    @Override
    public Actor hit(float x, float y, boolean touchable) {
        if (touchable && getTouchable() == Touchable.disabled) return null;
        if (!isVisible()) return null;

        // Ensure we don't register hits outside of our scissor bounds!
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return null;
        }

        float worldX = camera.position.x + (x - getWidth() / 2f) * camera.zoom;
        float worldY = camera.position.y + (y - getHeight() / 2f) * camera.zoom;

        Vector2 point = new Vector2();
        Array<Actor> children = getChildren();
        for (int i = children.size - 1; i >= 0; i--) {
            Actor child = children.get(i);
            if (!child.isVisible()) continue;
            child.parentToLocalCoordinates(point.set(worldX, worldY));
            Actor hit = child.hit(point.x, point.y, touchable);
            if (hit != null) return hit;
        }

        return this;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();

        tempCoords.set(0, 0);
        localToStageCoordinates(tempCoords);
        if (getStage() != null) {
            getStage().getViewport().project(tempCoords);
        }

        int screenX = (int) tempCoords.x;
        int screenY = (int) tempCoords.y;
        int screenWidth = (int) getWidth();
        int screenHeight = (int) getHeight();

        worldViewport.setScreenBounds(screenX, screenY, screenWidth, screenHeight);
        worldViewport.setWorldSize(screenWidth, screenHeight);

        zoom.act(Gdx.graphics.getDeltaTime());
        camera.zoom = zoom.value;
        camera.update();

        oldProjection.set(batch.getProjectionMatrix());
        batch.end();

        worldViewport.apply();
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        HdpiUtils.glScissor(screenX, screenY, screenWidth, screenHeight);

        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        float worldW = camera.viewportWidth * camera.zoom;
        float worldH = camera.viewportHeight * camera.zoom;
        float worldX = camera.position.x - worldW / 2f;
        float worldY = camera.position.y - worldH / 2f;

        batch.setShader(gridShader);
        batch.begin();
        gridShader.setUniformf("zoom", camera.zoom);
        batch.draw(dummyTexture, worldX, worldY, worldW, worldH);
        batch.end();
        batch.setShader(null);

        batch.begin();
        for (Actor a : getChildren()) {
            a.draw(batch, parentAlpha);
        }
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (sizing) {
            float width = boxTopRight.x - boxBottomLeft.x;
            float height = boxTopRight.y - boxBottomLeft.y;

            shapeRenderer.begin(ShapeType.Filled);
            shapeRenderer.setColor(new Color(1, 1, 1, 0.1f));
            shapeRenderer.rect(boxBottomLeft.x, boxBottomLeft.y, width, height);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeType.Line);
            shapeRenderer.setColor(new Color(1, 1, 1, 0.8f));
            shapeRenderer.rect(boxBottomLeft.x, boxBottomLeft.y, width, height);
            shapeRenderer.end();

            batch.begin();

            float oldScaleX = font.getScaleX();
            float oldScaleY = font.getScaleY();
            font.getData().setScale(camera.zoom / 1.3f);

            String textWidth = String.format("%d", Math.abs((int) width));
            String textHeight = String.format("%d", Math.abs((int) height));

            GlyphLayout layoutWidth = new GlyphLayout(font, textWidth);
            GlyphLayout layoutHeight = new GlyphLayout(font, textHeight);

            boolean negativeWidth = width < 0;
            boolean negativeHeight = height < 0;

            float widthX = boxBottomLeft.x + width / 2f - layoutWidth.width / 2f;
            float widthY = boxBottomLeft.y - (layoutWidth.height) * (negativeHeight ? -1f : 0)
                    + 10 * (negativeHeight ? 1 : -1);
            font.draw(batch, layoutWidth, widthX, widthY);

            float heightX = boxBottomLeft.x - (layoutHeight.width) * (negativeWidth ? 0f : 1)
                    + 10 * (negativeWidth ? 1 : -1);
            float heightY = boxBottomLeft.y + height / 2f + layoutHeight.height / 2f;
            font.draw(batch, layoutHeight, heightX, heightY);

            font.getData().setScale(oldScaleX, oldScaleY);
            batch.end();
        }

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        if (getStage() != null) {
            getStage().getViewport().apply();
        }
        batch.setProjectionMatrix(oldProjection);

        batch.begin();
    }
}