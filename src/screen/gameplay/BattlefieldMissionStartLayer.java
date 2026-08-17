package screen.gameplay;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import java.util.List;

/** Modal shown before every level. GameplayScreen keeps simulation ticks frozen until Continue. */
public final class BattlefieldMissionStartLayer {
    private static final float WIDTH = 1280f;
    private static final float HEIGHT = 720f;

    private final Group root = new Group();
    private final Texture whiteTexture;
    private final Texture panelTexture;
    private final Texture bulletTexture;
    private final Skin skin;
    private final Runnable continueAction;
    private final TextButton.TextButtonStyle continueStyle;

    public BattlefieldMissionStartLayer(
            Texture whiteTexture,
            Texture panelTexture,
            Texture bulletTexture,
            Texture purpleButton,
            Texture purpleButtonDown,
            Skin skin,
            Runnable continueAction
    ) {
        this.whiteTexture = whiteTexture;
        this.panelTexture = panelTexture;
        this.bulletTexture = bulletTexture;
        this.skin = skin;
        this.continueAction = continueAction;
        this.continueStyle = styledButton("purple", purpleButton, purpleButtonDown);

        root.setSize(WIDTH, HEIGHT);
        root.setVisible(false);
    }

    public Group root() {
        return root;
    }

    public void show(BattlefieldMissionObjectives.MissionInfo info) {
        root.clearChildren();
        root.setVisible(true);

        Image shade = new Image(whiteTexture);
        shade.setColor(0f, 0f, 0f, 0.56f);
        shade.setBounds(0f, 0f, WIDTH, HEIGHT);
        shade.addListener(new ClickListener());
        root.addActor(shade);

        int objectiveCount = Math.max(
                1, info.objectives() == null ? 0 : info.objectives().size());

        // Keep normal/simple levels compact while allowing special levels
        // with multiple objectives to grow naturally.
        final float x = 315f;
        final float w = 650f;
        final float h = Math.min(360f, 285f + (objectiveCount - 1) * 42f);
        final float y = (HEIGHT - h) * 0.5f + 4f;

        Image board = new Image(panelTexture);
        board.setScaling(Scaling.stretch);
        board.setBounds(x, y, w, h);
        root.addActor(board);

        Label title = new Label("LEVEL OBJECTIVES", skin, "medium_outline");
        title.setAlignment(Align.center);
        title.setBounds(x + 70f, y + h - 86f, w - 140f, 50f);
        root.addActor(title);

        Label level = new Label(info.levelName().toUpperCase(), skin);
        level.setAlignment(Align.center);
        level.setBounds(x + 80f, y + h - 115f, w - 160f, 26f);
        root.addActor(level);

        float objectiveBaseY = y + h - 205f;
        addObjectives(info.objectives(), x + 116f, objectiveBaseY, 455f);

        TextButton continueButton = new TextButton("CONTINUE", continueStyle);
        continueButton.setBounds(x + 200f, y + 22f, 250f, 60f);
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float buttonX, float buttonY) {
                hide();
                if (continueAction != null) {
                    continueAction.run();
                }
            }
        });
        root.addActor(continueButton);
    }

    public void hide() {
        root.setVisible(false);
    }

    private void addObjectives(List<String> objectives, float x, float y, float width) {
        int count = Math.max(1, objectives == null ? 0 : objectives.size());
        float spacing = count > 2 ? 44f : 52f;

        if (objectives == null || objectives.isEmpty()) {
            addObjective("Don't let the zombies reach the house.", x, y, width);
            return;
        }

        for (int i = 0; i < objectives.size(); i++) {
            addObjective(objectives.get(i), x, y - i * spacing, width);
        }
    }

    private void addObjective(String text, float x, float y, float width) {
        Image bullet = new Image(bulletTexture);
        bullet.setScaling(Scaling.fit);
        bullet.setBounds(x, y + 6f, 22f, 22f);
        root.addActor(bullet);

        Label line = new Label(text, skin);
        line.setWrap(true);
        line.setAlignment(Align.left);
        line.setColor(0.20f, 0.16f, 0.08f, 1f);
        line.setBounds(x + 34f, y, width - 34f, 40f);
        root.addActor(line);
    }

    private TextButton.TextButtonStyle styledButton(
            String baseName, Texture up, Texture down
    ) {
        TextButton.TextButtonStyle style =
                new TextButton.TextButtonStyle(skin.get(baseName, TextButton.TextButtonStyle.class));
        style.up = new TextureRegionDrawable(new TextureRegion(up));
        style.over = new TextureRegionDrawable(new TextureRegion(up));
        style.down = new TextureRegionDrawable(new TextureRegion(down));
        return style;
    }
}
