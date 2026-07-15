package model.inGame;

import model.Position;
import model.Tile;
import model.enums.ObstacleType;
import model.enums.PlantTag;
import model.enums.PlantType;
import model.enums.TerrainType;
import model.inGame.plant.Plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameMap {
    public static final int DEFAULT_ROWS = 5;
    public static final int DEFAULT_COLUMNS = 9;

    private final int rows;
    private final int columns;
    private final Tile[][] tiles;

    public GameMap() {
        this(DEFAULT_ROWS, DEFAULT_COLUMNS);
    }

    public GameMap(int rows, int columns) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("Map dimensions must be positive.");
        }
        this.rows = rows;
        this.columns = columns;
        this.tiles = new Tile[rows][columns];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                tiles[row][column] = new Tile(new Position(row, column), TerrainType.NORMAL_EGYPT);
            }
        }
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public boolean isInside(Position position) {
        return position != null && position.getRow() >= 0 && position.getRow() < rows
                && position.getColumn() >= 0 && position.getColumn() < columns;
    }

    public Tile getTile(Position position) {
        if (!isInside(position)) {
            throw new IllegalArgumentException("Position is outside the map: " + position);
        }
        return tiles[position.getRow()][position.getColumn()];
    }

    public Tile getTile(int row, int column) {
        return getTile(new Position(row, column));
    }

    public void setTerrain(Position position, TerrainType terrain) {
        getTile(position).setTerrain(terrain);
    }

    public void setObstacle(Position position, ObstacleType obstacle) {
        getTile(position).setObstacle(obstacle);
    }

    public void clearObstacle(Position position) {
        getTile(position).setObstacle(ObstacleType.NONE);
    }

    public void placePlant(Plant plant, Position position) {
        if (plant == null) {
            throw new IllegalArgumentException("Plant is null.");
        }
        Tile tile = getTile(position);
        PlantType type = plant.getType();

        if (type == PlantType.GRAVE_BUSTER) {
            if (tile.getObstacle() != ObstacleType.GRAVE) {
                throw new IllegalStateException("Grave Buster must be planted on a grave.");
            }
            tile.placePrimary(plant);
        } else if (type == PlantType.HOT_POTATO) {
            if (tile.getObstacle() != ObstacleType.ICE) {
                throw new IllegalStateException("Hot Potato must be planted on ice.");
            }
            tile.placePrimary(plant);
        } else if (type == PlantType.LILY_PAD) {
            if (tile.getTerrain() != TerrainType.WATER || tile.getSupportPlant() != null) {
                throw new IllegalStateException("Lily Pad requires an empty water support layer.");
            }
            tile.placeSupport(plant);
        } else if (type == PlantType.PUMPKIN) {
            if (tile.getPrimaryPlant() == null || tile.getArmorPlant() != null) {
                throw new IllegalStateException("Pumpkin must cover an existing unarmored plant.");
            }
            tile.placeArmor(plant);
        } else {
            if (tile.blocksPlanting()) {
                throw new IllegalStateException("This tile is blocked by " + tile.getObstacle() + ".");
            }
            if (tile.getPrimaryPlant() != null) {
                throw new IllegalStateException("The primary plant layer is occupied.");
            }
            boolean waterPlant = plant.getEffectiveDefinition().hasTag(PlantTag.WATER);
            if (tile.getTerrain() == TerrainType.WATER && !waterPlant && !tile.hasLilyPad()) {
                throw new IllegalStateException("A Lily Pad is required on water.");
            }
            if (tile.getTerrain() != TerrainType.WATER && waterPlant) {
                throw new IllegalStateException("This plant requires water.");
            }
            tile.placePrimary(plant);
        }
        plant.setPosition(position);
    }

    public void removePlant(Plant plant) {
        if (plant == null || plant.getPosition() == null || !isInside(plant.getPosition())) {
            return;
        }
        getTile(plant.getPosition()).remove(plant);
    }

    public List<Plant> getPlants() {
        List<Plant> result = new ArrayList<>();
        for (Tile[] row : tiles) {
            for (Tile tile : row) {
                result.addAll(tile.getPlants());
            }
        }
        return Collections.unmodifiableList(result);
    }

    public Plant getPlantForZombieAttack(Position position) {
        Tile tile = getTile(position);
        if (tile.getArmorPlant() != null) {
            return tile.getArmorPlant();
        }
        if (tile.getPrimaryPlant() != null) {
            return tile.getPrimaryPlant();
        }
        return tile.getSupportPlant();
    }

    public Integer firstBlockingColumn(int row, double fromX, double toX) {
        int direction = toX >= fromX ? 1 : -1;
        int start = Math.max(0, Math.min(columns - 1, (int) Math.floor(fromX)));
        int end = Math.max(0, Math.min(columns - 1, (int) Math.floor(toX)));
        for (int column = start; direction > 0 ? column <= end : column >= end; column += direction) {
            if (tiles[row][column].blocksDirectProjectiles()) {
                return column;
            }
        }
        return null;
    }

    public List<Position> positionsInArea(Position center, int rowRadius, int columnRadius) {
        List<Position> positions = new ArrayList<>();
        for (int row = Math.max(0, center.getRow() - rowRadius);
             row <= Math.min(rows - 1, center.getRow() + rowRadius); row++) {
            for (int column = Math.max(0, center.getColumn() - columnRadius);
                 column <= Math.min(columns - 1, center.getColumn() + columnRadius); column++) {
                positions.add(new Position(row, column));
            }
        }
        return positions;
    }
}
