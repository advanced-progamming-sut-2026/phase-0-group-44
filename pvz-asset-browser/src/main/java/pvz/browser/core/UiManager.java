package pvz.browser.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import pvz.skin.PvzSkin;

public class UiManager {
    public enum SlideDirection {
        LEFT, RIGHT, UP, DOWN
    }

    public static Skin skin;
    public static SpriteBatch batch;
    private static Stage topStage;
    public static Stack modalStack, toastStack;

    private static AbstractScreen currentScreen;
    private static AbstractScreen previousScreen;
    private static SlideDirection direction;
    private static TemporalAction transition;
    private static float progress;

    public static void setCurrentScreen(AbstractScreen screen) {
        currentScreen = screen;
        screen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.input.setInputProcessor(getNewMultiplexer());
        screen.onEnter();
    }

    public static AbstractScreen getCurrentScreen() {
        return currentScreen;
    }

    private static InputMultiplexer getNewMultiplexer(){
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(topStage);
        multiplexer.addProcessor(currentScreen.uiStage);
        return multiplexer;
    }

    public static void setScreen(AbstractScreen next, SlideDirection slide, Interpolation interpolation, float duration) {
        previousScreen = currentScreen;

        if (currentScreen == null || duration <= 0f) {
            setCurrentScreen(next);
            return;
        }
        setCurrentScreen(next);

        direction = slide;
        progress = 0f;
        transition = new TemporalAction(duration, interpolation) {
            @Override
            protected void update(float percent) {
                progress = percent;
            }
        };
    }

    public static void render(float delta) {
        if (transition == null) {
            if (currentScreen != null) {
                topStage.act();
                
                currentScreen.act(delta);
                currentScreen.render(delta);
                topStage.draw();
            }
            return;
        }

        boolean done = transition.act(delta);
        float sx = direction == SlideDirection.RIGHT ? 1f : direction == SlideDirection.LEFT ? -1f : 0f;
        float sy = direction == SlideDirection.UP ? 1f : direction == SlideDirection.DOWN ? -1f : 0f;

        previousScreen.act(delta);
        currentScreen.act(delta);
        previousScreen.drawOffset(-progress * sx, -progress * sy);
        currentScreen.drawOffset((1f - progress) * sx, (1f - progress) * sy);

        topStage.act();
        topStage.draw();

        if (done) {
            transition = null;
            previousScreen = null;
            Gdx.input.setInputProcessor(getNewMultiplexer());
        }
    }

    public static void resize(int width, int height) {
        if (currentScreen != null) {
            currentScreen.resize(width, height);
        }
        if (previousScreen != null) {
            previousScreen.resize(width, height);
        }
        topStage.getViewport().update(width, height, true);
    }

    public static void init() {
        skin = PvzSkin.get();
        batch = new SpriteBatch();

        topStage = new Stage(new ScreenViewport(), batch);

        Stack topStack = new Stack();
        topStack.setFillParent(true);

        modalStack = new Stack();
        toastStack = new Stack();

        topStack.add(modalStack);
        topStack.add(toastStack);

        topStage.addActor(topStack);
    }
}
