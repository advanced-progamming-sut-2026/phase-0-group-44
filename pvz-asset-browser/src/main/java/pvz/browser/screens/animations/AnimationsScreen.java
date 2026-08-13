package pvz.browser.screens.animations;

import pvz.browser.core.UiManager;
import pvz.browser.core.Screens;
import pvz.browser.core.AbstractScreen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import pvz.browser.core.BrowserContext;
import pvz.browser.core.UiManager.SlideDirection;
import pvz.browser.screens.animations.AnimationsList;
import pvz.browser.screens.animations.ControlsPanel;
import pvz.browser.screens.animations.PartsList;
import pvz.browser.utils.PamUtils;
import pvz.browser.widgets.SplitPane;
import pvz.browser.widgets.Toast;
import pvz.browser.widgets.WorldActor;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.pam.PamPlayer.AnimationPart;

public class AnimationsScreen extends AbstractScreen {
    public static class AnimationStatus {
        public String currentPam = null;
        public String currentClip = null;
        public float currentTime = 0;
        public Map<String, Boolean> visibility = new HashMap<>();

        public boolean playing = true;
    }

    private final AnimationStatus currentAnimation;

    private final PamPlayer player;

    private ControlsPanel controlsPanel;
    private WorldActor worldView;
    private AnimationsList panel;
    private PartsList partsList;

    private final Actor pamActor;
    private AnimationPart partHovered;

    private Table worldViewWrapper;

    private Label pamLabel;
    private TextButton copyPamPathButton;

    public AnimationsScreen() {
        player = BrowserContext.player;
        currentAnimation = new AnimationStatus();

        rootTable.pad(10);

        panel = new AnimationsList() {
            @Override
            public void onAnimationSelect(String pamName) {
                currentAnimation.currentPam = pamName;
                currentAnimation.currentClip = null;
                currentAnimation.currentTime = 0;
                player.loadAsync(pamName, () -> {
                    List<String> clips = player.clips(pamName);
                    currentAnimation.currentClip = clips.isEmpty() ? null : clips.get(0);
                    controlsPanel.updateClip();
                    pamLabel.setText(PamUtils.getPamNameFromPath(pamName));

                    partsList.setRoot(player.getParts(currentAnimation.currentPam));

                    copyPamPathButton.setVisible(true);
                });
            }
        };

        controlsPanel = new ControlsPanel(currentAnimation) {
            @Override
            public void onNextLabel() {
                if (currentAnimation.currentPam == null || currentAnimation.currentClip == null)
                    return;

                List<String> labels = player.clips(currentAnimation.currentPam);
                if (labels == null || labels.isEmpty()) {
                    return;
                }
                int idx = labels.indexOf(currentAnimation.currentClip);
                if (idx < 0) idx = 0;
                currentAnimation.currentClip = labels.get((idx + 1) % labels.size());
                controlsPanel.updateClip();
            }

            @Override
            public void onPrevLabel() {
                if (currentAnimation.currentPam == null || currentAnimation.currentClip == null)
                    return;

                List<String> labels = player.clips(currentAnimation.currentPam);
                if (labels == null || labels.isEmpty()) {
                    return;
                }
                int idx = labels.indexOf(currentAnimation.currentClip);
                if (idx < 0) idx = 0;
                currentAnimation.currentClip = labels.get((idx - 1 + labels.size()) % labels.size());
                controlsPanel.updateClip();
            }

            @Override
            public void onChangeTime(float newTime) {
                currentAnimation.currentTime = newTime;
            }
        };

        worldView = new WorldActor();
        pamActor = new Actor() {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                if (currentAnimation.currentPam == null)
                    return;
                if (partHovered != null) {
                    batch.setColor(1, 1, 1, 0.4f);
                }
                BrowserContext.player.draw(batch, currentAnimation.currentPam, currentAnimation.currentClip,
                        currentAnimation.currentTime, 0, 0,
                        true, currentAnimation.visibility);
                if (partHovered != null) {
                    batch.setColor(1, 1, 1, 1);
                    BrowserContext.player.drawPart(batch, currentAnimation.currentPam, currentAnimation.currentClip,
                            currentAnimation.currentTime, 0, 0,
                            partHovered.name);
                }
            }
        };

        worldView.addActor(pamActor);

        Table middle = new Table();

        middle.defaults().space(10);
        rootTable.defaults().space(10);

        worldViewWrapper = new Table(skin);
        worldViewWrapper.setBackground("image_ui_if_bundle_reward_multiplier_bg_10");
        worldViewWrapper.pad(15);

        Stack worldStack = new Stack();

        Table detailsTable = new Table();
        detailsTable.top();

        pamLabel = new Label("No animation selected", UiManager.skin, "big_outline");
        pamLabel.setAlignment(Align.topLeft);
        pamLabel.setEllipsis(true);

        copyPamPathButton = new TextButton("copy path", skin, "green_small");
        copyPamPathButton.setVisible(false);

        detailsTable.pad(10);
        detailsTable.add(pamLabel).minWidth(0).growX();
        detailsTable.add(copyPamPathButton);

        worldStack.add(worldView);
        worldStack.add(detailsTable);
        worldViewWrapper.add(worldStack).grow();

        middle.add(worldViewWrapper).grow().row();
        middle.add(controlsPanel).growX();

        Table right = new Table();
        right.top();
        partsList = new PartsList(null) {
            @Override
            public void onClick(AnimationPart part) {
                if (currentAnimation.currentPam == null)
                    return;

                currentAnimation.visibility.put(part.name, !currentAnimation.visibility.getOrDefault(part.name, true));
            }

            @Override
            public void onHover(AnimationPart part) {
                partHovered = part;
                addAction(new TemporalAction() {
                    @Override
                    protected void update(float percent) {

                    }
                });
            }

            @Override
            public void onExit(AnimationPart part) {
                partHovered = null;
            }
        };
        right.add(partsList).grow();

        Table left = new Table();
        left.defaults().space(5);
        TextButton switchToAtlases = new TextButton("atlases", skin, "green_small");

        left.add(switchToAtlases).growX().row();
        left.add(panel).grow();

        SplitPane rightSplit = new SplitPane(middle, right);
        SplitPane leftSplit = new SplitPane(left, rightSplit);

        rightSplit.setSplitAmount(0.7f);
        leftSplit.setSplitAmount(0.23f);

        rootTable.add(leftSplit).grow();

        switchToAtlases.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                UiManager.setScreen(Screens.atlases, SlideDirection.DOWN, Interpolation.smoother, 0.5f);
            }
        });

        copyPamPathButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.getClipboard().setContents(currentAnimation.currentPam);
                new Toast("copied " + currentAnimation.currentPam + " to clipboard.");
            }
        });
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (currentAnimation.currentPam == null)
            return;

        if (currentAnimation.playing)
            currentAnimation.currentTime += delta;

        if (player.clipDurationSeconds(currentAnimation.currentPam,
                currentAnimation.currentClip) < currentAnimation.currentTime) {
            currentAnimation.currentTime = 0;
        }
    }

    // @Override
    // public void onEnter() {
    //     worldViewWrapper.addAction(
    //             Actions.sequence(
    //                     Actions.moveBy(0, 1000),
    //                     // Actions.delay(1),
    //                     Actions.moveBy(0, -1000, 0.7f, Interpolation.smoother)));
    //     controlsPanel.addAction(
    //             Actions.sequence(
    //                     Actions.moveBy(0, -1000),
    //                     // Actions.delay(1.2f),
    //                     Actions.moveBy(0, 1000, 0.7f, Interpolation.smoother)));

    //     panel.addAction(
    //             Actions.sequence(
    //                     Actions.hide(),
    //                     // Actions.delay(0.8f),
    //                     Actions.moveBy(-1000, 0),
    //                     Actions.show(),
    //                     Actions.moveBy(1000, 0, 0.7f, Interpolation.smoother)));

    //     partsList.addAction(
    //             Actions.sequence(
    //                     Actions.hide(),
    //                     // Actions.delay(0.8f),
    //                     Actions.moveBy(1000, 0),
    //                     Actions.show(),
    //                     Actions.moveBy(-1000, 0, 0.7f, Interpolation.smoother)));

    // }
}
