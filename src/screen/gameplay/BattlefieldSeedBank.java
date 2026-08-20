package screen.gameplay;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.Scaling;
import model.enums.PlantType;
import model.inGame.plant.PlantDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Left-side seed bank: one draggable card per plant chosen in
 * PlantSelectionScreen, in pick order. Dropping a card onto the board is
 * what plants it -- GameplayScreen (the only class with BattlefieldLayout)
 * owns turning drag coordinates into a cell and calling BoardController.
 */
public final class BattlefieldSeedBank {
    private static final float CARD_WIDTH = 78f;
    private static final float CARD_HEIGHT = 70f;
    private static final float TOP_Y = 650f;
    private static final float LEFT_X = 14f;
    private static final float ROW_GAP = 8f;
    private static final float DRAG_THRESHOLD = 6f;

    /** GameplayScreen implements this; it alone knows how to map stage
     *  coordinates to board cells and talk to BoardController. */
    public interface SeedDragHandler {
        /** Return false to refuse the drag (not enough sun / on cooldown). */
        boolean onDragStart(PlantType type);
        void onDragMove(PlantType type, float stageX, float stageY);
        void onDragEnd(PlantType type, float stageX, float stageY);
    }

    private final Skin skin;
    private final Texture cardBg;
    private final Texture cardSelectedBg;
    private final Texture lockedTintTexture;
    private final Function<PlantType, Texture> iconLoader;
    private final Table root;
    private final Map<PlantType, Stack> cardsByType = new LinkedHashMap<>();
    private final Map<PlantType, Integer> costByType = new LinkedHashMap<>();
    private final SeedDragHandler dragHandler;

    public BattlefieldSeedBank(
            Skin skin,
            Texture cardBg,
            Texture cardSelectedBg,
            Texture lockedTintTexture,
            Function<PlantType, Texture> iconLoader,
            List<PlantDefinition> chosenDefinitions,
            SeedDragHandler dragHandler
    ) {
        this.skin = skin;
        this.cardBg = cardBg;
        this.cardSelectedBg = cardSelectedBg;
        this.lockedTintTexture = lockedTintTexture;
        this.iconLoader = iconLoader;
        this.dragHandler = dragHandler;

        root = new Table();
        root.setTouchable(Touchable.enabled);

        root.top().left();
        float height = chosenDefinitions.size() * (CARD_HEIGHT + ROW_GAP);
        root.setBounds(LEFT_X, TOP_Y - height, CARD_WIDTH, height);

        for (PlantDefinition definition : chosenDefinitions) {
            root.add(buildCard(definition)).size(CARD_WIDTH, CARD_HEIGHT).padBottom(ROW_GAP).row();
        }
    }

    public Group actor() {
        return root;
    }

    private Stack buildCard(PlantDefinition definition) {
        PlantType type = definition.getType();
        costByType.put(type, definition.getCost());

        Stack stack = new Stack();
        stack.setTouchable(Touchable.enabled);

        Image background = new Image(cardBg);
        background.setTouchable(Touchable.disabled);
        stack.add(background);

        Table content = new Table();
        content.setTouchable(Touchable.disabled);

        Image icon = new Image(iconLoader.apply(type));
        icon.setScaling(Scaling.fit);
        icon.setTouchable(Touchable.disabled);

        content.add(icon)
                .size(CARD_WIDTH - 20f)
                .padTop(6f)
                .row();

        Label costLabel =
                new Label(String.valueOf(definition.getCost()), skin, "medium_outline");
        costLabel.setTouchable(Touchable.disabled);

        content.add(costLabel)
                .padTop(2f);

        stack.add(content);

        Image lockedTint = new Image(lockedTintTexture);
        lockedTint.setVisible(false);
        lockedTint.setTouchable(Touchable.disabled);
        stack.add(lockedTint);

        // Custom drag handling instead of DragListener: DragListener keeps a
        // private `pressedPointer` field that can get stuck (never reset to -1)
        // after certain event-propagation edge cases, silently blocking every
        // future press on the same card. We track our own pointer/drag state
        // here so nothing hidden can desync from reality.
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
                // Stop the event so it doesn't also bubble into other listeners
                // (e.g. a ClickListener on an ancestor) while a drag might start.
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
                    if (dragging) {
                        stack.setColor(1f, 1f, 1f, 0.5f);
                    } else {
                        // Refused (not enough sun / on cooldown) -- release the
                        // pointer claim so a fresh press can be tried again.
                        activePointer = -1;
                    }
                }
                if (dragging) {
                    dragHandler.onDragMove(type, event.getStageX(), event.getStageY());
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (pointer != activePointer) {
                    return;
                }
                // Always release the claim first, unconditionally, so the next
                // press on this card is never blocked no matter what happens below.
                activePointer = -1;
                boolean wasDragging = dragging;
                dragging = false;
                if (wasDragging) {
                    stack.setColor(1f, 1f, 1f, 1f);
                    dragHandler.onDragEnd(type, event.getStageX(), event.getStageY());
                }
            }
        });

        cardsByType.put(type, stack);

        return stack;
    }

    /** Call once per frame with the current sun total and a cooldown check. */
    public void sync(int sun, Predicate<PlantType> onCooldown) {
        for (Map.Entry<PlantType, Stack> entry : cardsByType.entrySet()) {
            PlantType type = entry.getKey();
            boolean affordable = sun >= costByType.get(type);
            boolean cooling = onCooldown.test(type);
            Image lockedTint = (Image) entry.getValue().getChildren().peek();
            lockedTint.setVisible(!affordable || cooling);
        }
    }
}