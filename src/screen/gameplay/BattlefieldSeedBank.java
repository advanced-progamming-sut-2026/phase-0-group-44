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
import java.util.function.ToDoubleFunction;


public final class BattlefieldSeedBank {
    private static final float CARD_WIDTH = 78f;
    private static final float CARD_HEIGHT = 70f;
    private static final float TOP_Y = 650f;
    private static final float LEFT_X = 14f;
    private static final float ROW_GAP = 8f;
    private static final float DRAG_THRESHOLD = 6f;


    public interface SeedDragHandler {

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
    private final Map<PlantType, Image> lockedTintByType = new LinkedHashMap<>();
    private final Map<PlantType, Label> cooldownLabelsByType = new LinkedHashMap<>();
    private final Map<PlantType, Label> boostLabelsByType = new LinkedHashMap<>();
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
        lockedTintByType.put(type, lockedTint);

        Label cooldownLabel = new Label("", skin, "medium_outline");
        cooldownLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        cooldownLabel.setColor(1f, 0.92f, 0.35f, 1f);
        cooldownLabel.setFontScale(0.66f);
        cooldownLabel.setBounds(4f, CARD_HEIGHT * 0.36f, CARD_WIDTH - 8f, 24f);
        cooldownLabel.setVisible(false);
        cooldownLabel.setTouchable(Touchable.disabled);
        stack.add(cooldownLabel);
        cooldownLabelsByType.put(type, cooldownLabel);

        Label boostLabel = new Label("BOOST", skin, "medium_outline");
        boostLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        boostLabel.setColor(0.48f, 1f, 0.38f, 1f);
        boostLabel.setFontScale(0.48f);
        boostLabel.setBounds(CARD_WIDTH - 42f, CARD_HEIGHT - 18f, 39f, 16f);
        boostLabel.setVisible(false);
        boostLabel.setTouchable(Touchable.disabled);
        stack.add(boostLabel);
        boostLabelsByType.put(type, boostLabel);

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
                    if (dragging) {
                        stack.setColor(1f, 1f, 1f, 0.5f);
                    } else {
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

    public void sync(
            int sun,
            Predicate<PlantType> onCooldown,
            ToDoubleFunction<PlantType> cooldownRemaining,
            Predicate<PlantType> boosted
    ) {
        for (Map.Entry<PlantType, Stack> entry : cardsByType.entrySet()) {
            PlantType type = entry.getKey();
            boolean affordable = sun >= costByType.get(type);
            boolean cooling = onCooldown.test(type);

            Image lockedTint = lockedTintByType.get(type);
            if (lockedTint != null) {
                lockedTint.setVisible(!affordable || cooling);
            }

            Label cooldown = cooldownLabelsByType.get(type);
            if (cooldown != null) {
                double remaining = Math.max(0.0, cooldownRemaining.applyAsDouble(type));
                cooldown.setVisible(cooling && remaining > 0.0);
                if (cooling && remaining > 0.0) {
                    cooldown.setText(String.format(java.util.Locale.ROOT, "%.1fs", remaining));
                }
            }

            Label boost = boostLabelsByType.get(type);
            if (boost != null) {
                boost.setVisible(boosted.test(type));
            }
        }
    }
}