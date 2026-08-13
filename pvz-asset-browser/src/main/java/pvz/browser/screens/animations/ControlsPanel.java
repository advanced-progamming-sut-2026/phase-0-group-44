package pvz.browser.screens.animations;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import pvz.browser.core.BrowserContext;
import pvz.browser.core.UiManager;
import pvz.browser.screens.animations.AnimationsScreen.AnimationStatus;
import pvz.libpvz.pam.PamPlayer;
import pvz.skin.BorderedTable;

public class ControlsPanel extends Table {
    private final Skin skin;
    private final PamPlayer player;
    private final AnimationStatus currentAnimation;

    public Label timeLabel;
    public Label currentClipLabel;
    public Slider progressBar;
    public ImageButton nextLabelButton;
    public ImageButton prevLabelButton;

    public ControlsPanel(AnimationStatus currentAnimation) {
        super(UiManager.skin);
        this.currentAnimation = currentAnimation;
        skin = UiManager.skin;
        player = BrowserContext.player;

        TextButton playButton = new TextButton("pause", skin, "green_small");

        setBackground("image_ui_if_bundle_reward_multiplier_bg_10");

        progressBar = new Slider(0f, 1f, 1/30f, false, skin);
        // progressBar.setProgrammaticChangeEvents(false);
        nextLabelButton = new ImageButton(skin, "next");
        prevLabelButton = new ImageButton(skin, "previous");
        timeLabel = new Label("", skin);

        currentClipLabel = new Label("", skin, "medium_outline");

        Table controlsTop = new Table();
        Table controlsBottom = new Table();
        controlsTop.defaults().space(5);
        controlsBottom.defaults().space(5);
        controlsBottom.add(playButton);

        timeLabel.setAlignment(Align.center);

        timeLabel.setColor(Color.BLACK);

        pad(10);
        controlsTop.add(timeLabel).width(50).center();
        controlsTop.add(progressBar).growX().row();
        controlsBottom.add(prevLabelButton).right();
        controlsBottom.add(currentClipLabel).expandX().center();
        controlsBottom.add(nextLabelButton).left();
        add(controlsTop).growX().row();
        add(controlsBottom).growX().row();

        nextLabelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onNextLabel();
            }
        });
        prevLabelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onPrevLabel();
            }
        });

        progressBar.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(currentAnimation.currentPam == null) return;
                currentAnimation.currentTime = progressBar.getValue();
            }
            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if(currentAnimation.currentPam == null) return;
                currentAnimation.currentTime = progressBar.getValue();
            }
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if(currentAnimation.currentPam == null) return true;
                currentAnimation.currentTime = progressBar.getValue();
                return true;
            }

        });

        playButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                currentAnimation.playing = !currentAnimation.playing;

                playButton.setText(currentAnimation.playing ? "pause" : "play");
            }
        });
    }

    public void onNextLabel() {

    }

    public void onPrevLabel() {

    }

    public void onChangeTime(float newTime) {
    }

    public void updateClip() {
        if (currentAnimation.currentPam == null)
            return;

        currentClipLabel.setText(currentAnimation.currentClip == null ? "NO_CLIP" : currentAnimation.currentClip);
        progressBar.setRange(0, player.clipDurationSeconds(currentAnimation.currentPam, currentAnimation.currentClip));
    }

    @Override
    public void act(float delta) {
        if (currentAnimation.currentPam == null) {
            super.act(delta);
            return;
        }

        timeLabel.setText(String.format("%.2f", currentAnimation.currentTime));
        progressBar.setValue(currentAnimation.currentTime);

        super.act(delta);
    }
}
