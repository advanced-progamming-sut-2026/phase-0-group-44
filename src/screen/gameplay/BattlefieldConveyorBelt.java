package screen.gameplay;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import model.enums.PlantType;
import model.sim.adventure.AdventureRuntimeState;

import java.util.Map;
import java.util.function.Function;

/**
 * Phase-2 view for Adventure conveyor-belt levels.
 *
 * <p>The model stores packets as counts per plant type.  This actor mirrors
 * those counts as draggable seed packets.  Planting still goes through the
 * existing {@link controller.BoardController}; the belt never changes model
 * state directly.</p>
 */
public final class BattlefieldConveyorBelt {
    private static final float ROOT_X = 12f;
    private static final float ROOT_Y = 312f;
    private static final float ROOT_WIDTH = 94f;
    private static final float ROOT_HEIGHT = 338f;
    private static final float CARD_WIDTH = 76f;
    private static final float CARD_HEIGHT = 58f;
    private static final float DRAG_THRESHOLD = 5f;

    private final Texture whiteTexture;
    private final Texture cardTexture;
    private final Skin skin;
    private final Function<PlantType, Texture> iconLoader;
    private final BattlefieldSeedBank.SeedDragHandler dragHandler;

    private final Group root = new Group();
    private final Group packetLayer = new Group();
    private final Label waitingLabel;
    private String renderedSignature = "";

    public BattlefieldConveyorBelt(
            Texture whiteTexture,
            Texture cardTexture,
            Skin skin,
            Function<PlantType, Texture> iconLoader,
            BattlefieldSeedBank.SeedDragHandler dragHandler
    ) {
        this.whiteTexture = whiteTexture;
        this.cardTexture = cardTexture;
        this.skin = skin;
        this.iconLoader = iconLoader;
        this.dragHandler = dragHandler;

        root.setBounds(ROOT_X, ROOT_Y, ROOT_WIDTH, ROOT_HEIGHT);
        root.setTouchable(Touchable.childrenOnly);

        Image shadow = solid(new Color(0f, 0f, 0f, 0.50f));
        shadow.setBounds(4f, -4f, ROOT_WIDTH, ROOT_HEIGHT);
        root.addActor(shadow);

        Image body = solid(new Color(0.10f, 0.12f, 0.08f, 0.96f));
        body.setBounds(0f, 0f, ROOT_WIDTH, ROOT_HEIGHT);
        root.addActor(body);

        Image railLeft = solid(new Color(0.67f, 0.55f, 0.24f, 1f));
        railLeft.setBounds(3f, 0f, 4f, ROOT_HEIGHT);
        root.addActor(railLeft);
        Image railRight = solid(new Color(0.67f, 0.55f, 0.24f, 1f));
        railRight.setBounds(ROOT_WIDTH - 7f, 0f, 4f, ROOT_HEIGHT);
        root.addActor(railRight);

        Label title = new Label("CONVEYOR", skin, "medium_outline");
        title.setAlignment(Align.center);
        title.setFontScale(0.72f);
        title.setColor(1f, 0.90f, 0.48f, 1f);
        title.setBounds(7f, ROOT_HEIGHT - 35f, ROOT_WIDTH - 14f, 28f);
        title.setTouchable(Touchable.disabled);
        root.addActor(title);

        packetLayer.setBounds(8f, 20f, ROOT_WIDTH - 16f, ROOT_HEIGHT - 60f);
        packetLayer.setTouchable(Touchable.childrenOnly);
        root.addActor(packetLayer);

        waitingLabel = new Label("WAITING\nFOR PLANTS...", skin);
        waitingLabel.setAlignment(Align.center);
        waitingLabel.setColor(0.78f, 0.80f, 0.70f, 1f);
        waitingLabel.setBounds(7f, 115f, ROOT_WIDTH - 14f, 70f);
        waitingLabel.setTouchable(Touchable.disabled);
        root.addActor(waitingLabel);
    }

    public Group actor() {
        return root;
    }

    /** Rebuilds only when the model packet counts change. */
    public void sync(AdventureRuntimeState state) {
        String signature = state == null ? "none" : state.getConveyorPackets().toString();
        if (signature.equals(renderedSignature)) {
            return;
        }
        renderedSignature = signature;
        packetLayer.clearChildren();

        if (state == null || state.getConveyorPackets().isEmpty()) {
            waitingLabel.setVisible(true);
            return;
        }
        waitingLabel.setVisible(false);

        float y = packetLayer.getHeight() - CARD_HEIGHT;
        for (Map.Entry<PlantType, Integer> entry : state.getConveyorPackets().entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            Stack card = buildPacket(entry.getKey(), entry.getValue());
            card.setBounds(1f, y, CARD_WIDTH, CARD_HEIGHT);
            packetLayer.addActor(card);
            y -= CARD_HEIGHT + 7f;
        }
    }

    private Stack buildPacket(PlantType type, int count) {
        Stack stack = new Stack();
        stack.setTouchable(Touchable.enabled);

        Image background = new Image(cardTexture);
        background.setTouchable(Touchable.disabled);
        stack.add(background);

        Table content = new Table();
        content.setTouchable(Touchable.disabled);

        Image icon = new Image(iconLoader.apply(type));
        icon.setScaling(Scaling.fit);
        icon.setTouchable(Touchable.disabled);
        content.add(icon).size(52f, 42f).padTop(2f);
        stack.add(content);

        Group badgeLayer = new Group();
        badgeLayer.setTouchable(Touchable.disabled);
        Label amount = new Label("x" + count, skin, "medium_outline");
        amount.setAlignment(Align.center);
        amount.setFontScale(0.72f);
        amount.setColor(1f, 0.94f, 0.48f, 1f);
        amount.setBounds(CARD_WIDTH - 32f, 2f, 29f, 22f);
        amount.setTouchable(Touchable.disabled);
        badgeLayer.addActor(amount);
        stack.add(badgeLayer);

        stack.addListener(new InputListener() {
            private int activePointer = -1;
            private float pressX;
            private float pressY;
            private boolean dragging;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button != com.badlogic.gdx.Input.Buttons.LEFT || activePointer != -1) {
                    return false;
                }
                activePointer = pointer;
                pressX = x;
                pressY = y;
                dragging = false;
                event.stop();
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (pointer != activePointer) {
                    return;
                }
                if (!dragging) {
                    float dx = x - pressX;
                    float dy = y - pressY;
                    if (Math.abs(dx) < DRAG_THRESHOLD && Math.abs(dy) < DRAG_THRESHOLD) {
                        return;
                    }
                    dragging = dragHandler.onDragStart(type);
                    if (!dragging) {
                        activePointer = -1;
                        return;
                    }
                    stack.setColor(1f, 1f, 1f, 0.52f);
                }
                dragHandler.onDragMove(type, event.getStageX(), event.getStageY());
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (pointer != activePointer) {
                    return;
                }
                activePointer = -1;
                boolean wasDragging = dragging;
                dragging = false;
                stack.setColor(Color.WHITE);
                if (wasDragging) {
                    dragHandler.onDragEnd(type, event.getStageX(), event.getStageY());
                }
            }
        });

        return stack;
    }

    private Image solid(Color color) {
        Image image = new Image(whiteTexture);
        image.setColor(color);
        image.setTouchable(Touchable.disabled);
        return image;
    }
}
