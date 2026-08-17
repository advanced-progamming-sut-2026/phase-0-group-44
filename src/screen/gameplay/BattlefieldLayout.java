package screen.gameplay;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Shared board-to-screen coordinate contract for environment, plants and zombies. */
public final class BattlefieldLayout {
    public static final int ROWS = 5;
    public static final int COLUMNS = 9;

    private final BattlefieldTheme theme;
    private final float viewportWidth;
    private final float viewportHeight;
    private final Rectangle boardBounds;
    private final float cellWidth;
    private final float cellHeight;

    public BattlefieldLayout(BattlefieldTheme theme, float viewportWidth, float viewportHeight) {
        this.theme = theme;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;

        float left = sourceX(theme.boardLeft());
        float right = sourceX(theme.boardRight());
        float top = sourceY(theme.boardTop());
        float bottom = sourceY(theme.boardBottom());
        boardBounds = new Rectangle(left, bottom, right - left, top - bottom);
        cellWidth = boardBounds.width / COLUMNS;
        cellHeight = boardBounds.height / ROWS;
    }

    public BattlefieldTheme theme() {
        return theme;
    }

    public Rectangle boardBounds() {
        return new Rectangle(boardBounds);
    }

    public float cellWidth() {
        return cellWidth;
    }

    public float cellHeight() {
        return cellHeight;
    }

    /** Model row 0 is rendered as the top lawn row. */
    public Rectangle cellBounds(int row, int column) {
        if (row < 0 || row >= ROWS || column < 0 || column >= COLUMNS) {
            return new Rectangle();
        }
        float x = boardBounds.x + column * cellWidth;
        float y = boardBounds.y + (ROWS - 1 - row) * cellHeight;
        return new Rectangle(x, y, cellWidth, cellHeight);
    }

    public Vector2 cellCenter(int row, int column) {
        Rectangle bounds = cellBounds(row, column);
        return new Vector2(bounds.x + bounds.width * 0.5f,
                bounds.y + bounds.height * 0.5f);
    }

    public float rowCenterY(int row) {
        return cellCenter(row, 0).y;
    }

    public float columnCenterX(int column) {
        return cellCenter(0, column).x;
    }

    /**
     * Mower slot just to the left of the first board column. The width is based
     * on a cell so every chapter remains aligned after calibration tweaks.
     */
    public Rectangle mowerBounds(int row) {
        float width = cellWidth * 0.92f;
        float height = cellHeight * 0.98f;
        float centerX = boardBounds.x - cellWidth * 0.62f;
        float centerY = rowCenterY(row);
        return new Rectangle(centerX - width * 0.5f, centerY - height * 0.5f, width, height);
    }

    /** Returns {row,column}, or {-1,-1} outside the playable 5x9 board. */
    public int[] screenToCell(float x, float y) {
        if (!boardBounds.contains(x, y)) {
            return new int[] {-1, -1};
        }
        int column = Math.min(COLUMNS - 1,
                Math.max(0, (int) ((x - boardBounds.x) / cellWidth)));
        int rowFromBottom = Math.min(ROWS - 1,
                Math.max(0, (int) ((y - boardBounds.y) / cellHeight)));
        int row = ROWS - 1 - rowFromBottom;
        return new int[] {row, column};
    }

    public float sourceX(float sourceX) {
        return sourceX / theme.sourceWidth() * viewportWidth;
    }

    /** Converts a top-left source-image y coordinate into Scene2D bottom-left y. */
    public float sourceY(float sourceYFromTop) {
        return viewportHeight - sourceYFromTop / theme.sourceHeight() * viewportHeight;
    }
}
