package screen.gameplay;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
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

        root.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("========== SEED BANK ROOT CLICKED ==========");
            }
        });
        root.top().left();
        float height = chosenDefinitions.size() * (CARD_HEIGHT + ROW_GAP);
        root.setBounds(LEFT_X, TOP_Y - height, CARD_WIDTH, height);

        System.out.println(
                "SEED BANK CREATED: x=" + root.getX()
                        + " y=" + root.getY()
                        + " w=" + root.getWidth()
                        + " h=" + root.getHeight()
                        + " children=" + chosenDefinitions.size()
        );

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
        stack.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);

        Image background = new Image(cardBg);
        background.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        stack.add(background);

        Table content = new Table();
        content.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);

        Image icon = new Image(iconLoader.apply(type));
        icon.setScaling(Scaling.fit);
        icon.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);

        content.add(icon)
                .size(CARD_WIDTH - 20f)
                .padTop(6f)
                .row();

        Label costLabel =
                new Label(String.valueOf(definition.getCost()), skin, "medium_outline");
        costLabel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);

        content.add(costLabel)
                .padTop(2f);

        stack.add(content);

        Image lockedTint = new Image(lockedTintTexture);
        lockedTint.setVisible(false);
        lockedTint.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        stack.add(lockedTint);

        DragListener dragListener = new DragListener() {
            private boolean active;

            @Override
            public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
            ) {
                if (button != com.badlogic.gdx.Input.Buttons.LEFT) {
                    return false;
                }

                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void dragStart(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer
            ) {

                active = dragHandler.onDragStart(type);

                if (active) {
                    stack.setColor(1f, 1f, 1f, 0.5f);
                }
            }

            @Override
            public void drag(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer
            ) {
                if (!active) {
                    return;
                }

                dragHandler.onDragMove(
                        type,
                        event.getStageX(),
                        event.getStageY()
                );
            }

            @Override
            public void dragStop(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer
            ) {
                if (!active) {
                    return;
                }

                stack.setColor(1f, 1f, 1f, 1f);

                dragHandler.onDragEnd(
                        type,
                        event.getStageX(),
                        event.getStageY()
                );

                active = false;
            }

            @Override
            public void cancel() {
                active = false;
                stack.setColor(1f, 1f, 1f, 1f);
            }
        };

        dragListener.setTapSquareSize(DRAG_THRESHOLD);
        stack.addListener(dragListener);

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