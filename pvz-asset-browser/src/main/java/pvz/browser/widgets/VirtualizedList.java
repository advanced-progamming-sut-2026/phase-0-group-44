package pvz.browser.widgets;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

/**
 * A vertically scrolling list that only keeps the rows currently on screen mounted as actors.
 *
 * <p>Every row has the same fixed {@code rowHeight}, so the total content height is known up front
 * ({@link #getPrefHeight()}) and the visible index range is a cheap bit of arithmetic against the
 * enclosing {@link ScrollPane}'s scroll offset. Rows entering view are built via the {@link CardFactory}
 * and added; rows leaving view are removed and reported through {@link CardFactory#onHidden}. Only about
 * a viewport-worth of actors ever exist at once, so a list of thousands of rich cards stays cheap.
 *
 * <p>Must be placed <b>directly</b> inside a {@link ScrollPane} with horizontal scrolling disabled
 * ({@code scroll.setScrollingDisabled(true, false)}) — the pane then stretches this widget to the
 * viewport width and only the vertical offset matters.
 *
 * @param <T> the item type backing each row
 */
public class VirtualizedList<T> extends WidgetGroup {

    /** Builds and releases the row actor for an item. The list never touches item resources itself. */
    public interface CardFactory<T> {
        /** Create the (clickable) actor shown for {@code item} at {@code index} as it scrolls into view. */
        Actor create(T item, int index);

        /** Called when the row scrolls out of view and its actor is removed. Dispose card-level state only. */
        default void onHidden(Actor view, T item, int index) {
        }
    }

    private final Array<T> items = new Array<>();
    private final float rowHeight;
    private final CardFactory<T> factory;
    private final int bufferRows;

    /** Index -> the actor currently mounted for that row. */
    private final IntMap<Actor> mounted = new IntMap<>();

    public VirtualizedList(float rowHeight, CardFactory<T> factory) {
        this(rowHeight, factory, 2);
    }

    public VirtualizedList(float rowHeight, CardFactory<T> factory, int bufferRows) {
        this.rowHeight = rowHeight;
        this.factory = factory;
        this.bufferRows = Math.max(0, bufferRows);
    }

    /** Replace the backing items, unmounting every currently visible row. Resets the view to the top. */
    public void setItems(Array<T> newItems) {
        unmountAll();
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        invalidateHierarchy();
    }

    public Array<T> getItems() {
        return items;
    }

    @Override
    public float getPrefWidth() {
        return 0f; // ScrollPane stretches us to the viewport width (horizontal scrolling disabled).
    }

    @Override
    public float getPrefHeight() {
        return items.size * rowHeight;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        refreshVisible();
    }

    /** Reconcile the mounted set with the rows the ScrollPane viewport currently shows. */
    private void refreshVisible() {
        if (items.size == 0) {
            unmountAll();
            return;
        }
        if (!(getParent() instanceof ScrollPane)) {
            return; // Not scrollable on its own; nothing meaningful to compute.
        }
        ScrollPane sp = (ScrollPane) getParent();
        float scrollY = sp.getVisualScrollY();
        float viewH = sp.getScrollHeight();

        int first = MathUtils.clamp(MathUtils.floor(scrollY / rowHeight) - bufferRows, 0, items.size - 1);
        int last = MathUtils.clamp(MathUtils.ceil((scrollY + viewH) / rowHeight) + bufferRows, 0, items.size - 1);

        // Unmount rows that fell outside the visible range.
        IntMap.Keys keys = mounted.keys();
        Array<Integer> toRemove = new Array<>();
        while (keys.hasNext) {
            int i = keys.next();
            if (i < first || i > last) {
                toRemove.add(i);
            }
        }
        for (Integer i : toRemove) {
            unmount(i);
        }

        // Mount newly visible rows.
        for (int i = first; i <= last; i++) {
            if (!mounted.containsKey(i)) {
                Actor view = factory.create(items.get(i), i);
                mounted.put(i, view);
                addActor(view);
                positionRow(i, view);
            }
        }
    }

    @Override
    public void layout() {
        // Width/height may have changed (resize); reposition everything currently mounted.
        IntMap.Entries<Actor> entries = mounted.entries();
        while (entries.hasNext) {
            IntMap.Entry<Actor> e = entries.next();
            positionRow(e.key, e.value);
        }
    }

    private void positionRow(int index, Actor view) {
        view.setBounds(0f, getHeight() - (index + 1) * rowHeight, getWidth(), rowHeight);
    }

    private void unmount(int index) {
        Actor view = mounted.remove(index);
        if (view != null) {
            removeActor(view);
            T item = index >= 0 && index < items.size ? items.get(index) : null;
            factory.onHidden(view, item, index);
        }
    }

    private void unmountAll() {
        IntMap.Keys keys = mounted.keys();
        Array<Integer> all = new Array<>();
        while (keys.hasNext) {
            all.add(keys.next());
        }
        for (Integer i : all) {
            unmount(i);
        }
    }
}
