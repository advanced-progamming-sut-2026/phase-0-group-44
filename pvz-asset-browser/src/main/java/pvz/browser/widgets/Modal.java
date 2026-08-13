package pvz.browser.widgets;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import pvz.skin.BorderedTable;
import pvz.skin.PvzSkin;
import pvz.browser.core.UiManager;

public abstract class Modal extends BorderedTable{
    protected final Table innerTable;
    protected final Skin skin;
    private Stack wrapper;
    private Table background;

    public Modal(boolean closeOnClick, boolean showCloseButton) {
        super();
        setTouchable(Touchable.enabled);

        skin = PvzSkin.get();

        innerTable = new Table(skin);
        innerTable.center();
        innerTable.setTouchable(Touchable.enabled);

        background = new Table(skin);
        background.setBackground("modal_background");
        wrapper = new Stack();

        wrapper.add(background);
        wrapper.add(innerTable);

        if(showCloseButton) {
            Button closeButton = new ImageButton(skin, "generic_close_circle");
            innerTable.add(closeButton).right().padRight(20).row();

            closeButton.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    close();
                }
            });
        }
        if(closeOnClick){
            innerTable.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (event.getTarget() == innerTable) {
                        close();
                    }
                }
            });
        }
    }

    protected abstract void addToWrapper();

    public Modal(boolean closeOnClick){
        this(closeOnClick, false);
    }

    public void show() {
        addToWrapper();
        // Cell<Modal> cell = wrapper.add(this);
        UiManager.modalStack.add(wrapper);
        onShow();

        // return cell;
    }

    public void onShow() {
        addAction(Actions.sequence(
            Actions.alpha(0),
            Actions.moveBy(0, 100),
            Actions.parallel(
                Actions.moveBy(0, -100, 0.5f, Interpolation.swingOut),
                Actions.alpha(1, 0.5f, Interpolation.exp5Out)
            )
        ));

        background.addAction(Actions.sequence(
            Actions.alpha(0),
            Actions.delay(0.2f),
            Actions.alpha(1, 0.4f, Interpolation.smoother)
        ));
    }

    public void close() {
        addAction(Actions.sequence(
            Actions.delay(0.2f),
            Actions.parallel(
                Actions.moveBy(0, 100, 0.5f, Interpolation.swingIn),
                Actions.alpha(0, 0.5f, Interpolation.exp5Out)
            ),
            Actions.run(() -> {
                wrapper.remove();
                wrapper.clearChildren(true);
        
                onClose();
            })
        ));

        background.addAction(Actions.sequence(
            Actions.alpha(0, 0.4f, Interpolation.smooth)
        ));
    }

    public void onClose() {
    }
}
