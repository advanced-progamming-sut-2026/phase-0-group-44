package screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

/**
 * Reusable plant card: icon, an optional top-left badge (level), a bottom
 * label (cost or seed-packet progress), a locked tint, a boosted background
 * swap, and a selection highlight. Shared by CollectionScreen's plant grid
 * and PlantSelectionScreen's browse grid + sidebar slots, so all render
 * plant info identically instead of each screen re-implementing its own
 * card (per the spec's explicit reuse requirement). Intended to back the
 * in-game HUD's selected-plant tray too, once that screen exists.
 *
 * Uses two swappable background textures (normal / boosted-gold) supplied
 * by the caller, plus the skin's existing "white_pixel" drawable for the
 * selection tint overlay -- no other new art assets required.
 */
public final class PlantCardWidget extends Table {

    private final Texture normalBackground;
    private final Texture goldBackground;
    private final Image iconImage;
    private final Label topBadge;
    private final Label bottomLabel;
    private final Image selectionOverlay;
    private final Stack iconStack;

    public PlantCardWidget(Skin skin, float iconSize, Texture normalBackground, Texture goldBackground) {
        this.normalBackground = normalBackground;
        this.goldBackground = goldBackground;

        setBackground(new TextureRegionDrawable(new TextureRegion(normalBackground)));

        iconStack = new Stack();

        iconImage = new Image();
        iconImage.setScaling(Scaling.fit);
        iconStack.add(iconImage);

        selectionOverlay = new Image(skin.getDrawable("white_pixel"));
        selectionOverlay.setColor(0.3f, 1f, 0.3f, 0.35f);
        selectionOverlay.setVisible(false);
        iconStack.add(selectionOverlay);

        Table topRow = new Table();
        topRow.top().left();
        topBadge = new Label("", skin, "medium_outline");
        topRow.add(topBadge).pad(2f, 4f, 0f, 0f);
        iconStack.add(topRow);

        add(iconStack).size(iconSize).padTop(6f).row();

        bottomLabel = new Label("", skin, "medium_outline");
        add(bottomLabel).padTop(2f);
    }

    /** Exposes the icon/overlay Stack so callers can layer their own extra
     *  actors (e.g. a lock icon) on top without PlantCardWidget needing to
     *  know about every context that uses it. */
    public Stack getIconStack() {
        return iconStack;
    }

    public void setIcon(Texture icon) {
        iconImage.setDrawable(icon == null ? null : new TextureRegionDrawable(icon));
    }

    public void setLocked(boolean locked) {
        iconImage.setColor(locked ? new Color(0.45f, 0.45f, 0.45f, 1f) : Color.WHITE);
    }

    public void setBoosted(boolean boosted) {
        setBackground(new TextureRegionDrawable(new TextureRegion(boosted ? goldBackground : normalBackground)));
    }

    public void setSelected(boolean selected) {
        selectionOverlay.setVisible(selected);
    }

    public void setTopBadge(String text) {
        topBadge.setText(text == null ? "" : text);
    }

    public void setBottomLabel(String text) {
        bottomLabel.setText(text == null ? "" : text);
    }
}