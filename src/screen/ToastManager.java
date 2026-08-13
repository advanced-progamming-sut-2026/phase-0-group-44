package screen;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

/** Shows short-lived error/info notifications on top of any screen's stage. */
public final class ToastManager {

    private static final float VISIBLE_SECONDS = 2.5f;
    private static final float FADE_SECONDS = 0.4f;

    private final Stage stage;
    private final Skin skin;

    public ToastManager(Stage stage, Skin skin) {
        this.stage = stage;
        this.skin = skin;
    }

    public void showError(String message) {
        // نکته: پالت رنگیِ pvz2_skin.json فقط "Red" (حرف اول بزرگ) را
        // تعریف کرده، نه "red". چون Skin.getColor() نسبت به حروف بزرگ و
        // کوچک حساس است، "red" یک GdxRuntimeException می‌داد.
        show(message, "Red");
    }

    public void showInfo(String message) {
        show(message, "white");
    }

    private void show(String message, String colorName) {
        Label label = new Label(message, skin, "medium_outline");
        label.setColor(skin.getColor(colorName));
        label.setAlignment(Align.center);
        label.setWrap(true);
        label.setWidth(Math.min(600f, stage.getWidth() - 80f));
        label.setPosition(
                (stage.getWidth() - label.getWidth()) / 2f,
                stage.getHeight() - 90f
        );
        label.getColor().a = 0f;
        stage.addActor(label);

        label.addAction(Actions.sequence(
                Actions.fadeIn(FADE_SECONDS),
                Actions.delay(VISIBLE_SECONDS),
                Actions.fadeOut(FADE_SECONDS),
                Actions.removeActor()
        ));
    }
}