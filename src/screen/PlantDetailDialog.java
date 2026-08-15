package screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import model.inGame.plant.PlantCollectionView;

import java.util.function.Consumer;

/** Detail popup for a single owned plant. Styled to match NewsDialog. */
final class PlantDetailDialog extends Dialog {

    private static final float CONTENT_WIDTH = 420f;

    private final Skin skin;
    private final PlantCollectionView view;
    private final Consumer<PlantCollectionView> onUpgrade;

    PlantDetailDialog(Skin skin, PlantCollectionView view, Consumer<PlantCollectionView> onUpgrade) {
        super(view.getDefinition().getName(), skin);
        this.skin = skin;
        this.view = view;
        this.onUpgrade = onUpgrade;
        configureTitleBar();
        buildContent();
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

    private void buildContent() {
        Table panel = new Table();
        panel.top().left();
        panel.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        panel.pad(20f);

        Label info = new Label(view.getDisplayText(), skin, "medium_outline");
        info.setWrap(true);
        panel.add(info).width(CONTENT_WIDTH).padBottom(16f).row();

        if (view.getCard().canUpgrade()) {
            TextButton upgradeButton = new TextButton(
                    "Upgrade (" + view.getCard().getUpgradeCoinCost() + " coins, "
                            + view.getCard().getUpgradeSeedPacketCost() + " seed packets)",
                    skin, "purple");
            upgradeButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    onUpgrade.accept(view);
                    hide();
                }
            });
            panel.add(upgradeButton).width(CONTENT_WIDTH);
        } else {
            panel.add(new Label("Maximum level reached.", skin, "medium_outline"));
        }

        getContentTable().pad(18f, 22f, 22f, 22f);
        getContentTable().add(panel);
    }
}