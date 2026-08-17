package screen.gameplay;

import com.badlogic.gdx.graphics.Color;
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
import model.inGame.GameOutcome;

import java.util.List;

/**
 * Presentation-only pause and result flow.
 *
 * Pause layout follows the Phase-2 reference: mission/objectives on the panel,
 * then SAVE & EXIT / RESTART / RESUME along the bottom.
 */
public final class BattlefieldPauseOutcomeLayer {
    private static final float WIDTH = 1280f;
    private static final float HEIGHT = 720f;

    private final Texture whiteTexture;
    private final Texture pauseBoardTexture;
    private final Texture victoryTexture;
    private final Texture defeatTexture;
    private final Texture bulletTexture;
    private final Skin skin;
    private final boolean previewMode;
    private final BattlefieldMissionObjectives.MissionInfo missionInfo;
    private final Runnable resumeAction;
    private final Runnable restartAction;
    private final Runnable saveExitAction;
    private final Runnable exitAction;

    private final TextButton.TextButtonStyle brownStyle;
    private final TextButton.TextButtonStyle purpleStyle;

    private final Group root = new Group();
    private final Group pauseGroup = new Group();
    private final Group outcomeGroup = new Group();

    private final Label outcomeTitle;
    private final Label outcomeSubtitle;
    private final Image outcomeIcon;
    private final TextButton retryButton;
    private final TextButton continueButton;

    public BattlefieldPauseOutcomeLayer(
            Texture whiteTexture,
            Texture pauseBoardTexture,
            Texture victoryTexture,
            Texture defeatTexture,
            Texture bulletTexture,
            Texture brownButton,
            Texture brownButtonDown,
            Texture purpleButton,
            Texture purpleButtonDown,
            Skin skin,
            boolean previewMode,
            BattlefieldMissionObjectives.MissionInfo missionInfo,
            Runnable resumeAction,
            Runnable restartAction,
            Runnable saveExitAction,
            Runnable exitAction
    ) {
        this.whiteTexture = whiteTexture;
        this.pauseBoardTexture = pauseBoardTexture;
        this.victoryTexture = victoryTexture;
        this.defeatTexture = defeatTexture;
        this.bulletTexture = bulletTexture;
        this.skin = skin;
        this.previewMode = previewMode;
        this.missionInfo = missionInfo;
        this.resumeAction = resumeAction;
        this.restartAction = restartAction;
        this.saveExitAction = saveExitAction;
        this.exitAction = exitAction;

        brownStyle = styledButton("purple", brownButton, brownButtonDown);
        purpleStyle = styledButton("purple", purpleButton, purpleButtonDown);

        root.setSize(WIDTH, HEIGHT);
        pauseGroup.setSize(WIDTH, HEIGHT);
        outcomeGroup.setSize(WIDTH, HEIGHT);

        buildPause();
        OutcomeWidgets widgets = buildOutcome();
        outcomeTitle = widgets.title();
        outcomeSubtitle = widgets.subtitle();
        outcomeIcon = widgets.icon();
        retryButton = widgets.retry();
        continueButton = widgets.continueButton();

        pauseGroup.setVisible(false);
        outcomeGroup.setVisible(false);
        root.addActor(pauseGroup);
        root.addActor(outcomeGroup);
    }

    public Group root() {
        return root;
    }

    public void setPaused(boolean paused) {
        if (outcomeGroup.isVisible()) {
            pauseGroup.setVisible(false);
            return;
        }
        pauseGroup.setVisible(paused);
    }

    public void showOutcome(GameOutcome outcome, String levelName) {
        if (outcome == null || outcome == GameOutcome.RUNNING) {
            return;
        }

        pauseGroup.setVisible(false);
        outcomeGroup.setVisible(true);

        boolean won = outcome == GameOutcome.WON;
        outcomeTitle.setText(won ? "LEVEL COMPLETE!" : "LEVEL FAILED");
        outcomeTitle.setColor(won
                ? new Color(1f, 0.91f, 0.22f, 1f)
                : new Color(1f, 0.44f, 0.34f, 1f));

        String cleanedLevel = levelName == null || levelName.isBlank()
                ? "ADVENTURE LEVEL" : levelName.toUpperCase();
        outcomeSubtitle.setText(won
                ? cleanedLevel + "\nPROGRESS SAVED"
                : cleanedLevel + "\nTRY A DIFFERENT SETUP");

        Texture texture = won ? victoryTexture : defeatTexture;
        outcomeIcon.setDrawable(new TextureRegionDrawable(new TextureRegion(texture)));
        outcomeIcon.setColor(won
                ? Color.WHITE
                : new Color(1f, 0.82f, 0.82f, 1f));

        retryButton.setText(won ? "REPLAY" : "TRY AGAIN");
        continueButton.setText(previewMode
                ? "BACK TO PREVIEW"
                : (won ? "CONTINUE" : "EXIT"));
    }

    private void buildPause() {
        pauseGroup.addActor(shade(0.64f));

        final float x = 315f;
        final float y = 128f;
        final float w = 650f;
        final float h = 440f;

        Image board = new Image(pauseBoardTexture);
        board.setScaling(Scaling.stretch);
        board.setBounds(x, y, w, h);
        pauseGroup.addActor(board);

        Label title = new Label("GAME PAUSED", skin, "medium_outline");
        title.setAlignment(Align.center);
        title.setBounds(x + 130f, y + 389f, w - 260f, 46f);
        pauseGroup.addActor(title);

        Label level = new Label(missionInfo.levelName().toUpperCase(), skin);
        level.setAlignment(Align.center);
        level.setColor(0.31f, 0.22f, 0.09f, 1f);
        level.setBounds(x + 100f, y + 286f, w - 200f, 26f);
        pauseGroup.addActor(level);

        addObjectives(missionInfo.objectives(), x + 115f, y + 208f, 460f);

        TextButton saveExit = new TextButton(
                previewMode ? "EXIT PREVIEW" : "SAVE & EXIT", brownStyle);
        saveExit.setBounds(x + 32f, y + 38f, 185f, 58f);
        saveExit.addListener(click(saveExitAction));
        pauseGroup.addActor(saveExit);

        TextButton restart = new TextButton(
                previewMode ? "RESET PREVIEW" : "RESTART", brownStyle);
        restart.setBounds(x + 232f, y + 38f, 185f, 58f);
        restart.addListener(click(restartAction));
        pauseGroup.addActor(restart);

        TextButton resume = new TextButton("RESUME", purpleStyle);
        resume.setBounds(x + 432f, y + 38f, 185f, 58f);
        resume.addListener(click(resumeAction));
        pauseGroup.addActor(resume);
    }

    private void addObjectives(List<String> objectives, float x, float y, float width) {
        List<String> lines = objectives == null || objectives.isEmpty()
                ? List.of("Don't let the zombies reach the house.")
                : objectives;
        float spacing = lines.size() > 2 ? 44f : 54f;

        for (int i = 0; i < lines.size(); i++) {
            float lineY = y - i * spacing;
            Image bullet = new Image(bulletTexture);
            bullet.setScaling(Scaling.fit);
            bullet.setBounds(x, lineY + 5f, 22f, 22f);
            pauseGroup.addActor(bullet);

            Label line = new Label(lines.get(i), skin);
            line.setWrap(true);
            line.setAlignment(Align.left);
            line.setColor(0.20f, 0.16f, 0.08f, 1f);
            line.setBounds(x + 34f, lineY, width - 34f, 38f);
            pauseGroup.addActor(line);
        }
    }

    private OutcomeWidgets buildOutcome() {
        outcomeGroup.addActor(shade(0.68f));

        Image panel = new Image(whiteTexture);
        panel.setColor(0.035f, 0.055f, 0.04f, 0.96f);
        panel.setBounds(365f, 190f, 550f, 350f);
        outcomeGroup.addActor(panel);
        addAccentFrame(outcomeGroup, 365f, 190f, 550f, 350f,
                new Color(1f, 0.70f, 0.08f, 0.98f));

        Image icon = new Image(victoryTexture);
        icon.setScaling(Scaling.fit);
        icon.setBounds(405f, 350f, 130f, 130f);
        outcomeGroup.addActor(icon);

        Label title = new Label("LEVEL COMPLETE!", skin, "medium_outline");
        title.setAlignment(Align.left);
        title.setBounds(555f, 426f, 310f, 48f);
        outcomeGroup.addActor(title);

        Label subtitle = new Label("", skin);
        subtitle.setAlignment(Align.left);
        subtitle.setWrap(true);
        subtitle.setBounds(555f, 344f, 310f, 75f);
        outcomeGroup.addActor(subtitle);

        TextButton retry = new TextButton("REPLAY", purpleStyle);
        retry.setBounds(415f, 245f, 210f, 58f);
        retry.addListener(click(restartAction));
        outcomeGroup.addActor(retry);

        TextButton continueButton = new TextButton("CONTINUE", purpleStyle);
        continueButton.setBounds(655f, 245f, 210f, 58f);
        continueButton.addListener(click(() -> {
            if (previewMode) {
                restartAction.run();
            } else {
                exitAction.run();
            }
        }));
        outcomeGroup.addActor(continueButton);

        return new OutcomeWidgets(title, subtitle, icon, retry, continueButton);
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

    private Image shade(float alpha) {
        Image shade = new Image(whiteTexture);
        shade.setColor(0f, 0f, 0f, alpha);
        shade.setBounds(0f, 0f, WIDTH, HEIGHT);
        shade.addListener(new ClickListener());
        return shade;
    }

    private void addAccentFrame(
            Group group,
            float x, float y, float width, float height,
            Color color
    ) {
        addBar(group, x, y + height - 5f, width, 5f, color);
        addBar(group, x, y, width, 5f, color);
        addBar(group, x, y, 5f, height, color);
        addBar(group, x + width - 5f, y, 5f, height, color);
    }

    private void addBar(
            Group group,
            float x, float y, float width, float height,
            Color color
    ) {
        Image bar = new Image(whiteTexture);
        bar.setColor(color);
        bar.setBounds(x, y, width, height);
        group.addActor(bar);
    }

    private ClickListener click(Runnable action) {
        return new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (action != null) {
                    action.run();
                }
            }
        };
    }

    private record OutcomeWidgets(
            Label title,
            Label subtitle,
            Image icon,
            TextButton retry,
            TextButton continueButton
    ) { }
}
