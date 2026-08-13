package pvz.browser.widgets;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import pvz.browser.core.UiManager;

public class Toast extends Table {
    private boolean removed = false;
    private Table wrapper;

    public Toast(String message) {
        super();

        Label label = new Label(message, UiManager.skin, "bundle_reward_multiplier");
        add(label);
        
        pack();
        
        wrapper = new Table();
        wrapper.right().bottom().pad(50);

        setTouchable(Touchable.enabled);

        wrapper.add(this);

        UiManager.toastStack.add(wrapper);

        addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                removeToast();
            }
        });

        addAction(Actions.sequence(
            Actions.moveBy(0, -100),
            Actions.moveBy(0, 100, 0.5f, Interpolation.swingOut),
            Actions.delay(1f),
            Actions.run(() -> {
                removeToast();
            }
        )));
    }

    public void removeToast(){
        if(removed) return;
        removed = true;
        addAction(Actions.sequence(
            Actions.moveBy(0, -100, 0.5f, Interpolation.swingIn),
            Actions.run(() -> {
                wrapper.remove();
            })
        ));
    }
}
