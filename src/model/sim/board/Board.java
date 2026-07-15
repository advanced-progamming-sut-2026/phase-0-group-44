package model.sim.board;

import model.enums.TerrainType;

/**
 * The board grid and the one place that owns coordinate conventions.
 *
 * <p>Columns are the horizontal axis (x), rows the vertical axis (y). Plant
 * coordinates are integer tiles; zombie horizontal positions may be continuous,
 * so validation distinguishes the two. Default size is 5 rows by 9 columns
 * unless a level overrides it.</p>
 */
public class Board {

    public static final int DEFAULT_ROWS = 5;
    public static final int DEFAULT_COLUMNS = 9;

    private final int rows;
    private final int columns;
    private final Tile[][] tiles;

    public Board() {
        this(DEFAULT_ROWS, DEFAULT_COLUMNS, TerrainType.NORMAL_EGYPT);
    }

    public Board(int rows, int columns, TerrainType defaultTerrain) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("Board size must be positive.");
        }

        this.rows = rows;
        this.columns = columns;
        this.tiles = new Tile[rows][columns];

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                tiles[row][column] = new Tile(row, column, defaultTerrain);
            }
        }
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    /** Whether an integer plant coordinate is on the board. */
    public boolean isValidTile(int x, int y) {
        return x >= 0 && x < columns && y >= 0 && y < rows;
    }

    /** Whether a (possibly continuous) zombie coordinate is within board bounds. */
    public boolean isValidZombitePosition(double x, int row) {
        return x >= 0 && x < columns && row >= 0 && row < rows;
    }

    public Tile tileAt(int x, int y) {
        if (!isValidTile(x, y)) {
            return null;
        }

        return tiles[y][x];
    }

    /** The 8 neighbours of a tile (existing tiles only). */
    public java.util.List<Tile> neighboursOf(int x, int y) {
        java.util.List<Tile> neighbours = new java.util.ArrayList<>();

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }

                Tile tile = tileAt(x + dx, y + dy);

                if (tile != null) {
                    neighbours.add(tile);
                }
            }
        }

        return neighbours;
    }
}
