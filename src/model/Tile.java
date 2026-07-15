package model;

import model.enums.ObstacleType;
import model.enums.TerrainType;
import model.enums.PlantType;
import model.inGame.plant.Plant;

import java.util.ArrayList;
import java.util.List;

public class Tile {
    private final Position position;
    private TerrainType terrain;
    private ObstacleType obstacle = ObstacleType.NONE;
    private Plant supportPlant;
    private Plant primaryPlant;
    private Plant armorPlant;

    public Tile(Position position, TerrainType terrain) {
        this.position = position;
        this.terrain = terrain;
    }

    public Position getPosition() {
        return position;
    }

    public TerrainType getTerrain() {
        return terrain;
    }

    public void setTerrain(TerrainType terrain) {
        this.terrain = terrain;
    }

    public ObstacleType getObstacle() {
        return obstacle;
    }

    public void setObstacle(ObstacleType obstacle) {
        this.obstacle = obstacle == null ? ObstacleType.NONE : obstacle;
    }

    public boolean blocksPlanting() {
        return obstacle == ObstacleType.GRAVE || obstacle == ObstacleType.CRATER
                || obstacle == ObstacleType.PROJECTILE_BLOCKER;
    }

    public boolean blocksDirectProjectiles() {
        return obstacle == ObstacleType.GRAVE || obstacle == ObstacleType.PROJECTILE_BLOCKER;
    }

    public Plant getSupportPlant() {
        return supportPlant;
    }

    public Plant getPrimaryPlant() {
        return primaryPlant;
    }

    public Plant getArmorPlant() {
        return armorPlant;
    }

    public List<Plant> getPlants() {
        List<Plant> result = new ArrayList<>(3);
        if (supportPlant != null) {
            result.add(supportPlant);
        }
        if (primaryPlant != null) {
            result.add(primaryPlant);
        }
        if (armorPlant != null) {
            result.add(armorPlant);
        }
        return result;
    }

    public void placeSupport(Plant plant) {
        supportPlant = plant;
    }

    public void placePrimary(Plant plant) {
        primaryPlant = plant;
    }

    public void placeArmor(Plant plant) {
        armorPlant = plant;
    }

    public void remove(Plant plant) {
        if (supportPlant == plant) {
            supportPlant = null;
        }
        if (primaryPlant == plant) {
            primaryPlant = null;
        }
        if (armorPlant == plant) {
            armorPlant = null;
        }
    }

    public boolean hasLilyPad() {
        return supportPlant != null && supportPlant.getType() == PlantType.LILY_PAD;
    }
}
