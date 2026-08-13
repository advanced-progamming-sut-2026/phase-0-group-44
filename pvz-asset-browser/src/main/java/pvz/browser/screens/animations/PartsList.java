package pvz.browser.screens.animations;

import pvz.browser.widgets.Toast;
import pvz.browser.widgets.SplitPane;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import pvz.browser.core.UiManager;
import pvz.libpvz.pam.PamPlayer.AnimationPart;

class PartNode extends Tree.Node {
    public PartNode(AnimationPart part) {
        super(new Table());
        Table content = (Table) getActor();
        content.pad(5);
        content.setTouchable(Touchable.enabled);
        
        Table internal = new Table(UiManager.skin);
        internal.setBackground("image_ui_mainmenu_text_entry_field_10");

        content.add(internal).growX();

        Label label = new Label(part.resource ? "image" : part.name, UiManager.skin, "medium");
        label.setColor(Color.BLACK);
        label.setAlignment(Align.left);
        label.setEllipsis(true);
        
        internal.add(label).minWidth(0).growX();
        internal.pad(7);
        internal.setTouchable(Touchable.disabled);
        internal.setTransform(true);

        content.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer != -1)
                    return;

                internal.setOrigin(Align.center);
                internal.addAction(Actions.parallel(
                        Actions.alpha(0.5f, 0.5f, Interpolation.swingOut)));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer != -1)
                    return;

                internal.setOrigin(Align.center);
                internal.addAction(Actions.parallel(
                        Actions.alpha(1, 0.5f, Interpolation.smooth)));
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                internal.addAction(Actions.parallel(
                        Actions.alpha(0.2f, 0.2f, Interpolation.smooth)));
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                internal.addAction(Actions.parallel(
                        Actions.alpha(0.5f, 0.2f, Interpolation.smooth)));
            }
        });
    }
}

public class PartsList extends Table {
    private Tree tree;

    private void addPartRecursive(Tree.Node parentNode, AnimationPart part) {
        if (part == null)
            return;

        PartNode node = new PartNode(part);

        node.getActor().addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.getClipboard().setContents(part.name);
                new Toast("copied " + part.name + " to clipboard.");
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                onHover(part);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                onExit(part);
            }
        });

        if (parentNode != null) {
            parentNode.add(node);
        } else {
            tree.add(node);
        }

        for (AnimationPart child : part.children) {
            addPartRecursive(node, child);
        }
    }

    public PartsList(AnimationPart root) {
        super(UiManager.skin);
        setBackground("image_ui_if_bundle_reward_multiplier_bg_10");
        pad(10);

        

        tree = new Tree(UiManager.skin) {
            @Override
            public float getPrefWidth() {
                return 0; // Prevent the Tree from pushing the SplitPane wider
            }

            @Override
            public void layout() {
                super.layout();
                for (Object n : getRootNodes()) {
                    clampNodeWidth((Node) n);
                }
            }

            private void clampNodeWidth(Node n) {
                if (n.getActor() != null) {
                    float available = getWidth() - n.getActor().getX();
                    if (n.getActor().getWidth() > available) {
                        n.getActor().setWidth(available);
                    }
                }
                if (n.isExpanded()) {
                    for (Object child : n.getChildren()) {
                        clampNodeWidth((Node) child);
                    }
                }
            }
        };

        ScrollPane scroll = new ScrollPane(tree, UiManager.skin);
        add(scroll).grow();
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        if (root != null) {
            addPartRecursive(null, root);
        }
    }

    public void setRoot(AnimationPart part) {
        tree.clearChildren();
        if (part != null) {
            addPartRecursive(null, part);
        }
    }

    public void onClick(AnimationPart part) {
    }

    public void onHover(AnimationPart part) {
    }

    public void onExit(AnimationPart part) {
    }
}
