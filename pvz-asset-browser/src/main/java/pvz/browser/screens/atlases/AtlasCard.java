package pvz.browser.screens.atlases;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import pvz.browser.utils.PamUtils;
import pvz.browser.core.BrowserContext;
import pvz.browser.core.UiManager;

public class AtlasCard extends Table {
    private Image image;

    public AtlasCard(String id) {
        super(UiManager.skin);
        Skin skin = UiManager.skin;

        Table internal = new Table(skin);

        internal.setBackground("image_ui_mainmenu_mm_settings_tab_10");

        String name = id;

        Label titleLabel = new Label(cleanName(name), skin, "medium");
        titleLabel.setColor(Color.BLACK);

        titleLabel.setEllipsis(true);

        Table left = new Table();
        left.add(titleLabel).growX().minWidth(0);

        image = new Image();
        image.setScaling(Scaling.fit);


        BrowserContext.textures.loadAsync(id, () -> {
        });

        internal.add(left).grow();
        internal.add(image).growY().width(60);
        internal.setTransform(true);

        add(internal).pad(2.5f, 5, 2.5f, 10).grow();

        setTouchable(Touchable.enabled);
        internal.setTouchable(Touchable.disabled);

        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClick();
            }
        });

        addListener(new ClickListener() {

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer != -1)
                    return;

                internal.setOrigin(Align.center);
                internal.addAction(Actions.parallel(
                        Actions.moveBy(10, 0, 0.5f, Interpolation.swingOut),
                        Actions.alpha(0.5f, 0.5f, Interpolation.swingOut)));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer != -1)
                    return;

                internal.setOrigin(Align.center);
                internal.addAction(Actions.parallel(
                        Actions.moveBy(-10, 0, 0.5f, Interpolation.bounceOut),
                        Actions.alpha(1, 0.5f, Interpolation.smooth)));
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                internal.addAction(Actions.parallel(
                        Actions.moveBy(15, 0, 0.2f, Interpolation.swingOut)));
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                internal.addAction(Actions.parallel(
                        Actions.moveBy(-15, 0, 0.2f, Interpolation.swingOut)));
            }
        });
    }

    public void onClick() {

    }

    private String cleanName(String id) {
        return PamUtils.prettify(id.replaceFirst("_(768|1536)_\\d+$", "").replaceFirst("DELAYLOAD_", "").replaceFirst("ATLASIMAGE_ATLAS_", ""));
    }

    public void setImage(TextureRegion region) {
        this.image.setDrawable(new TextureRegionDrawable(region));
    }
}
