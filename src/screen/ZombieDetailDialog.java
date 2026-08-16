package screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import model.inGame.zombie.ZombieDefinition;

/** Detail popup for a spotted zombie. Styled to match NewsDialog. */
final class ZombieDetailDialog extends Dialog {

    private static final float CONTENT_WIDTH = 420f;

    private final Skin skin;

    ZombieDetailDialog(Skin skin, ZombieDefinition definition) {
        super(definition.getName(), skin);
        this.skin = skin;
        configureTitleBar();
        buildContent(definition);
    }

    void showCentered(Stage stage) {
        show(stage);
        pack();
        centerOn(stage);
    }

    void centerOn(Stage stage) {
        setPosition(
                Math.round((stage.getWidth() - getWidth()) / 2f),
                Math.round((stage.getHeight() - getHeight()) / 2f)
        );
    }

    private void configureTitleBar() {
        getTitleTable().pad(10f, 18f, 8f, 14f);
        getTitleTable().add().expandX();

        ImageButton closeButton = new ImageButton(skin, "generic_close_circle");
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
            }
        });
        getTitleTable().add(closeButton).size(46f, 42f).right();
    }

    private void buildContent(ZombieDefinition definition) {
        Table panel = new Table();
        panel.top().left();
        panel.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        panel.pad(20f);

        Label info = new Label(definition.getDisplayText(), skin, "medium_outline");
        info.setWrap(true);
        panel.add(info).width(CONTENT_WIDTH);

        getContentTable().pad(18f, 22f, 22f, 22f);
        getContentTable().add(panel);
    }
}